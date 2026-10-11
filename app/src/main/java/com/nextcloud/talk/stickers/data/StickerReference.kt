/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.nextcloud.talk.models.json.opengraph.Reference

internal const val R_STIKER_RICH_OBJECT_TYPE = "r-stiker"

/**
 * Image of a resolved r-stiker reference, or null when the server did not answer with a usable sticker.
 * The image is served by the instance the sticker lives on, which may differ from the own server when the
 * sticker was received through federation.
 */
fun Reference.stickerImageUrl(): String? {
    if (richObjectType != R_STIKER_RICH_OBJECT_TYPE || accessible == false) return null
    return richObject?.sticker?.imageUrl ?: richObject?.sticker?.thumbnailUrl ?: richObject?.thumb
}
