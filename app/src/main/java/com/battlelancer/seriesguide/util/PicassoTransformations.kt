// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.squareup.picasso.Transformation
import kotlin.math.min

/** A [Transformation] used to crop a [Bitmap] to a circle.  */
class CircleTransformation : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val shortestEdge = min(source.width, source.height)

        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        p.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            .apply {
                val isNotSquare = source.width != source.height
                if (isNotSquare) {
                    // Center the canvas over the image.
                    val deltaX = (source.width - shortestEdge) / 2
                    val deltaY = (source.height - shortestEdge) / 2
                    val matrix = Matrix()
                        .apply { setTranslate(-deltaX.toFloat(), -deltaY.toFloat()) }
                    setLocalMatrix(matrix)
                }
            }

        val transformed = Bitmap.createBitmap(shortestEdge, shortestEdge, Bitmap.Config.ARGB_8888)
        val c = Canvas(transformed)
        // Use oval instead of circle to avoid having to calculate radius.
        c.drawOval(RectF(0f, 0f, shortestEdge.toFloat(), shortestEdge.toFloat()), p)

        // Picasso requires the original Bitmap to be recycled if this isn't returning it.
        source.recycle()

        // Release any references to avoid memory leaks.
        p.shader = null
        c.setBitmap(null)

        return transformed
    }

    override fun key(): String {
        return "CircleTransformation"
    }
}

/** A [Transformation] used to crop a [Bitmap] to have round corners.  */
class RoundedCornerTransformation(
    /** The corner radius  */
    private val radius: Float
) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height

        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        p.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val transformed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(transformed)
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, p)

        // Picasso requires the original Bitmap to be recycled if we aren't returning it
        source.recycle()

        // Release any references to avoid memory leaks
        p.shader = null
        c.setBitmap(null)

        return transformed
    }

    override fun key(): String {
        return "RoundedCornerTransformation(radius=$radius)"
    }
}
