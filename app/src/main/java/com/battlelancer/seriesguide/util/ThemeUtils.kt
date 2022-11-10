package com.battlelancer.seriesguide.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
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

    @JvmStatic
    fun getColorFromAttribute(context: Context, @AttrRes attribute: Int): Int {
        return ContextCompat.getColor(
            context,
            Utils.resolveAttributeToResourceId(context.theme, attribute)
        )
    }

    @JvmStatic
    fun SlidingTabLayout.setDefaultStyle() {
        setCustomTabView(R.layout.tabstrip_item_transparent, R.id.textViewTabStripItem)
        setSelectedIndicatorColors(getColorFromAttribute(context, R.attr.colorPrimary))
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
     * ViewGroup only dispatches windows insets starting from the first child until one
     * consumes them. So it may be necessary to manually dispatch insets to all children, e.g. a
     * LinearLayout containing an app bar and a BottomNavigationView is a common case.
     * See [dispatchWindowInsetsToAllChildren].
     *
     * Note: on Android 10+ BottomNavigationView still does draw behind the navigation bar using
     * button mode, however, the system adds an almost opaque scrim so color is barely noticeable.
     *
     * Scroll views or RecyclerViews should add bottom padding matching the navigation bar, see
     * [applySystemBarInset].
     */
    fun configureEdgeToEdge(window: Window) {
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
            isUsingLightSystemBar(statusBarColor, isLightBackground)
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
     * Configure app bar to hide content scrolling below it. Use with a toolbar
     * that scrolls out of view.
     */
    fun configureAppBarForContentBelow(activity: Activity) {
        activity.findViewById<AppBarLayout>(R.id.sgAppBarLayout)
            .statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(activity)
    }

    /**
     * Sets a window insets dispatch listener and changes the bottom padding to the system bar
     * inset. Consumes the insets so no children of the given view will receive them.
     */
    fun applySystemBarInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemBarInsets.bottom
            )
            // Return CONSUMED to not pass the window insets down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * ViewGroup only dispatches windows insets starting from the first child until one
     * consumes them. Call this to manually dispatch insets to all direct children.
     */
    fun dispatchWindowInsetsToAllChildren(viewGroup: ViewGroup) {
        ViewCompat.setOnApplyWindowInsetsListener(viewGroup) { v, insets ->
            var consumed = false
            (v as ViewGroup).forEach { child ->
                val childResult = ViewCompat.dispatchApplyWindowInsets(child, insets)
                if (childResult.isConsumed) {
                    consumed = true
                }
            }
            if (consumed) WindowInsetsCompat.CONSUMED else insets
        }
    }
}