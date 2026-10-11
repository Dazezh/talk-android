/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import android.app.Application
import com.nextcloud.talk.api.NcApiCoroutines
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.models.json.capabilities.Capabilities
import com.nextcloud.talk.models.json.capabilities.RStikerCapability
import com.nextcloud.talk.models.json.capabilities.StickersCapability
import com.nextcloud.talk.models.json.stickers.StickerLibraryCacheJson
import com.nextcloud.talk.models.json.stickers.StickerListJson
import com.nextcloud.talk.models.json.stickers.StickerPackCacheJson
import com.nextcloud.talk.models.json.stickers.StickerPackJson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class StickerRepositoryImplTest {

    private val user = userWithRevisionRoute()
    private val logger = mock<Logger>()
    private fun repository(api: NcApiCoroutines, cache: StickerCache) = StickerRepositoryImpl(api, cache, logger)

    @Test
    fun `the pack list is parsed from the ocs envelope`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickersRevision(any(), any()) } doReturn REVISION_BODY.toResponseBody(JSON)
                on { getStickerPacks(any(), any()) } doReturn PACKS.toResponseBody(JSON)
            }
            val cache = mock<StickerCache>()

            val library = repository(api, cache).refreshLibrary(user)

            assertEquals(REVISION, library?.revision)
            assertEquals(1, library?.packs?.size)
            assertEquals("mochicat", library?.packs?.first()?.name)
            assertEquals("Mochi Cat", library?.packs?.first()?.displayName)
            assertEquals(2, library?.packs?.first()?.stickerCount)
            assertFalse(library?.fromCache ?: true)
            verify(cache).storeLibrary(eq(ACCOUNT_ID), any())
        }

    @Test
    fun `an unchanged revision keeps the cached pack list`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickersRevision(any(), any()) } doReturn REVISION_BODY.toResponseBody(JSON)
            }
            val cache = mock<StickerCache> {
                on { getLibrary(ACCOUNT_ID) } doReturn cachedLibrary()
            }

            val library = repository(api, cache).refreshLibrary(user)

            assertEquals(listOf("mochicat"), library?.packs?.map { it.name })
            assertEquals(true, library?.fromCache)
            verify(api, never()).getStickerPacks(any(), any())
        }

    @Test
    fun `a forced refresh fetches the pack list despite an unchanged revision`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickersRevision(any(), any()) } doReturn REVISION_BODY.toResponseBody(JSON)
                on { getStickerPacks(any(), any()) } doReturn PACKS.toResponseBody(JSON)
            }
            val cache = mock<StickerCache> {
                on { getLibrary(ACCOUNT_ID) } doReturn cachedLibrary()
            }

            val library = repository(api, cache).refreshLibrary(user, force = true)

            assertEquals(listOf("mochicat"), library?.packs?.map { it.name })
            assertFalse(library?.fromCache ?: true)
            verify(cache).storeLibrary(eq(ACCOUNT_ID), any())
        }

    @Test
    fun `a changed revision fetches and caches the pack list again`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickersRevision(any(), any()) } doReturn NEW_REVISION_BODY.toResponseBody(JSON)
                on { getStickerPacks(any(), any()) } doReturn PACKS.toResponseBody(JSON)
            }
            val cache = mock<StickerCache> {
                on { getLibrary(ACCOUNT_ID) } doReturn cachedLibrary()
            }

            val library = repository(api, cache).refreshLibrary(user)

            assertEquals(NEW_REVISION, library?.revision)
            verify(cache).storeLibrary(eq(ACCOUNT_ID), any())
        }

    @Test
    fun `a failing revision request does not block the pack list`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickersRevision(any(), any()) } doSuspendableAnswer { throw IOException("no revision") }
                on { getStickerPacks(any(), any()) } doReturn PACKS.toResponseBody(JSON)
            }
            val cache = mock<StickerCache>()

            val library = repository(api, cache).refreshLibrary(user)

            assertEquals(1, library?.packs?.size)
            assertNull(library?.revision)

            val stored = argumentCaptor<StickerLibraryCacheJson>()
            verify(cache).storeLibrary(eq(ACCOUNT_ID), stored.capture())
            assertEquals(1, stored.firstValue.packs?.size)
        }

    @Test
    fun `a server without the revision route still loads its packs`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickerPacks(any(), any()) } doReturn PACKS.toResponseBody(JSON)
            }
            val cache = mock<StickerCache>()

            val library = repository(api, cache).refreshLibrary(userWithRevisionRoute(offered = false))

            assertEquals(1, library?.packs?.size)
            verify(api, never()).getStickersRevision(any(), any())
        }

    @Test
    fun `a failing pack request is reported and leaves the cache alone`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                // the cached revision is older than the one the server reports, so the packs are fetched again
                on { getStickersRevision(any(), any()) } doReturn NEW_REVISION_BODY.toResponseBody(JSON)
                on { getStickerPacks(any(), any()) } doSuspendableAnswer { throw IOException("offline") }
            }
            val cache = mock<StickerCache> {
                on { getLibrary(ACCOUNT_ID) } doReturn cachedLibrary()
            }

            assertNull(repository(api, cache).refreshLibrary(user))
            verify(cache, never()).storeLibrary(any(), any())
        }

    @Test
    fun `the first page of a pack is parsed and cached with the current revision`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickers(any(), any(), any(), any()) } doReturn PAGE.toResponseBody(JSON)
            }
            val cache = mock<StickerCache>()

            val page = repository(api, cache).fetchPage(user, PACK, CURSOR, REVISION)

            assertEquals(2, page?.stickers?.size)
            assertEquals("bW9jaGljYXQ6MC53ZWJw", page?.stickers?.first()?.id)
            assertEquals("0", page?.stickers?.first()?.title)
            assertEquals(STICKER_URL, page?.stickers?.first()?.resourceUrl)
            assertEquals(60, page?.nextCursor)

            val cached = argumentCaptor<StickerPackCacheJson>()
            verify(cache).storePackPage(eq(ACCOUNT_ID), eq(PACK), cached.capture())
            assertEquals(REVISION, cached.firstValue.revision)
            assertEquals(2, cached.firstValue.page?.entries?.size)
        }

    @Test
    fun `further pages are not written to the cache`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickers(any(), any(), any(), any()) } doReturn PAGE.toResponseBody(JSON)
            }
            val cache = mock<StickerCache>()

            repository(api, cache).fetchPage(user, PACK, SECOND_PAGE_CURSOR, REVISION)

            verify(cache, never()).storePackPage(any(), any(), any())
        }

    @Test
    fun `a cached page is only used for the revision it was fetched with`() =
        runTest {
            val cachedPage = StickerPackCacheJson(REVISION, System.currentTimeMillis(), StickerListJson(null, null, 0))
            val cache = mock<StickerCache> {
                on { getPackPage(ACCOUNT_ID, PACK) } doReturn cachedPage
            }
            val stickerRepository = repository(mock<NcApiCoroutines>(), cache)

            assertNull(stickerRepository.getCachedPage(user, PACK, OTHER_REVISION))
            assertEquals(0, stickerRepository.getCachedPage(user, PACK, REVISION)?.stickers?.size)
        }

    @Test
    fun `a payload without the ocs envelope is rejected`() =
        runTest {
            val api = mock<NcApiCoroutines> {
                on { getStickersRevision(any(), any()) } doReturn REVISION_BODY.toResponseBody(JSON)
                on { getStickerPacks(any(), any()) } doReturn BARE_PACKS.toResponseBody(JSON)
            }
            val cache = mock<StickerCache>()

            assertNull(repository(api, cache).refreshLibrary(user))
            verify(cache, never()).storeLibrary(any(), any())
        }

    private fun cachedLibrary() =
        StickerLibraryCacheJson(
            REVISION,
            System.currentTimeMillis(),
            listOf(StickerPackJson(PACK, "Mochi Cat", "cats", 2))
        )

    private fun userWithRevisionRoute(offered: Boolean = true) =
        User(
            id = ACCOUNT_ID,
            userId = "alice",
            username = "alice",
            baseUrl = "https://cloud.example.com",
            token = "token",
            capabilities = Capabilities(
                coreCapability = null,
                spreedCapability = null,
                notificationsCapability = null,
                themingCapability = null,
                externalCapability = null,
                provisioningCapability = null,
                userStatusCapability = null,
                passwordPolicy = null,
                rStikerCapability = RStikerCapability(1, 1, StickersCapability(null, offered), false)
            )
        )

    companion object {
        private const val ACCOUNT_ID = 7L
        private const val CURSOR = 0
        private const val SECOND_PAGE_CURSOR = 60
        private const val REVISION = "rev1"
        private const val NEW_REVISION = "rev2"
        private const val OTHER_REVISION = "rev3"
        private const val PACK = "mochicat"
        private const val STICKER_URL = "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw"
        private const val REVISION_BODY = """{"ocs":{"data":{"revision":"rev1"}}}"""
        private const val NEW_REVISION_BODY = """{"ocs":{"data":{"revision":"rev2"}}}"""

        private val JSON = "application/json".toMediaType()

        private val PACKS = """
            {
              "ocs": {
                "meta": { "status": "ok", "statuscode": 200, "message": "OK" },
                "data": [
                  {"name":"mochicat","displayName":"Mochi Cat","description":"cats","stickerCount":2}
                ]
              }
            }
        """.trimIndent()

        private val BARE_PACKS = """
            [
              {"name":"mochicat","displayName":"Mochi Cat","description":"cats","stickerCount":2}
            ]
        """.trimIndent()

        private val PAGE = """
            {
              "ocs": {
                "meta": { "status": "ok", "statuscode": 200, "message": "OK" },
                "data": {
                  "entries": [
                    {
                      "id": "bW9jaGljYXQ6MC53ZWJw",
                      "name": "0.webp",
                      "title": "0",
                      "width": 512,
                      "height": 512,
                      "thumbnailUrl": "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw",
                      "resourceUrl": "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw"
                    },
                    {
                      "id": "bW9jaGljYXQ6MS53ZWJw",
                      "name": "1.webp",
                      "title": "1",
                      "width": 512,
                      "height": 512,
                      "thumbnailUrl": "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MS53ZWJw",
                      "resourceUrl": "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MS53ZWJw"
                    }
                  ],
                  "cursor": 60,
                  "total": 42
                }
              }
            }
        """.trimIndent()
    }
}
