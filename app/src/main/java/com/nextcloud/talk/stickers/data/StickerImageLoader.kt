/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.nextcloud.talk.api.NcApiCoroutines
import com.nextcloud.talk.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delivers sticker images to the UI. Every image is looked up in the local sticker cache first and only
 * downloaded when it is not cached yet or its copy passed [StickerImageCache.MAX_AGE_MS].
 */
@Singleton
class StickerImageLoader @Inject constructor(
    private val ncApi: NcApiCoroutines,
    private val cache: StickerImageCache,
    private val logger: Logger
) {

    /**
     * The image of [url] as a local file, null when nothing could be delivered. An outdated copy is still
     * returned when the download fails, so a sticker that was seen before stays visible without a connection.
     */
    suspend fun load(url: String, credentials: String?): File? =
        withContext(Dispatchers.IO) {
            val cached = cache.find(url)
            if (cached != null && !cached.outdated) {
                cached.file
            } else {
                downloadAndStore(url, credentials) ?: cached?.file
            }
        }

    private suspend fun downloadAndStore(url: String, credentials: String?): File? {
        val bytes = download(url, credentials) ?: return null
        return cache.store(url, bytes)?.file
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun download(url: String, credentials: String?): ByteArray? =
        try {
            ncApi.getStickerImage(credentials, url).bytes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "Sticker image could not be downloaded", e)
            null
        }

    companion object {
        private val TAG = StickerImageLoader::class.java.simpleName
    }
}
