package com.battlelancer.seriesguide.util

import android.content.Context
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import kotlinx.coroutines.delay

/**
 * Helps highlight a toolbar item.
 */
object HighlightTools {

    private const val BASE_PREF_KEY = "com.uwetrottmann.seriesguide.seenFeature."

    enum class Feature(val index: Int) {
        SHOW_FILTER(1),
        MOVIE_FILTER(2)
    }

    fun shouldHighlight(context: Context, feature: Feature): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(BASE_PREF_KEY + feature.index, false)
    }

    fun setSeen(context: Context, feature: Feature) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(BASE_PREF_KEY + feature.index, true)
        }
    }

    fun highlightSgToolbarItem(
        feature: Feature,
        activity: AppCompatActivity,
        scope: LifecycleCoroutineScope,
        @IdRes menuItemId: Int,
        @StringRes textRes: Int,
        condition: () -> Boolean
    ) {
        if (shouldHighlight(activity, feature)) {
            scope.launchWhenResumed {
                if (!condition()) {
                    return@launchWhenResumed
                }
                // Instead of a complicated global layout listener setup,
                // just check a few times for the menu item to exist.
                val toolbar = activity.findViewById<Toolbar>(R.id.sgToolbar)
                var menuItem: View? = null
                for (i in 0 until 10) {
                    menuItem = toolbar.findViewById(menuItemId)
                    if (menuItem != null) break
                    delay(100)
                }
                menuItem?.let {
                    TapTargetView.showFor(
                        activity,
                        TapTarget.forView(it, activity.getString(textRes)),
                        object : TapTargetView.Listener() {
                            override fun onTargetDismissed(
                                view: TapTargetView?,
                                userInitiated: Boolean
                            ) {
                                setSeen(activity, feature)
                            }
                        }
                    )
                }
            }
        }
    }

}