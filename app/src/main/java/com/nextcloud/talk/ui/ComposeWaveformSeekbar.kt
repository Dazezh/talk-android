/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val WAVEFORM_THUMB_SIZE = 20
const val WAVEFORM_SIZE = 30
const val MAX_HEIGHT = 100
const val OVERLAP = 0.025
const val PROGRESS = 0.97f

// One thickness for every waveform bar and for the gaps between them, so the bars look the same on
// every density. The previous Stroke.DefaultMiter value is a raw pixel width, which read as a hairline.
private val waveformBarWidth = 3.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeWaveformSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    waveData: FloatArray,
    enabled: Boolean
) {
    val thumbSize = WAVEFORM_THUMB_SIZE.dp
    val inversePrimary = MaterialTheme.colorScheme.inversePrimary
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Slider(
        value = value,
        enabled = enabled,
        onValueChange = onValueChange,
        track = {
            Box(
                modifier = modifier
                    .drawWithCache {
                        val barWidth = waveformBarWidth.toPx()
                        onDrawBehind {
                            val height = this.size.height
                            val width = this.size.width
                            val midpoint = height / 2f
                            val gaps = (waveData.size - 1).coerceAtLeast(1)
                            val barGap = (width - waveData.size * barWidth) / gaps

                            for (i in waveData.indices) {
                                val x: Float = i * (barWidth + barGap)

                                if (x < 0f || x > width) continue

                                val y: Float = (waveData[i] * height).coerceIn(0f, midpoint)

                                val isXBeforeThumb = x <= value * width

                                drawLine(
                                    if (isXBeforeThumb) inversePrimary else onPrimaryContainer,
                                    start = Offset(x, midpoint - y),
                                    end = Offset(x, midpoint + y),
                                    strokeWidth = barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
            )
        },
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(inversePrimary, shape = CircleShape)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun Preview() {
    val waveData = remember { FloatArray(WAVEFORM_SIZE) { (Math.random() % 1).toFloat() } }

    ComposeWaveformSeekBar(
        PROGRESS,
        {},
        modifier = Modifier
            .height(MAX_HEIGHT.dp)
            .fillMaxWidth()
            .padding(8.dp),
        waveData,
        true
    )
}
