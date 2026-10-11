/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.nextcloud.talk.api.NcApiCoroutines
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.stickers.data.StickerImageCache.CachedStickerImage
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.File
import java.io.IOException

class StickerImageLoaderTest {

    private val logger = mock<Logger>()

    @Test
    fun `a fresh cached sticker is drawn without a request`() =
        runTest {
            val file = tempFile()
            val ncApi = mock<NcApiCoroutines>()
            val cache = mock<StickerImageCache> {
                on { find(URL) } doReturn CachedStickerImage(file, outdated = false)
            }

            assertEquals(file, loader(ncApi, cache).load(URL, CREDENTIALS))
            verify(ncApi, never()).getStickerImage(any(), any())
        }

    @Test
    fun `a sticker that is not cached is downloaded and stored`() =
        runTest {
            val file = tempFile()
            val cache = mock<StickerImageCache> {
                on { store(eq(URL), any()) } doReturn CachedStickerImage(file, outdated = false)
            }

            assertEquals(file, loader(apiReturningImage(), cache).load(URL, CREDENTIALS))

            val stored = argumentCaptor<ByteArray>()
            verify(cache).store(eq(URL), stored.capture())
            assertArrayEquals(BYTES, stored.firstValue)
        }

    @Test
    fun `a cached sticker that passed the retention time is downloaded again`() =
        runTest {
            val file = tempFile()
            val api = apiReturningImage()
            val cache = mock<StickerImageCache> {
                on { find(URL) } doReturn CachedStickerImage(file, outdated = true)
                on { store(eq(URL), any()) } doReturn CachedStickerImage(file, outdated = false)
            }

            assertEquals(file, loader(api, cache).load(URL, CREDENTIALS))
            verify(api).getStickerImage(CREDENTIALS, URL)
            verify(cache).store(eq(URL), any())
        }

    @Test
    fun `a failing download falls back to the cached sticker`() =
        runTest {
            val file = tempFile()
            val cache = mock<StickerImageCache> {
                on { find(URL) } doReturn CachedStickerImage(file, outdated = true)
            }

            assertEquals(file, loader(apiFailing(), cache).load(URL, CREDENTIALS))
        }

    @Test
    fun `a failing download without a cached sticker delivers nothing`() =
        runTest {
            assertNull(loader(apiFailing(), mock<StickerImageCache>()).load(URL, CREDENTIALS))
        }

    private fun loader(ncApi: NcApiCoroutines, cache: StickerImageCache) = StickerImageLoader(ncApi, cache, logger)

    private fun apiReturningImage() = apiThrowing(null)

    private fun apiFailing() = apiThrowing(IOException("offline"))

    private fun apiThrowing(failure: Exception?) =
        mock<NcApiCoroutines> {
            if (failure == null) {
                on { getStickerImage(any(), any()) } doReturn BYTES.toResponseBody(IMAGE)
            } else {
                on { getStickerImage(any(), any()) } doSuspendableAnswer { throw failure }
            }
        }

    private fun tempFile(): File = File.createTempFile("sticker", ".img").apply { deleteOnExit() }

    companion object {
        private const val URL = "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw"
        private const val CREDENTIALS = "Basic dXNlcjpwYXNzd29yZA=="
        private val BYTES = "sticker-image".toByteArray()
        private val IMAGE = "image/webp".toMediaType()
    }
}
