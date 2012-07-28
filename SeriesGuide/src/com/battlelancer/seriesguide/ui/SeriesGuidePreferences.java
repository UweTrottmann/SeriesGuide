/*
 * Copyright 2011 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Allows tweaking of various SeriesGuide settings.
 */
public class SeriesGuidePreferences extends SherlockPreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final String KEY_TRAKTPWD = "com.battlelancer.seriesguide.traktpwd";

    public static final String KEY_TRAKTUSER = "com.battlelancer.seriesguide.traktuser";

    public static final String KEY_ONLY_FUTURE_EPISODES = "onlyFutureEpisodes";

    public static final String KEY_ONLY_SEASON_EPISODES = "onlySeasonEpisodes";

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String NUMBERFORMAT_ENGLISH = "english";

    public static final String NUMBERFORMAT_ENGLISHLOWER = "englishlower";

    public static final String KEY_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    public static final String KEY_DATABASEIMPORTED = "com.battlelancer.seriesguide.dbimported";

    public static final String KEY_SHOW_SORT_ORDER = "showSorting";

    public static final String KEY_SEASON_SORT_ORDER = "seasonSorting";

    public static final String KEY_EPISODE_SORT_ORDER = "episodeSorting";

    public static final String KEY_SHOWFILTER = "com.battlelancer.seriesguide.showfilter";

    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public static final String KEY_UPDATEATLEASTEVERY = "com.battlelancer.seriesguide.updateatleastevery";

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_HIDEIMAGES = "hideimages";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    public static final String KEY_AUTOUPDATE = "com.battlelancer.seriesguide.autoupdate";

    public static final String KEY_ONLYWIFI = "com.battlelancer.seriesguide.autoupdatewlanonly";

    public static final String KEY_LASTUPDATE = "com.battlelancer.seriesguide.lastupdate";

    public static final String KEY_LASTTRAKTUPDATE = "com.battlelancer.seriesguide.lasttraktupdate";

    /**
     * Deprecated.
     */
    public static final String KEY_LAST_USED_SHARE_METHOD = "com.battlelancer.seriesguide.lastusedsharemethod";

    public static final String KEY_ONLYFAVORITES = "com.battlelancer.seriesguide.onlyfavorites";

    public static final String KEY_NOWATCHED = "com.battlelancer.seriesguide.activity.nowatched";

    public static final String KEY_UPCOMING_LIMIT = "com.battlelancer.seriesguide.upcominglimit";

    public static final String KEY_NOTIFICATIONS_ENABLED = "com.battlelancer.seriesguide.notifications";

    public static final String KEY_VIBRATE = "com.battlelancer.seriesguide.notifications.vibrate";

    public static final String KEY_RINGTONE = "com.battlelancer.seriesguide.notifications.ringtone";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_SHAREWITHTRAKT = "com.battlelancer.seriesguide.sharewithtrakt";

    public static final String KEY_SHAREWITHGETGLUE = "com.battlelancer.seriesguide.sharewithgetglue";

    public static final String KEY_THEME = "com.battlelancer.seriesguide.theme";

    public static final String SUPPORT_MAIL = "support@seriesgui.de";

    public static final String HELP_URL = "http://seriesgui.de/help";

    protected static final int ABOUT_DIALOG = 0;

    private static final String TRANSLATIONS_URL = "http://crowdin.net/project/seriesguide-translations/invite";

    private static final String TAG = "SeriesGuidePreferences";

    public static int THEME = R.style.SeriesGuideTheme;

    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent(TAG, "Click", label, (long) 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);

        final SeriesGuidePreferences activity = this;
        addPreferencesFromResource(R.layout.preferences);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        final String versionFinal = Utils.getVersion(this);

        // About
        Preference aboutPref = (Preference) findPreference("aboutPref");
        aboutPref.setSummary("v" + versionFinal + " (dbver " + SeriesGuideDatabase.DATABASE_VERSION
                + ")");
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("About dialog");

                showDialog(ABOUT_DIALOG);
                return true;
            }
        });

        // Clear image cache
        Preference clearCachePref = (Preference) findPreference("clearCache");
        clearCachePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Clear Image Cache");

                ImageProvider.getInstance(activity).clearCache();
                ImageProvider.getInstance(activity).clearExternalStorageCache();
                Toast.makeText(getApplicationContext(), getString(R.string.done),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Backup & Restore
        Preference backupPref = (Preference) findPreference("backup");
        backupPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(activity, BackupDeleteActivity.class));
                return true;
            }
        });

        // No aired episodes
        Preference futureepisodes = (Preference) findPreference(KEY_ONLY_FUTURE_EPISODES);
        futureepisodes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    EasyTracker.getTracker().trackEvent(TAG, "OnlyFutureEpisodes", "Enable",
                            (long) 0);
                } else {
                    EasyTracker.getTracker().trackEvent(TAG, "OnlyFutureEpisodes", "Disable",
                            (long) 0);
                }
                return false;
            }
        });

        // No special episodes
        Preference seasonEpisodes = (Preference) findPreference(KEY_ONLY_SEASON_EPISODES);
        seasonEpisodes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    EasyTracker.getTracker().trackEvent(TAG, "OnlySeasonEpisodes", "Enable",
                            (long) 0);
                } else {
                    EasyTracker.getTracker().trackEvent(TAG, "OnlySeasonEpisodes", "Disable",
                            (long) 0);
                }
                return false;
            }
        });

        // Disconnect GetGlue
        Preference getgluePref = (Preference) findPreference("clearGetGlueCredentials");
        getgluePref.setEnabled(GetGlue.isAuthenticated(prefs));
        getgluePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Disonnect GetGlue");

                GetGlue.clearCredentials(prefs);
                preference.setEnabled(false);
                return true;
            }
        });

        // trakt.tv
        findPreference("com.battlelancer.seriesguide.trakt").setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(SeriesGuidePreferences.this,
                                TraktSyncActivity.class));
                        return true;
                    }
                });

        // Help translate
        Preference helpTranslate = (Preference) findPreference("com.battlelancer.seriesguide.helpTranslate");
        helpTranslate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Help translate");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TRANSLATIONS_URL));
                startActivity(myIntent);
                return true;
            }
        });

        // Notifications
        Preference notificationsPref = findPreference(KEY_NOTIFICATIONS_ENABLED);
        // allow supporters to enable notfications
        if (Utils.isSupporterChannel(this)) {
            notificationsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (((CheckBoxPreference) preference).isChecked()) {
                        EasyTracker.getTracker().trackEvent(TAG, "Notifications", "Enable",
                                (long) 0);
                    } else {
                        EasyTracker.getTracker().trackEvent(TAG, "Notifications", "Disable",
                                (long) 0);
                    }

                    Utils.runNotificationService(SeriesGuidePreferences.this);
                    return true;
                }
            });
        } else {
            notificationsPref.setEnabled(false);
            notificationsPref.setSummary(R.string.onlyx);
        }

        // Theme switcher
        Preference themePref = findPreference(KEY_THEME);
        themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference.getKey().equals(KEY_THEME)) {
                    Utils.updateTheme((String) newValue);
                }
                return true;
            }
        });

        // GA opt-out
        findPreference(KEY_GOOGLEANALYTICS).setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (preference.getKey().equals(KEY_GOOGLEANALYTICS)) {
                            boolean isEnabled = (Boolean) newValue;
                            GoogleAnalytics.getInstance(activity).setAppOptOut(isEnabled);
                            return true;
                        }
                        return false;
                    }
                });

        // show currently set values for list prefs
        setListPreferenceSummary(themePref);
        setListPreferenceSummary(findPreference(KEY_UPCOMING_LIMIT));
        setListPreferenceSummary(findPreference(KEY_LANGUAGE));
        setListPreferenceSummary(findPreference(KEY_NUMBERFORMAT));
        ListPreference offsetPref = (ListPreference) findPreference(KEY_OFFSET);
        offsetPref.setSummary(getString(R.string.pref_offsetsummary, offsetPref.getEntry()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ABOUT_DIALOG: {
                final TextView message = new TextView(this);
                message.setAutoLinkMask(Linkify.ALL);
                message.setText(getString(R.string.about_message));
                message.setPadding(10, 5, 10, 5);
                message.setTextSize(16);
                final ScrollView aboutScroll = new ScrollView(this);
                aboutScroll.addView(message);
                return new AlertDialog.Builder(this).setTitle(getString(R.string.about))
                        .setCancelable(true).setIcon(R.drawable.icon)
                        .setPositiveButton(getString(android.R.string.ok), null)
                        .setView(aboutScroll).create();
            }
        }
        return null;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // always navigate back to the home activity
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            NavUtils.navigateUpTo(this,
                    new Intent(Intent.ACTION_MAIN).setClass(this, ShowsActivity.class));
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_UPCOMING_LIMIT) || key.equals(KEY_LANGUAGE)
                || key.equals(KEY_NUMBERFORMAT) || key.equals(KEY_THEME)) {
            setListPreferenceSummary(findPreference(key));
        }

        if (key.equals(KEY_OFFSET)) {
            ListPreference pref = (ListPreference) findPreference(key);
            // Set summary to be the user-description for the selected value
            pref.setSummary(getString(R.string.pref_offsetsummary, pref.getEntry()));

            // run notification service to take care of potential time shifts
            // when changing the time offset
            Utils.runNotificationService(SeriesGuidePreferences.this);
        }
    }

    private void setListPreferenceSummary(Preference pref) {
        ListPreference listPref = (ListPreference) pref;
        // Set summary to be the user-description for the selected value
        listPref.setSummary(listPref.getEntry());
    }
}
