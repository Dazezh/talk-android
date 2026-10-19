/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.ui.theme.BannerPanel
import com.nextcloud.talk.ui.theme.ViewThemeUtils
import com.nextcloud.talk.utils.preview.ComposePreviewUtils

@Composable
fun CallStartedBanner(viewThemeUtils: ViewThemeUtils, onJoinVideoCall: () -> Unit, onJoinAudioCall: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = remember { viewThemeUtils.getColorScheme(context) }

    BannerPanel {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_phone),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.call_in_progress),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            CallActionButton(
                icon = R.drawable.ic_videocam_grey_600_24dp,
                description = R.string.video_call,
                onClick = onJoinVideoCall
            )
            Spacer(modifier = Modifier.size(8.dp))
            CallActionButton(
                icon = R.drawable.ic_phone,
                description = R.string.audio_call,
                onClick = onJoinAudioCall
            )
        }
    }
}

@Composable
private fun CallActionButton(icon: Int, description: Int, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(name = "Light Mode")
@Composable
fun CallStartedBannerPreview() {
    val context = LocalContext.current
    val previewUtils = ComposePreviewUtils.getInstance(context)
    val viewThemeUtils = previewUtils.viewThemeUtils
    val colorScheme = viewThemeUtils.getColorScheme(context)

    MaterialTheme(colorScheme = colorScheme) {
        CallStartedBanner(
            viewThemeUtils = viewThemeUtils,
            onJoinVideoCall = {},
            onJoinAudioCall = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CallStartedBannerPreviewDark() {
    CallStartedBannerPreview()
}

@Preview(name = "R-t-L", locale = "ar")
@Composable
fun CallStartedBannerPreviewRtl() {
    CallStartedBannerPreview()
}
