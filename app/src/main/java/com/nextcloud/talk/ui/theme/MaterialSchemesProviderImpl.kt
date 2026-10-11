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
import javax.inject.Inject

internal class MaterialSchemesProviderImpl @Inject constructor(context: Context) : MaterialSchemesProvider {

    private val schemes: MaterialSchemes = BrandMaterialSchemes(context)

    override fun getMaterialSchemesForCurrentUser(): MaterialSchemes = schemes
}
