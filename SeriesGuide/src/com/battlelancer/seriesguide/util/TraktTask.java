
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.ui.dialogs.TraktCancelCheckinDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktCredentialsDialogFragment;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;
import com.jakewharton.trakt.services.ShowService.CheckinBuilder;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

public class TraktTask extends AsyncTask<Void, Void, Response> {

    private static final String TAG = "TraktTask";

    private Bundle mArgs;

    private final Context mContext;

    private final FragmentManager mFm;

    private TraktAction mAction;

    private OnTraktActionCompleteListener mListener;

    public interface OnTraktActionCompleteListener {
        public void onTraktActionComplete(boolean wasSuccessfull);
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
     * Check into an episode. Optionally provide a checkin message.
     * 
     * @param tvdbid
     * @param season
     * @param episode
     * @param message
     * @return TraktTask
     */
    public TraktTask checkin(int tvdbid, int season, int episode, String message) {
        mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.CHECKIN_EPISODE.index);
        mArgs.putInt(ShareItems.TVDBID, tvdbid);
        mArgs.putInt(ShareItems.SEASON, season);
        mArgs.putInt(ShareItems.EPISODE, episode);
        mArgs.putString(ShareItems.SHARESTRING, message);
        return this;
    }

    /**
     * Rate an episode.
     * 
     * @param showTvdbid
     * @param season
     * @param episode
     * @param rating
     * @return TraktTask
     */
    public TraktTask rateEpisode(int showTvdbid, int season, int episode, Rating rating) {
        mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_EPISODE.index);
        mArgs.putInt(ShareItems.TVDBID, showTvdbid);
        mArgs.putInt(ShareItems.SEASON, season);
        mArgs.putInt(ShareItems.EPISODE, episode);
        mArgs.putString(ShareItems.RATING, rating.toString());
        return this;
    }

    /**
     * Rate a show.
     * 
     * @param tvdbid
     * @param rating
     * @return TraktTask
     */
    public TraktTask rateShow(int tvdbid, Rating rating) {
        mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_SHOW.index);
        mArgs.putInt(ShareItems.TVDBID, tvdbid);
        mArgs.putString(ShareItems.RATING, rating.toString());
        return this;
    }

    /**
     * Post a shout for a show.
     * 
     * @param tvdbid
     * @param shout
     * @param isSpoiler
     * @return TraktTask
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
     * @return TraktTask
     */
    public TraktTask shout(int tvdbid, int season, int episode, String shout, boolean isSpoiler) {
        shout(tvdbid, shout, isSpoiler);
        mArgs.putInt(ShareItems.SEASON, season);
        mArgs.putInt(ShareItems.EPISODE, episode);
        return this;
    }

    @Override
    protected Response doInBackground(Void... params) {
        // we need this value in onPostExecute, so get it already here
        mAction = TraktAction.values()[mArgs.getInt(ShareItems.TRAKTACTION)];

        // check for network connection
        if (!Utils.isNetworkConnected(mContext)) {
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.offline);
            return r;
        }

        // check for valid credentials
        if (!Utils.isTraktCredentialsValid(mContext)) {
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
            r.error = mContext.getString(R.string.trakt_generalerror);
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

        try {
            Response r = null;

            switch (mAction) {
                case CHECKIN_EPISODE: {
                    final String message = mArgs.getString(ShareItems.SHARESTRING);

                    final CheckinBuilder checkinBuilder = manager.showService().checkin(tvdbid)
                            .season(season).episode(episode);
                    if (message != null && message.length() != 0) {
                        checkinBuilder.message(message);
                    }
                    r = checkinBuilder.fire();

                    break;
                }
                case RATE_EPISODE: {
                    final Rating rating = Rating.fromValue(mArgs.getString(ShareItems.RATING));
                    r = manager.rateService().episode(tvdbid).season(season).episode(episode)
                            .rating(rating).fire();
                    break;
                }
                case RATE_SHOW: {
                    final Rating rating = Rating.fromValue(mArgs.getString(ShareItems.RATING));
                    r = manager.rateService().show(tvdbid).rating(rating).fire();
                    break;
                }
                case SHOUT: {
                    final String shout = mArgs.getString(ShareItems.SHARESTRING);
                    final boolean isSpoiler = mArgs.getBoolean(ShareItems.ISSPOILER);

                    if (episode == 0) {
                        r = manager.shoutService().show(tvdbid).shout(shout).spoiler(isSpoiler)
                                .fire();
                    } else {
                        r = manager.shoutService().episode(tvdbid).season(season).episode(episode)
                                .shout(shout).spoiler(isSpoiler).fire();
                    }
                }
            }

            return r;
        } catch (TraktException e) {
            fireTrackerEvent(e.getMessage());
            Log.w(ShareUtils.TAG, e);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        } catch (ApiException e) {
            fireTrackerEvent(e.getMessage());
            Log.w(ShareUtils.TAG, e);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        }
    }

    @Override
    protected void onPostExecute(Response r) {
        // dismiss a potential progress dialog
        if (mAction == TraktAction.CHECKIN_EPISODE) {
            Fragment prev = mFm.findFragmentByTag("progress-dialog");
            if (prev != null) {
                FragmentTransaction ft = mFm.beginTransaction();
                ft.remove(prev);
                ft.commit();
            }
        }

        if (r != null) {
            if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {

                // all good
                Toast.makeText(mContext,
                        mContext.getString(R.string.trakt_success) + ": " + r.message,
                        Toast.LENGTH_SHORT).show();

                if (mListener != null) {
                    mListener.onTraktActionComplete(true);
                }

            } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                if (r.wait != 0) {

                    // looks like a check in is in progress
                    TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                            .newInstance(mArgs, r.wait);
                    FragmentTransaction ft = mFm.beginTransaction();
                    newFragment.show(ft, "cancel-checkin-dialog");

                } else {

                    // well, something went wrong
                    Toast.makeText(mContext, r.error, Toast.LENGTH_LONG).show();

                }

                if (mListener != null) {
                    mListener.onTraktActionComplete(false);
                }
            }
        } else {
            // fail, gather valid credentials first
            TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                    .newInstance(mArgs);
            FragmentTransaction ft = mFm.beginTransaction();
            newFragment.show(ft, "traktdialog");

            // notify that our first run completed, however due to invalid
            // credentials we have not done anything
            if (mListener != null) {
                mListener.onTraktActionComplete(true);
            }
        }
    }

    private void fireTrackerEvent(String message) {
        AnalyticsUtils.getInstance(mContext).trackEvent(TAG, "Update result", message, 0);
    }
}
