/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.settings.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.chat.wallpaper.ChatWallpaperRepository
import com.nextcloud.talk.chat.wallpaper.loadWallpaperImage
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.ui.theme.BrandMaterialSchemes
import com.nextcloud.talk.utils.preferences.StickerSize
import dynamiccolor.MaterialDynamicColors

private const val PREVIEW_MAX_DIMENSION = 1080
private const val PREVIEW_SCRIM_ALPHA = 0.45f
private const val PREVIEW_STICKER_BORDER = 2
private val previewHeight = 240.dp
private val previewPadding = 12.dp
private val previewShape = RoundedCornerShape(16.dp)
private val previewStickerSize = 76.dp

internal data class ChatPreviewState(val wallpaperPath: String?, val accentSeed: Int, val stickerSize: StickerSize)

private data class PreviewAccent(val primary: Color, val bubble: Color, val onBubble: Color)

/**
 * Shows how the chat will look with the staged settings: the wallpaper behind, two sample bubbles
 * and a sample sticker whose size follows the selected sticker size.
 */
@Composable
internal fun ChatPreview(
    state: ChatPreviewState,
    repository: ChatWallpaperRepository,
    user: User?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val accent = remember(state.accentSeed, isDark) { previewAccent(state.accentSeed, isDark) }
    val wallpaper by produceState<ImageBitmap?>(initialValue = null, state.wallpaperPath, user) {
        val path = state.wallpaperPath
        if (path != null && user != null) {
            value = repository.wallpaperFile(context, user, path)?.let {
                loadWallpaperImage(it, PREVIEW_MAX_DIMENSION)
            }
        }
    }
    val previewWallpaper = wallpaper

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = previewPadding, vertical = 8.dp)
            .height(previewHeight)
            .clip(previewShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (previewWallpaper != null) {
            Image(
                bitmap = previewWallpaper,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = PREVIEW_SCRIM_ALPHA))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(previewPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreviewBubble(
                text = stringResource(R.string.nc_chat_settings_preview_incoming),
                incoming = true,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PreviewBubble(
                text = stringResource(R.string.nc_chat_settings_preview_outgoing),
                incoming = false,
                containerColor = accent.bubble,
                contentColor = accent.onBubble
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PreviewSticker(size = previewStickerSize * state.stickerSize.scale, tint = accent.primary)
            }
        }
    }
}

@Composable
private fun PreviewSticker(size: Dp, tint: Color) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(previewShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(PREVIEW_STICKER_BORDER.dp, MaterialTheme.colorScheme.outlineVariant, previewShape)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_ralk_letter_r),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@Composable
private fun PreviewBubble(text: String, incoming: Boolean, containerColor: Color, contentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End
    ) {
        Surface(shape = previewShape, color = containerColor, contentColor = contentColor) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun previewAccent(seed: Int, isDark: Boolean): PreviewAccent {
    val scheme = BrandMaterialSchemes.schemeFor(seed, isDark)
    val colors = MaterialDynamicColors()

    return PreviewAccent(
        primary = Color(colors.primary().getArgb(scheme)),
        bubble = Color(colors.primaryContainer().getArgb(scheme)),
        onBubble = Color(colors.onPrimaryContainer().getArgb(scheme))
    )
}
