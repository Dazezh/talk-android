/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

/**
 * Loads a sticker image for the current composition. Hosts provide the loader of the application, so the
 * picker and the chat draw their stickers from the same cache.
 */
val LocalStickerImageLoader = staticCompositionLocalOf<suspend (url: String, credentials: String?) -> File?> {
    { _, _ -> null }
}

/**
 * Draws a sticker as a plain image without any background. The image is read from the sticker cache of the app
 * and only downloaded when it is not cached yet, so a sticker opened in the picker is also drawn in a chat
 * message and the other way around.
 */
@Composable
fun StickerImage(url: String, authHeader: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val loadSticker = LocalStickerImageLoader.current
    val image by produceState<File?>(initialValue = null, url, authHeader) {
        value = loadSticker(url, authHeader)
    }

    image?.let {
        AsyncImage(
            model = it,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}
