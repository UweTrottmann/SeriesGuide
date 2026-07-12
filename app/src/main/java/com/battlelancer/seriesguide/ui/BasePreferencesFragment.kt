// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * A [androidx.preference.PreferenceFragmentCompat] which has a helper method to easily display the
 * current settings value of a [android.preference.ListPreference]. Also configures the RecyclerView
 * displaying the settings to adjust bottom padding for the navigation bar.
 */
abstract class BasePreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        // Add bottom padding to make it obvious where the list ends and to avoid accidental taps
        // on the navigation bar.
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.large_padding)
        recyclerView.updatePadding(bottom = bottomPadding)
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
