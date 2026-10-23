/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.chat.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.max

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
            CenterCropDrawable(bitmap),
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

/**
 * Fills the view with the bitmap without distorting it: the picture is scaled up until it covers
 * the bounds and the overflow is cut off, like `ContentScale.Crop` does for Compose images.
 */
private class CenterCropDrawable(private val bitmap: Bitmap) : Drawable() {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()

    init {
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    override fun draw(canvas: Canvas) {
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        val scale = max(bounds.width() / bitmapWidth, bounds.height() / bitmapHeight)

        matrix.setScale(scale, scale)
        matrix.postTranslate(
            bounds.left + (bounds.width() - bitmapWidth * scale) / 2f,
            bounds.top + (bounds.height() - bitmapHeight * scale) / 2f
        )
        paint.shader?.setLocalMatrix(matrix)

        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
