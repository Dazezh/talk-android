/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.models.json.stickers

import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject

/**
 * Persisted first page of a sticker pack. Only the first page is cached, further pages are fetched on
 * demand while the user scrolls, so the whole library is never downloaded up front.
 */
@JsonObject
data class StickerPackCacheJson(
    @JsonField(name = ["revision"])
    var revision: String?,
    @JsonField(name = ["cachedAt"])
    var cachedAt: Long,
    @JsonField(name = ["page"])
    var page: StickerListJson?
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, 0, null)
}
