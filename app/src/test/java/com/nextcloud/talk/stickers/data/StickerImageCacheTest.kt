/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nextcloud.talk.logger.Logger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class StickerImageCacheTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cache = StickerImageCache(context, mock<Logger>())

    @Test
    fun `a sticker that was never stored has no cached image`() {
        assertNull(cache.find(URL))
    }

    @Test
    fun `a stored sticker is found with its bytes`() {
        val stored = cache.store(URL, BYTES)

        val found = cache.find(URL)

        assertNotNull(found)
        assertEquals(stored?.file, found?.file)
        assertFalse(found!!.outdated)
        assertArrayEquals(BYTES, found.file.readBytes())
    }

    @Test
    fun `two stickers do not share one file`() {
        cache.store(URL, BYTES)

        assertNull(cache.find(OTHER_URL))
    }

    @Test
    fun `a sticker that passed the retention time is outdated`() {
        val stored = cache.store(URL, BYTES)!!
        stored.file.setLastModified(System.currentTimeMillis() - StickerImageCache.MAX_AGE_MS - 1)

        assertTrue(cache.find(URL)!!.outdated)
    }

    @Test
    fun `storing drops the stickers that passed the retention time`() {
        val outdated = cache.store(URL, BYTES)!!
        outdated.file.setLastModified(System.currentTimeMillis() - StickerImageCache.MAX_AGE_MS - 1)

        cache.store(OTHER_URL, BYTES)

        assertFalse(outdated.file.exists())
        assertNotNull(cache.find(OTHER_URL))
    }

    companion object {
        private const val URL = "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw"
        private const val OTHER_URL = "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MS53ZWJw"
        private val BYTES = "sticker-image".toByteArray()
    }
}
