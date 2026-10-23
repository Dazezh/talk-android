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
 * One locally stored sticker, used for the favorites and for the recently sent stickers. The pack is kept so
 * an entry can be dropped once the server stops offering that pack.
 */
@JsonObject
data class StickerUsageEntryJson(
    @JsonField(name = ["id"])
    var id: String?,
    @JsonField(name = ["pack"])
    var pack: String?,
    @JsonField(name = ["title"])
    var title: String?,
    @JsonField(name = ["thumbnailUrl"])
    var thumbnailUrl: String?,
    @JsonField(name = ["resourceUrl"])
    var resourceUrl: String?,
    @JsonField(name = ["addedAt"])
    var addedAt: Long
) {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, null, null, null, null, 0)
}
