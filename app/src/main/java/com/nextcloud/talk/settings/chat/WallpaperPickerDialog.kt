/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.settings.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.chat.wallpaper.ChatWallpaper

/**
 * Lists the images found in the Ralk folder of the current account. The wallpapers are kept on the
 * server so every device of the account sees the same set; the picker only ever shows what was
 * successfully listed.
 */
@Composable
internal fun WallpaperPickerDialog(
    options: List<ChatWallpaper>?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (ChatWallpaper?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nc_chat_settings_wallpaper_section)) },
        text = {
            when {
                isLoading -> LoadingContent()
                options.isNullOrEmpty() -> Text(
                    text = stringResource(R.string.nc_chat_settings_wallpaper_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> WallpaperList(options = options, onSelect = onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.nc_common_dismiss))
            }
        }
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun WallpaperList(options: List<ChatWallpaper>, onSelect: (ChatWallpaper?) -> Unit) {
    Column(modifier = Modifier.heightIn(max = 360.dp)) {
        WallpaperRow(
            title = stringResource(R.string.nc_chat_settings_wallpaper_clear),
            subtitle = null,
            onClick = { onSelect(null) }
        )
        LazyColumn {
            items(items = options, key = { it.path }) { wallpaper ->
                WallpaperRow(
                    title = wallpaper.name,
                    subtitle = wallpaper.path,
                    onClick = { onSelect(wallpaper) }
                )
            }
        }
    }
}

@Composable
private fun WallpaperRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
