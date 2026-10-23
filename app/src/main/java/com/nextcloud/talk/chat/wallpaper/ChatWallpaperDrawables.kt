/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.chat.wallpaper

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

private const val MAX_WALLPAPER_DIMENSION = 2048

/**
 * Draws the wallpaper with a scrim on top, so message bubbles stay readable on any picture. The
 * bitmap is sampled down to the screen size: the wallpaper is drawn once behind the list, not
 * animated, so it only has to be light on memory.
 */
fun buildWallpaperDrawable(file: File, scrimColor: Int): Drawable? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val bitmap = if (bounds.outWidth > 0 && bounds.outHeight > 0) {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_WALLPAPER_DIMENSION)
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    } else {
        null
    } ?: return null

    return LayerDrawable(
        arrayOf(
            BitmapDrawable(Resources.getSystem(), bitmap),
            ColorDrawable(scrimColor)
        )
    )
}

/**
 * Decodes the wallpaper for the Compose previews. Unlike [buildWallpaperDrawable] no scrim is added:
 * the callers draw their own.
 */
fun loadWallpaperImage(file: File, maxDimension: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    }

    return BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
}

private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) {
        sample *= 2
    }
    return sample
}
