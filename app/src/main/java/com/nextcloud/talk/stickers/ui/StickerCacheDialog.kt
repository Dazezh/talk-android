/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.nextcloud.talk.R

/**
 * Reports a caching request of the sticker library, which can take a while. The dialog closes itself as soon
 * as the packs arrived, and the user can also let the request finish in the background.
 */
@Composable
fun StickerCacheDialog(onContinueInBackground: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinueInBackground,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_margin))
            ) {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.nc_stickers_caching))
            }
        },
        confirmButton = {
            TextButton(onClick = onContinueInBackground) {
                Text(stringResource(R.string.nc_stickers_load_in_background))
            }
        }
    )
}
