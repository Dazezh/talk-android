/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data.model

/**
 * The sticker library of a server together with the revision it was fetched at and the time it was
 * stored, so callers can decide whether a refresh is still needed. [fromCache] tells whether the library
 * was served from the local cache instead of being fetched again.
 */
data class StickerLibrary(
    val revision: String?,
    val packs: List<StickerPack>,
    val cachedAt: Long,
    val fromCache: Boolean = false
)
