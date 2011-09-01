
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ShowInfo;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.getglueapi.PrepareRequestTokenActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class ShareUtils {

    protected static final String TAG = "ShareUtils";

    /**
     * Show a dialog allowing to chose from various sharing options. The given
     * {@link Bundle} has to include a sharestring, episodestring and imdbId.
     * 
     * @param shareData - a {@link Bundle} including all
     *            {@link ShareUtils.ShareItems}
     */
    public static void showShareDialog(FragmentManager manager, Bundle shareData) {
        // Create and show the dialog.
        ShareDialogFragment newFragment = ShareDialogFragment.newInstance(shareData);
        FragmentTransaction ft = manager.beginTransaction();
        newFragment.show(ft, "sharedialog");
    }

    public static class ShareDialogFragment extends DialogFragment {
        public static ShareDialogFragment newInstance(Bundle shareData) {
            ShareDialogFragment f = new ShareDialogFragment();
            f.setArguments(shareData);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String imdbId = getArguments().getString(ShareUtils.ShareItems.IMDBID);
            final String sharestring = getArguments().getString(ShareUtils.ShareItems.SHARESTRING);
            final CharSequence[] items = getResources().getStringArray(R.array.share_items);

            return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.share))
                    .setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    // GetGlue check-in
                                    if (imdbId.length() != 0) {
                                        showGetGlueDialog(getFragmentManager(), getArguments());
                                    } else {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.noIMDBentry), Toast.LENGTH_LONG)
                                                .show();
                                    }
                                    break;
                                case 1: {
                                    // trakt mark as seen
                                    getArguments().putInt(ShareItems.TRAKTACTION,
                                            TraktAction.SEEN_EPISODE.index());
                                    new TraktTask(getActivity(), getFragmentManager(),
                                            getArguments()).execute();
                                    break;
                                }
                                case 2: {
                                    // Android apps
                                    String text = sharestring;
                                    if (imdbId.length() != 0) {
                                        text += " " + ShowInfo.IMDB_TITLE_URL + imdbId;
                                    }

                                    Intent i = new Intent(Intent.ACTION_SEND);
                                    i.setType("text/plain");
                                    i.putExtra(Intent.EXTRA_TEXT, text);
                                    startActivity(Intent.createChooser(i,
                                            getString(R.string.share_episode)));
                                    break;
                                }
                                case 3: {
                                    // trakt rate
                                    getArguments().putInt(ShareItems.TRAKTACTION,
                                            TraktAction.RATE_EPISODE.index());
                                    TraktRateDialogFragment newFragment = TraktRateDialogFragment
                                            .newInstance(getArguments());
                                    FragmentTransaction ft = getFragmentManager()
                                            .beginTransaction();
                                    newFragment.show(ft, "traktratedialog");
                                    break;
                                }
                            }
                        }
                    }).create();
        }
    }

    public static void showGetGlueDialog(FragmentManager manager, Bundle shareData) {
        // Create and show the dialog.
        GetGlueDialogFragment newFragment = GetGlueDialogFragment.newInstance(shareData);
        FragmentTransaction ft = manager.beginTransaction();
        newFragment.show(ft, "getgluedialog");
    }

    public static class GetGlueDialogFragment extends DialogFragment {

        public static GetGlueDialogFragment newInstance(Bundle shareData) {
            GetGlueDialogFragment f = new GetGlueDialogFragment();
            f.setArguments(shareData);
            return f;
        }

        private EditText input;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String episodestring = getArguments().getString(ShareItems.EPISODESTRING);
            final String imdbId = getArguments().getString(ShareItems.IMDBID);

            input = new EditText(getActivity());
            input.setMinLines(3);
            input.setGravity(Gravity.TOP);
            input.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(140)
            });

            if (savedInstanceState != null) {
                input.setText(savedInstanceState.getString("inputtext"));
            } else {
                input.setText(episodestring + ". #SeriesGuide");
            }

            return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.comment))
                    .setView(input)
                    .setPositiveButton(R.string.checkin, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onGetGlueCheckIn(getActivity(), input.getText().toString(), imdbId);
                        }
                    }).setNegativeButton(android.R.string.cancel, null).create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("inputtext", input.getText().toString());
        }
    }

    public static void onGetGlueCheckIn(final Activity activity, final String comment,
            final String imdbId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity
                .getApplicationContext());

        if (GetGlue.isAuthenticated(prefs)) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        GetGlue.checkIn(prefs, imdbId, comment);
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity,
                                        activity.getString(R.string.checkinsuccess),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        AnalyticsUtils.getInstance(activity).trackEvent("Sharing", "GetGlue",
                                "Success", 0);

                    } catch (final Exception e) {
                        Log.e(TAG, "GetGlue Check-In failed");
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(
                                        activity,
                                        activity.getString(R.string.checkinfailed) + " - "
                                                + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                        AnalyticsUtils.getInstance(activity).trackEvent("Sharing", "GetGlue",
                                "Failed", 0);

                    }
                }
            }).start();
        } else {
            Intent i = new Intent(activity, PrepareRequestTokenActivity.class);
            i.putExtra(SeriesGuideData.KEY_GETGLUE_IMDBID, imdbId);
            i.putExtra(SeriesGuideData.KEY_GETGLUE_COMMENT, comment);
            activity.startActivity(i);
        }
    }

    public interface ShareItems {
        String SEASON = "season";

        String IMDBID = "imdbId";

        String SHARESTRING = "sharestring";

        String EPISODESTRING = "episodestring";

        String EPISODE = "episode";

        String TVDBID = "tvdbid";

        String RATING = "rating";

        String TRAKTACTION = "traktaction";
    }

    public static String onCreateShareString(Context context, final Cursor episode) {
        String season = episode.getString(episode.getColumnIndexOrThrow(Episodes.SEASON));
        String number = episode.getString(episode.getColumnIndexOrThrow(Episodes.NUMBER));
        String title = episode.getString(episode.getColumnIndexOrThrow(Episodes.TITLE));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return SeriesGuideData.getNextEpisodeString(prefs, season, number, title);
    }

    public static void onAddCalendarEvent(Context context, String title, String description,
            String airdate, long airtime, String runtime) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("title", title);
        intent.putExtra("description", description);

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());
            boolean useUserTimeZone = prefs.getBoolean(SeriesGuidePreferences.KEY_USE_MY_TIMEZONE,
                    false);

            Calendar cal = SeriesGuideData.getLocalCalendar(airdate, airtime, useUserTimeZone,
                    context);

            long startTime = cal.getTimeInMillis();
            long endTime = startTime + Long.valueOf(runtime) * 60 * 1000;
            intent.putExtra("beginTime", startTime);
            intent.putExtra("endTime", endTime);

            context.startActivity(intent);

            AnalyticsUtils.getInstance(context).trackEvent("Sharing", "Calendar", "Success", 0);
        } catch (Exception e) {
            AnalyticsUtils.getInstance(context).trackEvent("Sharing", "Calendar", "Failed", 0);
            Toast.makeText(context, context.getString(R.string.addtocalendar_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static String toSHA1(byte[] convertme) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return byteArrayToHexString(md.digest(convertme));
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public enum TraktAction {
        SEEN_EPISODE(0), RATE_EPISODE(1);

        final private int mIndex;

        private TraktAction(int index) {
            mIndex = index;
        }

        public int index() {
            return mIndex;
        }
    }

    public static class TraktTask extends AsyncTask<Void, Void, String> {
        private final Context mContext;

        private final FragmentManager mManager;

        private final Bundle mTraktData;

        /**
         * Do the specified TraktAction. traktData should include all required
         * parameters.
         * 
         * @param context
         * @param manager
         * @param traktData
         * @param action
         */
        public TraktTask(Context context, FragmentManager manager, Bundle traktData) {
            mContext = context;
            mManager = manager;
            mTraktData = traktData;
        }

        @Override
        protected String doInBackground(Void... params) {
            if (!isTraktCredentialsValid(mContext)) {
                // return an empty string, so onPostExecute displays a
                // credentials dialog which calls us again.
                return "";
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext
                    .getApplicationContext());

            ServiceManager manager = new ServiceManager();
            final String username = prefs.getString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
            String password = prefs.getString(SeriesGuidePreferences.PREF_TRAKTPWD, "");

            try {
                password = SimpleCrypto.decrypt(password, mContext);
            } catch (Exception e1) {
                // password could not be decrypted
                return mContext.getString(R.string.trakt_decryptfail);
            }

            manager.setAuthentication(username, ShareUtils.toSHA1(password.getBytes()));
            manager.setApiKey(Constants.TRAKT_API_KEY);

            if (isCancelled()) {
                return null;
            }

            final int tvdbid = mTraktData.getInt(ShareItems.TVDBID);
            final int season = mTraktData.getInt(ShareItems.SEASON);
            final int episode = mTraktData.getInt(ShareItems.EPISODE);
            final TraktAction action = TraktAction.values()[mTraktData
                    .getInt(ShareItems.TRAKTACTION)];

            try {
                switch (action) {
                    case SEEN_EPISODE: {
                        manager.showService().episodeSeen(tvdbid).episode(season, episode).fire();
                        break;
                    }
                    case RATE_EPISODE: {
                        final Rating rating = Rating.fromValue(mTraktData
                                .getString(ShareItems.RATING));
                        manager.rateService().episode(tvdbid).season(season).episode(episode)
                                .rating(rating).fire();
                        break;
                    }
                }
                return null;
            } catch (ApiException e) {
                return "API error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(mContext, mContext.getString(R.string.trakt_success),
                        Toast.LENGTH_SHORT).show();
            } else if (result.length() == 0) {
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance(mTraktData);
                FragmentTransaction ft = mManager.beginTransaction();
                newFragment.show(ft, "traktdialog");
            } else {
                Toast.makeText(mContext,
                        mContext.getString(R.string.trakt_error) + " (" + result + ")",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class TraktCredentialsDialogFragment extends DialogFragment {

        private boolean isForwardingGivenTask;

        public static TraktCredentialsDialogFragment newInstance(Bundle traktData) {
            TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
            f.setArguments(traktData);
            f.isForwardingGivenTask = true;
            return f;
        }

        public static TraktCredentialsDialogFragment newInstance() {
            TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
            f.isForwardingGivenTask = false;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());
            String username = prefs.getString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
            String password = prefs.getString(SeriesGuidePreferences.PREF_TRAKTPWD, "");
            try {
                password = SimpleCrypto.decrypt(password, context);
            } catch (Exception e) {
                // failed to decrypt password, user should enter it again
                password = "";
            }

            AlertDialog.Builder builder;

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, null);

            builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            builder.setTitle("trakt.tv");
            ((EditText) layout.findViewById(R.id.username)).setText(username);
            ((EditText) layout.findViewById(R.id.password)).setText(password);
            final View mailviews = layout.findViewById(R.id.mailviews);
            mailviews.setVisibility(View.GONE);
            ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                    .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                mailviews.setVisibility(View.VISIBLE);
                            } else {
                                mailviews.setVisibility(View.GONE);
                            }
                        }
                    });

            builder.setPositiveButton(R.string.save, new OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                            .getApplicationContext());
                    final String username = ((EditText) layout.findViewById(R.id.username))
                            .getText().toString();
                    String password = ((EditText) layout.findViewById(R.id.password)).getText()
                            .toString();
                    boolean isNewAccount = ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                            .isChecked();

                    try {
                        password = SimpleCrypto.encrypt(password, context);
                    } catch (Exception e) {
                        // password could not be encrypted, store a blank one
                        password = "";
                    }

                    Editor editor = prefs.edit();
                    editor.putString(SeriesGuidePreferences.PREF_TRAKTUSER, username);
                    editor.putString(SeriesGuidePreferences.PREF_TRAKTPWD, password);
                    editor.commit();

                    if (isNewAccount) {
                        final String email = ((EditText) layout.findViewById(R.id.email)).getText()
                                .toString();

                        // validate credentials and mail
                        if (!isTraktCredentialsValid(context) || email.length() == 0) {
                            Toast.makeText(context,
                                    context.getString(R.string.trakt_credentials_invalid),
                                    Toast.LENGTH_LONG).show();
                            editor.putString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
                            editor.putString(SeriesGuidePreferences.PREF_TRAKTPWD, "");
                            editor.commit();
                            return;
                        }

                        // TODO: make asynchronous
                        // create new account
                        final ServiceManager manager = new ServiceManager();
                        manager.setApiKey(Constants.TRAKT_API_KEY);
                        manager.setAuthentication(username, ShareUtils.toSHA1(password.getBytes()));
                        final Response response = manager.accountService()
                                .create(username, password, email).fire();

                        if (response.getStatus().equalsIgnoreCase("success")) {
                            Toast.makeText(context,
                                    response.getStatus() + ": " + response.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context,
                                    response.getStatus() + ": " + response.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            editor.putString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
                            editor.putString(SeriesGuidePreferences.PREF_TRAKTPWD, "");
                            editor.commit();
                            return;
                        }
                    }

                    if (isForwardingGivenTask) {
                        new TraktTask(context, getFragmentManager(), getArguments()).execute();
                    }
                }
            });
            builder.setNegativeButton(R.string.dontsave, null);

            return builder.create();
        }
    }

    public static class TraktRateDialogFragment extends DialogFragment {

        public static TraktRateDialogFragment newInstance(Bundle traktData) {
            TraktRateDialogFragment f = new TraktRateDialogFragment();
            f.setArguments(traktData);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            AlertDialog.Builder builder;

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.trakt_rate_dialog, null);
            final Button totallyNinja = (Button) layout.findViewById(R.id.totallyninja);
            final Button weakSauce = (Button) layout.findViewById(R.id.weaksauce);

            totallyNinja.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Rating rating = Rating.Love;
                    getArguments().putString(ShareItems.RATING, rating.toString());
                    new TraktTask(context, getFragmentManager(), getArguments()).execute();
                    dismiss();
                }
            });

            weakSauce.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Rating rating = Rating.Hate;
                    getArguments().putString(ShareItems.RATING, rating.toString());
                    new TraktTask(context, getFragmentManager(), getArguments()).execute();
                    dismiss();
                }
            });

            builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    public static boolean isTraktCredentialsValid(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.PREF_TRAKTPWD, "");

        return (!username.equalsIgnoreCase("") && !password.equalsIgnoreCase(""));
    }
}
