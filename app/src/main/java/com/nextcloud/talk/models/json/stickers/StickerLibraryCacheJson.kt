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
 * Persisted snapshot of the sticker library. It is stored together with the library [revision] it was
 * fetched with, so a revision change can invalidate the cache without touching the server again.
 */
@JsonObject
data class StickerLibraryCacheJson(
    @JsonField(name = ["revision"])
    var revision: String?,
    @JsonField(name = ["cachedAt"])
    var cachedAt: Long,
    @JsonField(name = ["packs"])
    var packs: List<StickerPackJson>?
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, 0, null)
}
