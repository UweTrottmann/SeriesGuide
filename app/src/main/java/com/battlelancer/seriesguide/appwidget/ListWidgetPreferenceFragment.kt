package com.battlelancer.seriesguide.appwidget

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.WidgetSettings
import com.battlelancer.seriesguide.util.Utils

/**
 * Shows settings fragment for a specific app widget, hosted inside a [ListWidgetPreferenceActivity]
 * activity.
 */
class ListWidgetPreferenceFragment : PreferenceFragmentCompat() {

    private lateinit var typePref: ListPreference
    private lateinit var showsSortPref: ListPreference
    private lateinit var onlyPremieresPref: CheckBoxPreference
    private lateinit var onlyCollectedPref: CheckBoxPreference
    private lateinit var hideWatchedPreference: CheckBoxPreference
    private lateinit var isInfinitePref: CheckBoxPreference
    private lateinit var themePref: ListPreference
    private lateinit var backgroundPref: ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        val appWidgetId = requireArguments().getInt(ARG_WIDGET_ID)

        // Type.
        typePref = createListPref(
            WidgetSettings.KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId,
            R.string.pref_widget_type,
            R.array.widgetType,
            R.array.widgetTypeData,
            getString(R.string.widget_default_type)
        )

        // Shows only sort order.
        showsSortPref = createListPref(
            WidgetSettings.KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER + appWidgetId,
            R.string.action_shows_sort,
            R.array.widgetShowSortOrder,
            R.array.widgetShowSortOrderData,
            getString(R.string.widget_default_show_sort_order)
        )

