// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.preferences

import android.app.backup.BackupManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.settings.UpdateSettings
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.streaming.StreamingSearchConfigureDialog
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.ui.BasePreferencesFragment
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.color.DynamicColors
import com.uwetrottmann.androidutils.AndroidUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SgPreferencesFragment : BasePreferencesFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val settings = arguments?.getString(PreferencesActivityImpl.EXTRA_SETTINGS_SCREEN)
        when (settings) {
            null -> {
                setPreferencesFromResource(R.xml.settings_root, rootKey)
                setupRootSettings()
            }
            KEY_SCREEN_BASIC -> {
                setPreferencesFromResource(R.xml.settings_basic, rootKey)
                setupBasicSettings()
            }
            KEY_SCREEN_NOTIFICATIONS -> {
                setPreferencesFromResource(R.xml.settings_notifications, rootKey)
                setupNotificationSettings()
            }
        }
    }

    private fun setupRootSettings() {
        // Clear image cache
        findPreference<Preference>(KEY_CLEAR_CACHE)!!.setOnPreferenceClickListener {
            // try to open app info where user can clear app cache folders
            var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + requireActivity().packageName)
            if (!Utils.tryStartActivity(activity, intent, false)) {
                // try to open all apps view if detail view not available
                intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                Utils.tryStartActivity(activity, intent, true)
            }

            true
        }

        findPreference<SwitchPreferenceCompat>(DisplaySettings.KEY_DYNAMIC_COLOR)!!.apply {
            if (DynamicColors.isDynamicColorAvailable()) {
                setOnPreferenceChangeListener { _, _ ->
                    restartApp()
                    true
                }
            } else {
                isVisible = false
            }
        }

        if (BuildConfig.DEBUG) {
            findPreference<SwitchPreferenceCompat>(AppSettings.KEY_USER_DEBUG_MODE_ENBALED)!!.apply {
                isEnabled = false
                isChecked = true
            }
        }
    }

    /**
     *  Restart to apply new theme, go back to this settings screen.
     *  This will lose the existing task stack, but that's fine.
     */
    private fun restartApp() {
        TaskStackBuilder.create(requireContext())
            .addNextIntent(Intent(activity, ShowsActivity::class.java))
            .addNextIntent(Intent(activity, MoreOptionsActivity::class.java))
            .addNextIntent(requireActivity().intent)
            .startActivities()
    }

    private fun updateRootSettings() {
        val hasAccessToX = Utils.hasAccessToX(activity)

        // notifications link
        findPreference<Preference>(KEY_SCREEN_NOTIFICATIONS)!!.apply {
            if (hasAccessToX && NotificationSettings.isNotificationsEnabled(requireContext())) {
                summary = NotificationSettings.getLatestToIncludeTresholdValue(requireContext())
            } else {
                setSummary(R.string.pref_notificationssummary)
            }
        }

        // Theme switcher
        findPreference<ListPreference>(DisplaySettings.KEY_THEME)!!.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                if (DisplaySettings.KEY_THEME == preference.key) {
                    ThemeUtils.updateTheme(newValue as String)
                }
                true
            }
            setListPreferenceSummary(this)
        }

        // show currently set values for list prefs
        setListPreferenceSummary(findPreference(DisplaySettings.KEY_NUMBERFORMAT))

        // set current value of auto-update pref
        findPreference<SwitchPreferenceCompat>(UpdateSettings.KEY_AUTOUPDATE)!!.isChecked =
            SgSyncAdapter.isSyncAutomatically(requireContext())
    }

    private fun setupNotificationSettings() {
        findPreference<Preference>(KEY_BATTERY_SETTINGS)?.setOnPreferenceClickListener {
            // Try to open app info where user can configure battery settings.
            var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + requireActivity().packageName))
            if (!Utils.tryStartActivity(activity, intent, false)) {
                // Open all apps view if detail view not available.
                intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                Utils.tryStartActivity(activity, intent, true)
            }
            true
        }
        findPreference<Preference>(KEY_PRECISE_NOTIFICATION_SETTINGS)?.setOnPreferenceClickListener {
            // Note: the preference is only shown on Android 12+.
            if (AndroidUtils.isAtLeastS) {
                // Try to open the exact alarm settings.
                Utils.tryStartActivity(
                    activity,
                    NotificationSettings.buildRequestExactAlarmSettingsIntent(requireContext()),
                    true
                )
            }
            true
        }
    }

    private fun updateNotificationSettings() {
        updateThresholdSummary(findPreference(NotificationSettings.KEY_THRESHOLD)!!)
        updateSelectionSummary(findPreference(NotificationSettings.KEY_SELECTION)!!)

        val isSupporter = Utils.hasAccessToX(requireContext())
        if (isSupporter) {
            // Disable advanced notification settings if notifications are disabled.
            enableAdvancedNotificationSettings(
                NotificationSettings.isNotificationsEnabled(requireContext())
            )
        } else {
            enableAdvancedNotificationSettings(false)
        }

        if (AndroidUtils.isAtLeastOreo) {
            // Android 8+: use system settings to manage notifications.
            val channelsPref: Preference = findPreference(NotificationSettings.KEY_CHANNELS)!!
            if (isSupporter) {
                if (NotificationSettings.areNotificationsAllowed(requireContext())) {
                    channelsPref.summary = null
                } else {
                    channelsPref.setSummary(R.string.notifications_allow_reason)
                }
                channelsPref.setOnPreferenceClickListener {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                    // At least NVIDIA Shield (8.0.0) can not handle this, so guard.
                    Utils.tryStartActivity(activity, intent, true)
                    true
                }
            } else {
                channelsPref.setSummary(R.string.onlyx)
                channelsPref.setOnPreferenceClickListener {
                    Utils.advertiseSubscription(requireContext())
                    true
                }
            }
        } else {
            val enabledPref: SwitchPreferenceCompat =
                findPreference(NotificationSettings.KEY_ENABLED)!!
            if (isSupporter) {
                enabledPref.setSummary(R.string.pref_notificationssummary)
                enabledPref.setOnPreferenceChangeListener { _, newValue ->
                    val isChecked = newValue as Boolean
                    enableAdvancedNotificationSettings(isChecked)
                    // Schedule or remove notification service alarm.
                    NotificationService.trigger(requireContext())
                    true
                }
            } else {
                enabledPref.setSummary(R.string.onlyx)
                enabledPref.setOnPreferenceChangeListener { _, _ ->
                    Utils.advertiseSubscription(requireContext())
                    // prevent value from getting saved
                    false
                }
                enabledPref.isChecked = false
            }
        }
    }

    private fun enableAdvancedNotificationSettings(isEnabled: Boolean) {
        val thresholdPref: Preference = findPreference(NotificationSettings.KEY_THRESHOLD)!!
        val selectionPref: Preference = findPreference(NotificationSettings.KEY_SELECTION)!!
        val hiddenPref: Preference = findPreference(NotificationSettings.KEY_IGNORE_HIDDEN)!!
        val onlyNextPref: Preference = findPreference(NotificationSettings.KEY_ONLY_NEXT_EPISODE)!!
        thresholdPref.isEnabled = isEnabled
        selectionPref.isEnabled = isEnabled
        hiddenPref.isEnabled = isEnabled
        onlyNextPref.isEnabled = isEnabled
        if (!AndroidUtils.isAtLeastOreo) {
            // Pre-Android 8.0 notification settings, managed by system on newer versions.
            val vibratePref: Preference = findPreference(NotificationSettings.KEY_VIBRATE)!!
            val ringtonePref: Preference = findPreference(NotificationSettings.KEY_RINGTONE)!!
            vibratePref.isEnabled = isEnabled
            ringtonePref.isEnabled = isEnabled
        }
    }

    private fun setupBasicSettings() {
        // show currently set values for some prefs
        updateStreamSearchServiceSummary(findPreference(StreamingSearch.KEY_SETTING_REGION)!!)
        updateTimeOffsetSummary(findPreference(DisplaySettings.KEY_SHOWS_TIME_OFFSET)!!)

        findPreference<Preference>(ShowsSettings.KEY_LANGUAGE_FALLBACK)!!.also {
            updateFallbackLanguageSummary(it)
            it.setOnPreferenceClickListener {
                L10nDialogFragment.show(
                    parentFragmentManager,
                    ShowsSettings.getShowsLanguageFallback(requireContext()),
                    TAG_LANGUAGE_FALLBACK,
                    titleRes = R.string.pref_language_fallback
                )
                true
            }
        }

    }

    override fun onStart() {
        super.onStart()

        // update summary values not handled by onSharedPreferenceChanged
        val settings = arguments?.getString(PreferencesActivityImpl.EXTRA_SETTINGS_SCREEN)
        if (settings == null) {
            updateRootSettings()
        } else if (settings == KEY_SCREEN_NOTIFICATIONS) {
            // Android 8+: notification settings depend on system settings, update on showing this.
            updateNotificationSettings()
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext()).also {
            it.registerOnSharedPreferenceChangeListener(this)
        }
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        PreferenceManager.getDefaultSharedPreferences(requireContext()).also {
            it.unregisterOnSharedPreferenceChangeListener(this)
        }
        EventBus.getDefault().unregister(this)
    }

    override fun onPreferenceTreeClick(
        preference: Preference
    ): Boolean {
        val key = preference.key ?: return super.onPreferenceTreeClick(preference)

        // other screens
        if (key.startsWith("screen_")) {
            (activity as SeriesGuidePreferences).switchToSettings(key)
            return true
        }

        // links
        when (key) {
            LINK_KEY_AUTOBACKUP -> {
                startActivity(DataLiberationActivity.intentToShowAutoBackup(requireActivity()))
                return true
            }
            LINK_KEY_DATALIBERATION -> {
                startActivity(Intent(activity, DataLiberationActivity::class.java))
                return true
            }
        }// fall through

        // settings
        val supportFragmentManager = (activity as AppCompatActivity)
            .supportFragmentManager
        if (NotificationSettings.KEY_THRESHOLD == key) {
            NotificationThresholdDialogFragment().safeShow(
                supportFragmentManager, "notification-threshold"
            )
            return true
        }
        if (NotificationSettings.KEY_SELECTION == key) {
            NotificationSelectionDialogFragment().safeShow(
                supportFragmentManager, "notification-selection"
            )
            return true
        }
        if (StreamingSearch.KEY_SETTING_REGION == key) {
            StreamingSearchConfigureDialog().safeShow(
                supportFragmentManager, "streaming-service"
            )
            return true
        }
        if (DisplaySettings.KEY_SHOWS_TIME_OFFSET == key) {
            TimeOffsetDialogFragment().safeShow(
                supportFragmentManager, "time-offset"
            )
            return true
        }
        if (NotificationSettings.KEY_RINGTONE == key) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                // show silent and default options
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(
                    RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI
                )

            // restore selected sound or silent (empty string)
            val existingValue = NotificationSettings.getNotificationsRingtone(requireContext())
            intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                if (TextUtils.isEmpty(existingValue)) null else Uri.parse(existingValue)
            )

            Utils.tryStartActivityForResult(this, intent, REQUEST_CODE_RINGTONE)
            return true
        }
        if (AppSettings.KEY_USER_DEBUG_MODE_ENBALED == key) {
            Toast.makeText(
                context, R.string.pref_user_debug_mode_note, Toast.LENGTH_LONG
            ).show()
            return false // Let the pref handle the click (and change its value).
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (REQUEST_CODE_RINGTONE == requestCode) {
            if (data != null) {
                // Ringtone preference only used before Android 8.0, no need to use new API.
                @Suppress("DEPRECATION") var ringtoneUri: Uri? = data.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI
                )
                if (AndroidUtils.isNougatOrHigher) {
                    // Xiaomi devices incorrectly return file:// uris on N
                    // protect against FileUriExposedException
                    if (ringtoneUri != null /* not silent */ && "content" != ringtoneUri.scheme) {
                        ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI
                    }
                }
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    // map silent (null) to empty string
                    putString(NotificationSettings.KEY_RINGTONE, ringtoneUri?.toString() ?: "")
                }
            }
            return
        }
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null) {
            return // Preferences were cleared, do nothing.
        }
        val pref: Preference? = findPreference(key)
        if (pref != null) {
            BackupManager(pref.context).dataChanged()

            // update pref summary text
            if (DisplaySettings.KEY_NUMBERFORMAT == key
                || DisplaySettings.KEY_THEME == key) {
                setListPreferenceSummary(pref as ListPreference)
            }
            if (ShowsSettings.KEY_LANGUAGE_FALLBACK == key) {
                updateFallbackLanguageSummary(pref)
            }
            if (DisplaySettings.KEY_SHOWS_TIME_OFFSET == key) {
                updateTimeOffsetSummary(pref)
            }
            if (NotificationSettings.KEY_THRESHOLD == key) {
                updateThresholdSummary(pref)
            }
            if (NotificationSettings.KEY_VIBRATE == key
                && NotificationSettings.isNotificationVibrating(pref.context)) {
                // demonstrate vibration pattern used by SeriesGuide
                val vibrator = requireActivity().getSystemService<Vibrator>()
                @Suppress("DEPRECATION") // Not visible on O+, no need to use new API.
                vibrator?.vibrate(NotificationService.VIBRATION_PATTERN, -1)
            }
            if (StreamingSearch.KEY_SETTING_REGION == key) {
                updateStreamSearchServiceSummary(pref)
            }
        }

        // pref changes that require the notification service to be reset
        if (DisplaySettings.KEY_SHOWS_TIME_OFFSET == key
            || NotificationSettings.KEY_THRESHOLD == key) {
            resetAndRunNotificationsService(requireActivity())
        }

        // pref changes that require the widgets to be updated
        if (DisplaySettings.KEY_SHOWS_TIME_OFFSET == key
            || DisplaySettings.KEY_HIDE_SPECIALS == key
            || DisplaySettings.KEY_DISPLAY_EXACT_DATE == key
            || DisplaySettings.KEY_PREVENT_SPOILERS == key) {
            // update any widgets
            ListWidgetProvider.notifyDataChanged(requireActivity())
        }

        if (ShowsSettings.KEY_LANGUAGE_FALLBACK == key) {
            // reset last updated date of all episodes so they will get updated
            Thread {
                android.os.Process
                    .setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                SgRoomDatabase.getInstance(requireContext()).sgEpisode2Helper()
                    .resetLastUpdatedForAll()
            }.start()
        }

        // Toggle auto-update on SyncAdapter
        if (UpdateSettings.KEY_AUTOUPDATE == key) {
            if (pref != null) {
                val autoUpdatePref = pref as SwitchPreferenceCompat
                SgSyncAdapter.setSyncAutomatically(requireContext(), autoUpdatePref.isChecked)
            }
        }

        if (AppSettings.KEY_SEND_ERROR_REPORTS == key) {
            pref?.also {
                val switchPref = pref as SwitchPreferenceCompat
                AppSettings.setSendErrorReports(switchPref.context, switchPref.isChecked, false)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(@Suppress("UNUSED_PARAMETER") event: PreferencesActivityImpl.UpdateSummariesEvent) {
        if (!isResumed) {
            return
        }
        // update summary values not handled by onSharedPreferenceChanged
        val settings = arguments?.getString(PreferencesActivityImpl.EXTRA_SETTINGS_SCREEN)
        if (settings != null && settings == KEY_SCREEN_NOTIFICATIONS) {
            updateNotificationSettings()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: L10nDialogFragment.LanguageChangedEvent) {
        if (event.tag == TAG_LANGUAGE_FALLBACK) {
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putString(ShowsSettings.KEY_LANGUAGE_FALLBACK, event.selectedLanguageCode)
            }
        }
    }

    private fun setListPreferenceSummary(listPref: ListPreference?) {
        if (listPref == null) {
            return
        }
        // Set summary to be the user-description for the selected value
        val entry = listPref.entry
        listPref.summary = entry?.toString()?.replace("%".toRegex(), "%%") ?: ""
    }

    private fun updateThresholdSummary(thresholdPref: Preference) {
        thresholdPref.summary = NotificationSettings.getLatestToIncludeTresholdValue(
            thresholdPref.context
        )
    }

    private fun updateSelectionSummary(selectionPref: Preference) {
        val countOfShowsNotifyOn =
            SgRoomDatabase.getInstance(requireContext()).sgShow2Helper().countShowsNotifyEnabled()
        selectionPref.summary = getString(
            R.string.pref_notifications_select_shows_summary,
            countOfShowsNotifyOn
        )
    }

    private fun updateStreamSearchServiceSummary(pref: Preference) {
        pref.summary = StreamingSearch.getCurrentRegionOrSelectString(requireContext())
    }

    private fun updateTimeOffsetSummary(offsetListPref: Preference) {
        offsetListPref.summary = getString(
            R.string.pref_offsetsummary,
            DisplaySettings.getShowsTimeOffset(requireContext())
        )
    }

    private fun updateFallbackLanguageSummary(pref: Preference) {
        pref.summary = LanguageTools.getShowLanguageStringFor(
            requireContext(),
            ShowsSettings.getShowsLanguageFallback(requireContext())
        )
    }

    /**
     * Resets and runs the notification service to take care of potential time shifts when e.g.
     * changing the time offset.
     */
    private fun resetAndRunNotificationsService(context: Context) {
        NotificationSettings.resetLastEpisodeAirtime(context)
        NotificationService.trigger(context)
    }

    companion object {

        // Preference keys
        private const val KEY_CLEAR_CACHE = "clearCache"

        //    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";
        //    public static final String KEY_TAPE_INTERVAL = "com.battlelancer.seriesguide.tapeinterval";
        private const val KEY_BATTERY_SETTINGS =
            "com.battlelancer.seriesguide.notifications.battery"
        private const val KEY_PRECISE_NOTIFICATION_SETTINGS =
            "com.battlelancer.seriesguide.notifications.notifications.precise"

        // links
        private const val LINK_BASE_KEY = "com.battlelancer.seriesguide.settings."
        private const val LINK_KEY_AUTOBACKUP = LINK_BASE_KEY + "autobackup"
        private const val LINK_KEY_DATALIBERATION = LINK_BASE_KEY + "dataliberation"

        private const val KEY_SCREEN_BASIC = "screen_basic"
        private const val KEY_SCREEN_NOTIFICATIONS = "screen_notifications"

        private const val REQUEST_CODE_RINGTONE = 0
        private const val TAG_LANGUAGE_FALLBACK = "PREF_LANGUAGE_FALLBACK"

    }
}
