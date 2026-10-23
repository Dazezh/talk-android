/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.chat.wallpaper

import android.content.Context
import android.net.Uri
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.filebrowser.webdav.ReadFolderListingOperation
import com.nextcloud.talk.remotefilebrowser.model.RemoteFileBrowserItem
import com.nextcloud.talk.utils.ApiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class ChatWallpaper(val path: String, val name: String)

/**
 * Reads the chat wallpapers from the shared folder in the user storage and uploads new ones there.
 * The folder is created on demand, the list only ever contains images.
 */
class ChatWallpaperRepository @Inject constructor(private val okHttpClient: OkHttpClient) {

    suspend fun listWallpapers(user: User): List<ChatWallpaper> = withContext(Dispatchers.IO) { readWallpapers(user) }

    private fun readWallpapers(user: User): List<ChatWallpaper> {
        val operation = ReadFolderListingOperation(okHttpClient, user, WALLPAPER_FOLDER, DEPTH_ONE)
        val items = operation.readRemotePath().data as? List<*> ?: return emptyList()

        return items.filterIsInstance<RemoteFileBrowserItem>()
            .filter { item -> item.isFile && item.mimeType?.startsWith(IMAGE_PREFIX) == true }
            .mapNotNull { item ->
                val path = item.path?.removePrefix("/") ?: return@mapNotNull null
                ChatWallpaper(path = path, name = item.displayName ?: path.substringAfterLast('/'))
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun wallpaperFile(context: Context, user: User, path: String): File? =
        withContext(Dispatchers.IO) { cachedOrDownloaded(context, user, path) }

    private fun cachedOrDownloaded(context: Context, user: User, path: String): File? {
        val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }
        val cached = File(cacheDirectory, cacheName(path))

        return cached.takeIf { isUsable(it) || downloadWallpaper(user, path, it) }
    }

    private fun isUsable(file: File): Boolean = file.exists() && file.length() > MIN_CACHED_SIZE

    private fun downloadWallpaper(user: User, path: String, target: File): Boolean {
        val credentials = ApiUtils.getCredentials(user.username, user.token)
        val baseUrl = user.baseUrl
        val userId = user.userId
        if (credentials == null || baseUrl == null || userId == null) {
            return false
        }

        return runCatching { download(path, credentials, baseUrl, userId, target) }.getOrDefault(false)
    }

    private fun download(path: String, credentials: String, baseUrl: String, userId: String, target: File): Boolean {
        val request = Request.Builder()
            .url(ApiUtils.getUrlForFileDownload(baseUrl, userId, path))
            .header(AUTHORIZATION_HEADER, credentials)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                return false
            }
            target.outputStream().use { output -> body.byteStream().copyTo(output) }
        }

        return true
    }

    private fun cacheName(path: String): String = Integer.toHexString(path.hashCode())

    suspend fun uploadWallpaper(context: Context, user: User, source: Uri, fileName: String): ChatWallpaper? =
        withContext(Dispatchers.IO) { upload(context, user, source, fileName) }

    private fun upload(context: Context, user: User, source: Uri, fileName: String): ChatWallpaper? {
        val credentials = ApiUtils.getCredentials(user.username, user.token)
        val baseUrl = user.baseUrl
        val userId = user.userId
        if (credentials == null || baseUrl == null || userId == null) {
            return null
        }

        val path = "$FOLDER_NAME/$fileName"
        val uploaded = runCatching {
            ensureFolder(ApiUtils.getUrlForFileDownload(baseUrl, userId, FOLDER_NAME), credentials) &&
                putFile(
                    url = ApiUtils.getUrlForFileDownload(baseUrl, userId, path),
                    credentials = credentials,
                    body = requestBody(context, source)
                )
        }.getOrDefault(false)

        return if (uploaded) ChatWallpaper(path = path, name = fileName) else null
    }

    /** Creates the wallpaper folder; an already existing folder answers with 405, which is fine. */
    private fun ensureFolder(url: String, credentials: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .method(METHOD_MKCOL, null)
            .header(AUTHORIZATION_HEADER, credentials)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.isSuccessful || response.code == HTTP_CODE_METHOD_NOT_ALLOWED
        }
    }

    private fun putFile(url: String, credentials: String, body: RequestBody): Boolean {
        val request = Request.Builder()
            .url(url)
            .put(body)
            .header(AUTHORIZATION_HEADER, credentials)
            .build()

        return okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
    }

    private fun requestBody(context: Context, source: Uri): RequestBody {
        val mimeType = context.contentResolver.getType(source) ?: DEFAULT_MIME_TYPE
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() } ?: ByteArray(0)

        return bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    }

    private companion object {
        const val WALLPAPER_FOLDER = "/Ralk"
        const val FOLDER_NAME = "Ralk"
        const val DEPTH_ONE = 1
        const val IMAGE_PREFIX = "image/"
        const val CACHE_DIRECTORY = "chat-wallpapers"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val MIN_CACHED_SIZE = 2L
        const val METHOD_MKCOL = "MKCOL"
        const val HTTP_CODE_METHOD_NOT_ALLOWED = 405
        const val DEFAULT_MIME_TYPE = "image/jpeg"
    }
}
