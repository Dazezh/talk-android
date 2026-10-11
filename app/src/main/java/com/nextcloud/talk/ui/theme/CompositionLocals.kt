/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2017-2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.nextcloud.talk.models.json.opengraph.Reference
import com.nextcloud.talk.utils.message.MessageUtils

val LocalViewThemeUtils = staticCompositionLocalOf<ViewThemeUtils> {
    error("ViewThemeUtils not provided")
}

val LocalMessageUtils = staticCompositionLocalOf<MessageUtils> {
    error("MessageUtils not provided")
}

/** Resolves a link through the server reference API. Returns null when not available or in previews. */
val LocalReferenceResolver = staticCompositionLocalOf<suspend (url: String) -> Reference?> {
    { null }
}
