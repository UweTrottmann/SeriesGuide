/*
 * Copyright 2013 Uwe Trottmann
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

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.FlagTapeEntry.Flag;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.seriesguide.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Helps flag episodes in the local database and readies them for submission to trakt.tv.
 */
public class FlagTask extends AsyncTask<Void, Integer, Integer> {

    /**
     * Sent once the database ops are finished, sending to trakt may still be in progress or queued
     * due to no available connection.
     */
    public class FlagTaskCompletedEvent {

        public FlagTaskType mType;

        public FlagTaskCompletedEvent(FlagTaskType type) {
            mType = type;
        }
    }

    public enum FlagAction {
        EPISODE_WATCHED,

        EPISODE_COLLECTED,

        EPISODE_WATCHED_PREVIOUS,

        SEASON_WATCHED,

        SEASON_COLLECTED,

        SHOW_WATCHED,

        SHOW_COLLECTED;
    }

    public static abstract class FlagTaskType {

        protected Context mContext;

        // TODO Deprecate by using more polymorphism.
        protected FlagAction mAction;

        protected int mShowTvdbId;

        protected int mEpisodeFlag;

        public abstract Uri getUri();

        public abstract String getSelection();

        public abstract List<Flag> getEpisodes();

        public FlagTaskType(Context context, int showTvdbId) {
            mContext = context;
            mShowTvdbId = showTvdbId;
        }

        public int getShowTvdbId() {
            return mShowTvdbId;
        }

        /**
         * Builds a list of {@link Flag} objects to pass to a {@link FlagTapedTask} to submit to
         * trakt.
         */
        protected List<Flag> createEpisodeFlags() {
            List<Flag> episodes = Lists.newArrayList();

            // determine uri
            Uri uri = getUri();
            String selection = getSelection();

            // query and add episodes to list
            final Cursor episodeCursor = mContext.getContentResolver().query(
                    uri,
                    new String[]{
                            Episodes.SEASON, Episodes.NUMBER
                    }, selection, null, null);
            if (episodeCursor != null) {
                while (episodeCursor.moveToNext()) {
                    episodes.add(new Flag(episodeCursor.getInt(0), episodeCursor.getInt(1)));
                }
                episodeCursor.close();
            }

            return episodes;
        }

        /**
         * Return the column which should get updated, either {@link Episodes} .WATCHED or {@link
         * Episodes}.COLLECTED.
         */
        protected abstract String getColumn();

        protected abstract ContentValues getContentValues();

        /**
         * Builds and executes the database op required to flag episodes in the local database.
         */
        public void updateDatabase() {
            // determine query uri
            Uri uri = getUri();
            if (uri == null) {
                return;
            }

            // build and execute query
            ContentValues values = getContentValues();
            mContext.getContentResolver().update(uri, values, getSelection(), null);

            // notify the content provider for udpates
            mContext.getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
            mContext.getContentResolver()
                    .notifyChange(ListItems.CONTENT_WITH_DETAILS_URI, null);
        }

        /**
         * Determines the last watched episode and returns its TVDb id or -1 if it can't be
         * determined.
         */
        protected abstract int getLastEpisodeTvdbId();

        /**
         * Saves the last watched episode for a show to the database.
         */
        public void storeLastEpisode() {
            int lastWatchedId = getLastEpisodeTvdbId();
            if (lastWatchedId != -1) {
                // set latest watched
                ContentValues values = new ContentValues();
                values.put(Shows.LASTWATCHEDID, lastWatchedId);
                mContext.getContentResolver().update(
                        Shows.buildShowUri(String.valueOf(mShowTvdbId)),
                        values, null, null);
            }
        }

        /**
         * Returns the text which should be prepended to the submission status message. Tells e.g.
         * which episode was flagged watched.
         */
        public abstract String getNotificationText();
    }

    /**
     * Flagging single episodes watched or collected.
     */
    public static abstract class EpisodeType extends FlagTaskType {

        protected int mEpisodeTvdbId;

        protected int mSeason;

        protected int mEpisode;

