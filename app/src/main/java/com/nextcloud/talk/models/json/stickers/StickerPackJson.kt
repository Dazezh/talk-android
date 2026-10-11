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
data class StickerPackJson(
    @JsonField(name = ["name"])
    var name: String?,
    @JsonField(name = ["displayName"])
    var displayName: String?,
    @JsonField(name = ["description"])
    var description: String?,
    @JsonField(name = ["stickerCount"])
    var stickerCount: Int
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, null, null, 0)
}
