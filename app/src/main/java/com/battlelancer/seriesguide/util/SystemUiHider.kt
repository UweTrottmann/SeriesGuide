package com.battlelancer.seriesguide.util

import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.battlelancer.seriesguide.util.SystemUiHider.Companion.FLAG_FULLSCREEN
import com.battlelancer.seriesguide.util.SystemUiHider.Companion.FLAG_HIDE_NAVIGATION

/**
 * A utility class that helps with showing and hiding system UI such as the
 * status bar and navigation/system bar on API 11+ devices.
 *
 * For more on system bars, see [System Bars](http://developer.android.com/design/get-started/ui-overview.html#system-bars).
 *
 * @see android.view.View.setSystemUiVisibility
 * @see android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
 */
class SystemUiHider private constructor(
    /**
     * The activity associated with this UI hider object.
     */
    private val activity: AppCompatActivity,
    /**
     * The view on which [View.setSystemUiVisibility] will be called.
     */
    private val anchorView: View,
    /**
     * The current UI hider flags.
     *
     * @see FLAG_FULLSCREEN
     * @see FLAG_HIDE_NAVIGATION
     */
    flags: Int
) {
    /**
     * Flags for [View.setSystemUiVisibility] to use when showing the
     * system UI.
     */
    private var showFlags: Int

    /**
     * Flags for [View.setSystemUiVisibility] to use when hiding the
     * system UI.
     */
    private var hideFlags: Int

    /**
     * Flags to test against the first parameter in
     * [android.view.View.OnSystemUiVisibilityChangeListener.onSystemUiVisibilityChange]
     * to determine the system UI visibility state.
     */
    private var testFlags: Int

    /**
     * Whether or not the system UI is currently visible. This is cached from
     * [android.view.View.OnSystemUiVisibilityChangeListener].
     */
    var isVisible = true
        private set

    /**
     * A callback, to be triggered when the system UI visibility changes.
     */
    var onVisibilityChangeListener: OnVisibilityChangeListener? = null

    init {
        showFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        hideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        testFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE

        if (flags and FLAG_FULLSCREEN == FLAG_FULLSCREEN) {
            // If the client requested fullscreen, add flags relevant to hiding
            // the status bar.
            showFlags = showFlags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            hideFlags =
                hideFlags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        if (flags and FLAG_HIDE_NAVIGATION == FLAG_HIDE_NAVIGATION) {
            // If the client requested hiding navigation, add relevant flags.
            showFlags = showFlags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            hideFlags = hideFlags or (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            testFlags = testFlags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    /**
     * Sets up the system UI hider. Should be called from [AppCompatActivity.onCreate].
     */
    fun setup() {
        anchorView.setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener)
    }

    /**
     * Hide the system UI.
     */
    fun hide() {
        anchorView.systemUiVisibility = hideFlags
    }

    /**
     * Show the system UI.
     */
    fun show() {
        anchorView.systemUiVisibility = showFlags
    }

    /**
     * Toggle the visibility of the system UI.
     */
    fun toggle() {
        if (isVisible) {
            hide()
        } else {
            show()
        }
    }

    /**
     * A callback interface used to listen for system UI visibility changes.
     */
    interface OnVisibilityChangeListener {
        /**
         * Called when the system UI visibility has changed.
         *
         * @param visible True if the system UI is visible.
         */
        fun onVisibilityChange(visible: Boolean)
    }

    private val systemUiVisibilityChangeListener: OnSystemUiVisibilityChangeListener =
        object : OnSystemUiVisibilityChangeListener {
            override fun onSystemUiVisibilityChange(vis: Int) {
                // Test against testFlags to see if the system UI is visible.
                val supportActionBar = activity.supportActionBar
                if (vis and testFlags != 0) {
                    // As we use the appcompat toolbar as an action bar, we must manually hide it
                    supportActionBar?.hide()

                    // Trigger the registered listener and cache the visibility state.
                    onVisibilityChangeListener?.onVisibilityChange(false)
                    isVisible = false
                } else {
                    anchorView.systemUiVisibility = showFlags

                    // As we use the appcompat toolbar as an action bar, we must manually show it
                    supportActionBar?.show()

                    // Trigger the registered listener and cache the visibility state.
                    onVisibilityChangeListener?.onVisibilityChange(true)
                    isVisible = true
                }
            }
        }

    companion object {
        /**
         * When this flag is set, [show] and [hide] will toggle
         * the visibility of the status bar. If there is a navigation bar, show and
         * hide will toggle low profile mode.
         */
        const val FLAG_FULLSCREEN = 0x2

        /**
         * When this flag is set, [show] and [hide] will toggle
         * the visibility of the navigation bar, if it's present on the device and
         * the device allows hiding it. In cases where the navigation bar is present
         * but cannot be hidden, show and hide will toggle low profile mode.
         */
        const val FLAG_HIDE_NAVIGATION = FLAG_FULLSCREEN or 0x4

        /**
         * Creates and returns an instance of [SystemUiHider].
         *
         * @param activity The activity whose window's system UI should be controlled by this class.
         * @param anchorView The view on which [View.setSystemUiVisibility] will be called.
         * @param flags Either 0 or any combination of [FLAG_FULLSCREEN] and [FLAG_HIDE_NAVIGATION].
         */
        fun getInstance(
            activity: AppCompatActivity, anchorView: View,
            flags: Int
        ): SystemUiHider = SystemUiHider(activity, anchorView, flags)
    }
}