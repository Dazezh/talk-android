/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.models.json.stickers

import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject

@JsonObject
data class StickerListJson(
    @JsonField(name = ["entries"])
    var entries: List<StickerJson>?,
    @JsonField(name = ["cursor"])
    var cursor: Int?,
    @JsonField(name = ["total"])
    var total: Int
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, null, 0)
}