        public EpisodeType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId);
            mEpisodeTvdbId = episodeTvdbId;
            mSeason = season;
            mEpisode = episode;
            mEpisodeFlag = episodeFlags;
        }

        @Override
        public Uri getUri() {
            return Episodes.buildEpisodeUri(String.valueOf(mEpisodeTvdbId));
        }

        @Override
        public String getSelection() {
            return null;
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), mEpisodeFlag);
            return values;
        }

        @Override
        public List<Flag> getEpisodes() {
            List<Flag> episodes = Lists.newArrayList();
            // flag a single episode
            episodes.add(new Flag(mSeason, mEpisode));
            return episodes;
        }
    }

    public static class EpisodeWatchedType extends EpisodeType {

        public EpisodeWatchedType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags);
            mAction = FlagAction.EPISODE_WATCHED;
        }

        @Override
        protected String getColumn() {
            return Episodes.WATCHED;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            if (EpisodeTools.isUnwatched(mEpisodeFlag)) {
                // unwatched episode
                int lastWatchedId = -1;

                final Cursor show = mContext.getContentResolver().query(
                        Shows.buildShowUri(String.valueOf(mShowTvdbId)),
                        new String[]{
                                Shows._ID, Shows.LASTWATCHEDID
                        }, null, null, null);
                if (show != null) {
                    // identical to last watched episode?
                    if (show.moveToFirst() && show.getInt(1) == mEpisodeTvdbId) {
                        // get latest watched before this one
                        String season = String.valueOf(mSeason);
                        final Cursor latestWatchedEpisode = mContext.getContentResolver()
                                .query(Episodes.buildEpisodesOfShowUri(String
                                        .valueOf(mShowTvdbId)),
                                        PROJECTION_EPISODE, SELECTION_PREVIOUS_WATCHED,
                                        new String[]{
                                                season, season, String.valueOf(mEpisode)
                                        }, ORDER_PREVIOUS_WATCHED);
                        if (latestWatchedEpisode != null) {
                            if (latestWatchedEpisode.moveToFirst()) {
                                lastWatchedId = latestWatchedEpisode.getInt(0);
                            }

                            latestWatchedEpisode.close();
                        }
                    }

                    show.close();
                }

                return lastWatchedId;
            } else {
                // watched or skipped episode
                return mEpisodeTvdbId;
            }
        }

        @Override
        public String getNotificationText() {
            if (EpisodeTools.isSkipped(mEpisodeFlag)) {
                // skipping is not sent to trakt, no need for a message
                return null;
            }

            // show episode seen/unseen message
            String number = Utils.getEpisodeNumber(mContext, mSeason, mEpisode);
            return mContext.getString(
                    EpisodeTools.isWatched(mEpisodeFlag) ? R.string.trakt_seen
                            : R.string.trakt_notseen,
                    number);
        }
    }

    public static class EpisodeCollectedType extends EpisodeType {

        public EpisodeCollectedType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags);
            mAction = FlagAction.EPISODE_COLLECTED;
        }

        @Override
        protected String getColumn() {
            return Episodes.COLLECTED;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            // we don't care
            return -1;
        }

        @Override
        public String getNotificationText() {
            String number = Utils.getEpisodeNumber(mContext, mSeason, mEpisode);
            return mContext.getString(mEpisodeFlag == 1 ? R.string.trakt_collected
                    : R.string.trakt_notcollected, number);
        }
    }

    /**
     * Flagging whole seasons watched or collected.
     */
    public static abstract class SeasonType extends FlagTaskType {

        protected int mSeasonTvdbId;

        protected int mSeason;

        public SeasonType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId);
            mSeasonTvdbId = seasonTvdbId;
            mSeason = season;
            mEpisodeFlag = episodeFlags;
        }

        public int getSeasonTvdbId() {
            return mSeasonTvdbId;
        }

        @Override
        public Uri getUri() {
            return Episodes.buildEpisodesOfSeasonUri(String.valueOf(mSeasonTvdbId));
        }

        @Override
        public String getSelection() {
            return null;
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), mEpisodeFlag);
            return values;
        }

        @Override
        public List<Flag> getEpisodes() {
            if (mEpisodeFlag != 0) {
                // watched, skipped or collected season
                List<Flag> episodes = Lists.newArrayList();
                episodes.add(new Flag(mSeason, -1));
                return episodes;
            } else {
                // unwatched, not collected season
                return createEpisodeFlags();
            }
        }
    }

    public static class SeasonWatchedType extends SeasonType {

        public SeasonWatchedType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId, seasonTvdbId, season, episodeFlags);
            mAction = FlagAction.SEASON_WATCHED;
        }

        @Override
        protected String getColumn() {
            return Episodes.WATCHED;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            if (EpisodeTools.isUnwatched(mEpisodeFlag)) {
                // unwatched season
                // just reset
                return 0;
            } else {
                // watched or skipped season
                int lastWatchedId = -1;

                // get the last flagged episode of the season
                final Cursor seasonEpisodes = mContext.getContentResolver().query(
                        Episodes.buildEpisodesOfSeasonUri(String.valueOf(mSeasonTvdbId)),
                        PROJECTION_EPISODE, null, null, Episodes.NUMBER + " DESC");
                if (seasonEpisodes != null) {
                    if (seasonEpisodes.moveToFirst()) {
                        lastWatchedId = seasonEpisodes.getInt(0);
                    }

                    seasonEpisodes.close();
                }

                return lastWatchedId;
            }
        }

        @Override
        public String getNotificationText() {
            if (EpisodeTools.isSkipped(mEpisodeFlag)) {
                // skipping is not sent to trakt, no need for a message
                return null;
            }

            String number = Utils.getEpisodeNumber(mContext, mSeason, -1);
            return mContext.getString(
                    EpisodeTools.isWatched(mEpisodeFlag) ? R.string.trakt_seen
                            : R.string.trakt_notseen,
                    number);
        }
    }

    public static class SeasonCollectedType extends SeasonType {

        public SeasonCollectedType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId, seasonTvdbId, season, episodeFlags);
            mAction = FlagAction.SEASON_COLLECTED;
        }

        @Override
        protected String getColumn() {
            return Episodes.COLLECTED;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            return -1;
        }

        @Override
        public String getNotificationText() {
            String number = Utils.getEpisodeNumber(mContext, mSeason, -1);
            return mContext.getString(mEpisodeFlag == 1 ? R.string.trakt_collected
                    : R.string.trakt_notcollected, number);
        }
    }

    public static abstract class ShowType extends FlagTaskType {

        public ShowType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId);
            mEpisodeFlag = episodeFlags;
        }

        @Override
        public Uri getUri() {
            return Episodes.buildEpisodesOfShowUri(String.valueOf(mShowTvdbId));
        }

        @Override
        public String getSelection() {
            return null;
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), mEpisodeFlag);
            return values;
        }

        @Override
        public List<Flag> getEpisodes() {
            // only for removing flags we need single episodes
            if (mEpisodeFlag == 0) {
                return createEpisodeFlags();
            } else {
                return null;
            }
        }

        @Override
        public String getNotificationText() {
            return null;
        }
    }

    public static class ShowWatchedType extends ShowType {

        public ShowWatchedType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId, episodeFlags);
            mAction = FlagAction.SHOW_WATCHED;
        }

        @Override
        protected String getColumn() {
            return Episodes.WATCHED;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            if (EpisodeTools.isUnwatched(mEpisodeFlag)) {
                // just reset
                return 0;
            } else {
                // we don't care
                return -1;
            }
        }
    }

    public static class ShowCollectedType extends ShowType {

        public ShowCollectedType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId, episodeFlags);
            mAction = FlagAction.SHOW_COLLECTED;
        }

        @Override
        protected String getColumn() {
            return Episodes.COLLECTED;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            // we don't care
            return -1;
        }
    }

    public static class EpisodeWatchedPreviousType extends FlagTaskType {

        private long mEpisodeFirstAired;

        public EpisodeWatchedPreviousType(Context context, int showTvdbId, long episodeFirstAired) {
            super(context, showTvdbId);
            mEpisodeFirstAired = episodeFirstAired;
            mAction = FlagAction.EPISODE_WATCHED_PREVIOUS;
        }

        @Override
        public Uri getUri() {
            return Episodes.buildEpisodesOfShowUri(String.valueOf(mShowTvdbId));
        }

        @Override
        public String getSelection() {
            return Episodes.FIRSTAIREDMS + "<" + mEpisodeFirstAired + " AND "
                    + Episodes.FIRSTAIREDMS + ">0";
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(Episodes.WATCHED, EpisodeFlags.WATCHED);
            return values;
        }

        @Override
        public List<Flag> getEpisodes() {
            return createEpisodeFlags();
        }

        @Override
        protected String getColumn() {
            // not used
            return null;
        }

        @Override
        protected int getLastEpisodeTvdbId() {
            // we don't care
            return -1;
        }

        @Override
        public String getNotificationText() {
            return null;
        }
    }

    private FlagTaskType mType;

    private Context mContext;

    private boolean mIsTraktInvolved;

    private int mShowTvdbId;

    public FlagTask(Context context, int showTvdbId) {
        mContext = context.getApplicationContext();
        mShowTvdbId = showTvdbId;
    }

    public FlagTask episodeWatched(int episodeTvdbId, int season, int episode,
            int episodeFlags) {
        EpisodeTools.validateFlags(episodeFlags);
        mType = new EpisodeWatchedType(mContext, mShowTvdbId, episodeTvdbId, season, episode,
                episodeFlags);
        return this;
    }

    public FlagTask episodeCollected(int episodeTvdbId, int season, int episode,
            boolean isFlag) {
        mType = new EpisodeCollectedType(mContext, mShowTvdbId, episodeTvdbId, season, episode,
                isFlag ? 1 : 0);
        return this;
    }

    /**
     * Flag all episodes previous to this one as watched. Previous in terms of air date.
     */
    public FlagTask episodeWatchedPrevious(long episodeFirstAired) {
        mType = new EpisodeWatchedPreviousType(mContext, mShowTvdbId, episodeFirstAired);
        return this;
    }

    public FlagTask seasonWatched(int seasonTvdbId, int season, int episodeFlags) {
        EpisodeTools.validateFlags(episodeFlags);
        mType = new SeasonWatchedType(mContext, mShowTvdbId, seasonTvdbId, season, episodeFlags);
        return this;
    }

    public FlagTask seasonCollected(int seasonTvdbId, int season, boolean isFlag) {
        mType = new SeasonCollectedType(mContext, mShowTvdbId, seasonTvdbId, season,
                isFlag ? 1 : 0);
        return this;
    }

    public FlagTask showWatched(boolean isFlag) {
        mType = new ShowWatchedType(mContext, mShowTvdbId, isFlag ? 1 : 0);
        return this;
    }

    public FlagTask showCollected(boolean isFlag) {
        mType = new ShowCollectedType(mContext, mShowTvdbId, isFlag ? 1 : 0);
        return this;
    }

    /**
     * Run the task on the thread pool.
     */
    public void execute() {
        AndroidUtils.executeAsyncTask(this, new Void[]{});
    }

    @Override
    protected Integer doInBackground(Void... params) {
        /**
         * Do net send to trakt if we skipped episodes, this is not supported by trakt.
         * However, if the skipped flag is removed this will be handled identical
         * to flagging as unwatched.
         */
        // check for valid trakt credentials
        mIsTraktInvolved = !EpisodeTools.isSkipped(mType.mEpisodeFlag)
                && ServiceUtils.hasTraktCredentials(mContext);

        // prepare trakt stuff
        if (mIsTraktInvolved) {
            if (!Utils.isAllowedConnection(mContext)) {
                return -1;
            }

            List<Flag> episodes = mType.getEpisodes();

            // convert to boolean flag used by trakt (un/watched, un/collected)
            boolean isFlag = !EpisodeTools.isUnwatched(mType.mEpisodeFlag);

            // Add a new taped flag task to the tape queue
            FlagTapeEntryQueue.getInstance(mContext).add(
                    new FlagTapeEntry(mType.mAction, mType.mShowTvdbId, episodes, isFlag));
        }

        // always update local database
        mType.updateDatabase();
        mType.storeLastEpisode();

        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == -1) {
            Toast.makeText(mContext, R.string.offline, Toast.LENGTH_LONG).show();
        }
        // display a small toast if submission to trakt was successful
        else if (mIsTraktInvolved) {
            int status = R.string.trakt_submitqueued;

            if (mType.mAction == FlagAction.SHOW_WATCHED
                    || mType.mAction == FlagAction.SHOW_COLLECTED
                    || mType.mAction == FlagAction.EPISODE_WATCHED_PREVIOUS) {
                // simple ack
                Toast.makeText(mContext,
                        mContext.getString(status),
                        Toast.LENGTH_SHORT).show();
            } else {
                // detailed ack
                String message = mType.getNotificationText();
                Toast.makeText(mContext,
                        message + " " + mContext.getString(status),
                        Toast.LENGTH_SHORT).show();
            }
        }

        EventBus.getDefault().post(new FlagTaskCompletedEvent(mType));
    }

    /**
     * Lower season or if season is equal has to have a lower episode number.
     */
    private static final String SELECTION_PREVIOUS_WATCHED = Episodes.SEASON + "<? OR "
            + "(" + Episodes.SEASON + "=? AND " + Episodes.NUMBER + "<?)";

    private static final String ORDER_PREVIOUS_WATCHED = Episodes.FIRSTAIREDMS + " DESC,"
            + Episodes.SEASON + " DESC,"
            + Episodes.NUMBER + " DESC";

    private static final String[] PROJECTION_EPISODE = new String[]{
            Episodes._ID
    };
}
