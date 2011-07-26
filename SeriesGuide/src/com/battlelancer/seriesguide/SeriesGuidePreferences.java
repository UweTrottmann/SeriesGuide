
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.ui.TraktSyncActivity;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.SimpleCrypto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SeriesGuidePreferences extends PreferenceActivity {

    public static final String PREF_TRAKTPWD = "com.battlelancer.seriesguide.traktpwd";

    public static final String PREF_TRAKTUSER = "com.battlelancer.seriesguide.traktuser";

    public static final String KEY_USE_MY_TIMEZONE = "com.battlelancer.seriesguide.usemytimezone";

    public static final String KEY_ONLY_FUTURE_EPISODES = "onlyFutureEpisodes";
    
    public static final String KEY_ONLY_SEASON_EPISODES = "onlySeasonEpisodes";

    protected static final int ABOUT_DIALOG = 0;

    private static final int TRAKT_DIALOG = 1;

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String KEY_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    private static final String TAG = "SeriesGuidePreferences";

    public static final String KEY_DATABASEIMPORTED = "com.battlelancer.seriesguide.dbimported";

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent(TAG, "Click", label, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SeriesGuidePreferences activity = this;
        addPreferencesFromResource(R.layout.preferences);

        Preference aboutPref = (Preference) findPreference("aboutPref");
        try {
            aboutPref.setSummary("v"
                    + getPackageManager().getPackageInfo(getPackageName(),
                            PackageManager.GET_META_DATA).versionName + " (dbver "
                    + SeriesGuideDatabase.DATABASE_VERSION + ")");
        } catch (NameNotFoundException e) {
            aboutPref.setSummary("Unkown version");
        }
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("About dialog");

                showDialog(ABOUT_DIALOG);
                return true;
            }
        });

        // Changelog button
        Preference changelogPref = (Preference) findPreference("changelogPref");
        changelogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // track event
                fireTrackerEvent("Changelog");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse(SeriesGuideData.CHANGELOG_URL));
                startActivity(myIntent);
                return true;
            }
        });

        // Help button
        Preference helpPref = (Preference) findPreference("helpPref");
        helpPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // track event
                fireTrackerEvent("Help");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://code.google.com/p/seriesguide/wiki/Help"));
                startActivity(myIntent);
                return true;
            }
        });

        Preference clearCachePref = (Preference) findPreference("clearCache");
        clearCachePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // track event
                fireTrackerEvent("Clear Image Cache");

                ((SeriesGuideApplication) getApplication()).getImageCache().clear();
                ((SeriesGuideApplication) getApplication()).getImageCache().clearExternal();
                Toast.makeText(getApplicationContext(), getString(R.string.done),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Preference backupPref = (Preference) findPreference("backup");
        backupPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(activity, BackupDelete.class));
                return true;
            }
        });

        Preference thetvdbCredit = (Preference) findPreference("thetvdb");
        thetvdbCredit.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // track event
                fireTrackerEvent("Go to thetvdb.com");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://www.thetvdb.com"));
                startActivity(myIntent);
                return true;
            }
        });

        Preference futureepisodes = (Preference) findPreference(KEY_ONLY_FUTURE_EPISODES);
        futureepisodes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    // track event
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlyFutureEpisodes", "Enable", 0);
                } else {
                    // track event
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlyFutureEpisodes", "Disable", 0);
                }
                return false;
            }
        });

        Preference seasonEpisodes = (Preference) findPreference(KEY_ONLY_SEASON_EPISODES);
        seasonEpisodes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    // track event
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlySeasonEpisodes", "Enable", 0);
                } else {
                    // track event
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlySeasonEpisodes", "Disable", 0);
                }
                return false;
            }
        });

        // Disconnect GetGlue
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        Preference getgluePref = (Preference) findPreference("clearGetGlueCredentials");
        getgluePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // track event
                fireTrackerEvent("Disonnect GetGlue");

                GetGlue.clearCredentials(prefs);
                preference.setEnabled(false);
                return true;
            }
        });
        getgluePref.setEnabled(GetGlue.isAuthenticated(prefs));

        Preference helpTranslate = (Preference) findPreference("com.battlelancer.seriesguide.helpTranslate");
        helpTranslate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // track event
                fireTrackerEvent("Help translate");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://crowdin.net/project/seriesguide-translations/invite"));
                startActivity(myIntent);
                return true;
            }
        });

        CheckBoxPreference useMyTimezone = (CheckBoxPreference) findPreference("com.battlelancer.seriesguide.usemytimezone");
        useMyTimezone.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    // track event
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings", "Use my time zone",
                            "Enable", 0);
                } else {
                    // track event
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings", "Use my time zone",
                            "Disable", 0);
                }
                return false;
            }
        });

        Preference traktCred = findPreference("com.battlelancer.seriesguide.traktcredentials");
        traktCred.setEnabled(ShareUtils.isTraktCredentialsValid(this));
        traktCred.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // as we can not access the fragment manager, just allow
                // deletion of credentials (create new class that extends on
                // FragmentActivity, then this extending it)
                prefs.edit().putString(SeriesGuidePreferences.PREF_TRAKTUSER, "")
                        .putString(SeriesGuidePreferences.PREF_TRAKTPWD, "").commit();
                preference.setEnabled(false);
                return true;
            }
        });

        Preference feedback = (Preference) findPreference("com.battlelancer.seriesguide.feedback");
        feedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);

                intent.setType("plain/text");

                intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                    "seriesguide@battlelancer.com"
                });

                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "SeriesGuide Feedback");

                intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                startActivity(Intent.createChooser(intent, "Send mail..."));
                return true;
            }
        });

        findPreference("com.battlelancer.seriesguide.donate").setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    public boolean onPreferenceClick(Preference preference) {
                        // track event
                        fireTrackerEvent("Donate");

                        Intent myIntent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=VVBLMQBSBU74L"));
                        startActivity(myIntent);
                        return true;
                    }
                });

        findPreference("com.battlelancer.seriesguide.traktsync").setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(SeriesGuidePreferences.this,
                                TraktSyncActivity.class));
                        return true;
                    }
                });

        // experimental: start alarm service to set sample alarms when clicking this pref
        // findPreference("com.battlelancer.seriesguide.notifications").setOnPreferenceClickListener(
        // new OnPreferenceClickListener() {
        //
        // @Override
        // public boolean onPreferenceClick(Preference preference) {
        // Intent i = new Intent(SeriesGuidePreferences.this,
        // AlarmManagerService.class);
        // startService(i);
        // return true;
        // }
        // });
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/Settings");
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
            case TRAKT_DIALOG: {
                AlertDialog.Builder builder;

                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, null);

                builder = new AlertDialog.Builder(this);
                builder.setView(layout);
                builder.setTitle(R.string.pref_trakt);

                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                String username = prefs.getString(PREF_TRAKTUSER, "");
                String password = prefs.getString(PREF_TRAKTPWD, "");

                try {
                    password = SimpleCrypto.decrypt(password, this);
                } catch (Exception e) {
                    // could not decrypt password
                    password = "";
                }

                ((EditText) layout.findViewById(R.id.username)).setText(username);
                ((EditText) layout.findViewById(R.id.password)).setText(password);

                builder.setPositiveButton(R.string.save, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(getApplicationContext());
                        String username = ((EditText) layout.findViewById(R.id.username)).getText()
                                .toString();
                        String password = ((EditText) layout.findViewById(R.id.password)).getText()
                                .toString();

                        try {
                            password = SimpleCrypto.encrypt(password, SeriesGuidePreferences.this);
                        } catch (Exception e) {
                            // could not encrypt password
                            password = "";
                        }

                        Editor editor = prefs.edit();
                        editor.putString(PREF_TRAKTUSER, username);
                        editor.putString(PREF_TRAKTPWD, password);
                        editor.commit();

                        // force reloading so unsaved changes get purged
                        removeDialog(TRAKT_DIALOG);
                    }
                });

                builder.setNegativeButton(R.string.dontsave, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // force reloading so unsaved changes get purged
                        removeDialog(TRAKT_DIALOG);
                    }
                });

                return builder.create();
            }

        }
        return null;
    }

}
