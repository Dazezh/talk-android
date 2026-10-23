/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.utils.preferences

import android.content.Context
import androidx.core.content.ContextCompat
import com.nextcloud.talk.R

/**
 * Appearance settings that have to be readable synchronously: the colour scheme is built while views
 * are being inflated and composed, long before a coroutine from the DataStore based preferences
 * could deliver a value.
 */
class RalkAppearancePreferences(context: Context) {

    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** ARGB seed the whole colour scheme is derived from. */
    var accentSeed: Int
        get() = preferences.getInt(KEY_ACCENT_SEED, NO_ACCENT)
        set(value) = preferences.edit().putInt(KEY_ACCENT_SEED, value).apply()

    fun accentSeedOrDefault(context: Context): Int =
        accentSeed.takeIf { it != NO_ACCENT } ?: ContextCompat.getColor(context, R.color.brand_seed)

    var stickerSize: StickerSize
        get() = StickerSize.entries.getOrElse(preferences.getInt(KEY_STICKER_SIZE, StickerSize.MEDIUM.ordinal)) {
            StickerSize.MEDIUM
        }
        set(value) = preferences.edit().putInt(KEY_STICKER_SIZE, value.ordinal).apply()

    private companion object {
        const val FILE_NAME = "ralk_appearance"
        const val KEY_ACCENT_SEED = "accent_seed"
        const val KEY_STICKER_SIZE = "sticker_size"
        const val NO_ACCENT = 0
    }
}

@Suppress("MagicNumber")
enum class StickerSize(val label: String, val scale: Float) {
    SMALL("S", 0.75f),
    MEDIUM("M", 1f),
    LARGE("L", 1.25f)
}