        // Show/episode favorites filter.
        val onlyFavoritesPref = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId,
            R.string.only_favorites,
            false
        )

        // Episode only filters.
        onlyPremieresPref = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_ONLY_PREMIERES + appWidgetId,
            R.string.calendar_only_premieres,
            false
        )
        onlyCollectedPref = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_ONLY_COLLECTED + appWidgetId,
            R.string.calendar_only_collected,
            false
        )
        hideWatchedPreference = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId,
            R.string.hide_watched,
            true
        )
        isInfinitePref = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_IS_INFINITE + appWidgetId,
            R.string.pref_infinite_scrolling,
            false
        )
        val isHideWatchedButtonPref = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_IS_HIDE_WATCH_BUTTON + appWidgetId,
            R.string.pref_hide_watch_button,
            false
        )

        // Appearance.
        themePref = createListPref(
            WidgetSettings.KEY_PREFIX_WIDGET_THEME + appWidgetId,
            R.string.pref_theme,
            R.array.widget_themes,
            R.array.widget_themes_data,
            getString(R.string.widget_theme_system)
        )
        backgroundPref = createListPref(
            WidgetSettings.KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId,
            R.string.pref_widget_opacity,
            R.array.widgetOpacity,
            R.array.widgetOpacityData,
            WidgetSettings.DEFAULT_WIDGET_BACKGROUND_OPACITY
        )
        val isLargeFontPref = createCheckBoxPref(
            WidgetSettings.KEY_PREFIX_WIDGET_IS_LARGE_FONT + appWidgetId,
            R.string.pref_large_font,
            false
        )

        // Use settings file specific for this widget.
        preferenceManager.apply {
            sharedPreferencesName = WidgetSettings.SETTINGS_FILE
            sharedPreferencesMode = 0
        }

        // Build the preference screen.
        val prefScreen = preferenceManager.createPreferenceScreen(activity).apply {
            addPreference(typePref)
            addPreference(showsSortPref)
            addPreference(onlyFavoritesPref)
            addPreference(onlyPremieresPref)
            addPreference(onlyCollectedPref)
            addPreference(hideWatchedPreference)
            addPreference(isInfinitePref)
            addPreference(isHideWatchedButtonPref)
            val appearanceCategory = PreferenceCategory(activity).apply {
                setTitle(R.string.pref_appearance)
            }
            // Need to add to screen first so added prefs can get unique IDs.
            addPreference(appearanceCategory)
            appearanceCategory.apply {
                addPreference(themePref)
                addPreference(backgroundPref)
                addPreference(isLargeFontPref)
            }
        }
        preferenceScreen = prefScreen

        // After restoring any values prevent persisting changes (only save on user command).
        disablePersistence(prefScreen)

        // Display current value as summary.
        bindPreferenceSummaryToValue(typePref)
        bindPreferenceSummaryToValue(showsSortPref)
        bindPreferenceSummaryToValue(backgroundPref)
        bindPreferenceSummaryToValue(themePref)

        // Disable saving some prefs not available for non-supporters.
        if (!Utils.hasAccessToX(activity)) {
            val onDisablePreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                    Utils.advertiseSubscription(activity)
                    false
                }
            typePref.apply {
                onPreferenceChangeListener = onDisablePreferenceChangeListener
                setSummary(R.string.onlyx)
            }
            backgroundPref.apply {
                onPreferenceChangeListener = onDisablePreferenceChangeListener
                setSummary(R.string.onlyx)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger listener to update pref states based on current type.
        preferenceChangeListener.onPreferenceChange(typePref, typePref.value)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.widget_config_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save -> {
                saveAllPreferences() // Persistence is disabled, save manually.
                (activity as ListWidgetPreferenceActivity).updateWidget()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createCheckBoxPref(
        key: String,
        @StringRes titleRes: Int,
        defaultValue: Boolean
    ): CheckBoxPreference {
        return CheckBoxPreference(activity).also {
            it.key = key
            it.setTitle(titleRes)
            it.setDefaultValue(defaultValue)
        }
    }

    private fun createListPref(
        key: String,
        @StringRes title: Int,
        @ArrayRes entries: Int,
        @ArrayRes values: Int,
        defaultValue: String
    ): ListPreference {
        return ListPreference(activity).also {
            it.key = key
            it.setTitle(title)
            it.setEntries(entries)
            it.setEntryValues(values)
            it.setDefaultValue(defaultValue)
            it.positiveButtonText = null
            it.negativeButtonText = null
        }
    }

    /**
     * Walks through all preferences of the [group] and sets [Preference.setPersistent] false.
     */
    private fun disablePersistence(group: PreferenceGroup) {
        val count = group.preferenceCount
        for (i in 0 until count) {
            val pref = group.getPreference(i)
            if (pref is PreferenceGroup) {
                disablePersistence(pref)
            } else {
                pref.isPersistent = false
            }
        }
    }

    private fun saveAllPreferences() {
        preferenceManager.sharedPreferences.edit {
            savePreferences(preferenceScreen, this)
        }
    }

    private fun savePreferences(
        group: PreferenceGroup,
        editor: SharedPreferences.Editor
    ) {
        val count = group.preferenceCount
        for (i in 0 until count) {
            val pref = group.getPreference(i)
            val key = pref.key
            when (pref) {
                is PreferenceGroup -> {
                    savePreferences(pref, editor)
                }
                is CheckBoxPreference -> {
                    editor.putBoolean(key, pref.isChecked)
                }
                is ListPreference -> {
                    editor.putString(key, pref.value)
                }
            }
        }
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    private fun bindPreferenceSummaryToValue(preference: ListPreference) {
        // Set the listener to watch for value changes.
        preference.onPreferenceChangeListener = preferenceChangeListener

        // Trigger the listener immediately with the preference's current value.
        preferenceChangeListener.onPreferenceChange(preference, preference.value)
    }

    private val preferenceChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            // Enable/disable settings that (do not) apply to shows, if type is shows.
            if (typePref.key == preference.key) {
                val newTypeValue = newValue as String
                val displayingShows = getString(R.string.widget_type_shows) == newTypeValue
                showsSortPref.isEnabled = displayingShows
                onlyPremieresPref.isEnabled = !displayingShows
                onlyCollectedPref.isEnabled = !displayingShows
                hideWatchedPreference.isEnabled = !displayingShows
                isInfinitePref.isEnabled = !displayingShows
            }

            // If system theme is used disable unsupported transparency setting.
            if (themePref.key == preference.key) {
                val newTypeValue = newValue as String
                val isSystemTheme = getString(R.string.widget_theme_system) == newTypeValue
                backgroundPref.isEnabled = !isSystemTheme
                if (isSystemTheme) {
                    backgroundPref.value = WidgetSettings.DEFAULT_WIDGET_BACKGROUND_OPACITY
                    // Need to manually trigger change listener for whatever reason.
                    backgroundPref.onPreferenceChangeListener.onPreferenceChange(
                        backgroundPref,
                        backgroundPref.value
                    )
                }
            }

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val stringValue = newValue.toString()
                val index = preference.findIndexOfValue(stringValue)
                // Set the summary to reflect the new value. Escape '%'.
                preference.setSummary(
                    if (index >= 0) {
                        preference.entries[index].toString().replace("%".toRegex(), "%%")
                    } else null
                )
            }

            true
        }

    companion object {

        private const val ARG_WIDGET_ID = "appWidgetId"

        fun newInstance(appWidgetId: Int): ListWidgetPreferenceFragment {
            return ListWidgetPreferenceFragment().apply {
                arguments = bundleOf(ARG_WIDGET_ID to appWidgetId)
            }
        }

    }
}