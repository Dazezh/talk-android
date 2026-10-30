/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui.chat

import androidx.compose.ui.graphics.Color
import hct.Hct

private const val AUTHOR_CHROMA = 44.0
private const val AUTHOR_TONE_LIGHT = 40.0
private const val AUTHOR_TONE_DARK = 82.0
private const val HUE_RANGE = 360

/**
 * Stable colour for the author of a message: the hue is derived from the actor key, so a participant
 * keeps the same colour in every conversation. The tone follows the theme, which keeps the name
 * readable on the bubble in both modes.
 */
internal fun authorColor(actorKey: String, isDark: Boolean): Color {
    val hue = (actorKey.hashCode() % HUE_RANGE + HUE_RANGE) % HUE_RANGE
    val tone = if (isDark) AUTHOR_TONE_DARK else AUTHOR_TONE_LIGHT

    return Color(Hct.from(hue.toDouble(), AUTHOR_CHROMA, tone).toInt())
}
