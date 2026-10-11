/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import android.content.Context
import com.nextcloud.talk.logger.Logger
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File cache for sticker images. A sticker that was drawn in the picker or in a chat message is kept on the
 * device for [MAX_AGE_MS], so it is shown again without a new download and stays visible without a
 * connection.
 */
@Singleton
class StickerImageCache @Inject constructor(private val context: Context, private val logger: Logger) {

    /**
     * The cached image of [url], or null when it was never stored. [CachedStickerImage.outdated] marks copies
     * that passed [MAX_AGE_MS] and are fetched again when the server can be reached.
     */
    fun find(url: String): CachedStickerImage? {
        val file = fileFor(url)
        val now = System.currentTimeMillis()
        return file.takeIf { it.isFile }?.let { CachedStickerImage(it, !isFresh(it, now)) }
    }

    /** Writes [bytes] as the image of [url] and removes the images that passed [MAX_AGE_MS]. */
    fun store(url: String, bytes: ByteArray): CachedStickerImage? {
        val file = fileFor(url)
        return try {
            file.writeBytes(bytes)
            removeOutdated(System.currentTimeMillis())
            CachedStickerImage(file, outdated = false)
        } catch (e: IOException) {
            logger.w(TAG, "Sticker image could not be cached", e)
            null
        }
    }

    private fun isFresh(file: File, now: Long): Boolean = now - file.lastModified() <= MAX_AGE_MS

    private fun removeOutdated(now: Long) {
        directory().listFiles()?.forEach { file ->
            if (!isFresh(file, now)) {
                file.delete()
            }
        }
    }

    private fun fileFor(url: String): File = File(directory(), nameOf(url))

    private fun directory(): File = File(context.noBackupFilesDir, DIRECTORY).apply { mkdirs() }

    private fun nameOf(url: String): String =
        MessageDigest.getInstance(DIGEST).digest(url.toByteArray()).joinToString("") { byte ->
            (byte.toInt() and BYTE_MASK).toString(RADIX).padStart(2, '0')
        }

    data class CachedStickerImage(val file: File, val outdated: Boolean)

    companion object {
        /** A cached sticker is downloaded again only after this time. */
        const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

        private val TAG = StickerImageCache::class.java.simpleName
        private const val DIRECTORY = "stickers"
        private const val DIGEST = "SHA-256"
        private const val BYTE_MASK = 0xFF
        private const val RADIX = 16
    }
}
