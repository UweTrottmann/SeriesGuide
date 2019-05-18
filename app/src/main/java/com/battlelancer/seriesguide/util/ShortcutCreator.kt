package com.battlelancer.seriesguide.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.os.Build
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Add a shortcut to the overview page of the given show to the Home screen.
 *
 * @param showTitle The name of the shortcut.
 * @param posterPath A TVDb show poster path.
 * @param showTvdbId The TVDb ID of the show.
 */
class ShortcutCreator(
    localContext: Context,
    private val showTitle: String,
    private val posterPath: String,
    private val showTvdbId: Int
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
        val posterUrl = TvdbImageTools.smallSizeUrl(posterPath)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
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
        val shortcutIntent = OverviewActivity.intentShow(context, showTvdbId)
        shortcutIntent.action = Intent.ACTION_MAIN
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val builder = ShortcutInfo.Builder(
                    context,
                    "shortcut-show-$showTvdbId"
                )
                    .setIntent(shortcutIntent)
                    .setShortLabel(showTitle)
                if (posterBitmap == null) {
                    builder.setIcon(
                        Icon.createWithResource(context, R.drawable.ic_shortcut_show)
                    )
                } else {
                    builder.setIcon(Icon.createWithAdaptiveBitmap(posterBitmap))
                }
                val pinShortcutInfo = builder.build()

                // Create the PendingIntent object only if your app needs to be notified
                // that the user allowed the shortcut to be pinned. Note that, if the
                // pinning operation fails, your app isn't notified. We assume here that the
                // app has implemented a method called createShortcutResultIntent() that
                // returns a broadcast intent.
                val pinnedShortcutCallbackIntent =
                    shortcutManager.createShortcutResultIntent(pinShortcutInfo)

                // Configure the intent so that your app's broadcast receiver gets
                // the callback successfully.
                val successCallback = PendingIntent.getBroadcast(
                    context, 0,
                    pinnedShortcutCallbackIntent, 0
                )

                shortcutManager.requestPinShortcut(
                    pinShortcutInfo,
                    successCallback.intentSender
                )
            }
        } else {
            // Intent that actually creates the shortcut
            val intent = Intent()
            @Suppress("DEPRECATION")
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            @Suppress("DEPRECATION")
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, showTitle)
            if (posterBitmap == null) {
                // Fall back to the app icon
                @Suppress("DEPRECATION")
                intent.putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_app)
                )
            } else {
                @Suppress("DEPRECATION")
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, posterBitmap)
            }
            intent.action = ACTION_INSTALL_SHORTCUT
            context.sendBroadcast(intent)

            // drop to home screen, launcher should animate to new shortcut
            context.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** A [Transformation] used to draw a [Bitmap] with round corners  */
    private class RoundedCornerTransformation
    /** Constructor for `RoundedCornerTransformation`  */
    internal constructor(
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

    companion object {
        /** [Intent] action used to create the shortcut  */
        private const val ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT"
    }

}