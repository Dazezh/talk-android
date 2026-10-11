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
class StickersViewModel @Inject constructor(private val repository: StickerRepository, private val logger: Logger) :
    ViewModel() {

    data class StickerPickerState(
        val packs: List<StickerPack> = emptyList(),
        val selectedTab: Int = TAB_EMOJI,
        val stickers: List<Sticker> = emptyList(),
        val nextCursor: Int? = null,
        val isLoadingStickers: Boolean = false,
        val credentials: String = "",
        val isCachingInProgress: Boolean = false
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
            val cachedLibrary = repository.getCachedLibrary(user)
            if (cachedLibrary == null) {
                // the very first request fills the cache and can take a while, so the wait is reported
                _state.update { it.copy(isCachingInProgress = true) }
            } else {
                applyLibrary(cachedLibrary)
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
            repository.refreshLibrary(currentUser, force = true)?.let { library -> applyLibrary(library) }
        }
    }

    /**
     * Called when the picker becomes visible. The revision is always checked here, even when the last check
     * happened only moments ago.
     */
    fun onPickerOpened() {
        checkForUpdates(force = true)
    }

    fun selectTab(index: Int) {
        val current = _state.value
        if (index == current.selectedTab && current.stickers.isNotEmpty()) return

        _state.update { it.copy(selectedTab = index, stickers = emptyList(), nextCursor = null) }
        currentPackName()?.let { loadPack(it) }
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
            repository.refreshLibrary(currentUser)?.let { library -> applyLibrary(library) }
        }
    }

    private suspend fun applyLibrary(library: StickerLibrary) {
        val previousRevision = revision
        val revisionChanged = library.revision != previousRevision
        if (library.packs.isEmpty()) {
            logger.w(TAG, "The server offers stickers but its sticker library is empty")
        } else {
            logger.d(TAG, "Sticker library applied: ${library.packs.size} packs at revision ${library.revision}")
        }
        revision = library.revision
        // a successful request ends the caching wait, a failed one leaves the dialog to the user
        _state.update { it.copy(packs = library.packs, isCachingInProgress = false) }
        // servers without a revision route cannot tell whether the packs changed, so every fetched list counts
        if (!library.fromCache && (revisionChanged || library.revision == null)) {
            _libraryCached.emit(Unit)
        }

        val openPack = currentPackName() ?: return
        if (library.packs.none { it.name == openPack }) {
            _state.update { it.copy(selectedTab = TAB_EMOJI, stickers = emptyList(), nextCursor = null) }
        } else if (revisionChanged) {
            loadPack(openPack)
        }
    }

    private fun loadPack(packName: String) {
        val currentUser = user ?: return
        packJob?.cancel()
        packJob = viewModelScope.launch {
            val cached = repository.getCachedPage(currentUser, packName, revision)
            if (cached != null) {
                emitPackPage(cached.stickers, cached.nextCursor)
                return@launch
            }

            _state.update { it.copy(isLoadingStickers = true, stickers = emptyList(), nextCursor = null) }
            val page = repository.fetchPage(currentUser, packName, CURSOR_FIRST_PAGE, revision)
            if (page == null) {
                _state.update { it.copy(isLoadingStickers = false) }
            } else {
                emitPackPage(page.stickers, page.nextCursor)
            }
        }
    }

    private fun emitPackPage(stickers: List<Sticker>, nextCursor: Int?) {
        _state.update { it.copy(stickers = stickers, nextCursor = nextCursor, isLoadingStickers = false) }
    }

    private fun currentPackName(): String? {
        val current = _state.value
        return current.packs.getOrNull(current.selectedTab - 1)?.name
    }

    companion object {
        private val TAG = StickersViewModel::class.java.simpleName
        const val TAB_EMOJI = 0
        private const val CURSOR_FIRST_PAGE = 0
        private const val REVISION_CHECK_THROTTLE_MS = 30L * 1000
    }
}
