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

package com.battlelancer.seriesguide.util;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.RateDialogFragment;
import com.battlelancer.seriesguide.util.tasks.RateEpisodeTask;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.enums.Rating;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class EpisodeTools {

    private static final int EPISODE_MAX_BATCH_SIZE = 500;

    /**
     * Lower season or if season is equal has to have a lower episode number. Must be watched or
     * skipped, excludes special episodes (because their release times are spread over all
     * seasons).
     */
    private static final String SELECTION_PREVIOUS_WATCHED =
            SeriesGuideContract.Episodes.SEASON + ">0"
                    + " AND " + SeriesGuideContract.Episodes.WATCHED + "!=" + EpisodeFlags.UNWATCHED
                    + " AND (" + SeriesGuideContract.Episodes.SEASON + "<? OR "
                    + "(" + SeriesGuideContract.Episodes.SEASON + "=? AND "
                    + SeriesGuideContract.Episodes.NUMBER + "<?)"
                    + ")";
    /**
     * Order by season, then by number, then by release time.
     */
    private static final String ORDER_PREVIOUS_WATCHED =
            SeriesGuideContract.Episodes.SEASON + " DESC" + ","
                    + SeriesGuideContract.Episodes.NUMBER + " DESC" + ","
                    + SeriesGuideContract.Episodes.FIRSTAIREDMS + " DESC";
    private static final String[] PROJECTION_EPISODE = new String[] {
            SeriesGuideContract.Episodes._ID
    };

    /**
     * Checks the database whether there is an entry for this episode.
     */
    public static boolean isEpisodeExists(Context context, int episodeTvdbId) {
        Cursor query = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), new String[] {
                        SeriesGuideContract.Episodes._ID }, null, null, null
        );
        if (query == null) {
            return false;
        }

        boolean isExists = query.getCount() > 0;
        query.close();

        return isExists;
    }

    public static boolean isCollected(int collectedFlag) {
        return collectedFlag == 1;
    }

    public static boolean isSkipped(int episodeFlags) {
        return episodeFlags == EpisodeFlags.SKIPPED;
    }

    public static boolean isUnwatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.UNWATCHED;
    }

    public static boolean isWatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.WATCHED;
    }

    /**
     * Display a {@link com.battlelancer.seriesguide.ui.dialogs.RateDialogFragment} to rate an
     * episode.
     */
    public static void displayRateDialog(Context context, FragmentManager fragmentManager,
            int episodeTvdbId) {
        if (!TraktCredentials.ensureCredentials(context)) {
            return;
        }
        RateDialogFragment newFragment = RateDialogFragment.newInstanceEpisode(episodeTvdbId);
        newFragment.show(fragmentManager, "ratedialog");
    }

    /**
     * Store the rating for the given episode in the database and send it to trakt.
     */
    public static void rate(Context context, int episodeTvdbId, Rating rating) {
        AndroidUtils.executeOnPool(new RateEpisodeTask(context, rating, episodeTvdbId));
    }

    public static void validateFlags(int episodeFlags) {
        boolean hasValidFlag = false;

        if (isUnwatched(episodeFlags)) {
            return;
        }
        if (isSkipped(episodeFlags)) {
            return;
        }
        if (isWatched(episodeFlags)) {
            return;
        }

        if (!hasValidFlag) {
            throw new IllegalArgumentException(
                    "Did not pass a valid episode flag. See EpisodeFlags class for details.");
        }
    }

    public static void episodeWatched(Context context, int showTvdbId, int episodeTvdbId,
            int season, int episode, int episodeFlags) {
        validateFlags(episodeFlags);
        execute(context,
                new EpisodeWatchedType(context, showTvdbId, episodeTvdbId, season, episode,
                        episodeFlags)
        );
    }

    public static void episodeCollected(Context context, int showTvdbId, int episodeTvdbId,
            int season, int episode, boolean isFlag) {
        execute(context,
                new EpisodeCollectedType(context, showTvdbId, episodeTvdbId, season, episode,
                        isFlag ? 1 : 0)
        );
    }

    /**
     * Flags all episodes released previous to this one as watched (excluding episodes with no
     * release date).
     */
    public static void episodeWatchedPrevious(Context context, int showTvdbId,
            long episodeFirstAired) {
        execute(context,
                new EpisodeWatchedPreviousType(context, showTvdbId, episodeFirstAired)
        );
    }

    public static void seasonWatched(Context context, int showTvdbId, int seasonTvdbId, int season,
            int episodeFlags) {
        validateFlags(episodeFlags);
        execute(context,
                new SeasonWatchedType(context, showTvdbId, seasonTvdbId, season, episodeFlags)
        );
    }

    public static void seasonCollected(Context context, int showTvdbId, int seasonTvdbId,
            int season, boolean isFlag) {
        execute(context,
                new SeasonCollectedType(context, showTvdbId, seasonTvdbId, season, isFlag ? 1 : 0)
        );
    }

    public static void showWatched(Context context, int showTvdbId, boolean isFlag) {
        execute(context,
                new ShowWatchedType(context, showTvdbId, isFlag ? 1 : 0)
        );
    }

    public static void showCollected(Context context, int showTvdbId, boolean isFlag) {
        execute(context,
                new ShowCollectedType(context, showTvdbId, isFlag ? 1 : 0)
        );
    }

    /**
     * Run the task on the thread pool.
     */
    private static void execute(@NonNull Context context, @NonNull FlagType type) {
        AndroidUtils.executeOnPool(
                new EpisodeFlagTask(context.getApplicationContext(), type)
        );
    }

    public enum EpisodeAction {
        EPISODE_WATCHED,

        EPISODE_COLLECTED,

        EPISODE_WATCHED_PREVIOUS,

        SEASON_WATCHED,

        SEASON_COLLECTED,

        SHOW_WATCHED,

        SHOW_COLLECTED
    }

    /**
     * Sent once sending to services and the database ops are finished.
     */
    public static class EpisodeActionCompletedEvent {

        public FlagType mType;

        public EpisodeActionCompletedEvent(FlagType type) {
            mType = type;
        }
    }

    public static abstract class FlagType {

        protected Context mContext;

        protected EpisodeAction mAction;

        protected int mShowTvdbId;

        protected int mEpisodeFlag;

        public FlagType(Context context, int showTvdbId) {
            mContext = context;
            mShowTvdbId = showTvdbId;
        }

        public abstract Uri getUri();

        public abstract String getSelection();

        /**
         * Builds a list of episodes ready to upload to hexagon. However, the show TVDb id is not
         * set. It should be set in a wrapping {@link com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList}.
         */
        public List<Episode> getEpisodesForHexagon() {
            List<Episode> episodes = new ArrayList<>();

            // determine uri
            Uri uri = getUri();
            String selection = getSelection();

            // query and add episodes to list
            final Cursor episodeCursor = mContext.getContentResolver().query(
                    uri,
                    new String[] {
                            SeriesGuideContract.Episodes.SEASON, SeriesGuideContract.Episodes.NUMBER
                    }, selection, null, null
            );
            if (episodeCursor != null) {
                while (episodeCursor.moveToNext()) {
                    Episode episode = new Episode();
                    setEpisodeProperties(episode);
                    episode.setSeasonNumber(episodeCursor.getInt(0));
                    episode.setEpisodeNumber(episodeCursor.getInt(1));
                    episodes.add(episode);
                }
                episodeCursor.close();
            }

            return episodes;
        }

        @Nullable
        public abstract List<SyncSeason> getEpisodesForTrakt();

        public int getShowTvdbId() {
            return mShowTvdbId;
        }

        /**
         * Set any additional properties besides show id, season or episode number.
         */
        protected abstract void setEpisodeProperties(Episode episode);

        /**
         * Builds a list of {@link com.uwetrottmann.trakt.v2.entities.SyncSeason} objects to submit
         * to trakt.
         */
        protected List<SyncSeason> buildTraktEpisodeList() {
            List<SyncSeason> seasons = new ArrayList<>();

            // determine uri
            Uri uri = getUri();
            String selection = getSelection();

            // query and add episodes to list
            // sort ascending by season, then number for trakt
            final Cursor episodeCursor = mContext.getContentResolver().query(
                    uri,
                    new String[] {
                            SeriesGuideContract.Episodes.SEASON, SeriesGuideContract.Episodes.NUMBER
                    },
                    selection,
                    null,
                    SeriesGuideContract.Episodes.SORT_SEASON_ASC + ", "
                            + SeriesGuideContract.Episodes.SORT_NUMBER_ASC
            );
            if (episodeCursor != null) {
                SyncSeason currentSeason = null;
                while (episodeCursor.moveToNext()) {
                    int seasonNumber = episodeCursor.getInt(0);

                    // start new season?
                    if (currentSeason == null || seasonNumber > currentSeason.number) {
                        currentSeason = new SyncSeason().number(seasonNumber);
                        currentSeason.episodes = new LinkedList<>();
                        seasons.add(currentSeason);
                    }

                    // add episode
                    currentSeason.episodes.add(new SyncEpisode().number(episodeCursor.getInt(1)));
                }
                episodeCursor.close();
            }

            return seasons;
        }

        /**
         * Return the column which should get updated, either {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes}
         * .WATCHED or {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes}.COLLECTED.
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
            mContext.getContentResolver()
                    .notifyChange(SeriesGuideContract.Episodes.CONTENT_URI, null);
            mContext.getContentResolver()
                    .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);
        }

        /**
         * Determines the last watched episode and returns its TVDb id or -1 if it can't be
         * determined.
         */
        protected abstract int getLastWatchedEpisodeTvdbId();

        /**
         * Saves the last watched episode for a show to the database.
         */
        public void storeLastEpisode() {
            int lastWatchedId = getLastWatchedEpisodeTvdbId();
            if (lastWatchedId != -1) {
                // set latest watched
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Shows.LASTWATCHEDID, lastWatchedId);
                mContext.getContentResolver().update(
                        SeriesGuideContract.Shows.buildShowUri(String.valueOf(mShowTvdbId)),
                        values, null, null);
            }
        }

        /**
         * Will be called after {@link #updateDatabase()} and {@link #storeLastEpisode()}. Do any
         * additional operations here.
         */
        protected abstract void onPostExecute();

        /**
         * Returns the text which should be prepended to the submission status message. Tells e.g.
         * which episode was flagged watched.
         */
        public abstract String getNotificationText();
    }

    /**
     * Flagging single episodes watched or collected.
     */
    public static abstract class EpisodeType extends FlagType {

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
            return SeriesGuideContract.Episodes.buildEpisodeUri(String.valueOf(mEpisodeTvdbId));
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
        public List<Episode> getEpisodesForHexagon() {
            List<Episode> episodes = new ArrayList<>();

            Episode episode = new Episode();
            setEpisodeProperties(episode);
            episode.setSeasonNumber(mSeason);
            episode.setEpisodeNumber(mEpisode);
            episodes.add(episode);

            return episodes;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // flag a single episode
            List<SyncSeason> seasons = new LinkedList<>();
            seasons.add(new SyncSeason().number(mSeason)
                    .episodes(new SyncEpisode().number(mEpisode)));
            return seasons;
        }
    }

    public static class EpisodeWatchedType extends EpisodeType {

        public EpisodeWatchedType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags);
            mAction = EpisodeAction.EPISODE_WATCHED;
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setWatchedFlag(mEpisodeFlag);
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            if (isUnwatched(mEpisodeFlag)) {
                // unwatched episode

                int lastWatchedId = -1; // don't change last watched episode by default

                // if modified episode is identical to last watched one (e.g. was just watched),
                // find an appropriate last watched episode
                final Cursor show = mContext.getContentResolver().query(
                        SeriesGuideContract.Shows.buildShowUri(String.valueOf(mShowTvdbId)),
                        new String[] {
                                SeriesGuideContract.Shows._ID,
                                SeriesGuideContract.Shows.LASTWATCHEDID
                        }, null, null, null
                );
                if (show != null) {
                    // identical to last watched episode?
                    if (show.moveToFirst() && show.getInt(1) == mEpisodeTvdbId) {
                        if (mSeason == 0) {
                            // keep last watched (= this episode) if we got a special
                            show.close();
                            return -1;
                        }
                        lastWatchedId = 0; // re-set if we don't find one

                        // get latest watched before this one
                        String season = String.valueOf(mSeason);
                        final Cursor latestWatchedEpisode = mContext.getContentResolver()
                                .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(String
                                                .valueOf(mShowTvdbId)),
                                        PROJECTION_EPISODE, SELECTION_PREVIOUS_WATCHED,
                                        new String[] {
                                                season, season, String.valueOf(mEpisode)
                                        }, ORDER_PREVIOUS_WATCHED
                                );
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
        protected void onPostExecute() {
            if (isWatched(mEpisodeFlag)) {
                // create activity entry for watched episode
                ActivityTools.addActivity(mContext, mEpisodeTvdbId, mShowTvdbId);
            } else if (isUnwatched(mEpisodeFlag)) {
                // remove any previous activity entries for this episode
                // use case: user accidentally toggled watched flag
                ActivityTools.removeActivity(mContext, mEpisodeTvdbId);
            }
        }

        @Override
        public String getNotificationText() {
            if (isSkipped(mEpisodeFlag)) {
                // skipping is not sent to trakt, no need for a message
                return null;
            }

            // show episode seen/unseen message
            String number = Utils.getEpisodeNumber(mContext, mSeason, mEpisode);
            return mContext.getString(
                    isWatched(mEpisodeFlag) ? R.string.trakt_seen
                            : R.string.trakt_notseen,
                    number
            );
        }
    }

    public static class EpisodeCollectedType extends EpisodeType {

        public EpisodeCollectedType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags);
            mAction = EpisodeAction.EPISODE_COLLECTED;
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setIsInCollection(isCollected(mEpisodeFlag));
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            // we don't care
            return -1;
        }

        @Override
        protected void onPostExecute() {
            // do nothing
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
    public static abstract class SeasonType extends FlagType {

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
            return SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                    String.valueOf(mSeasonTvdbId));
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), mEpisodeFlag);
            return values;
        }

        @Override
        protected void onPostExecute() {
            // do nothing
        }
    }

    public static class SeasonWatchedType extends SeasonType {

        private final long currentTime;

        public SeasonWatchedType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId, seasonTvdbId, season, episodeFlags);
            mAction = EpisodeAction.SEASON_WATCHED;
            currentTime = TimeTools.getCurrentTime(context);
        }

        @Override
        public String getSelection() {
            if (isUnwatched(mEpisodeFlag)) {
                // set unwatched
                // include watched or skipped episodes
                return SeriesGuideContract.Episodes.SELECTION_WATCHED_OR_SKIPPED;
            } else {
                // set watched or skipped
                // do NOT mark watched episodes again to avoid trakt adding a new watch
                // only mark episodes that have been released until within the hour
                return SeriesGuideContract.Episodes.FIRSTAIREDMS + "<=" + (currentTime
                        + DateUtils.HOUR_IN_MILLIS)
                        + " AND " + SeriesGuideContract.Episodes.SELECTION_HAS_RELEASE_DATE
                        + " AND " + SeriesGuideContract.Episodes.SELECTION_UNWATCHED_OR_SKIPPED;
            }
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setWatchedFlag(mEpisodeFlag);
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            if (isUnwatched(mEpisodeFlag)) {
                // unwatched season
                // just reset
                return 0;
            } else {
                // watched or skipped season
                int lastWatchedId = -1;

                // get the last flagged episode of the season
                final Cursor seasonEpisodes = mContext.getContentResolver().query(
                        SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                                String.valueOf(mSeasonTvdbId)),
                        PROJECTION_EPISODE,
                        SeriesGuideContract.Episodes.FIRSTAIREDMS + "<=" + (currentTime
                                + DateUtils.HOUR_IN_MILLIS), null,
                        SeriesGuideContract.Episodes.NUMBER + " DESC"
                );
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
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }

        @Override
        protected void onPostExecute() {
            // do nothing
        }

        @Override
        public String getNotificationText() {
            if (isSkipped(mEpisodeFlag)) {
                // skipping is not sent to trakt, no need for a message
                return null;
            }

            String number = Utils.getEpisodeNumber(mContext, mSeason, -1);
            return mContext.getString(
                    isWatched(mEpisodeFlag) ? R.string.trakt_seen
                            : R.string.trakt_notseen,
                    number
            );
        }
    }

    public static class SeasonCollectedType extends SeasonType {

        public SeasonCollectedType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId, seasonTvdbId, season, episodeFlags);
            mAction = EpisodeAction.SEASON_COLLECTED;
        }

        @Override
        public String getSelection() {
            // include all episodes of season
            return null;
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setIsInCollection(isCollected(mEpisodeFlag));
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            return -1;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // flag the whole season
            List<SyncSeason> seasons = new LinkedList<>();
            seasons.add(new SyncSeason().number(mSeason));
            return seasons;
        }

        @Override
        public String getNotificationText() {
            String number = Utils.getEpisodeNumber(mContext, mSeason, -1);
            return mContext.getString(mEpisodeFlag == 1 ? R.string.trakt_collected
                    : R.string.trakt_notcollected, number);
        }
    }

    public static abstract class ShowType extends FlagType {

        public ShowType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId);
            mEpisodeFlag = episodeFlags;
        }

        @Override
        public Uri getUri() {
            return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(String.valueOf(mShowTvdbId));
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), mEpisodeFlag);
            return values;
        }

        @Override
        protected void onPostExecute() {
            // do nothing
        }

        @Override
        public String getNotificationText() {
            return null;
        }
    }

    public static class ShowWatchedType extends ShowType {

        private final long currentTime;

        public ShowWatchedType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId, episodeFlags);
            mAction = EpisodeAction.SHOW_WATCHED;
            currentTime = TimeTools.getCurrentTime(context);
        }

        @Override
        public String getSelection() {
            if (isUnwatched(mEpisodeFlag)) {
                // set unwatched
                // include watched or skipped episodes
                return SeriesGuideContract.Episodes.SELECTION_WATCHED_OR_SKIPPED
                        + " AND " + SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
            } else {
                // set watched or skipped
                // do NOT mark watched episodes again to avoid trakt adding a new watch
                // only mark episodes that have been released until within the hour
                return SeriesGuideContract.Episodes.FIRSTAIREDMS + "<=" + (currentTime
                        + DateUtils.HOUR_IN_MILLIS)
                        + " AND " + SeriesGuideContract.Episodes.SELECTION_HAS_RELEASE_DATE
                        + " AND " + SeriesGuideContract.Episodes.SELECTION_UNWATCHED_OR_SKIPPED
                        + " AND " + SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
            }
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setWatchedFlag(mEpisodeFlag);
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            if (isUnwatched(mEpisodeFlag)) {
                // just reset
                return 0;
            } else {
                // we don't care
                return -1;
            }
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }
    }

    public static class ShowCollectedType extends ShowType {

        public ShowCollectedType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId, episodeFlags);
            mAction = EpisodeAction.SHOW_COLLECTED;
        }

        @Override
        public String getSelection() {
            // only exclude specials (here will only affect database + hexagon)
            return SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setIsInCollection(isCollected(mEpisodeFlag));
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            // we don't care
            return -1;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // send whole show
            return null;
        }
    }

    public static class EpisodeWatchedPreviousType extends FlagType {

        private long mEpisodeFirstAired;

        public EpisodeWatchedPreviousType(Context context, int showTvdbId, long episodeFirstAired) {
            super(context, showTvdbId);
            mEpisodeFirstAired = episodeFirstAired;
            mAction = EpisodeAction.EPISODE_WATCHED_PREVIOUS;
        }

        @Override
        public Uri getUri() {
            return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(String.valueOf(mShowTvdbId));
        }

        @Override
        public String getSelection() {
            // must
            // - be released before current episode,
            // - have a release date,
            // - be unwatched or skipped
            return SeriesGuideContract.Episodes.FIRSTAIREDMS + "<" + mEpisodeFirstAired
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_HAS_RELEASE_DATE
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_UNWATCHED_OR_SKIPPED;
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(SeriesGuideContract.Episodes.WATCHED, EpisodeFlags.WATCHED);
            return values;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setWatchedFlag(EpisodeFlags.WATCHED);
        }

        @Override
        protected String getColumn() {
            // not used
            return null;
        }

        @Override
        protected int getLastWatchedEpisodeTvdbId() {
            // we don't care
            return -1;
        }

        @Override
        protected void onPostExecute() {
            // do nothing
        }

        @Override
        public String getNotificationText() {
            return null;
        }
    }

    private static class EpisodeFlagTask extends AsyncTask<Void, Void, Integer> {

        private final Context mContext;
        private final FlagType mType;

        private boolean mIsSendingToTrakt;
        private boolean mIsSendingToHexagon;

        public EpisodeFlagTask(Context context, FlagType type) {
            mContext = context.getApplicationContext();
            mType = type;
        }

        @Override
        protected void onPreExecute() {
            // network ops may run long, so immediately show a status toast
            mIsSendingToHexagon = HexagonTools.isSignedIn(mContext);
            if (mIsSendingToHexagon) {
                Toast.makeText(mContext, R.string.hexagon_api_queued, Toast.LENGTH_SHORT).show();
            }
            mIsSendingToTrakt = TraktCredentials.get(mContext).hasCredentials()
                    && !isSkipped(mType.mEpisodeFlag);
            if (mIsSendingToTrakt) {
                Toast.makeText(mContext, R.string.trakt_submitqueued, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // upload to hexagon
            if (mIsSendingToHexagon) {
                if (!AndroidUtils.isNetworkConnected(mContext)) {
                    return ERROR_NETWORK;
                }

                int result = uploadToHexagon(mContext, mType.getShowTvdbId(),
                        mType.getEpisodesForHexagon());
                if (result < 0) {
                    return result;
                }
            }

            // upload to trakt
            /**
             * Do net send skipped episodes, this is not supported by trakt.
             * However, if the skipped flag is removed this will be handled identical
             * to flagging as unwatched.
             */
            if (mIsSendingToTrakt) {
                if (!AndroidUtils.isNetworkConnected(mContext)) {
                    return ERROR_NETWORK;
                }

                int result = uploadToTrakt(mContext,
                        mType.getShowTvdbId(),
                        mType.mAction,
                        mType.getEpisodesForTrakt(),
                        !isUnwatched(mType.mEpisodeFlag));
                if (result < 0) {
                    return result;
                }
            }

            // update local database (if uploading went smoothly or not uploading at all)
            mType.updateDatabase();
            mType.storeLastEpisode();
            mType.onPostExecute();

            return SUCCESS;
        }

        private static int uploadToHexagon(Context context, int showTvdbId,
                @NonNull List<Episode> batch) {
            EpisodeList uploadWrapper = new EpisodeList();
            uploadWrapper.setShowTvdbId(showTvdbId);

            // upload in small batches
            List<Episode> smallBatch = new ArrayList<>();
            while (!batch.isEmpty()) {
                // batch small enough?
                if (batch.size() <= EPISODE_MAX_BATCH_SIZE) {
                    smallBatch = batch;
                } else {
                    // build smaller batch
                    for (int count = 0; count < EPISODE_MAX_BATCH_SIZE; count++) {
                        if (batch.isEmpty()) {
                            break;
                        }
                        smallBatch.add(batch.remove(0));
                    }
                }

                // upload
                uploadWrapper.setEpisodes(smallBatch);
                if (!Upload.flagsToHexagon(context, uploadWrapper)) {
                    return ERROR_HEXAGON_API;
                }

                // prepare for next batch
                smallBatch.clear();
            }

            return SUCCESS;
        }

        /**
         * @param flags Send {@code null} to upload complete show.
         */
        private static int uploadToTrakt(Context context, int showTvdbId, EpisodeAction flagAction,
                @Nullable List<SyncSeason> flags, boolean isAddNotDelete) {
            if (flags != null && flags.isEmpty()) {
                // nothing to upload
                return SUCCESS;
            }

            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
            if (trakt == null) {
                return ERROR_TRAKT_AUTH;
            }
            Sync traktSync = trakt.sync();

            // outer wrapper and show are always required
            SyncShow show = new SyncShow().id(ShowIds.tvdb(showTvdbId));
            SyncItems items = new SyncItems().shows(show);

            // add season or episodes
            if (flagAction == EpisodeAction.SEASON_WATCHED
                    || flagAction == EpisodeAction.SEASON_COLLECTED
                    || flagAction == EpisodeAction.EPISODE_WATCHED
                    || flagAction == EpisodeAction.EPISODE_COLLECTED
                    || flagAction == EpisodeAction.EPISODE_WATCHED_PREVIOUS) {
                show.seasons(flags);
            }

            // execute network call
            try {
                SyncResponse response = null;

                switch (flagAction) {
                    case SHOW_WATCHED:
                    case SEASON_WATCHED:
                    case EPISODE_WATCHED:
                        if (isAddNotDelete) {
                            response = traktSync.addItemsToWatchedHistory(items);
                        } else {
                            response = traktSync.deleteItemsFromWatchedHistory(items);
                        }
                        break;
                    case SHOW_COLLECTED:
                    case SEASON_COLLECTED:
                    case EPISODE_COLLECTED:
                        if (isAddNotDelete) {
                            response = traktSync.addItemsToCollection(items);
                        } else {
                            response = traktSync.deleteItemsFromCollection(items);
                        }
                        break;
                    case EPISODE_WATCHED_PREVIOUS:
                        response = traktSync.addItemsToWatchedHistory(items);
                        break;
                }

                // check if any items were not found
                if (!isSyncSuccessful(response)) {
                    return ERROR_TRAKT_API;
                }
            } catch (RetrofitError e) {
                Timber.e(e, "uploadToTrakt: failed");
                return ERROR_TRAKT_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return ERROR_TRAKT_AUTH;
            }

            return SUCCESS;
        }

        /**
         * If the {@link com.uwetrottmann.trakt.v2.entities.SyncResponse} is invalid or any show,
         * season or episode was not found returns {@code false}.
         */
        private static boolean isSyncSuccessful(SyncResponse response) {
            if (response == null || response.not_found == null) {
                // invalid response, assume failure
                return false;
            }

            if (response.not_found.shows != null && !response.not_found.shows.isEmpty()) {
                // show not found
                return false;
            }
            if (response.not_found.seasons != null && !response.not_found.seasons.isEmpty()) {
                // show exists, but seasons not found
                return false;
            }
            if (response.not_found.episodes != null && !response.not_found.episodes.isEmpty()) {
                // show and season exists, but episodes not found
                return false;
            }

            return true;
        }

        private static final int SUCCESS = 0;
        private static final int ERROR_NETWORK = -1;
        private static final int ERROR_TRAKT_AUTH = -2;
        private static final int ERROR_TRAKT_API = -3;
        private static final int ERROR_HEXAGON_API = -4;

        @Override
        protected void onPostExecute(Integer result) {
            // handle errors
            Integer errorResId = null;
            switch (result) {
                case ERROR_NETWORK:
                    errorResId = R.string.offline;
                    break;
                case ERROR_TRAKT_AUTH:
                    errorResId = R.string.trakt_error_credentials;
                    break;
                case ERROR_TRAKT_API:
                    errorResId = R.string.trakt_error_general;
                    break;
                case ERROR_HEXAGON_API:
                    errorResId = R.string.hexagon_api_error;
                    break;
            }
            if (errorResId != null) {
                Toast.makeText(mContext, errorResId, Toast.LENGTH_LONG).show();
                return;
            }

            // success!
            // notify UI it may do relevant updates
            EventBus.getDefault().post(new EpisodeActionCompletedEvent(mType));

            // update latest episode for the changed show
            AndroidUtils.executeOnPool(new LatestEpisodeUpdateTask(mContext),
                    mType.getShowTvdbId());

            // display success message
            if (mIsSendingToTrakt) {
                int status = R.string.trakt_success;
                if (mType.mAction == EpisodeAction.SHOW_WATCHED
                        || mType.mAction == EpisodeAction.SHOW_COLLECTED
                        || mType.mAction == EpisodeAction.EPISODE_WATCHED_PREVIOUS) {
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
        }
    }

    public static class Download {

        /**
         * Downloads all episodes changed since the last time this was called and applies changes to
         * the database.
         */
        public static boolean flagsFromHexagon(Context context) {
            List<Episode> episodes;
            boolean hasMoreEpisodes = true;
            String cursor = null;
            long currentTime = System.currentTimeMillis();
            DateTime lastSyncTime = new DateTime(HexagonSettings.getLastEpisodesSyncTime(context));

            Timber.d("flagsFromHexagon: downloading changed episode flags since " + lastSyncTime);

            while (hasMoreEpisodes) {
                try {
                    Episodes.Get request = HexagonTools.getEpisodesService(context).get()
                            .setUpdatedSince(lastSyncTime)
                            .setLimit(EPISODE_MAX_BATCH_SIZE);
                    if (!TextUtils.isEmpty(cursor)) {
                        request.setCursor(cursor);
                    }

                    EpisodeList response = request.execute();
                    if (response == null) {
                        // we're done here
                        Timber.d("flagsFromHexagon: response was null, done here");
                        break;
                    }

                    episodes = response.getEpisodes();

                    // check for more items
                    if (response.getCursor() != null) {
                        cursor = response.getCursor();
                    } else {
                        hasMoreEpisodes = false;
                    }
                } catch (IOException e) {
                    Timber.e(e, "flagsFromHexagon: failed to download changed episode flags");
                    return false;
                }

                if (episodes == null || episodes.size() == 0) {
                    // nothing to do here
                    break;
                }

                // build batch of episode flag updates
                ArrayList<ContentProviderOperation> batch = new ArrayList<>();
                for (Episode episode : episodes) {
                    ContentValues values = new ContentValues();
                    if (episode.getWatchedFlag() != null) {
                        values.put(SeriesGuideContract.Episodes.WATCHED, episode.getWatchedFlag());
                    }
                    if (episode.getIsInCollection() != null) {
                        values.put(SeriesGuideContract.Episodes.COLLECTED,
                                episode.getIsInCollection());
                    }

                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(SeriesGuideContract.Episodes.CONTENT_URI)
                            .withSelection(SeriesGuideContract.Shows.REF_SHOW_ID + "="
                                    + episode.getShowTvdbId() + " AND "
                                    + SeriesGuideContract.Episodes.SEASON + "="
                                    + episode.getSeasonNumber() + " AND "
                                    + SeriesGuideContract.Episodes.NUMBER + "="
                                    + episode.getEpisodeNumber(), null)
                            .withValues(values)
                            .build();

                    batch.add(op);
                }

                // execute database update
                try {
                    DBUtils.applyInSmallBatches(context, batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e, "flagsFromHexagon: failed to apply changed episode flag updates");
                    return false;
                }
            }

            // store new last sync time
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(HexagonSettings.KEY_LAST_SYNC_EPISODES, currentTime)
                    .commit();

            return true;
        }

        /**
         * Downloads watched, skipped or collected episodes of this show from Hexagon and applies
         * those flags to episodes in the database.
         *
         * @return Whether the download was successful and all changes were applied to the database.
         */
        public static boolean flagsFromHexagon(Context context, int showTvdbId) {
            Timber.d("flagsFromHexagon: downloading episode flags for show " + showTvdbId);
            List<Episode> episodes;
            boolean hasMoreEpisodes = true;
            String cursor = null;

            Uri episodesOfShowUri = SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId);

            while (hasMoreEpisodes) {
                // abort if connection is lost
                if (!AndroidUtils.isNetworkConnected(context)) {
                    Timber.e("flagsFromHexagon: no network connection");
                    return false;
                }

                try {
                    // build request
                    Episodes.Get request = HexagonTools.getEpisodesService(context).get()
                            .setShowTvdbId(showTvdbId)
                            .setLimit(EPISODE_MAX_BATCH_SIZE);
                    if (!TextUtils.isEmpty(cursor)) {
                        request.setCursor(cursor);
                    }

                    // execute request
                    EpisodeList response = request.execute();
                    if (response == null) {
                        break;
                    }

                    episodes = response.getEpisodes();

                    // check for more items
                    if (response.getCursor() != null) {
                        cursor = response.getCursor();
                    } else {
                        hasMoreEpisodes = false;
                    }
                } catch (IOException e) {
                    Timber.e(e, "flagsFromHexagon: failed to download episode flags for show "
                            + showTvdbId);
                    return false;
                }

                if (episodes == null || episodes.size() == 0) {
                    // nothing to do here
                    break;
                }

                // build batch of episode flag updates
                ArrayList<ContentProviderOperation> batch = new ArrayList<>();
                for (Episode episode : episodes) {
                    ContentValues values = new ContentValues();
                    if (episode.getWatchedFlag() != null
                            && episode.getWatchedFlag() != EpisodeFlags.UNWATCHED) {
                        values.put(SeriesGuideContract.Episodes.WATCHED, episode.getWatchedFlag());
                    }
                    if (episode.getIsInCollection() != null
                            && episode.getIsInCollection()) {
                        values.put(SeriesGuideContract.Episodes.COLLECTED,
                                episode.getIsInCollection());
                    }

                    if (values.size() == 0) {
                        // skip if episode has neither a watched flag or is in collection
                        continue;
                    }

                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(episodesOfShowUri)
                            .withSelection(SeriesGuideContract.Episodes.SEASON + "="
                                    + episode.getSeasonNumber() + " AND "
                                    + SeriesGuideContract.Episodes.NUMBER + "="
                                    + episode.getEpisodeNumber(), null)
                            .withValues(values)
                            .build();

                    batch.add(op);
                }

                // execute database update
                try {
                    DBUtils.applyInSmallBatches(context, batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e, "flagsFromHexagon: failed to apply episode flag updates for show "
                            + showTvdbId);
                    return false;
                }
            }

            return true;
        }
    }

    public static class Upload {

        private interface FlaggedEpisodesQuery {
            String[] PROJECTION = new String[] {
                    SeriesGuideContract.Episodes._ID,
                    SeriesGuideContract.Episodes.SEASON,
                    SeriesGuideContract.Episodes.NUMBER,
                    SeriesGuideContract.Episodes.WATCHED,
                    SeriesGuideContract.Episodes.COLLECTED
            };

            String SELECTION = SeriesGuideContract.Episodes.WATCHED + "!=" + EpisodeFlags.UNWATCHED
                    + " OR " + SeriesGuideContract.Episodes.COLLECTED + "=1";

            int SEASON = 1;
            int NUMBER = 2;
            int WATCHED = 3;
            int IN_COLLECTION = 4;
        }

        /**
         * Uploads all watched, skipped or collected episodes of this show to Hexagon.
         *
         * @return Whether the upload was successful.
         */
        public static boolean flagsToHexagon(Context context, int showTvdbId) {
            Timber.d("flagsToHexagon: uploading episode flags for show " + showTvdbId);

            // query for watched, skipped or collected episodes
            Cursor query = context.getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                            FlaggedEpisodesQuery.PROJECTION, FlaggedEpisodesQuery.SELECTION,
                            null, null
                    );
            if (query == null) {
                Timber.e("flagsToHexagon: episode flags query was null");
                return false;
            }
            if (query.getCount() == 0) {
                Timber.d("flagsToHexagon: no episode flags to upload");
                query.close();
                return true;
            }

            // build list of episodes to upload
            List<Episode> episodes = new ArrayList<>();
            while (query.moveToNext()) {
                Episode episode = new Episode();
                episode.setSeasonNumber(query.getInt(FlaggedEpisodesQuery.SEASON));
                episode.setEpisodeNumber(query.getInt(FlaggedEpisodesQuery.NUMBER));

                int watchedFlag = query.getInt(FlaggedEpisodesQuery.WATCHED);
                if (!EpisodeTools.isUnwatched(watchedFlag)) {
                    episode.setWatchedFlag(watchedFlag);
                }

                boolean isInCollection = EpisodeTools.isCollected(
                        query.getInt(FlaggedEpisodesQuery.IN_COLLECTION));
                if (isInCollection) {
                    episode.setIsInCollection(true);
                }

                episodes.add(episode);

                // upload a batch
                if (episodes.size() == EPISODE_MAX_BATCH_SIZE || query.isLast()) {
                    EpisodeList episodeList = new EpisodeList();
                    episodeList.setEpisodes(episodes);
                    episodeList.setShowTvdbId(showTvdbId);

                    try {
                        HexagonTools.getEpisodesService(context).save(episodeList).execute();
                    } catch (IOException e) {
                        // abort
                        Timber.e(e, "flagsToHexagon: failed to upload episode flags for show "
                                + showTvdbId);
                        query.close();
                        return false;
                    }

                    // clear array
                    episodes = new ArrayList<>();
                }
            }

            query.close();

            return true;
        }

        /**
         * Upload the given episodes to Hexagon. Assumes the given episode wrapper has valid
         * values.
         */
        public static boolean flagsToHexagon(Context context, EpisodeList episodes) {
            try {
                HexagonTools.getEpisodesService(context).save(episodes).execute();
            } catch (IOException e) {
                Timber.e(e, "flagsToHexagon: failed to upload episodes for show "
                        + episodes.getShowTvdbId());
                return false;
            }

            return true;
        }
    }
}
