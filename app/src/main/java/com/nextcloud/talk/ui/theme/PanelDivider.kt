/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui.theme

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Hairline between a top bar and the panel below it.
 *
 * Panels and toolbars share the same surface colour, so every separator between them uses this one
 * colour instead of picking its own. It is deliberately the outline colour and not a container
 * colour: as a 1dp line the softer container greys all but disappear against the surface.
 */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}
