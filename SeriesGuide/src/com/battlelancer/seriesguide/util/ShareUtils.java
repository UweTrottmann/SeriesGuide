
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.getglueapi.GetGlue.CheckInTask;
import com.battlelancer.seriesguide.getglueapi.PrepareRequestTokenActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowInfoActivity;
import com.battlelancer.seriesguide.util.ShareUtils.TraktTask.OnTraktActionCompleteListener;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
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
import android.text.InputFilter;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class ShareUtils {

    public static final String KEY_GETGLUE_COMMENT = "com.battlelancer.seriesguide.getglue.comment";

    public static final String KEY_GETGLUE_IMDBID = "com.battlelancer.seriesguide.getglue.imdbid";

    protected static final String TAG = "ShareUtils";

    public enum ShareMethod {
        CHECKIN_GETGLUE(0, R.string.menu_checkin_getglue, R.drawable.ic_getglue),

        CHECKIN_TRAKT(1, R.string.menu_checkin_trakt, R.drawable.ic_trakt),

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
        final String imdbId = args.getString(ShareUtils.ShareItems.IMDBID);
        final String sharestring = args.getString(ShareUtils.ShareItems.SHARESTRING);

        // save used share method, so we can use it later for the quick share
        // button
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().putInt(SeriesGuidePreferences.KEY_LAST_USED_SHARE_METHOD, shareMethod.index)
                .commit();

        switch (shareMethod) {
            case CHECKIN_GETGLUE: {
                // GetGlue check in
                if (imdbId.length() != 0) {
                    showGetGlueDialog(fm, args);
                } else {
                    Toast.makeText(activity, activity.getString(R.string.checkin_impossible),
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            case CHECKIN_TRAKT: {
                // trakt check in

                // DialogFragment.show() will take care of
                // adding the fragment
                // in a transaction. We also want to remove
                // any currently showing
                // dialog, so make our own transaction and
                // take care of that here.
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("progress-dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ProgressDialog newFragment = ProgressDialog.newInstance();
                newFragment.show(ft, "progress-dialog");

                // start the trakt check in task, add the
                // dialog as listener
                args.putInt(ShareItems.TRAKTACTION, TraktAction.CHECKIN_EPISODE.index());
                new TraktTask(activity, fm, args, null).execute();

                break;
            }
            case MARKSEEN_TRAKT: {
                // trakt mark as seen
                args.putInt(ShareItems.TRAKTACTION, TraktAction.SEEN_EPISODE.index());
                new TraktTask(activity, fm, args, listener).execute();
                break;
            }
            case RATE_TRAKT: {
                // trakt rate
                args.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_EPISODE.index());
                TraktRateDialogFragment newFragment = TraktRateDialogFragment.newInstance(args);
                FragmentTransaction ft = fm.beginTransaction();
                newFragment.show(ft, "traktratedialog");
                break;
            }
            case OTHER_SERVICES: {
                // Android apps
                String text = sharestring;
                if (imdbId.length() != 0) {
                    text += " " + ShowInfoActivity.IMDB_TITLE_URL + imdbId;
                }

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, text);
                activity.startActivity(Intent.createChooser(i,
                        activity.getString(R.string.share_episode)));
                break;
            }
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
            final FragmentActivity activity = getActivity();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());

            final LinearLayout layout = new LinearLayout(getActivity());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(16, 16, 16, 16);

            input = new EditText(getActivity());
            input.setMinLines(3);
            input.setMaxLines(5);
            input.setGravity(Gravity.TOP);
            input.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(140)
            });

            layout.addView(input, layoutParams);

            if (savedInstanceState != null) {
                input.setText(savedInstanceState.getString("inputtext"));
            } else {
                input.setText(episodestring);
            }

            return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.comment))
                    .setView(layout)
                    .setPositiveButton(R.string.checkin, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String comment = input.getText().toString();

                            // if we are authenticated go ahead, if not
                            // authenticate first
                            if (GetGlue.isAuthenticated(prefs)) {
                                new CheckInTask(imdbId, comment, activity).execute();
                            } else {
                                Intent i = new Intent(activity, PrepareRequestTokenActivity.class);
                                i.putExtra(ShareUtils.KEY_GETGLUE_IMDBID, imdbId);
                                i.putExtra(ShareUtils.KEY_GETGLUE_COMMENT, comment);
                                startActivity(i);
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, null).create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("inputtext", input.getText().toString());
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

        String ISSPOILER = "isspoiler";
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
        SEEN_EPISODE(0), RATE_EPISODE(1), CHECKIN_EPISODE(2), SHOUT(3);

        final private int index;

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

    public static class TraktTask extends AsyncTask<Void, Void, Response> {

        private Bundle mArgs;

        private final Context mContext;

        private final FragmentManager mFm;

        private TraktAction mAction;

        private OnTraktActionCompleteListener mListener;

        public interface OnTraktActionCompleteListener {
            public void onTraktActionComplete();
        }

        /**
         * Initial constructor. Call <b>one</b> of the setup-methods, like
         * {@code shout(tvdbid, shout, isSpoiler)}, afterwards.
         * 
         * @param context
         * @param fm
         * @param listener
         */
        public TraktTask(Context context, FragmentManager fm, OnTraktActionCompleteListener listener) {
            mContext = context;
            mFm = fm;
            mListener = listener;
            mArgs = new Bundle();
        }

        /**
         * Fast constructor, allows passing of an already pre-built {@code args}
         * {@link Bundle}.
         * 
         * @param context
         * @param manager
         * @param args
         * @param listener
         */
        public TraktTask(Context context, FragmentManager manager, Bundle args,
                OnTraktActionCompleteListener listener) {
            this(context, manager, listener);
            mArgs = args;
        }

        /**
         * Post a shout for a show.
         * 
         * @param tvdbid
         * @param shout
         * @param isSpoiler
         * @return
         */
        public TraktTask shout(int tvdbid, String shout, boolean isSpoiler) {
            mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.SHOUT.index);
            mArgs.putInt(ShareItems.TVDBID, tvdbid);
            mArgs.putString(ShareItems.SHARESTRING, shout);
            mArgs.putBoolean(ShareItems.ISSPOILER, isSpoiler);
            return this;
        }

        /**
         * Post a shout for an episode.
         * 
         * @param tvdbid
         * @param season
         * @param episode
         * @param shout
         * @param isSpoiler
         * @return
         */
        public TraktTask shout(int tvdbid, int season, int episode, String shout, boolean isSpoiler) {
            shout(tvdbid, shout, isSpoiler);
            mArgs.putInt(ShareItems.SEASON, season);
            mArgs.putInt(ShareItems.EPISODE, episode);
            return this;
        }

        @Override
        protected Response doInBackground(Void... params) {
            // check for valid credentials
            if (!isTraktCredentialsValid(mContext)) {
                // return null so a credentials dialog is displayed
                // it will call us again with valid credentials
                return null;
            }

            // get an authenticated trakt-java ServiceManager
            ServiceManager manager;
            try {
                manager = Utils.getServiceManagerWithAuth(mContext, false);
            } catch (Exception e) {
                // password could not be decrypted
                Response r = new Response();
                r.status = TraktStatus.FAILURE;
                r.error = mContext.getString(R.string.trakt_decryptfail);
                return r;
            }

            // get values used by all actions
            final int tvdbid = mArgs.getInt(ShareItems.TVDBID);
            final int season = mArgs.getInt(ShareItems.SEASON);
            final int episode = mArgs.getInt(ShareItems.EPISODE);

            // last chance to abort
            if (isCancelled()) {
                return null;
            }

            // execute the trakt action
            mAction = TraktAction.values()[mArgs.getInt(ShareItems.TRAKTACTION)];
            try {
                Response r = null;

                switch (mAction) {
                    case CHECKIN_EPISODE: {
                        r = manager.showService().checkin(tvdbid).season(season).episode(episode)
                                .fire();
                        break;
                    }
                    case SEEN_EPISODE: {
                        manager.showService().episodeSeen(tvdbid).episode(season, episode).fire();
                        r = new Response();
                        r.status = TraktStatus.SUCCESS;
                        r.message = mContext.getString(R.string.trakt_seen);

                        // immediately mark episode as seen locally
                        ContentValues values = new ContentValues();
                        values.put(Episodes.WATCHED, true);
                        mContext.getContentResolver().update(
                                Episodes.buildEpisodesOfShowUri(String.valueOf(tvdbid)), values,
                                Episodes.SEASON + "=? AND " + Episodes.NUMBER + "=?", new String[] {
                                        String.valueOf(season), String.valueOf(episode)
                                });
                        mContext.getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
                        break;
                    }
                    case RATE_EPISODE: {
                        final Rating rating = Rating.fromValue(mArgs.getString(ShareItems.RATING));
                        r = manager.rateService().episode(tvdbid).season(season).episode(episode)
                                .rating(rating).fire();
                        break;
                    }
                    case SHOUT: {
                        final String shout = mArgs.getString(ShareItems.SHARESTRING);
                        final boolean isSpoiler = mArgs.getBoolean(ShareItems.ISSPOILER);

                        if (episode == 0) {
                            r = manager.shoutService().show(tvdbid).shout(shout).fire();
                        } else {
                            r = manager.shoutService().episode(tvdbid).season(season)
                                    .episode(episode).shout(shout).fire();
                        }
                    }
                }

                return r;
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
        }

        @Override
        protected void onPostExecute(Response r) {
            FragmentTransaction ft = mFm.beginTransaction();

            // dismiss a potential progress dialog
            if (mAction == TraktAction.CHECKIN_EPISODE) {
                Fragment prev = mFm.findFragmentByTag("progress-dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
            }

            if (r != null) {
                if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {
                    // all good
                    Toast.makeText(mContext,
                            mContext.getString(R.string.trakt_success) + ": " + r.message,
                            Toast.LENGTH_SHORT).show();

                    if (mAction == TraktAction.CHECKIN_EPISODE) {
                        ft.commit();
                    }
                } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                    if (r.wait != 0) {
                        // looks like a check in is in progress
                        TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                                .newInstance(mArgs, r.wait);
                        newFragment.show(ft, "cancel-checkin-dialog");
                    } else {
                        // well, something went wrong
                        Toast.makeText(mContext,
                                mContext.getString(R.string.trakt_error) + ": " + r.error,
                                Toast.LENGTH_LONG).show();

                        if (mAction == TraktAction.CHECKIN_EPISODE) {
                            ft.commit();
                        }
                    }
                }
            } else {
                // fail, gather valid credentials first
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance(mArgs);
                newFragment.show(ft, "traktdialog");
            }

            // notify that our first run completed, however due to invalid
            // credentials we might not have done anything
            if (mListener != null) {
                mListener.onTraktActionComplete();
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
                            disconnectbtn.setEnabled(true);

                            if (response == null) {
                                status.setText(R.string.trakt_generalerror);
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

                                    // relaunch the trakt task which called
                                    // us
                                    new TraktTask(context, fm, args, null).execute();
                                }
                            } else if (response.status.equals(TraktStatus.FAILURE)) {
                                // credentials were wrong or account
                                // creation failed
                                status.setText(response.error);
                            } else {
                                // unknown error message from trakt
                                status.setText(R.string.trakt_generalerror);
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
