
package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.services.ShowService;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Helps flag episodes in the local database and on trakt.tv.
 */
public class FlagTask extends AsyncTask<Void, Integer, Integer> {

    private static final int FAILED = -1;

    private static final int OFFLINE = -2;

    private static final int SUCCESS = 0;

    private static final String TAG = "FlagTask";

    public interface OnFlagListener {
        /**
         * Called if flags were sent to trakt or failed to got sent and were not
         * set locally. If trakt is not used, called once flags are set in the
         * local database.
         */
        public void onFlagCompleted(FlagAction action, int showId, int itemId, boolean isSuccessful);
    }

    public enum FlagAction {
        EPISODE_WATCHED,

        EPISODE_COLLECTED,

        EPISODE_WATCHED_PREVIOUS,

        SEASON_WATCHED,

        SHOW_WATCHED;
    }

    private Context mContext;

    private int mShowId;

    private int mItemId;

    private int mSeason;

    private int mEpisode;

    private long mFirstAired;

    private boolean mIsFlag;

    private FlagAction mAction;

    private OnFlagListener mListener;

    private boolean mIsTraktInvolved;

    public FlagTask(Context context, int showId, OnFlagListener listener) {
        mContext = context;
        mShowId = showId;
        mListener = listener;
    }

    /**
     * Set the item id of the episode, season or show.
     */
    public FlagTask setItemId(int itemId) {
        mItemId = itemId;
        return this;
    }

    /**
     * Set whether to add or remove the flag. This value is ignored when using
     * {@link #episodeWatchedPrevious()} as it always adds the watched flag.
     */
    public FlagTask setFlag(boolean flag) {
        mIsFlag = flag;
        return this;
    }

    public FlagTask episodeWatched(int seasonNumber, int episodeNumber) {
        mSeason = seasonNumber;
        mEpisode = episodeNumber;
        mAction = FlagAction.EPISODE_WATCHED;
        return this;
    }

    public FlagTask episodeCollected(int seasonNumber, int episodeNumber) {
        mSeason = seasonNumber;
        mEpisode = episodeNumber;
        mAction = FlagAction.EPISODE_COLLECTED;
        return this;
    }

    /**
     * Flag all episodes previous to this one as watched. Previous in terms of
     * air date.
     * 
     * @param firstaired
     */
    public FlagTask episodeWatchedPrevious(long firstaired) {
        mFirstAired = firstaired;
        mAction = FlagAction.EPISODE_WATCHED_PREVIOUS;
        return this;
    }

    public FlagTask seasonWatched(int seasonNumber) {
        mSeason = seasonNumber;
        mAction = FlagAction.SEASON_WATCHED;
        return this;
    }

    public FlagTask showWatched() {
        mAction = FlagAction.SHOW_WATCHED;
        return this;
    }

    /**
     * Run the task on the thread pool.
     */
    public void execute() {
        AndroidUtils.executeAsyncTask(this, new Void[] {});
    }

    @Override
    protected Integer doInBackground(Void... params) {
        mIsTraktInvolved = Utils.isTraktCredentialsValid(mContext);

        // do trakt stuff
        // check for valid trakt credentials
        if (mIsTraktInvolved) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return OFFLINE;
            }

