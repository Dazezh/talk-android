/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.ui.theme

import android.content.Context
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.talk.R
import dynamiccolor.DynamicScheme
import dynamiccolor.Variant
import hct.Hct
import palettes.TonalPalette

/**
 * Material 3 scheme derived from the brand seed. Surfaces are kept neutral grey and the brand hue is
 * reserved for the accent roles, so screens do not pick up the tinted, washed out surfaces that
 * Material You generates for accent colours. Contrast stays with the Material colour spec, which
 * derives the accessible tone pairs for every role.
 */
internal class BrandMaterialSchemes(context: Context) : MaterialSchemes {

    private val seed: Hct = Hct.fromInt(ContextCompat.getColor(context, R.color.brand_seed))

    override val lightScheme: DynamicScheme = scheme(isDark = false)
    override val darkScheme: DynamicScheme = scheme(isDark = true)

    private fun scheme(isDark: Boolean): DynamicScheme =
        DynamicScheme(
            seed,
            Variant.TONAL_SPOT,
            isDark,
            CONTRAST_LEVEL,
            TonalPalette.fromHueAndChroma(seed.hue, seed.chroma),
            TonalPalette.fromHueAndChroma(seed.hue, seed.chroma * MUTED_CHROMA_RATIO),
            TonalPalette.fromHueAndChroma(seed.hue, seed.chroma * FAINT_CHROMA_RATIO),
            TonalPalette.fromHueAndChroma(seed.hue, NEUTRAL_CHROMA),
            TonalPalette.fromHueAndChroma(seed.hue, NEUTRAL_CHROMA)
        )

    private companion object {
        const val CONTRAST_LEVEL = 0.0
        const val MUTED_CHROMA_RATIO = 0.4
        const val FAINT_CHROMA_RATIO = 0.2
        const val NEUTRAL_CHROMA = 0.0
    }
}
