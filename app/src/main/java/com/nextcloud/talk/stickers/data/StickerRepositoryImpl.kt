/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.bluelinelabs.logansquare.LoganSquare
import com.google.gson.JsonParser
import com.nextcloud.talk.api.NcApiCoroutines
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.models.json.stickers.StickerJson
import com.nextcloud.talk.models.json.stickers.StickerLibraryCacheJson
import com.nextcloud.talk.models.json.stickers.StickerListJson
import com.nextcloud.talk.models.json.stickers.StickerPackCacheJson
import com.nextcloud.talk.models.json.stickers.StickerPackJson
import com.nextcloud.talk.models.json.stickers.StickerRevisionJson
import com.nextcloud.talk.stickers.data.model.Sticker
import com.nextcloud.talk.stickers.data.model.StickerLibrary
import com.nextcloud.talk.stickers.data.model.StickerPack
import com.nextcloud.talk.stickers.data.model.StickerPage
import com.nextcloud.talk.utils.ApiUtils
import com.nextcloud.talk.utils.CapabilitiesUtil
import com.nextcloud.talk.utils.UserIdUtils
import kotlinx.coroutines.CancellationException

class StickerRepositoryImpl(
    private val ncApi: NcApiCoroutines,
    private val cache: StickerCache,
    private val logger: Logger
) : StickerRepository {

    override suspend fun getCachedLibrary(user: User): StickerLibrary? {
        val accountId = accountId(user) ?: return null
        return cache.getLibrary(accountId)?.toDomainModel()
    }

    override suspend fun getCachedPage(user: User, packName: String, revision: String?): StickerPage? {
        val accountId = accountId(user) ?: return null
        val cached = cache.getPackPage(accountId, packName)
        return cached?.takeIf { it.revision == revision }?.page?.toDomainModel()
    }

    override suspend fun refreshLibrary(user: User, force: Boolean): StickerLibrary? {
        val accountId = accountId(user) ?: return null
        val revision = fetchRevisionIfOffered(user)
        val cached = cache.getLibrary(accountId)
        val usableCache = cached?.takeIf { !force && !isExpired(it.cachedAt) && isConfirmedBy(it.revision, revision) }
        return usableCache?.toDomainModel() ?: fetchAndStoreLibrary(user, accountId, revision ?: cached?.revision)
    }

    override suspend fun fetchPage(user: User, packName: String, cursor: Int, revision: String?): StickerPage? {
        val page = fetchStickerPage(user, packName, cursor) ?: return null
        if (cursor == CURSOR_FIRST_PAGE) {
            accountId(user)?.let { id ->
                cache.storePackPage(id, packName, StickerPackCacheJson(revision, currentTime(), page))
            }
        }
        return page.toDomainModel()
    }

    /**
     * The revision is only asked for when the server announces the route. An unknown revision is not an error:
     * the pack list is then simply fetched again, so a server without a working revision route still gets a
     * usable picker.
     */
    private suspend fun fetchRevisionIfOffered(user: User): String? =
        if (CapabilitiesUtil.getStickersCapability(user)?.revision == true) {
            fetchRevision(user)
        } else {
            null
        }

    private fun isConfirmedBy(cachedRevision: String?, revision: String?): Boolean =
        revision != null && cachedRevision == revision

    private suspend fun fetchAndStoreLibrary(user: User, accountId: Long, revision: String?): StickerLibrary? {
        val packs = fetchPacks(user) ?: return null
        val library = StickerLibraryCacheJson(revision, currentTime(), packs)
        cache.storeLibrary(accountId, library)
        return library.toDomainModel(fromCache = false)
    }

    private suspend fun fetchRevision(user: User): String? =
        request {
            val baseUrl = user.baseUrl!!
            val body = ncApi.getStickersRevision(user.getCredentials(), ApiUtils.getUrlForStickersRevision(baseUrl))
                .string()
            LoganSquare.parse(payloadOf(body), StickerRevisionJson::class.java).revision
        }

    private suspend fun fetchPacks(user: User): List<StickerPackJson>? =
        request {
            val baseUrl = user.baseUrl!!
            val body = ncApi.getStickerPacks(user.getCredentials(), ApiUtils.getUrlForStickerPacks(baseUrl)).string()
            LoganSquare.parseList(payloadOf(body), StickerPackJson::class.java)
        }

    private suspend fun fetchStickerPage(user: User, packName: String, cursor: Int): StickerListJson? =
        request {
            val baseUrl = user.baseUrl!!
            val body = ncApi.getStickers(
                user.getCredentials(),
                ApiUtils.getUrlForStickers(baseUrl, packName),
                cursor,
                PAGE_SIZE
            ).string()
            LoganSquare.parse(payloadOf(body), StickerListJson::class.java)
        }

    /**
     * The r-stiker routes answer inside the OCS envelope, so the payload is read from `ocs.data`. A response
     * without that envelope is treated as a failed request and therefore never replaces the cached library.
     */
    private fun payloadOf(body: String): String =
        JsonParser.parseString(body).asJsonObject.getAsJsonObject(OCS)?.get(DATA)?.toString()
            ?: error("The r-stiker response does not contain an OCS payload")

    private fun accountId(user: User): Long? = UserIdUtils.getIdForUser(user).takeIf { it != UserIdUtils.NO_ID }

    private fun isExpired(cachedAt: Long): Boolean = currentTime() - cachedAt > CACHE_TTL_MS

    private fun currentTime(): Long = System.currentTimeMillis()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> request(block: suspend () -> T?): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "Sticker request failed, cached data is kept", e)
            null
        }

    private fun StickerLibraryCacheJson.toDomainModel(fromCache: Boolean = true): StickerLibrary =
        StickerLibrary(revision, packs.orEmpty().mapNotNull { it.toDomainModel() }, cachedAt, fromCache)

    private fun StickerPackJson.toDomainModel(): StickerPack? {
        val packName = name
        if (packName.isNullOrBlank()) {
            logger.w(TAG, "Sticker pack without a name was ignored")
            return null
        }
        return StickerPack(packName, displayName ?: packName, description, stickerCount)
    }

    private fun StickerListJson.toDomainModel(): StickerPage =
        StickerPage(entries.orEmpty().mapNotNull { it.toDomainModel() }, cursor, total)

    private fun StickerJson.toDomainModel(): Sticker? {
        val stickerId = id
        val resource = resourceUrl
        if (stickerId == null || resource == null) return null
        return Sticker(stickerId, title ?: name ?: stickerId, thumbnailUrl ?: resource, resource)
    }

    companion object {
        private val TAG = StickerRepositoryImpl::class.java.simpleName
        private const val OCS = "ocs"
        private const val DATA = "data"
        private const val CURSOR_FIRST_PAGE = 0
        private const val PAGE_SIZE = 60
        private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }
}
