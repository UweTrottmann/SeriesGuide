package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Add a shortcut to the overview page of the given show to the Home screen.
 */
class ShortcutCreator(
    localContext: Context,
    private val showTitle: String,
    private val posterPath: String,
    private val showTmdbId: Int
) {

    private val context = localContext.applicationContext

    /**
     * Prepares the bitmap for the shortcut on [Dispatchers.IO],
     * when ready suggest to (O+) or pins the shortcut.
     */
    suspend fun prepareAndPinShortcut() {
        val bitmap = withContext(Dispatchers.IO) {
            createBitmap()
        }
        withContext(Dispatchers.Main) {
            pinShortcut(bitmap)
        }
    }

    private suspend fun createBitmap(): Bitmap? = suspendCoroutine { continuation ->
        // Try to get the show poster
        val posterUrl = ImageTools.tmdbOrTvdbPosterUrl(posterPath, context)
        if (posterUrl == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        val requestCreator = Picasso.get()
            .load(posterUrl)
            .centerCrop()
            .memoryPolicy(MemoryPolicy.NO_STORE)
            .networkPolicy(NetworkPolicy.NO_STORE)
            .resizeDimen(
                R.dimen.show_poster_width_shortcut,
                R.dimen.show_poster_height_shortcut
            )
        if (!AndroidUtils.isAtLeastOreo) {
            // on O+ we use 108x108dp adaptive icon, no need to cut its corners
            // pre-O full bitmap is displayed, so cut corners for nicer icon shape
            requestCreator.transform(
                RoundedCornerTransformation(posterUrl, 10f)
            )
        }

        try {
            continuation.resume(requestCreator.get())
        } catch (e: IOException) {
            Timber.e(e, "Could not load show poster for shortcut %s", posterPath)
            continuation.resume(null)
        }
    }

    private fun pinShortcut(posterBitmap: Bitmap?) {
        // Intent used when the shortcut is tapped
        val shortcutIntent = OverviewActivity.intentShowByTmdbId(context, showTmdbId)
        shortcutIntent.action = Intent.ACTION_MAIN
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val builder = ShortcutInfoCompat.Builder(
                context,
                "shortcut-show-tmdb-$showTmdbId"
            )
                .setIntent(shortcutIntent)
                .setShortLabel(showTitle)
            val isAtLeastOreo = AndroidUtils.isAtLeastOreo
            if (posterBitmap == null) {
                // Note: only use adaptive icon on O+, app icon on older SDKs.
                builder.setIcon(
                    IconCompat.createWithResource(
                        context,
                        if (isAtLeastOreo) R.drawable.ic_shortcut_show else R.mipmap.ic_app
                    )
                )
            } else {
                // Note: do not use compat adaptive bitmap API, results in very small circle bitmap.
                // Use bitmap with rounded corners (see createBitmap()) instead.
                builder.setIcon(
                    if (isAtLeastOreo) IconCompat.createWithAdaptiveBitmap(posterBitmap)
                    else IconCompat.createWithBitmap(posterBitmap)
                )
            }
            val pinShortcutInfo = builder.build()

            // Note: still requires com.android.launcher.permission.INSTALL_SHORTCUT
            // in manifest to support API levels before Oreo (26).
            ShortcutManagerCompat.requestPinShortcut(
                context,
                pinShortcutInfo,
                null
            )

            // On old SDKs that do not have ShortcutManager,
            // drop to home screen, launcher should animate to new shortcut.
            if (!isAtLeastOreo) {
                context.startActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } else {
            // Pinning shortcuts is not possible
            // (neither using new ShortcutManager or old ACTION_INSTALL_SHORTCUT intent).
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show()
        }
    }

    /** A [Transformation] used to draw a [Bitmap] with round corners  */
    private class RoundedCornerTransformation(
        /** A key used to uniquely identify this [Transformation]  */
        private val key: String,
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
            return key
        }
    }

}