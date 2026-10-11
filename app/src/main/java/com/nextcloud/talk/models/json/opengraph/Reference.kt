/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Marcel Hibbe <dev@mhibbe.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.models.json.opengraph

import android.os.Parcelable
import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonObject
data class Reference(
    @JsonField(name = ["richObjectType"])
    var richObjectType: String? = null,
    @JsonField(name = ["richObject"])
    var richObject: RichObject? = null,
    @JsonField(name = ["openGraphObject"])
    var openGraphObject: OpenGraphObject? = null,

    // null when the server did not send the field at all, which is not the same as an inaccessible link
    @JsonField(name = ["accessible"])
    var accessible: Boolean? = null
) : Parcelable {
    // This constructor is added to work with the 'com.bluelinelabs.logansquare.annotation.JsonObject'
    constructor() : this(null, null, null, null)
}
