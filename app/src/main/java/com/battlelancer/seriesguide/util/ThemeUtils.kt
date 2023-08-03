package com.battlelancer.seriesguide.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout

/**
 * Helper methods to configure the appearance of the app.
 */
object ThemeUtils {

    private const val EDGE_TO_EDGE_BAR_ALPHA = 128

    /**
     * Sets the global app theme variable. Applied by all activities once they are created.
     */
    @Synchronized
    fun updateTheme(themeIndex: String) {
        SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_DayNight
        when (themeIndex.toIntOrNull() ?: 0) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else ->
                // Defaults as recommended by https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94
                if (AndroidUtils.isAtLeastQ) {
                    AppCompatDelegate
                        .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate
                        .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
        }
    }

    /**
     * Resolves the given attribute to the resource id for the given theme.
     */
    @JvmStatic
    @AnyRes
    fun resolveAttributeToResourceId(
        theme: Resources.Theme,
        @AttrRes attributeResId: Int
    ): Int {
        val outValue = TypedValue()
        theme.resolveAttribute(attributeResId, outValue, true)
        return outValue.resourceId
    }

    @JvmStatic
    fun getColorFromAttribute(context: Context, @AttrRes attribute: Int): Int {
        return ContextCompat.getColor(
            context,
            resolveAttributeToResourceId(context.theme, attribute)
        )
    }

    @JvmStatic
    fun SlidingTabLayout.setDefaultStyle() {
        setCustomTabView(R.layout.tabstrip_item_transparent, R.id.textViewTabStripItem)
        setSelectedIndicatorColors(
            getColorFromAttribute(context, androidx.appcompat.R.attr.colorPrimary)
        )
        setUnderlineColor(getColorFromAttribute(context, R.attr.sgColorDivider))
    }

    /**
     * If [DisplaySettings.isDynamicColorsEnabled] applies Material 3 Dynamic Colors theme overlay
     * if available on the device.
     * https://m3.material.io/libraries/mdc-android/color-theming
     */
    fun setTheme(activity: Activity, themeResId: Int = SeriesGuidePreferences.THEME) {
        activity.setTheme(themeResId)
        if (DisplaySettings.isDynamicColorsEnabled(activity)) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }

    /**
     * Configures the window, status and navigation bar of an activity for edge to edge display.
     *
     * Call after `super.onCreate` and before `setContentView`.
     *
     * The Material 3 AppBarLayout should use `android:fitsSystemWindows="true"`
     * to receive window insets so it can adjust padding when drawing behind the system bars.
     *
     * The root view group likely needs to be padded start and end to handle a navigation bar in
     * button mode in landscape, see [configureForEdgeToEdge].
     *
     * Scroll views or RecyclerViews should add bottom padding matching the navigation bar, see
     * [applyBottomPaddingForNavigationBar].
     *
     * Note: on Android 10+ BottomNavigationView *does* draw behind the navigation bar using
     * button mode automatically, however, the system adds an almost opaque scrim so its color is
     * barely noticeable.
     */
    fun configureEdgeToEdge(window: Window, forceDarkStatusBars: Boolean = false) {
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge
        // Let the app draw from edge to edge (below status and navigation bar).
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val colorBackground =
            MaterialColors.getColor(window.context, android.R.attr.colorBackground, Color.BLACK)
        val isLightBackground = MaterialColors.isColorLight(colorBackground)

        val statusBarColor = getStatusBarColor(window.context)
        val navigationBarColor = getNavigationBarColor(window.context)

        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor

        // For transparent status bars (M+), check if the background has a light color;
        // for colored status bars check if itself has a light color.
        // If a light color, tell the system to color icons accordingly.
        setLightStatusBar(
            window,
            if (forceDarkStatusBars) {
                false
            } else {
                isUsingLightSystemBar(statusBarColor, isLightBackground)
            }
        )
        // Do the same check for nav bars
        // (only difference: transparent nav bars supported since O_MR1+).
        setLightNavigationBar(
            window,
            isUsingLightSystemBar(navigationBarColor, isLightBackground)
        )
    }

