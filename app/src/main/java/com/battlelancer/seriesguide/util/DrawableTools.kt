@file:JvmName("DrawableTools")
package com.battlelancer.seriesguide.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes

fun getBitmapFromVectorDrawable(themeContext: Context, @DrawableRes res: Int): Bitmap {
    val drawable = themeContext.getDrawable(res)!!

    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}
