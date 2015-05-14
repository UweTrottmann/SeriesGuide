/*
/*
 * Copyright 2014 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
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
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;
import java.util.LinkedList;
import java.util.List;

/**
 * Allows tweaking of various SeriesGuide settings. Does NOT inherit from {@link
 * com.battlelancer.seriesguide.ui.BaseActivity} to avoid handling actions which might be confusing
 * while adjusting settings.
 */
public class SeriesGuidePreferences extends AppCompatActivity {

    private static final String TAG = "Settings";

    // Preference keys
    private static final String KEY_CLEAR_CACHE = "clearCache";

    public static final String KEY_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    public static final String KEY_DATABASEIMPORTED = "com.battlelancer.seriesguide.dbimported";

    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public static final String SUPPORT_MAIL = "support@seriesgui.de";

    private static final String KEY_ABOUT = "aboutPref";

    public static final String KEY_TAPE_INTERVAL = "com.battlelancer.seriesguide.tapeinterval";

    public static @StyleRes int THEME = R.style.Theme_SeriesGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            Fragment f = new SettingsHeadersFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, f);
            ft.commit();
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.sgToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public void onBackPressed() {
        // Because we use the platform fragment manager we need to pop fragments on our own
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void switchToSettings(String settingsId) {
        Bundle args = new Bundle();
        args.putString("settings", settingsId);
        Fragment f = new SettingsFragment();
        f.setArguments(args);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, f);
        ft.addToBackStack(null);
        ft.commit();
    }

    private static OnPreferenceChangeListener sNoOpChangeListener
            = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Utils.advertiseSubscription(preference.getContext());
            // prevent value from getting saved
            return false;
        }
    };

    protected static void setupBasicSettings(final Activity activity, final Intent startIntent,
            Preference noAiredPref, Preference noSpecialsPref, Preference languagePref,
            Preference themePref, Preference numberFormatPref, Preference updatePref) {
        // No aired episodes
        noAiredPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    Utils.trackCustomEvent(activity, TAG, "OnlyFutureEpisodes", "Enable");
                } else {
                    Utils.trackCustomEvent(activity, TAG, "OnlyFutureEpisodes", "Disable");
                }
                return false;
            }
        });

        // No special episodes
        noSpecialsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    Utils.trackCustomEvent(activity, TAG, "OnlySeasonEpisodes", "Enable");
                } else {
                    Utils.trackCustomEvent(activity, TAG, "OnlySeasonEpisodes", "Disable");
                }
                return false;
            }
        });

        // Theme switcher
        if (Utils.hasAccessToX(activity)) {
            themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (DisplaySettings.KEY_THEME.equals(preference.getKey())) {
                        Utils.updateTheme((String) newValue);

                        // restart to apply new theme (actually build an entirely new task stack)
                        TaskStackBuilder.create(activity)
                                .addNextIntent(new Intent(activity, ShowsActivity.class))
                                .addNextIntent(startIntent)
                                .startActivities();
                    }
                    return true;
                }
            });
            setListPreferenceSummary((ListPreference) themePref);
        } else {
            themePref.setOnPreferenceChangeListener(sNoOpChangeListener);
            themePref.setSummary(R.string.onlyx);
        }

        // set current value of auto-update pref
        ((CheckBoxPreference) updatePref).setChecked(SgSyncAdapter.isSyncAutomatically(activity));

        // show currently set values for list prefs
        setListPreferenceSummary((ListPreference) languagePref);
        setListPreferenceSummary((ListPreference) numberFormatPref);
    }

    protected static void setupNotifiationSettings(final Context context,
            Preference notificationsPref, final Preference notificationsFavOnlyPref,
            final Preference vibratePref, final Preference ringtonePref,
            final Preference notificationsThresholdPref) {
        // allow supporters to enable notifications
        if (Utils.hasAccessToX(context)) {
            notificationsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isChecked = ((CheckBoxPreference) preference).isChecked();
                    if (isChecked) {
                        Utils.trackCustomEvent(context, TAG, "Notifications", "Enable");
                    } else {
                        Utils.trackCustomEvent(context, TAG, "Notifications", "Disable");
                    }

                    notificationsThresholdPref.setEnabled(isChecked);
                    notificationsFavOnlyPref.setEnabled(isChecked);
                    vibratePref.setEnabled(isChecked);
                    ringtonePref.setEnabled(isChecked);

                    Utils.runNotificationService(context);
                    return true;
                }
            });
            notificationsFavOnlyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetAndRunNotificationsService(context);
                    return true;
                }
            });
            // disable advanced notification settings if notifications are disabled
            boolean isNotificationsEnabled = NotificationSettings.isNotificationsEnabled(context);
            notificationsThresholdPref.setEnabled(isNotificationsEnabled);
            notificationsFavOnlyPref.setEnabled(isNotificationsEnabled);
            vibratePref.setEnabled(isNotificationsEnabled);
            ringtonePref.setEnabled(isNotificationsEnabled);
        } else {
            notificationsPref.setOnPreferenceChangeListener(sNoOpChangeListener);
            ((CheckBoxPreference) notificationsPref).setChecked(false);
            notificationsPref.setSummary(R.string.onlyx);
            notificationsThresholdPref.setEnabled(false);
            notificationsFavOnlyPref.setEnabled(false);
            vibratePref.setEnabled(false);
            ringtonePref.setEnabled(false);
        }

        setListPreferenceSummary((ListPreference) notificationsThresholdPref);
    }

    protected static void setupAdvancedSettings(final Context context,
            Preference upcomingPref, Preference offsetPref, Preference analyticsPref,
            Preference clearCachePref) {

        // Clear image cache
        clearCachePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // try to open app info where user can clear app cache folders
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                if (!Utils.tryStartActivity(context, intent, false)) {
                    // try to open all apps view if detail view not available
                    intent = new Intent(
                            android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    Utils.tryStartActivity(context, intent, true);
                }

                return true;
            }
        });

        // GA opt-out
        analyticsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference.getKey().equals(AppSettings.KEY_GOOGLEANALYTICS)) {
                    boolean isEnabled = (Boolean) newValue;
                    GoogleAnalytics.getInstance(context).setAppOptOut(isEnabled);
                    return true;
                }
                return false;
            }
        });

        // show currently set values for list prefs
        setListPreferenceSummary((ListPreference) upcomingPref);
        ListPreference offsetListPref = (ListPreference) offsetPref;
        offsetListPref.setSummary(context.getString(R.string.pref_offsetsummary,
                offsetListPref.getEntry()));
    }

    protected static void setupAboutSettings(Context context, Preference aboutPref) {
        final String versionFinal = Utils.getVersion(context);

        // About
        aboutPref.setSummary("v" + versionFinal + " (Database v"
                + SeriesGuideDatabase.DATABASE_VERSION + ")");
    }

    /**
     * Resets and runs the notification service to take care of potential time shifts when e.g.
     * changing the time offset.
     */
    private static void resetAndRunNotificationsService(Context context) {
        NotificationService.resetLastEpisodeAirtime(PreferenceManager
                .getDefaultSharedPreferences(context));
        Utils.runNotificationService(context);
    }

    public static void setListPreferenceSummary(ListPreference listPref) {
        // Set summary to be the user-description for the selected value
        listPref.setSummary(listPref.getEntry().toString().replaceAll("%", "%%"));
    }

    public static class SettingsHeadersFragment extends Fragment {
        private HeaderAdapter adapter;
        private ListView listView;

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_settings_headers, container, false);

            listView = (ListView) v.findViewById(R.id.listViewSettingsHeaders);

            return v;
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            adapter = new HeaderAdapter(getActivity(), buildHeaders());
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Header item = adapter.getItem(position);
                    ((SeriesGuidePreferences) getActivity()).switchToSettings(item.settingsId);
                }
            });
        }

        private List<Header> buildHeaders() {
            List<Header> headers = new LinkedList<>();

            headers.add(new Header(R.string.prefs_category_basic, "basic"));
            headers.add(new Header(R.string.pref_notifications, "notifications"));
            headers.add(new Header(R.string.prefs_category_sharing, "sharing"));
            headers.add(new Header(R.string.prefs_category_advanced, "advanced"));
            headers.add(new Header(R.string.prefs_category_about, "about"));

            return headers;
        }

        private static class HeaderAdapter extends ArrayAdapter<Header> {
            private final LayoutInflater mInflater;

            private static class HeaderViewHolder {
                TextView title;

                public HeaderViewHolder(View view) {
                    title = (TextView) view.findViewById(R.id.textViewSettingsHeader);
                }
            }

            public HeaderAdapter(Context context, List<Header> headers) {
                super(context, 0, headers);
                mInflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                HeaderViewHolder viewHolder;
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_settings_header, parent, false);
                    viewHolder = new HeaderViewHolder(convertView);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (HeaderViewHolder) convertView.getTag();
                }

                viewHolder.title.setText(getContext().getString(getItem(position).titleRes));

                return convertView;
            }
        }

        public static final class Header {
            public int titleRes;
            public String settingsId;

            public Header(int titleResId, String settingsId) {
                this.titleRes = titleResId;
                this.settingsId = settingsId;
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            switch (settings) {
                case "basic":
                    addPreferencesFromResource(R.xml.settings_basic);
                    setupBasicSettings(
                            getActivity(),
                            getActivity().getIntent(),
                            findPreference(DisplaySettings.KEY_NO_RELEASED_EPISODES),
                            findPreference(DisplaySettings.KEY_HIDE_SPECIALS),
                            findPreference(DisplaySettings.KEY_LANGUAGE),
                            findPreference(DisplaySettings.KEY_THEME),
                            findPreference(DisplaySettings.KEY_NUMBERFORMAT),
                            findPreference(UpdateSettings.KEY_AUTOUPDATE)
                    );
                    break;
                case "notifications":
                    addPreferencesFromResource(R.xml.settings_notifications);
                    setupNotifiationSettings(
                            getActivity(),
                            findPreference(NotificationSettings.KEY_ENABLED),
                            findPreference(NotificationSettings.KEY_FAVONLY),
                            findPreference(NotificationSettings.KEY_VIBRATE),
                            findPreference(NotificationSettings.KEY_RINGTONE),
                            findPreference(NotificationSettings.KEY_THRESHOLD)
                    );
                    break;
                case "sharing":
                    addPreferencesFromResource(R.xml.settings_services);
                    break;
                case "advanced":
                    addPreferencesFromResource(R.xml.settings_advanced);
                    setupAdvancedSettings(
                            getActivity(),
                            findPreference(AdvancedSettings.KEY_UPCOMING_LIMIT),
                            findPreference(KEY_OFFSET),
                            findPreference(AppSettings.KEY_GOOGLEANALYTICS),
                            findPreference(KEY_CLEAR_CACHE)
                    );
                    break;
                case "about":
                    addPreferencesFromResource(R.xml.settings_about);
                    setupAboutSettings(
                            getActivity(),
                            findPreference(KEY_ABOUT)
                    );
                    break;
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref != null) {
                new BackupManager(pref.getContext()).dataChanged();
            }

            if (AdvancedSettings.KEY_UPCOMING_LIMIT.equals(key)
                    || DisplaySettings.KEY_LANGUAGE.equals(key)
                    || DisplaySettings.KEY_NUMBERFORMAT.equals(key)
                    || DisplaySettings.KEY_THEME.equals(key)
                    || NotificationSettings.KEY_THRESHOLD.equals(key)
                    ) {
                if (pref != null) {
                    setListPreferenceSummary((ListPreference) pref);
                }
            }

            if (DisplaySettings.KEY_LANGUAGE.equals(key)) {
                // reset last edit date of all episodes so they will get updated
                new Thread(new Runnable() {
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put(Episodes.LAST_EDITED, 0);
                        getActivity().getContentResolver()
                                .update(Episodes.CONTENT_URI, values, null, null);
                    }
                }).start();
            }

            if (key.equals(KEY_OFFSET)) {
                if (pref != null) {
                    ListPreference listPref = (ListPreference) pref;
                    // Set summary to be the user-description for the selected
                    // value
                    listPref.setSummary(
                            getString(R.string.pref_offsetsummary, listPref.getEntry()));

                    resetAndRunNotificationsService(getActivity());
                }
            }

            if (NotificationSettings.KEY_THRESHOLD.equals(key)) {
                if (pref != null) {
                    resetAndRunNotificationsService(getActivity());
                }
            }

            // Toggle auto-update on SyncAdapter
            if (UpdateSettings.KEY_AUTOUPDATE.equals(key)) {
                if (pref != null) {
                    CheckBoxPreference checkBoxPref = (CheckBoxPreference) pref;
                    SgSyncAdapter.setSyncAutomatically(getActivity(), checkBoxPref.isChecked());
                }
            }
        }
    }
}
