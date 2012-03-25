
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowInfoActivity;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;

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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class ShareUtils {

    public static final String KEY_GETGLUE_COMMENT = "com.battlelancer.seriesguide.getglue.comment";

    public static final String KEY_GETGLUE_IMDBID = "com.battlelancer.seriesguide.getglue.imdbid";

    protected static final String TAG = "ShareUtils";

    public enum ShareMethod {
        // first two kept for compatibility reasons
        CHECKIN_GETGLUE(0, 0, 0),

        CHECKIN_TRAKT(1, 0, 0),

        MARKSEEN_TRAKT(2, R.string.menu_markseen_trakt, R.drawable.ic_trakt_seen),

        RATE_TRAKT(3, R.string.menu_rate_trakt, R.drawable.trakt_love_large),

        OTHER_SERVICES(4, R.string.menu_share_others, R.drawable.ic_action_share);

        ShareMethod(int index, int titleRes, int drawableRes) {
            this.index = index;
            this.titleRes = titleRes;
            this.drawableRes = drawableRes;
        }

        public int index;

        public int titleRes;

        public int drawableRes;
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

        String ISSPOILER = "isspoiler";
    }

    /**
     * Share an episode via the given {@link ShareMethod}.
     * 
     * @param activity
     * @param args - a {@link Bundle} including all
     *            {@link ShareUtils.ShareItems}
     * @param shareMethod the {@link ShareMethod} to use
     * @param listener listener will be notified once trakt task is finished
     *            (only first time)
     */
    public static void onShareEpisode(FragmentActivity activity, Bundle args,
            ShareMethod shareMethod, OnTraktActionCompleteListener listener) {
        final FragmentManager fm = activity.getSupportFragmentManager();

        // save used share method for the quick share button
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().putInt(SeriesGuidePreferences.KEY_LAST_USED_SHARE_METHOD, shareMethod.index)
                .commit();

        switch (shareMethod) {
            case MARKSEEN_TRAKT: {
                // trakt mark as seen
                args.putInt(ShareItems.TRAKTACTION, TraktAction.SEEN_EPISODE.index());
                new TraktTask(activity, fm, args, listener).execute();
                break;
            }
            case RATE_TRAKT: {
                // trakt rate
                TraktRateDialogFragment newFragment = TraktRateDialogFragment.newInstance(
                        args.getInt(ShareItems.TVDBID), args.getInt(ShareItems.SEASON),
                        args.getInt(ShareItems.EPISODE));
                newFragment.show(fm, "traktratedialog");
                break;
            }
            case OTHER_SERVICES: {
                // Android apps
                IntentBuilder ib = ShareCompat.IntentBuilder.from(activity);

                String text = args.getString(ShareUtils.ShareItems.SHARESTRING);
                final String imdbId = args.getString(ShareUtils.ShareItems.IMDBID);
                if (imdbId.length() != 0) {
                    text += " " + ShowInfoActivity.IMDB_TITLE_URL + imdbId;
                }

                ib.setText(text);
                ib.setChooserTitle(R.string.share_episode);
                ib.setType("text/plain");
                ib.startChooser();
                break;
            }
        }
    }

    public static String onCreateShareString(Context context, final Cursor episode) {
        String season = episode.getString(episode.getColumnIndexOrThrow(Episodes.SEASON));
        String number = episode.getString(episode.getColumnIndexOrThrow(Episodes.NUMBER));
        String title = episode.getString(episode.getColumnIndexOrThrow(Episodes.TITLE));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Utils.getNextEpisodeString(prefs, season, number, title);
    }

    public static void onAddCalendarEvent(Context context, String title, String description,
            long airtime, String runtime) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("title", title);
        intent.putExtra("description", description);

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());

            Calendar cal = Utils.getAirtimeCalendar(airtime, prefs);

            long startTime = cal.getTimeInMillis();
            long endTime = startTime + Long.valueOf(runtime) * DateUtils.MINUTE_IN_MILLIS;
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

    public enum TraktAction {
        SEEN_EPISODE(0), RATE_EPISODE(1), CHECKIN_EPISODE(2), SHOUT(3), RATE_SHOW(4);

        final int index;

        private TraktAction(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    public interface TraktStatus {
        String SUCCESS = "success";

        String FAILURE = "failure";
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            getDialog().setTitle(R.string.pref_trakt);
            final Context context = getActivity().getApplicationContext();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, null);
            final FragmentManager fm = getFragmentManager();
            final Bundle args = getArguments();

            // restore the username from settings
            final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
            ((EditText) layout.findViewById(R.id.username)).setText(username);

            // new account toggle
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

            // status strip
            final TextView status = (TextView) layout.findViewById(R.id.status);
            final View progressbar = layout.findViewById(R.id.progressbar);
            final View progress = layout.findViewById(R.id.progress);
            progress.setVisibility(View.GONE);

            final Button connectbtn = (Button) layout.findViewById(R.id.connectbutton);
            final Button disconnectbtn = (Button) layout.findViewById(R.id.disconnectbutton);

            // disable disconnect button if there are no saved credentials
            if (username.length() == 0) {
                disconnectbtn.setEnabled(false);
            }

            connectbtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // prevent multiple instances
                    connectbtn.setEnabled(false);
                    disconnectbtn.setEnabled(false);

                    final String username = ((EditText) layout.findViewById(R.id.username))
                            .getText().toString();
                    final String passwordHash = Utils.toSHA1(((EditText) layout
                            .findViewById(R.id.password)).getText().toString().getBytes());
                    final String email = ((EditText) layout.findViewById(R.id.email)).getText()
                            .toString();
                    final boolean isNewAccount = ((CheckBox) layout
                            .findViewById(R.id.checkNewAccount)).isChecked();
                    final String traktApiKey = getResources().getString(R.string.trakt_apikey);

                    AsyncTask<String, Void, Response> accountValidatorTask = new AsyncTask<String, Void, Response>() {

                        @Override
                        protected void onPreExecute() {
                            progress.setVisibility(View.VISIBLE);
                            progressbar.setVisibility(View.VISIBLE);
                            status.setText(R.string.waitplease);
                        }

                        @Override
                        protected Response doInBackground(String... params) {
                            // SHA of any password is always non-empty, so only
                            // check for an empty username
                            if (username.length() == 0) {
                                return null;
                            }

                            // check for connectivity
                            if (!Utils.isNetworkConnected(context)) {
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = context.getString(R.string.offline);
                                return r;
                            }

                            // use a separate ServiceManager here to avoid
                            // setting wrong credentials
                            final ServiceManager manager = new ServiceManager();
                            manager.setApiKey(traktApiKey);
                            manager.setAuthentication(username, passwordHash);
                            // TODO test the hell out of SSL API
                            manager.setUseSsl(true);

                            Response response = null;

                            try {
                                if (isNewAccount) {
                                    // create new account
                                    response = manager.accountService()
                                            .create(username, passwordHash, email).fire();
                                } else {
                                    // validate existing account
                                    response = manager.accountService().test().fire();
                                }
                            } catch (TraktException te) {
                                response = te.getResponse();
                            } catch (ApiException ae) {
                                response = null;
                            }

                            return response;
                        }

                        @Override
                        protected void onPostExecute(Response response) {
                            progressbar.setVisibility(View.GONE);
                            connectbtn.setEnabled(true);

                            if (response == null) {
                                status.setText(R.string.trakt_generalerror);
                                return;
                            }
                            if (response.status.equals(TraktStatus.FAILURE)) {
                                status.setText(response.error);
                                return;
                            }

                            String passwordEncr;
                            // try to encrypt the password before storing it
                            try {
                                passwordEncr = SimpleCrypto.encrypt(passwordHash, context);
                            } catch (Exception e) {
                                // password encryption failed
                                status.setText(R.string.trakt_generalerror);
                                return;
                            }

                            // prepare writing credentials to settings
                            Editor editor = prefs.edit();
                            editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, username)
                                    .putString(SeriesGuidePreferences.KEY_TRAKTPWD, passwordEncr);

                            if (response.status.equals(TraktStatus.SUCCESS)
                                    && passwordEncr.length() != 0 && editor.commit()) {
                                // set new auth data for service manager
                                try {
                                    Utils.getServiceManagerWithAuth(context, true);
                                } catch (Exception e) {
                                    status.setText(R.string.trakt_generalerror);
                                    return;
                                }

                                // all went through
                                dismiss();

                                if (isForwardingGivenTask) {
                                    if (TraktAction.values()[args.getInt(ShareItems.TRAKTACTION)] == TraktAction.CHECKIN_EPISODE) {
                                        FragmentTransaction ft = fm.beginTransaction();
                                        Fragment prev = fm.findFragmentByTag("progress-dialog");
                                        if (prev != null) {
                                            ft.remove(prev);
                                        }
                                        ProgressDialog newFragment = ProgressDialog.newInstance();
                                        newFragment.show(ft, "progress-dialog");
                                    }

                                    // relaunch the trakt task which called us
                                    new TraktTask(context, fm, args, null).execute();
                                }
                            }
                        }
                    };

                    accountValidatorTask.execute();
                }
            });

            disconnectbtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // clear trakt credentials
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            Editor editor = prefs.edit();
                            editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, "").putString(
                                    SeriesGuidePreferences.KEY_TRAKTPWD, "");
                            editor.commit();

                            try {
                                Utils.getServiceManagerWithAuth(context, false).setAuthentication(
                                        "", "");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            return null;
                        }
                    }.execute();

                    dismiss();
                }
            });

            return layout;
        }
    }

    public static class TraktCancelCheckinDialogFragment extends DialogFragment {

        private int mWait;

        public static TraktCancelCheckinDialogFragment newInstance(Bundle traktData, int wait) {
            TraktCancelCheckinDialogFragment f = new TraktCancelCheckinDialogFragment();
            f.setArguments(traktData);
            f.mWait = wait;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity().getApplicationContext();
            final FragmentManager fm = getFragmentManager();
            final Bundle args = getArguments();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(context.getString(R.string.traktcheckin_inprogress,
                    DateUtils.formatElapsedTime(mWait)));

            builder.setPositiveButton(R.string.traktcheckin_cancel, new OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    FragmentTransaction ft = fm.beginTransaction();
                    Fragment prev = fm.findFragmentByTag("progress-dialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ProgressDialog newFragment = ProgressDialog.newInstance();
                    newFragment.show(ft, "progress-dialog");

                    AsyncTask<String, Void, Response> cancelCheckinTask = new AsyncTask<String, Void, Response>() {

                        @Override
                        protected Response doInBackground(String... params) {

                            ServiceManager manager;
                            try {
                                manager = Utils.getServiceManagerWithAuth(context, false);
                            } catch (Exception e) {
                                // password could not be decrypted
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = context.getString(R.string.trakt_decryptfail);
                                return r;
                            }

                            Response response;
                            try {
                                response = manager.showService().cancelCheckin().fire();
                            } catch (TraktException te) {
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = te.getMessage();
                                return r;
                            } catch (ApiException e) {
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = e.getMessage();
                                return r;
                            }
                            return response;
                        }

                        @Override
                        protected void onPostExecute(Response r) {
                            if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {
                                // all good
                                Toast.makeText(
                                        context,
                                        context.getString(R.string.trakt_success) + ": "
                                                + r.message, Toast.LENGTH_SHORT).show();

                                // relaunch the trakt task which called us to
                                // try the check in again
                                new TraktTask(context, fm, args, null).execute();
                            } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                                // well, something went wrong
                                Toast.makeText(context,
                                        context.getString(R.string.trakt_error) + ": " + r.error,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    cancelCheckinTask.execute();
                }
            });
            builder.setNegativeButton(R.string.traktcheckin_wait, null);

            return builder.create();
        }
    }

    public static class TraktRateDialogFragment extends DialogFragment {

        /**
         * Create {@link TraktRateDialogFragment} to rate a show.
         * 
         * @param tvdbid
         * @return TraktRateDialogFragment
         */
        public static TraktRateDialogFragment newInstance(int tvdbid) {
            TraktRateDialogFragment f = new TraktRateDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_SHOW.index);
            args.putInt(ShareItems.TVDBID, tvdbid);
            f.setArguments(args);
            return f;
        }

        /**
         * Create {@link TraktRateDialogFragment} to rate an episode.
         * 
         * @param showTvdbid
         * @param season
         * @param episode
         * @return TraktRateDialogFragment
         */
        public static TraktRateDialogFragment newInstance(int showTvdbid, int season, int episode) {
            TraktRateDialogFragment f = new TraktRateDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_EPISODE.index);
            args.putInt(ShareItems.TVDBID, showTvdbid);
            args.putInt(ShareItems.SEASON, season);
            args.putInt(ShareItems.EPISODE, episode);
            f.setArguments(args);
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
                    new TraktTask(context, getFragmentManager(), getArguments(), null).execute();
                    dismiss();
                }
            });

            weakSauce.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Rating rating = Rating.Hate;
                    getArguments().putString(ShareItems.RATING, rating.toString());
                    new TraktTask(context, getFragmentManager(), getArguments(), null).execute();
                    dismiss();
                }
            });

            builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    public static class ProgressDialog extends DialogFragment {

        public static ProgressDialog newInstance() {
            ProgressDialog f = new ProgressDialog();
            f.setCancelable(false);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setStyle(STYLE_NO_TITLE, 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.progress_dialog, container, false);
            return v;
        }
    }

    public static boolean isTraktCredentialsValid(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");

        return (!username.equalsIgnoreCase("") && !password.equalsIgnoreCase(""));
    }
}
