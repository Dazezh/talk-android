/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.ui.theme

import android.content.Context
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.talk.R
import javax.inject.Inject

internal class MaterialSchemesProviderImpl @Inject constructor(private val context: Context) : MaterialSchemesProvider {

    override fun getMaterialSchemesForCurrentUser(): MaterialSchemes =
        MaterialSchemes.fromColor(ContextCompat.getColor(context, R.color.brand_seed))
}
