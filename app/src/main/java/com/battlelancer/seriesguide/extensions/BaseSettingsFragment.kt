package com.battlelancer.seriesguide.extensions

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * A [android.preference.PreferenceFragment] which has a helper method to easily display the
 * current settings value of a [android.preference.ListPreference].
 */
abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        // Adjust preferences RecyclerView bottom padding to navigation bar height.
        ThemeUtils.applyBottomPaddingForNavigationBar(recyclerView)
        return recyclerView
    }

    companion object {

        private val sSetSummaryListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    val stringValue = newValue.toString()
                    val index = preference.findIndexOfValue(stringValue)

                    // Set the summary to reflect the new value. Escape '%'.
                    preference.setSummary(
                        if (index >= 0) {
                            preference.entries[index].toString()
                                .replace("%".toRegex(), "%%")
                        } else {
                            null
                        }
                    )
                }
                true
            }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         */
        fun bindPreferenceSummaryToValue(
            prefs: SharedPreferences,
            preference: Preference
        ) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sSetSummaryListener

            // Trigger the listener immediately with the preference's current value.
            sSetSummaryListener
                .onPreferenceChange(preference, prefs.getString(preference.key, ""))
        }
    }
}
