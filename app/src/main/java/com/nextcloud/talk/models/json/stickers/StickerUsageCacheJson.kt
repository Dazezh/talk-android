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
 * Persisted favorites or recently sent stickers of one account. The entries are kept newest first, so the
 * storing order itself carries the date the stickers were added.
 */
@JsonObject
data class StickerUsageCacheJson(
    @JsonField(name = ["entries"])
    var entries: List<StickerUsageEntryJson>?
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null)
}
