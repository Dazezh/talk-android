/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.settings.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.ui.theme.BrandMaterialSchemes

// Seeds, not final accents: every seed is turned into the accent tone of the light and of the dark
// scheme by the same code that builds the app theme, so a swatch always matches what is applied.
@Suppress("MagicNumber")
internal val accentPresets = listOf(
    0xFFAF604C.toInt(),
    0xFF2E6DB4.toInt(),
    0xFF2E8B74.toInt(),
    0xFF4B8B3B.toInt(),
    0xFF7B5EA7.toInt(),
    0xFFB04A7A.toInt(),
    0xFFC07A1F.toInt(),
    0xFF5A5F66.toInt()
)

// The seed keeps a little saturation and brightness, otherwise the derived accent turns into a grey.
private const val MIN_SATURATION = 0.35f
private const val MIN_BRIGHTNESS = 0.35f
private const val HUE_STEPS = 12

@Suppress("MagicNumber")
private val hueRing = List(HUE_STEPS + 1) { step -> Color.hsv(step * 360f / HUE_STEPS, 1f, 1f) }

@Composable
internal fun AccentSwatch(seed: Int, selected: Boolean, onClick: () -> Unit) {
    AccentCircle(
        seed = seed,
        selected = selected,
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
internal fun CustomAccentSwatch(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.sweepGradient(hueRing))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.nc_chat_settings_accent_custom),
            tint = Color.White
        )
    }
}

@Composable
internal fun AccentColorPickerDialog(initialSeed: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val initialHsv = remember(initialSeed) {
        FloatArray(HSV_SIZE).also { android.graphics.Color.colorToHSV(initialSeed, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceAtLeast(MIN_SATURATION)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceAtLeast(MIN_BRIGHTNESS)) }

    val seed = Color.hsv(hue, saturation, brightness).toArgb()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nc_chat_settings_accent_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AccentCircle(seed = seed, selected = false, modifier = Modifier.size(56.dp))
                }
                SliderField(label = stringResource(R.string.nc_chat_settings_accent_hue)) {
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                }
                SliderField(label = stringResource(R.string.nc_chat_settings_accent_saturation)) {
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it.coerceAtLeast(MIN_SATURATION) },
                        valueRange = MIN_SATURATION..1f
                    )
                }
                SliderField(label = stringResource(R.string.nc_chat_settings_accent_brightness)) {
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it.coerceAtLeast(MIN_BRIGHTNESS) },
                        valueRange = MIN_BRIGHTNESS..1f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(seed) }) {
                Text(stringResource(R.string.nc_chat_settings_accent_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.nc_cancel))
            }
        }
    )
}

@Composable
private fun SliderField(label: String, slider: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        slider()
    }
}

@Composable
private fun AccentCircle(seed: Int, selected: Boolean, modifier: Modifier = Modifier) {
    val outerAccent = remember(seed) { Color(BrandMaterialSchemes.accentFor(seed, isDark = true)) }
    val innerAccent = remember(seed) { Color(BrandMaterialSchemes.accentFor(seed, isDark = false)) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(outerAccent)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(INNER_CIRCLE_FRACTION)
                .clip(CircleShape)
                .background(innerAccent)
        )
    }
}

private const val INNER_CIRCLE_FRACTION = 0.62f
private const val HSV_SIZE = 3
