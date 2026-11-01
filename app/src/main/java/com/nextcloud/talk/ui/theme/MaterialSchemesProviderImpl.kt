/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.ui.theme

import android.content.Context
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.models.json.capabilities.CapabilitiesDto
import com.nextcloud.talk.utils.preferences.RalkAppearancePreferences
import javax.inject.Inject

internal class MaterialSchemesProviderImpl @Inject constructor(context: Context) : MaterialSchemesProvider {

    private val appearancePreferences = RalkAppearancePreferences(context)
    private val appContext = context

    private var cachedSeed: Int = Int.MIN_VALUE
    private var schemes: MaterialSchemes? = null

    @Synchronized
    private fun brandSchemes(): MaterialSchemes {
        val seed = appearancePreferences.accentSeedOrDefault(appContext)
        val cached = schemes
        if (cached != null && cachedSeed == seed) {
            return cached
        }

        return BrandMaterialSchemes(appContext).also {
            schemes = it
            cachedSeed = seed
        }
    }

    override fun getMaterialSchemesForUser(user: User?): MaterialSchemes = brandSchemes()

    override fun getMaterialSchemesForCapabilities(capabilities: CapabilitiesDto?): MaterialSchemes = brandSchemes()

    override fun getMaterialSchemesForDefaultUser(): MaterialSchemes = brandSchemes()
}