    private fun getStatusBarColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // A light status bar is only supported on M+.
            // Use a translucent black status bar instead.
            val opaqueStatusBarColor: Int =
                MaterialColors.getColor(context, android.R.attr.statusBarColor, Color.BLACK)
            ColorUtils.setAlphaComponent(opaqueStatusBarColor, EDGE_TO_EDGE_BAR_ALPHA)
        } else {
            Color.TRANSPARENT
        }
    }

    private fun getNavigationBarColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            // A light navigation bar is only supported on O_MR1+.
            // Use a translucent black navigation bar instead.
            val opaqueNavBarColor =
                MaterialColors.getColor(context, android.R.attr.navigationBarColor, Color.BLACK)
            ColorUtils.setAlphaComponent(opaqueNavBarColor, EDGE_TO_EDGE_BAR_ALPHA)
        } else {
            Color.TRANSPARENT
        }
    }

    private fun setLightStatusBar(window: Window, isLight: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = isLight
    }

    private fun setLightNavigationBar(window: Window, isLight: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars = isLight
    }

    private fun isUsingLightSystemBar(
        systemBarColor: Int,
        isLightBackground: Boolean
    ): Boolean {
        return MaterialColors.isColorLight(systemBarColor)
                || (systemBarColor == Color.TRANSPARENT && isLightBackground)
    }

    /**
     * This ensures the [viewGroup] is padded when a navigation bar in button mode is used in
     * landscape configuration as the bar is drawn left or right instead of at the bottom.
     *
     * Note: this should not be used with the Window decor view as it breaks the navigation bar
     * background protection (it will be fully transparent).
     */
    fun configureForEdgeToEdge(viewGroup: ViewGroup) {
        ViewCompat.setOnApplyWindowInsetsListener(viewGroup) { v, insets ->
            // Safe guard to only apply navigation bar insets in landscape view.
            if (v.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Note: do not apply bottom padding as on large screens the navigation bar is
                // displayed opaque on the bottom.
                val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.setPaddingRelative(
                    navBarInsets.left,
                    v.paddingTop,
                    navBarInsets.right,
                    v.paddingBottom
                )
            }
            // Do *not* consume or modify insets so any other views receive them
            // (only required for pre-R, see View.sBrokenInsetsDispatch).
            insets
        }
    }

    /**
     * This ensures ViewPager2 does not consume insets and insets are passed on to other views.
     *
     * Since ViewPager2 1.1.0-beta01 it does custom inset handling in onApplyWindowInsets which
     * consumes insets to prevent child views modifying or consuming insets (which is possible pre-R
     * due to how system insets work, see View.sBrokenInsetsDispatch). However, this prevents other
     * views, like BottomNavigationView, to receive them. As long as no child views part of pages
     * consume or modify insets use this to restore the default behavior.
     */
    fun restoreDefaultWindowInsetsBehavior(viewPager2: ViewPager2) {
        ViewCompat.setOnApplyWindowInsetsListener(viewPager2) { _, insets ->
            insets
        }
    }

    /**
     * Configure app bar to hide content scrolling below it. Use with a toolbar
     * that scrolls out of view.
     */
    fun configureAppBarForContentBelow(activity: Activity) {
        activity.findViewById<AppBarLayout>(R.id.sgAppBarLayout)
            .statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(activity)
    }

    /**
     * Wrapper around [androidx.core.view.OnApplyWindowInsetsListener] which also passes the
     * initial padding or margin set on the view.
     */
    interface OnApplyWindowInsetsInitialPaddingListener {
        /**
         * When [set][View.setOnApplyWindowInsetsListener] on a View, this listener method will be
         * called instead of the view's own [View.onApplyWindowInsets] method. The [initialOffset]
         * is the view's original padding or margin which can be updated and will be applied to the
         * view automatically. This method should return a new [WindowInsetsCompat] with any insets
         * consumed.
         */
        fun onApplyWindowInsets(
            view: View, insets: WindowInsetsCompat, initialOffset: InitialOffset
        ): WindowInsetsCompat
    }

    /** Simple data object to store the initial padding or margin for a view.  */
    data class InitialOffset(
        val start: Int,
        val top: Int,
        val end: Int,
        val bottom: Int
    ) {
        fun applyAsPadding(view: View) = view.setPaddingRelative(start, top, end, bottom)

        fun applyAsMargin(view: View) = view.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = bottom
        }
    }

    /**
     * Sets a window insets dispatch listener and changes the bottom padding to the navigation bar
     * inset.
     */
    fun applyBottomPaddingForNavigationBar(view: View) {
        view.apply {
            // Get the current padding values of the view.
            val initialPadding = InitialOffset(
                paddingStart,
                paddingTop,
                paddingEnd,
                paddingBottom
            )
            // Sets an [androidx.core.view.OnApplyWindowInsetsListener] that calls the custom
            // listener with initial padding values of this view.
            // Note: this is based on similar code of the Material Components ViewUtils class.
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                navigationBarBottomPaddingListener.onApplyWindowInsets(v, insets, initialPadding)
            }
        }
    }

    // Re-use the same instance across all views, there is no view specific state.
    private val navigationBarBottomPaddingListener = object :
        OnApplyWindowInsetsInitialPaddingListener {
        override fun onApplyWindowInsets(
            view: View,
            insets: WindowInsetsCompat,
            initialOffset: InitialOffset
        ): WindowInsetsCompat {
            val navBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            initialOffset
                .copy(bottom = initialOffset.bottom + navBarInsets.bottom)
                .applyAsPadding(view)
            // Do *not* consume or modify insets so any other views receive them
            // (only required for pre-R, see View.sBrokenInsetsDispatch).
            return insets
        }
    }

    /**
     * Sets a window insets dispatch listener and changes the bottom margin to the navigation bar
     * inset.
     */
    fun applyBottomMarginForNavigationBar(view: View) {
        view.apply {
            // Get the current margin values of the view.
            val initialMargins = InitialOffset(
                marginStart,
                marginTop,
                marginEnd,
                marginBottom
            )
            // Sets an [androidx.core.view.OnApplyWindowInsetsListener] that calls the custom
            // listener with initial margin values of this view.
            // Note: this is based on similar code of the Material Components ViewUtils class.
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                navigationBarBottomMarginListener.onApplyWindowInsets(v, insets, initialMargins)
            }
        }
    }

    // Re-use the same instance across all views, there is no view specific state.
    private val navigationBarBottomMarginListener = object :
        OnApplyWindowInsetsInitialPaddingListener {
        override fun onApplyWindowInsets(
            view: View,
            insets: WindowInsetsCompat,
            initialOffset: InitialOffset
        ): WindowInsetsCompat {
            val navBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            initialOffset
                .copy(bottom = initialOffset.bottom + navBarInsets.bottom)
                .applyAsMargin(view)
            // Do *not* consume or modify insets so any other views receive them
            // (only required for pre-R, see View.sBrokenInsetsDispatch).
            return insets
        }
    }

}