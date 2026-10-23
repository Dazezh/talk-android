/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.stickers.data.StickerRepository
import com.nextcloud.talk.stickers.data.StickerUsageRepository
import com.nextcloud.talk.stickers.data.model.Sticker
import com.nextcloud.talk.stickers.data.model.StickerLibrary
import com.nextcloud.talk.stickers.data.model.StickerPack
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the sticker tabs of the emoji picker. The state always reflects what is available locally first;
 * the server is only asked for a revision when the picker is opened, when the capabilities were checked or
 * when a pack is opened for the first time.
 */
class StickersViewModel @Inject constructor(
    private val repository: StickerRepository,
    private val usageRepository: StickerUsageRepository,
    private val logger: Logger
) : ViewModel() {

    data class StickerPickerState(
        val tabs: List<StickerTab> = listOf(StickerTab.Emoji),
        val selectedTab: StickerTab = StickerTab.Emoji,
        val stickers: List<Sticker> = emptyList(),
        val nextCursor: Int? = null,
        val isLoadingStickers: Boolean = false,
        val credentials: String = "",
        val isCachingInProgress: Boolean = false,
        val favoriteIds: Set<String> = emptySet()
    )

    private val _state = MutableStateFlow(StickerPickerState())
    val state: StateFlow<StickerPickerState> = _state.asStateFlow()

    /** Emitted whenever the pack list was fetched again and something about it changed. */
    private val _libraryCached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val libraryCached: SharedFlow<Unit> = _libraryCached.asSharedFlow()

    private var user: User? = null
    private var revision: String? = null
    private var initialized = false
    private var lastRevisionCheckAt = 0L
    private var packJob: Job? = null
    private var packs: List<StickerPack> = emptyList()
    private var favorites: List<Sticker> = emptyList()
    private var recent: List<Sticker> = emptyList()

    fun initialize(user: User) {
        if (initialized) {
            checkForUpdates(force = false)
            return
        }
        initialized = true
        this.user = user
        revision = null
        _state.value = StickerPickerState(credentials = user.getCredentials())
        viewModelScope.launch {
            favorites = usageRepository.favorites(user)
            recent = usageRepository.recent(user)
            refreshTabs()
            val cachedLibrary = repository.getCachedLibrary(user)
            if (cachedLibrary == null) {
                // the very first request fills the cache and can take a while, so the wait is reported
                _state.update { it.copy(isCachingInProgress = true) }
            } else {
                applyLibrary(user, cachedLibrary)
            }
            checkForUpdates(force = false)
        }
    }

    /** Lets the caching request finish in the background and closes the dialog that reports it. */
    fun cacheInBackground() {
        _state.update { it.copy(isCachingInProgress = false) }
    }

    /**
     * Fetches and caches the pack list again on user request, ignoring the cached copy, and reports the wait
     * in the caching dialog. Unlike the automatic checks this runs even when the revision did not change.
     */
    fun forceRefresh() {
        val currentUser = user ?: return
        lastRevisionCheckAt = System.currentTimeMillis()
        _state.update { it.copy(isCachingInProgress = true) }
        viewModelScope.launch {
            repository.refreshLibrary(currentUser, force = true)?.let { library -> applyLibrary(currentUser, library) }
        }
    }

    /**
     * Called when the picker becomes visible. The revision is always checked here, even when the last check
     * happened only moments ago.
     */
    fun onPickerOpened() {
        checkForUpdates(force = true)
    }

    fun selectTab(tab: StickerTab) {
        val current = _state.value
        if (tab == current.selectedTab && current.stickers.isNotEmpty()) return

        packJob?.cancel()
        if (tab is StickerTab.Pack) {
            loadPack(tab.name)
        } else {
            emitLocalTab(tab)
        }
    }

    /** Adds [sticker] to the favorites or removes it again, whichever [favorite] asks for. */
    fun setFavorite(sticker: Sticker, favorite: Boolean) {
        val currentUser = user ?: return
        viewModelScope.launch {
            favorites = usageRepository.setFavorite(currentUser, sticker, favorite)
            refreshTabs()
        }
    }

    /** Remembers [sticker] as just sent, so it shows up in the recently sent stickers. */
    fun onStickerSent(sticker: Sticker) {
        val currentUser = user ?: return
        viewModelScope.launch {
            recent = usageRepository.rememberSent(currentUser, sticker)
            refreshTabs()
        }
    }

    fun loadMore() {
        val currentUser = user ?: return
        val current = _state.value
        val packName = currentPackName()
        val cursor = current.nextCursor
        val isIdle = !current.isLoadingStickers && packJob?.isActive != true
        if (packName == null || cursor == null || !isIdle) return

        packJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingStickers = true) }
            val page = repository.fetchPage(currentUser, packName, cursor, revision)
            _state.update { state ->
                if (page == null) {
                    state.copy(isLoadingStickers = false)
                } else {
                    state.copy(
                        stickers = (state.stickers + page.stickers).distinctBy { it.id },
                        nextCursor = page.nextCursor,
                        isLoadingStickers = false
                    )
                }
            }
        }
    }

    private fun checkForUpdates(force: Boolean) {
        val currentUser = user ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastRevisionCheckAt < REVISION_CHECK_THROTTLE_MS) return
        lastRevisionCheckAt = now

        viewModelScope.launch {
            repository.refreshLibrary(currentUser)?.let { library -> applyLibrary(currentUser, library) }
        }
    }

    /** Rebuilds the tab strip and refreshes the stickers of the favorites and recent tab. */
    private fun refreshTabs() {
        val current = _state.value
        val tabs = StickerTab.of(packs, favorites.isNotEmpty(), recent.isNotEmpty())
        val selected = if (current.selectedTab in tabs) current.selectedTab else StickerTab.Emoji
        _state.update {
            it.copy(tabs = tabs, favoriteIds = favorites.mapTo(mutableSetOf()) { sticker -> sticker.id })
        }
        if (selected is StickerTab.Pack) {
            if (selected != current.selectedTab) loadPack(selected.name)
        } else {
            emitLocalTab(selected)
        }
    }

    private fun emitLocalTab(tab: StickerTab) {
        val stickers = when (tab) {
            StickerTab.Favorites -> favorites
            StickerTab.Recent -> recent
            else -> emptyList()
        }
        _state.update {
            it.copy(selectedTab = tab, stickers = stickers, nextCursor = null, isLoadingStickers = false)
        }
    }

    private suspend fun applyLibrary(currentUser: User, library: StickerLibrary) {
        val previousRevision = revision
        val revisionChanged = library.revision != previousRevision
        if (library.packs.isEmpty()) {
            logger.w(TAG, "The server offers stickers but its sticker library is empty")
        } else {
            logger.d(TAG, "Sticker library applied: ${library.packs.size} packs at revision ${library.revision}")
        }
        revision = library.revision
        packs = library.packs
        if (!library.fromCache) {
            // a fetched pack list is authoritative, so stored stickers of vanishing packs are dropped
            usageRepository.prunePacks(currentUser, packs.mapTo(mutableSetOf()) { it.name })
            favorites = usageRepository.favorites(currentUser)
            recent = usageRepository.recent(currentUser)
        }
        // a successful request ends the caching wait, a failed one leaves the dialog to the user
        _state.update { it.copy(isCachingInProgress = false) }
        // servers without a revision route cannot tell whether the packs changed, so every fetched list counts
        if (!library.fromCache && (revisionChanged || library.revision == null)) {
            _libraryCached.emit(Unit)
        }
        refreshTabs()

        val openPack = currentPackName()
        if (openPack != null && revisionChanged && packs.any { it.name == openPack }) {
            loadPack(openPack)
        }
    }

    private fun loadPack(packName: String) {
        val currentUser = user ?: return
        val pack = packs.firstOrNull { it.name == packName } ?: return
        packJob?.cancel()
        packJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedTab = StickerTab.Pack(pack.name, pack.displayName),
                    stickers = emptyList(),
                    nextCursor = null
                )
            }
            val cached = repository.getCachedPage(currentUser, packName, revision)
            if (cached != null) {
                _state.update {
                    it.copy(stickers = cached.stickers, nextCursor = cached.nextCursor, isLoadingStickers = false)
                }
                return@launch
            }

            _state.update { it.copy(isLoadingStickers = true) }
            val page = repository.fetchPage(currentUser, packName, CURSOR_FIRST_PAGE, revision)
            if (page == null) {
                _state.update { it.copy(isLoadingStickers = false) }
            } else {
                _state.update {
                    it.copy(stickers = page.stickers, nextCursor = page.nextCursor, isLoadingStickers = false)
                }
                if (page.nextCursor == null) {
                    // the whole pack is known, so stored stickers that are gone can be dropped
                    usageRepository.prunePack(currentUser, packName, page.stickers.mapTo(mutableSetOf()) { it.id })
                    favorites = usageRepository.favorites(currentUser)
                    recent = usageRepository.recent(currentUser)
                    refreshTabs()
                }
            }
        }
    }

    private fun currentPackName(): String? = (_state.value.selectedTab as? StickerTab.Pack)?.name

    companion object {
        private val TAG = StickersViewModel::class.java.simpleName
        private const val CURSOR_FIRST_PAGE = 0
        private const val REVISION_CHECK_THROTTLE_MS = 30L * 1000
    }
}