            try {
                ServiceManager manager = Utils.getServiceManagerWithAuth(mContext, false);
                ShowService showService = manager.showService();

                switch (mAction) {
                    case EPISODE_WATCHED:
                        // flag a single episode watched
                        if (mIsFlag) {
                            showService.episodeSeen(mShowId).episode(mSeason, mEpisode).fire();
                        } else {
                            showService.episodeUnseen(mShowId).episode(mSeason, mEpisode).fire();
                        }
                        break;
                    case EPISODE_COLLECTED:
                        // flag a single episode collected
                        if (mIsFlag) {
                            showService.episodeLibrary(mShowId).episode(mSeason, mEpisode).fire();
                        } else {
                            showService.episodeUnlibrary(mShowId).episode(mSeason, mEpisode).fire();
                        }
                        break;
                    case SEASON_WATCHED:
                        // flag a whole season watched
                        if (mIsFlag) {
                            showService.seasonSeen(mShowId).season(mSeason).fire();
                        } else {
                            // TODO Support removing seen flags for season
                        }
                        break;
                    default:
                        break;
                }
            } catch (TraktException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
                return FAILED;
            } catch (ApiException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
                return FAILED;
            } catch (Exception e) {
                // password could likely not be decrypted
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
                return FAILED;
            }
        }

        // update local database if trakt did not fail or if it is not used
        updateDatabase(mItemId);

        return SUCCESS;
    }

    private void updateDatabase(int itemId) {
        // determine flag column
        String column;
        switch (mAction) {
            case EPISODE_WATCHED:
            case EPISODE_WATCHED_PREVIOUS:
            case SEASON_WATCHED:
            case SHOW_WATCHED:
                column = Episodes.WATCHED;
                break;
            case EPISODE_COLLECTED:
                column = Episodes.COLLECTED;
                break;
            default:
                return;
        }

        // determine query uri
        Uri uri;
        switch (mAction) {
            case EPISODE_WATCHED:
            case EPISODE_COLLECTED:
                uri = Episodes.buildEpisodeUri(String.valueOf(itemId));
                break;
            case SEASON_WATCHED:
                uri = Episodes.buildEpisodesOfSeasonUri(String.valueOf(itemId));
                break;
            case EPISODE_WATCHED_PREVIOUS:
            case SHOW_WATCHED:
                uri = Episodes.buildEpisodesOfShowUri(String.valueOf(mShowId));
                break;
            default:
                return;
        }

        // build and execute query
        ContentValues values = new ContentValues();
        if (mAction == FlagAction.EPISODE_WATCHED_PREVIOUS) {
            // mark all previously aired episodes
            if (mFirstAired > 0) {
                values.put(column, true);
                mContext.getContentResolver().update(
                        uri,
                        values,
                        Episodes.FIRSTAIREDMS + "<" + mFirstAired + " AND " + Episodes.FIRSTAIREDMS
                                + ">0", null);
            }
        } else {
            values.put(column, mIsFlag);
            mContext.getContentResolver().update(uri, values, null, null);
        }

        // notify the content provider for certain udpates
        switch (mAction) {
            case EPISODE_WATCHED:
            case EPISODE_COLLECTED:
            case EPISODE_WATCHED_PREVIOUS:
                mContext.getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        // display a small toast if submission to trakt was successful
        if (mIsTraktInvolved) {
            int message;
            switch (mAction) {
                case EPISODE_WATCHED:
                    if (mIsFlag) {
                        message = R.string.trakt_seen;
                    } else {
                        message = R.string.trakt_notseen;
                    }
                    break;
                case EPISODE_COLLECTED:
                    if (mIsFlag) {
                        message = R.string.trakt_collected;
                    } else {
                        message = R.string.trakt_notcollected;
                    }
                    break;
                default:
                    message = 0;
                    break;
            }

            if (message != 0) {
                int status = 0;
                int duration = 0;
                switch (result) {
                    case SUCCESS: {
                        status = R.string.trakt_submitsuccess;
                        duration = Toast.LENGTH_SHORT;
                        break;
                    }
                    case FAILED: {
                        status = R.string.trakt_submitfailed;
                        duration = Toast.LENGTH_LONG;
                        break;
                    }
                    case OFFLINE: {
                        status = R.string.offline;
                        duration = Toast.LENGTH_LONG;
                        break;
                    }
                }

                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(mContext);
                final String number = Utils.getEpisodeNumber(prefs, mSeason, mEpisode);
                Toast.makeText(mContext,
                        mContext.getString(message, number) + " " + mContext.getString(status),
                        duration).show();
            }
        }

        if (mListener != null) {
            mListener.onFlagCompleted(mAction, mShowId, mItemId, result == SUCCESS ? true : false);
        }
    }
}
