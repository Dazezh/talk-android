/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.models.json.capabilities

import android.os.Parcelable
import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@JsonObject
@Serializable
data class RStikerCapability(
    @JsonField(name = ["version"])
    var version: Int,
    @JsonField(name = ["protocol"])
    var protocol: Int,
    @JsonField(name = ["stickers"])
    var stickersCapability: StickersCapability?,
    @JsonField(name = ["smart-picker"])
    var smartPicker: Boolean
) : Parcelable {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(0, 0, null, false)
}
