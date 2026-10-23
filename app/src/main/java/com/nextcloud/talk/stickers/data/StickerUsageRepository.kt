/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.models.json.stickers.StickerUsageCacheJson
import com.nextcloud.talk.models.json.stickers.StickerUsageEntryJson
import com.nextcloud.talk.stickers.data.model.Sticker
import com.nextcloud.talk.utils.UserIdUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the user selected favorite stickers and the recently sent ones in the local storage of the account.
 * Favorites are stored in the order they were added; only the newest [MAX_FAVORITES] are handed out, so an
 * entry that does not fit yet appears as soon as a newer one is removed.
 */
@Singleton
class StickerUsageRepository @Inject constructor(private val cache: StickerCache, private val logger: Logger) {

    /** The newest favorite stickers, newest first. */
    suspend fun favorites(user: User): List<Sticker> = entriesOf(user, UsageKind.FAVORITES, MAX_FAVORITES)

    /** The recently sent stickers, newest first. */
    suspend fun recent(user: User): List<Sticker> = entriesOf(user, UsageKind.RECENT, MAX_RECENT)

    /** Adds [sticker] to the favorites or removes it and returns the resulting favorites. */
    suspend fun setFavorite(user: User, sticker: Sticker, favorite: Boolean): List<Sticker> {
        val accountId = accountId(user) ?: return emptyList()
        val entries = read(accountId, UsageKind.FAVORITES).filterNot { it.id == sticker.id }.toMutableList()
        if (favorite) {
            entries.add(FIRST_ENTRY, sticker.toEntry())
        }
        store(accountId, UsageKind.FAVORITES, entries)
        return entries.mapNotNull { it.toDomainModel() }.take(MAX_FAVORITES)
    }

    /** Remembers [sticker] as just sent and returns the resulting recently sent stickers. */
    suspend fun rememberSent(user: User, sticker: Sticker): List<Sticker> {
        val accountId = accountId(user) ?: return emptyList()
        val entries = read(accountId, UsageKind.RECENT).filterNot { it.id == sticker.id }.toMutableList()
        entries.add(FIRST_ENTRY, sticker.toEntry())
        val remembered = entries.take(MAX_RECENT)
        store(accountId, UsageKind.RECENT, remembered)
        return remembered.mapNotNull { it.toDomainModel() }
    }

    /** Drops the stored stickers of packs the server does not offer anymore. */
    suspend fun prunePacks(user: User, existingPacks: Set<String>) {
        prune(user) { it.pack == null || it.pack in existingPacks }
    }

    /** Drops the stored stickers of [packName] that are missing from its complete sticker list. */
    suspend fun prunePack(user: User, packName: String, existingIds: Set<String>) {
        prune(user) { it.pack != packName || it.id in existingIds }
    }

    private suspend fun prune(user: User, keep: (StickerUsageEntryJson) -> Boolean) {
        val accountId = accountId(user) ?: return
        UsageKind.entries.forEach { kind ->
            val entries = read(accountId, kind)
            val kept = entries.filter(keep)
            if (kept.size != entries.size) {
                logger.d(TAG, "Dropped ${entries.size - kept.size} stored stickers of $kind")
                store(accountId, kind, kept)
            }
        }
    }

    private suspend fun entriesOf(user: User, kind: UsageKind, limit: Int): List<Sticker> {
        val accountId = accountId(user) ?: return emptyList()
        return read(accountId, kind).mapNotNull { it.toDomainModel() }.take(limit)
    }

    private suspend fun read(accountId: Long, kind: UsageKind): List<StickerUsageEntryJson> {
        val stored = when (kind) {
            UsageKind.FAVORITES -> cache.getFavorites(accountId)
            UsageKind.RECENT -> cache.getRecent(accountId)
        }
        return stored?.entries.orEmpty()
    }

    private suspend fun store(accountId: Long, kind: UsageKind, entries: List<StickerUsageEntryJson>) {
        val value = StickerUsageCacheJson(entries)
        when (kind) {
            UsageKind.FAVORITES -> cache.storeFavorites(accountId, value)
            UsageKind.RECENT -> cache.storeRecent(accountId, value)
        }
    }

    private fun accountId(user: User): Long? = UserIdUtils.getIdForUser(user).takeIf { it != UserIdUtils.NO_ID }

    private fun StickerUsageEntryJson.toDomainModel(): Sticker? {
        val stickerId = id
        val thumbnail = thumbnailUrl
        val resource = resourceUrl
        if (stickerId == null || thumbnail == null || resource == null) return null
        return Sticker(stickerId, title ?: stickerId, thumbnail, resource, pack)
    }

    private fun Sticker.toEntry(): StickerUsageEntryJson =
        StickerUsageEntryJson(id, pack, title, thumbnailUrl, resourceUrl, System.currentTimeMillis())

    private enum class UsageKind { FAVORITES, RECENT }

    companion object {
        /** Favorite stickers that are shown at the same time. */
        const val MAX_FAVORITES = 50

        /** Recently sent stickers that are remembered, the oldest one is forgotten first. */
        const val MAX_RECENT = 100

        private val TAG = StickerUsageRepository::class.java.simpleName
        private const val FIRST_ENTRY = 0
    }
}
