/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.bluelinelabs.logansquare.LoganSquare
import com.nextcloud.talk.arbitrarystorage.ArbitraryStorageManager
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.models.json.stickers.StickerLibraryCacheJson
import com.nextcloud.talk.models.json.stickers.StickerPackCacheJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Keeps the sticker library and the first page of each opened sticker pack in the per-account arbitrary
 * storage. Entries are never discarded on their own: an entry stays usable until a newer one replaces it,
 * so a failing request always leaves the last successful state intact.
 */
class StickerCache @Inject constructor(
    private val arbitraryStorageManager: ArbitraryStorageManager,
    private val logger: Logger
) {

    suspend fun getLibrary(accountId: Long): StickerLibraryCacheJson? =
        read(accountId, KEY_LIBRARY, LIBRARY_STORAGE_OBJECT, StickerLibraryCacheJson::class.java)

    suspend fun storeLibrary(accountId: Long, library: StickerLibraryCacheJson) {
        write(accountId, KEY_LIBRARY, LIBRARY_STORAGE_OBJECT, library)
    }

    suspend fun getPackPage(accountId: Long, packName: String): StickerPackCacheJson? =
        read(accountId, KEY_PACK_PAGE, packName, StickerPackCacheJson::class.java)

    suspend fun storePackPage(accountId: Long, packName: String, page: StickerPackCacheJson) {
        write(accountId, KEY_PACK_PAGE, packName, page)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> read(accountId: Long, key: String, storageObject: String, type: Class<T>): T? =
        withContext(Dispatchers.IO) {
            val value = arbitraryStorageManager
                .getStorageSetting(accountId, key, storageObject)
                .blockingGet()
                ?.value
            if (value.isNullOrEmpty()) {
                null
            } else {
                try {
                    LoganSquare.parse(value, type)
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to read cached stickers for key $key", e)
                    null
                }
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun write(accountId: Long, key: String, storageObject: String, value: Any) {
        try {
            withContext(Dispatchers.IO) {
                arbitraryStorageManager.storeStorageSetting(
                    accountId,
                    key,
                    LoganSquare.serialize(value),
                    storageObject
                )
            }
        } catch (e: Exception) {
            // a sticker that cannot be persisted still has to be usable
            logger.e(TAG, "Failed to cache stickers for key $key", e)
        }
    }

    companion object {
        private val TAG = StickerCache::class.java.simpleName
        private const val KEY_LIBRARY = "r_stiker_library"
        private const val KEY_PACK_PAGE = "r_stiker_pack_page"

        // the arbitrary storage key consists of key plus object, so the library needs a fixed object
        private const val LIBRARY_STORAGE_OBJECT = ""
    }
}
