/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.stickers.data.model.StickerLibrary
import com.nextcloud.talk.stickers.data.model.StickerPage

/**
 * Access to the sticker library of a server. Reading methods never touch the network, fetching methods
 * only update the cache when the request succeeded and return null on any failure, so callers can always
 * fall back to the previously cached state.
 */
interface StickerRepository {

    /** The cached library including its revision and age, or null when nothing has been cached yet. */
    suspend fun getCachedLibrary(user: User): StickerLibrary?

    /** The cached first page of [packName], but only if it still belongs to the given [revision]. */
    suspend fun getCachedPage(user: User, packName: String, revision: String?): StickerPage?

    /**
     * Returns the library of the server. The pack list is only fetched again when the cached revision is not
     * confirmed by the server or the cache got too old; an unavailable revision route never blocks the list.
     * A forced refresh skips the cached copy and always fetches the packs again.
     */
    suspend fun refreshLibrary(user: User, force: Boolean = false): StickerLibrary?

    /** Fetches one page of [packName], null when the request failed. */
    suspend fun fetchPage(user: User, packName: String, cursor: Int, revision: String?): StickerPage?
}
