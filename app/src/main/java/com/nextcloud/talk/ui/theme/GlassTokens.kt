/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui.theme

/**
 * Translucency of the surfaces that float above the chat wallpaper. The pills, the typing banner and
 * the wallpaper scrim have to stay in tune, so their alphas live here instead of being repeated at
 * every call site.
 */
internal object GlassTokens {

    /** Background of the pills and banners that sit directly on the wallpaper. */
    const val PANEL_ALPHA = 0.45f

    /** Scrim drawn over the wallpaper so the message bubbles stay readable. */
    const val SCRIM_ALPHA = 0.45f
}
