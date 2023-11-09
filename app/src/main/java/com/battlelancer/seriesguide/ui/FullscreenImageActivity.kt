// Copyright 2013 Andrew Neal
// Copyright 2013-2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.ImageView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.SystemUiHider
import com.battlelancer.seriesguide.util.ThemeUtils
import com.github.chrisbanes.photoview.PhotoView
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

/**
 * Displays an image URL full screen in a zoomable view. If a preview image URL is provided, it is
 * shown as a placeholder until the higher resolution image loads. The preview image has to be
 * cached by Picasso already.
 */
class FullscreenImageActivity : BaseActivity() {

    /**
     * The instance of the [SystemUiHider] for this activity.
     */
    private lateinit var systemUiHider: SystemUiHider

    /**
     * Displays the poster or episode preview
     */
    private lateinit var photoView: PhotoView

    private var hideHandler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable = Runnable { systemUiHider.hide() }

    override fun configureEdgeToEdge() {
        ThemeUtils.configureEdgeToEdge(window, forceDarkStatusBars = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)
        setupActionBar()

        setupViews()
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupViews() {
        photoView = findViewById(R.id.fullscreen_content)

        // try to immediately show cached preview image
        val previewImagePath = intent.getStringExtra(EXTRA_PREVIEW_IMAGE)
        if (previewImagePath.isNullOrEmpty()) {
            loadLargeImage(false)
        } else {
            ImageTools.loadWithPicasso(this, previewImagePath)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(photoView, object : Callback {
                    override fun onSuccess() {
                        loadLargeImage(true)
                    }

                    override fun onError(e: Exception) {
                        loadLargeImage(false)
                    }
                })
        }

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        // Currently not using WindowInsetsControllerCompat as it resets zoom level when
        // hiding/showing system bars *sigh* and I don't want to find out why right now.
        systemUiHider = SystemUiHider.getInstance(
            this, photoView, SystemUiHider.FLAG_HIDE_NAVIGATION
        )
        systemUiHider.setup()

        photoView.setOnViewTapListener { _, _, _ -> systemUiHider.toggle() }
    }

    private fun loadLargeImage(hasPreviewImage: Boolean) {
        val imagePath: String? = intent.getStringExtra(EXTRA_IMAGE)?.let {
            // If empty, set to null so picasso shows error drawable.
            if (it.isEmpty()) null else it
        }

        val requestCreator = ImageTools.loadWithPicasso(this, imagePath)
        if (hasPreviewImage) {
            // Keep showing preview image if loading full image fails.
            requestCreator.noPlaceholder().into(photoView)
        } else {
            // No preview image? Show error image instead if loading full image fails.
            requestCreator
                .error(R.drawable.ic_photo_gray_24dp)
                .into(photoView, object : Callback {
                    override fun onSuccess() {
                        photoView.scaleType = ImageView.ScaleType.FIT_CENTER
                    }

                    override fun onError(e: Exception) {
                        photoView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                })
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delayedHide()
    }

    /**
     * Schedules a call to hide() in [DELAY_100_MS] milliseconds, canceling any previously scheduled
     * calls.
     */
    private fun delayedHide() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, DELAY_100_MS.toLong())
    }

    override fun onPause() {
        super.onPause()

        if (isFinishing) {
            // Always cancel the request here, this is safe to call even if the image has been loaded.
            // This ensures that the anonymous callback we have does not prevent the activity from
            // being garbage collected. It also prevents our callback from getting invoked even after the
            // activity has finished.
            Picasso.get().cancelRequest(photoView)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {

        /**
         * Image URL that has been cached already. Will show initially before replacing with larger
         * version.
         */
        private const val EXTRA_PREVIEW_IMAGE = "PREVIEW_IMAGE"
        private const val EXTRA_IMAGE = "IMAGE"

        private const val DELAY_100_MS = 100

        @JvmStatic
        fun intent(context: Context, previewImageUrl: String?, imageUrl: String?): Intent {
            return Intent(context, FullscreenImageActivity::class.java)
                .putExtra(EXTRA_PREVIEW_IMAGE, previewImageUrl)
                .putExtra(EXTRA_IMAGE, imageUrl)
        }
    }
}
