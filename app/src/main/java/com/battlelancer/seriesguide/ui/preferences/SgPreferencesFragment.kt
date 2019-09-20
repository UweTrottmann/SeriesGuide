package com.battlelancer.seriesguide.ui.preferences

import android.app.backup.BackupManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.backend.CloudSetupActivity
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.billing.BillingActivity
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.settings.UpdateSettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.streaming.StreamingSearchConfigureDialog
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.dialogs.NotificationSelectionDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.NotificationThresholdDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.TimeOffsetDialogFragment
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Shadows
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SgPreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val settings = arguments?.getString(SeriesGuidePreferences.EXTRA_SETTINGS_SCREEN)
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
            intent.data = Uri.parse("package:" + activity!!.packageName)
            if (!Utils.tryStartActivity(activity, intent, false)) {
                // try to open all apps view if detail view not available
                intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                Utils.tryStartActivity(activity, intent, true)
            }

            true
        }

        // display version as About summary
        findPreference<Preference>(KEY_ABOUT)!!.summary = Utils.getVersionString(activity)
    }

    private fun updateRootSettings() {
        val hasAccessToX = Utils.hasAccessToX(activity)

        // unlock all link
        findPreference<Preference>(LINK_KEY_UPGRADE)!!.apply {
            summary = if (hasAccessToX) getString(R.string.upgrade_success) else null
        }

        // notifications link
        findPreference<Preference>(KEY_SCREEN_NOTIFICATIONS)!!.apply {
            if (hasAccessToX && NotificationSettings.isNotificationsEnabled(activity)) {
                summary = NotificationSettings.getLatestToIncludeTresholdValue(activity)
            } else {
                setSummary(R.string.pref_notificationssummary)
            }
        }

        // SeriesGuide Cloud link
        findPreference<Preference>(LINK_KEY_CLOUD)!!.apply {
            if (hasAccessToX && HexagonSettings.isEnabled(activity)) {
                summary = HexagonSettings.getAccountName(activity)
            } else {
                setSummary(R.string.hexagon_description)
            }
        }

        // trakt link
        findPreference<Preference>(LINK_KEY_TRAKT)!!.apply {
            if (TraktCredentials.get(activity).hasCredentials()) {
                summary = TraktCredentials.get(activity).username
            } else {
                summary = null
            }
        }

        // Theme switcher
        findPreference<ListPreference>(DisplaySettings.KEY_THEME)!!.apply {
            if (hasAccessToX) {
                setOnPreferenceChangeListener { preference, newValue ->
                    if (DisplaySettings.KEY_THEME == preference.key) {
                        ThemeUtils.updateTheme(newValue as String)
                        Shadows.getInstance().resetShadowColor()

                        // restart to apply new theme, go back to this settings screen
                        TaskStackBuilder.create(activity!!)
                            .addNextIntent(Intent(activity, ShowsActivity::class.java))
                            .addNextIntent(activity!!.intent)
                            .startActivities()
                    }
                    true
                }
                setListPreferenceSummary(this)
            } else {
                onPreferenceChangeListener = sNoOpChangeListener
                setSummary(R.string.onlyx)
            }
        }

        // show currently set values for list prefs
        setListPreferenceSummary(findPreference(DisplaySettings.KEY_NUMBERFORMAT))

        // set current value of auto-update pref
        findPreference<SwitchPreferenceCompat>(UpdateSettings.KEY_AUTOUPDATE)!!.isChecked =
            SgSyncAdapter.isSyncAutomatically(activity)
    }

    private fun setupNotificationSettings() {
        val enabledPref: SwitchPreferenceCompat = findPreference(NotificationSettings.KEY_ENABLED)!!
        val thresholdPref: Preference = findPreference(NotificationSettings.KEY_THRESHOLD)!!
        val selectionPref: Preference = findPreference(NotificationSettings.KEY_SELECTION)!!
        val hiddenPref: Preference = findPreference(NotificationSettings.KEY_IGNORE_HIDDEN)!!
        // only visible pre-O
        val vibratePref: Preference? = findPreference(NotificationSettings.KEY_VIBRATE)
        val ringtonePref: Preference? = findPreference(NotificationSettings.KEY_RINGTONE)
        // only visible O+
        val channelsPref: Preference? = findPreference(NotificationSettings.KEY_CHANNELS)

        // allow supporters to enable notifications
        if (Utils.hasAccessToX(activity)) {
            enabledPref.setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue as Boolean

                thresholdPref.isEnabled = isChecked
                selectionPref.isEnabled = isChecked
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channelsPref?.isEnabled = isChecked
                } else {
                    vibratePref?.isEnabled = isChecked
                    ringtonePref?.isEnabled = isChecked
                }

                NotificationService.trigger(activity)
                true
            }
            // disable advanced notification settings if notifications are disabled
            val isNotificationsEnabled = NotificationSettings.isNotificationsEnabled(
                activity
            )
            thresholdPref.isEnabled = isNotificationsEnabled
            selectionPref.isEnabled = isNotificationsEnabled
            hiddenPref.isEnabled = isNotificationsEnabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channelsPref?.isEnabled = isNotificationsEnabled
            } else {
                vibratePref?.isEnabled = isNotificationsEnabled
                ringtonePref?.isEnabled = isNotificationsEnabled
            }
        } else {
            enabledPref.onPreferenceChangeListener = sNoOpChangeListener
            enabledPref.isChecked = false
            enabledPref.setSummary(R.string.onlyx)
            thresholdPref.isEnabled = false
            selectionPref.isEnabled = false
            hiddenPref.isEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channelsPref?.isEnabled = false
            } else {
                vibratePref?.isEnabled = false
                ringtonePref?.isEnabled = false
            }
        }

        updateThresholdSummary(thresholdPref)
        updateNotificationSettings()
    }

    private fun updateNotificationSettings() {
        updateSelectionSummary(findPreference(NotificationSettings.KEY_SELECTION)!!)
    }

    private fun setupBasicSettings() {
        // show currently set values for some prefs
        updateStreamSearchServiceSummary(findPreference(StreamingSearch.KEY_SETTING_SERVICE)!!)
        setListPreferenceSummary(findPreference(DisplaySettings.KEY_LANGUAGE_FALLBACK))
        updateTimeOffsetSummary(findPreference(DisplaySettings.KEY_SHOWS_TIME_OFFSET)!!)
    }

    override fun onStart() {
        super.onStart()

        // update summary values not handled by onSharedPreferenceChanged
        val settings = arguments?.getString(SeriesGuidePreferences.EXTRA_SETTINGS_SCREEN)
        if (settings == null) {
            updateRootSettings()
        }

        PreferenceManager.getDefaultSharedPreferences(activity).also {
            it.registerOnSharedPreferenceChangeListener(this)
        }
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        PreferenceManager.getDefaultSharedPreferences(activity).also {
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
            LINK_KEY_UPGRADE -> {
                startActivity(
                    Intent(
                        activity, if (Utils.isAmazonVersion()) {
                            AmazonBillingActivity::class.java
                        } else {
                            BillingActivity::class.java
                        }
                    )
                )
                return true
            }
            LINK_KEY_CLOUD -> {
                startActivity(Intent(activity, CloudSetupActivity::class.java))
                return true
            }
            LINK_KEY_TRAKT -> {
                startActivity(Intent(activity, ConnectTraktActivity::class.java))
                return true
            }
            LINK_KEY_AUTOBACKUP -> {
                startActivity(
                    Intent(activity, DataLiberationActivity::class.java).putExtra(
                        DataLiberationActivity.InitBundle.EXTRA_SHOW_AUTOBACKUP, true
                    )
                )
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
        if (StreamingSearch.KEY_SETTING_SERVICE == key) {
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
            val existingValue = NotificationSettings.getNotificationsRingtone(activity)
            intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                if (TextUtils.isEmpty(existingValue)) null else Uri.parse(existingValue)
            )

            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
            return true
        }
        if (NotificationSettings.KEY_CHANNELS == key) {
            // launch system settings app at settings for episodes channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, SgApp.NOTIFICATION_CHANNEL_EPISODES)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity!!.packageName)
                // at least NVIDIA Shield (8.0.0) can not handle this, so guard
                Utils.tryStartActivity(activity, intent, true)
            }
            return true
        }
        if (KEY_ABOUT == key) {
            val ft = fragmentManager!!.beginTransaction()
            ft.replace(R.id.containerSettings, AboutPreferencesFragment())
            ft.addToBackStack(null)
            ft.commit()
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (REQUEST_CODE_RINGTONE == requestCode) {
            if (data != null) {
                var ringtoneUri: Uri? = data.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Xiaomi devices incorrectly return file:// uris on N
                    // protect against FileUriExposedException
                    if (ringtoneUri != null /* not silent */ && "content" != ringtoneUri.scheme) {
                        ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI
                    }
                }
                PreferenceManager.getDefaultSharedPreferences(activity).edit {
                    // map silent (null) to empty string
                    putString(NotificationSettings.KEY_RINGTONE, ringtoneUri?.toString() ?: "")
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val pref: Preference? = findPreference(key)
        if (pref != null) {
            BackupManager(pref.context).dataChanged()

            // update pref summary text
            if (DisplaySettings.KEY_LANGUAGE_FALLBACK == key
                || DisplaySettings.KEY_NUMBERFORMAT == key
                || DisplaySettings.KEY_THEME == key) {
                setListPreferenceSummary(pref as ListPreference)
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
                val vibrator = activity!!.getSystemService<Vibrator>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            NotificationService.VIBRATION_PATTERN,
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION") // Using new API above.
                    vibrator?.vibrate(NotificationService.VIBRATION_PATTERN, -1)
                }
            }
            if (StreamingSearch.KEY_SETTING_SERVICE == key) {
                updateStreamSearchServiceSummary(pref)
            }
        }

        // pref changes that require the notification service to be reset
        if (DisplaySettings.KEY_SHOWS_TIME_OFFSET == key
            || NotificationSettings.KEY_THRESHOLD == key) {
            resetAndRunNotificationsService(activity!!)
        }

        // pref changes that require the widgets to be updated
        if (DisplaySettings.KEY_SHOWS_TIME_OFFSET == key
            || DisplaySettings.KEY_HIDE_SPECIALS == key
            || DisplaySettings.KEY_DISPLAY_EXACT_DATE == key
            || DisplaySettings.KEY_PREVENT_SPOILERS == key) {
            // update any widgets
            ListWidgetProvider.notifyDataChanged(activity)
        }

        if (DisplaySettings.KEY_LANGUAGE_FALLBACK == key) {
            // reset last edit date of all episodes so they will get updated
            Thread {
                android.os.Process
                    .setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

                val values = ContentValues()
                values.put(SeriesGuideContract.Episodes.LAST_UPDATED, 0)
                activity!!.contentResolver
                    .update(SeriesGuideContract.Episodes.CONTENT_URI, values, null, null)
            }.start()
        }

        // Toggle auto-update on SyncAdapter
        if (UpdateSettings.KEY_AUTOUPDATE == key) {
            if (pref != null) {
                val autoUpdatePref = pref as SwitchPreferenceCompat
                SgSyncAdapter.setSyncAutomatically(activity, autoUpdatePref.isChecked)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(@Suppress("UNUSED_PARAMETER") event: SeriesGuidePreferences.UpdateSummariesEvent) {
        if (!isResumed) {
            return
        }
        // update summary values not handled by onSharedPreferenceChanged
        val settings = arguments?.getString(SeriesGuidePreferences.EXTRA_SETTINGS_SCREEN)
        if (settings != null && settings == KEY_SCREEN_NOTIFICATIONS) {
            updateNotificationSettings()
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
        val countOfShowsNotifyOn = DBUtils.getCountOf(
            activity!!.contentResolver,
            SeriesGuideContract.Shows.CONTENT_URI,
            SeriesGuideContract.Shows.SELECTION_NOTIFY, null, 0
        )
        selectionPref.summary = getString(
            R.string.pref_notifications_select_shows_summary,
            countOfShowsNotifyOn
        )
    }

    private fun updateStreamSearchServiceSummary(pref: Preference) {
        val serviceOrEmptyOrNull = StreamingSearch.getServiceOrEmptyOrNull(activity!!)
        when {
            serviceOrEmptyOrNull == null -> pref.summary = null
            serviceOrEmptyOrNull.isEmpty() -> pref.setSummary(R.string.action_turn_off)
            else -> pref.summary = StreamingSearch.getServiceDisplayName(serviceOrEmptyOrNull)
        }
    }

    private fun updateTimeOffsetSummary(offsetListPref: Preference) {
        offsetListPref.summary = getString(
            R.string.pref_offsetsummary,
            DisplaySettings.getShowsTimeOffset(activity)
        )
    }

    /**
     * Resets and runs the notification service to take care of potential time shifts when e.g.
     * changing the time offset.
     */
    private fun resetAndRunNotificationsService(context: Context) {
        NotificationService.resetLastEpisodeAirtime(
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        NotificationService.trigger(context)
    }

    companion object {

        // Preference keys
        private const val KEY_CLEAR_CACHE = "clearCache"
        //    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";
        private const val KEY_ABOUT = "aboutPref"
        //    public static final String KEY_TAPE_INTERVAL = "com.battlelancer.seriesguide.tapeinterval";

        // links
        private const val LINK_BASE_KEY = "com.battlelancer.seriesguide.settings."
        private const val LINK_KEY_UPGRADE = LINK_BASE_KEY + "upgrade"
        private const val LINK_KEY_CLOUD = LINK_BASE_KEY + "cloud"
        private const val LINK_KEY_TRAKT = LINK_BASE_KEY + "trakt"
        private const val LINK_KEY_AUTOBACKUP = LINK_BASE_KEY + "autobackup"
        private const val LINK_KEY_DATALIBERATION = LINK_BASE_KEY + "dataliberation"

        private const val KEY_SCREEN_BASIC = "screen_basic"
        private const val KEY_SCREEN_NOTIFICATIONS = "screen_notifications"

        private const val REQUEST_CODE_RINGTONE = 0

        private val sNoOpChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, _: Any ->
                Utils.advertiseSubscription(preference.context)
                // prevent value from getting saved
                false
            }
    }
}
