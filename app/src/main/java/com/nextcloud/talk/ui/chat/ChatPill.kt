/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val PILL_ALPHA = 0.45f
private val pillCornerRadius = 12.dp
private val pillHorizontalPadding = 12.dp
private val pillVerticalPadding = 4.dp

/**
 * Rounded container for the small pieces of context in the chat: date headers, system messages and
 * the unread messages marker. The default container is translucent so the wallpaper stays visible
 * behind it; the pills that anchor the list (dates, unread marker) pass an opaque colour instead.
 */
@Composable
fun ChatPill(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = PILL_ALPHA),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(pillCornerRadius))
            .padding(horizontal = pillHorizontalPadding, vertical = pillVerticalPadding),
        content = content
    )
}
