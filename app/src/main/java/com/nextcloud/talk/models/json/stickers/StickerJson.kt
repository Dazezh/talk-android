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
data class StickerJson(
    @JsonField(name = ["id"])
    var id: String?,
    @JsonField(name = ["name"])
    var name: String?,
    @JsonField(name = ["title"])
    var title: String?,
    @JsonField(name = ["width"])
    var width: Int,
    @JsonField(name = ["height"])
    var height: Int,
    @JsonField(name = ["thumbnailUrl"])
    var thumbnailUrl: String?,
    @JsonField(name = ["resourceUrl"])
    var resourceUrl: String?
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, null, null, 0, 0, null, null)
}
