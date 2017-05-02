package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.ActivityTools;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EpisodeTaskTypes {

    private static final String[] PROJECTION_EPISODE = new String[] {
            SeriesGuideContract.Episodes._ID
    };

    public enum Action {
        EPISODE_COLLECTED(false),
        SEASON_COLLECTED(false),
        SHOW_COLLECTED(false),
        EPISODE_WATCHED(true),
        EPISODE_WATCHED_PREVIOUS(true),
        SEASON_WATCHED(true),
        SHOW_WATCHED(true);

        final boolean isWatchNotCollect;

        Action(boolean isWatchNotCollect) {
            this.isWatchNotCollect = isWatchNotCollect;
        }
    }

    public static abstract class FlagType {

        private Context context;
        private int showTvdbId;
        private int flagValue;
        private Action action;

        public FlagType(Context context, int showTvdbId, int flagValue, Action action) {
            this.context = context.getApplicationContext();
            this.action = action;
            this.showTvdbId = showTvdbId;
            this.flagValue = flagValue;
        }

        public Context getContext() {
            return context;
        }

        public int getShowTvdbId() {
            return showTvdbId;
        }

        public int getFlagValue() {
            return flagValue;
        }

        public Action getAction() {
            return action;
        }

        public abstract Uri getDatabaseUri();

        public abstract String getDatabaseSelection();

        /**
         * Builds a list of episodes ready to upload to hexagon. However, the show TVDb id is not
         * set. It should be set in a wrapping {@link com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList}.
         */
        public List<Episode> getEpisodesForHexagon() {
            List<Episode> episodes = new ArrayList<>();

            // determine uri
            Uri uri = getDatabaseUri();
            String selection = getDatabaseSelection();

            // query and add episodes to list
            final Cursor episodeCursor = context.getContentResolver().query(
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

        /**
         * Return {@code null} to upload the complete show.
         */
        @Nullable
        public abstract List<SyncSeason> getEpisodesForTrakt();

        /**
         * Set any additional properties besides show id, season or episode number.
         */
        protected abstract void setEpisodeProperties(Episode episode);

        /**
         * Builds a list of {@link com.uwetrottmann.trakt5.entities.SyncSeason} objects to submit to
         * trakt.
         */
        protected List<SyncSeason> buildTraktEpisodeList() {
            List<SyncSeason> seasons = new ArrayList<>();

            // determine uri
            Uri uri = getDatabaseUri();
            String selection = getDatabaseSelection();

            // query and add episodes to list
            // sort ascending by season, then number for trakt
            final Cursor episodeCursor = context.getContentResolver().query(
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
         * Return the column which should get updated, either {@link SeriesGuideContract.Episodes}
         * .WATCHED or {@link SeriesGuideContract.Episodes}.COLLECTED.
         */
        protected abstract String getColumn();

        protected abstract ContentValues getContentValues();

        /**
         * Builds and executes the database op required to flag episodes in the local database.
         */
        public void updateDatabase() {
            // determine query uri
            Uri uri = getDatabaseUri();
            if (uri == null) {
                return;
            }

            // build and execute query
            ContentValues values = getContentValues();
            context.getContentResolver().update(uri, values, getDatabaseSelection(), null);

            // notify the content provider for udpates
            context.getContentResolver()
                    .notifyChange(SeriesGuideContract.Episodes.CONTENT_URI, null);
            context.getContentResolver()
                    .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);
        }

        public void onPostExecute() {
            // by default
            // - do not update last watched episode
            // - do not set last watched time to now
            performPostExecute(-1, false);
        }

        /**
         * Will be called after {@link #updateDatabase()}. Do any additional operations here. Make
         * sure to call through to super.
         *
         * @param lastWatchedEpisodeId The last watched episode for a show to save to the database.
         * -1 for no-op.
         * @param setLastWatchedToNow Whether to set the last watched time of a show to now.
         */
        @CallSuper
        protected void performPostExecute(int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
            if (lastWatchedEpisodeId != -1 || setLastWatchedToNow) {
                ContentValues values = new ContentValues();
                if (lastWatchedEpisodeId != -1) {
                    values.put(SeriesGuideContract.Shows.LASTWATCHEDID, lastWatchedEpisodeId);
                }
                if (setLastWatchedToNow) {
                    values.put(SeriesGuideContract.Shows.LASTWATCHED_MS,
                            System.currentTimeMillis());
                }
                context.getContentResolver().update(
                        SeriesGuideContract.Shows.buildShowUri(String.valueOf(showTvdbId)),
                        values, null, null);
            }
        }

        /**
         * Tells for example which episode was flagged watched.
         */
        @Nullable
        public abstract String getConfirmationText();
    }

    /**
     * Flagging single episodes watched or collected.
     */
    public static abstract class EpisodeType extends FlagType {

        protected int episodeTvdbId;
        protected int season;
        protected int episode;

        public EpisodeType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int flagValue, Action action) {
            super(context, showTvdbId, flagValue, action);
            this.episodeTvdbId = episodeTvdbId;
            this.season = season;
            this.episode = episode;
        }

        @Override
        public Uri getDatabaseUri() {
            return SeriesGuideContract.Episodes.buildEpisodeUri(String.valueOf(episodeTvdbId));
        }

        @Override
        public String getDatabaseSelection() {
            return null;
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), getFlagValue());
            return values;
        }

        @Override
        public List<Episode> getEpisodesForHexagon() {
            List<Episode> episodes = new ArrayList<>();

            Episode episode = new Episode();
            setEpisodeProperties(episode);
            episode.setSeasonNumber(season);
            episode.setEpisodeNumber(this.episode);
            episodes.add(episode);

            return episodes;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // flag a single episode
            List<SyncSeason> seasons = new LinkedList<>();
            seasons.add(new SyncSeason().number(season)
                    .episodes(new SyncEpisode().number(episode)));
            return seasons;
        }
    }

    public static class EpisodeWatchedType extends EpisodeType {

        public EpisodeWatchedType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags,
                    Action.EPISODE_WATCHED);
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setWatchedFlag(getFlagValue());
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        private int getLastWatchedEpisodeTvdbId() {
            if (EpisodeTools.isUnwatched(getFlagValue())) {
                // unwatched episode

                int lastWatchedId = -1; // don't change last watched episode by default

                // if modified episode is identical to last watched one (e.g. was just watched),
                // find an appropriate last watched episode
                final Cursor show = getContext().getContentResolver().query(
                        SeriesGuideContract.Shows.buildShowUri(String.valueOf(getShowTvdbId())),
                        new String[] {
                                SeriesGuideContract.Shows._ID,
                                SeriesGuideContract.Shows.LASTWATCHEDID
                        }, null, null, null
                );
                if (show != null) {
                    // identical to last watched episode?
                    if (show.moveToFirst() && show.getInt(1) == episodeTvdbId) {
                        if (season == 0) {
                            // keep last watched (= this episode) if we got a special
                            show.close();
                            return -1;
                        }
                        lastWatchedId = 0; // re-set if we don't find one

                        // get latest watched before this one
                        String season = String.valueOf(this.season);
                        final Cursor latestWatchedEpisode = getContext().getContentResolver()
                                .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(String
                                                .valueOf(getShowTvdbId())),
                                        PROJECTION_EPISODE,
                                        SeriesGuideContract.Episodes.SELECTION_PREVIOUS_WATCHED,
                                        new String[] {
                                                season, season, String.valueOf(episode)
                                        }, SeriesGuideContract.Episodes.SORT_PREVIOUS_WATCHED
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
                return episodeTvdbId;
            }
        }

        @Override
        public void performPostExecute(int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            boolean unwatched = EpisodeTools.isUnwatched(getFlagValue());
            super.performPostExecute(getLastWatchedEpisodeTvdbId(), !unwatched);

            if (EpisodeTools.isWatched(getFlagValue())) {
                // create activity entry for watched episode
                ActivityTools.addActivity(getContext(), episodeTvdbId, getShowTvdbId());
            } else if (unwatched) {
                // remove any previous activity entries for this episode
                // use case: user accidentally toggled watched flag
                ActivityTools.removeActivity(getContext(), episodeTvdbId);
            }
            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());
        }

        @Override
        public String getConfirmationText() {
            if (EpisodeTools.isSkipped(getFlagValue())) {
                // skipping is not sent to trakt, no need for a message
                return null;
            }

            // show episode seen/unseen message
            String number = TextTools.getEpisodeNumber(getContext(), season, episode);
            return getContext().getString(
                    EpisodeTools.isWatched(getFlagValue()) ? R.string.trakt_seen
                            : R.string.trakt_notseen,
                    number
            );
        }
    }

    public static class EpisodeCollectedType extends EpisodeType {

        public EpisodeCollectedType(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags,
                    Action.EPISODE_COLLECTED);
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        public String getConfirmationText() {
            String number = TextTools.getEpisodeNumber(getContext(), season, episode);
            return getContext().getString(getFlagValue() == 1 ? R.string.trakt_collected
                    : R.string.trakt_notcollected, number);
        }
    }

    /**
     * Flagging whole seasons watched or collected.
     */
    public static abstract class SeasonType extends FlagType {

        protected int seasonTvdbId;
        protected int season;

        public SeasonType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int flagValue, Action action) {
            super(context, showTvdbId, flagValue, action);
            this.seasonTvdbId = seasonTvdbId;
            this.season = season;
        }

        public int getSeasonTvdbId() {
            return seasonTvdbId;
        }

        @Override
        public Uri getDatabaseUri() {
            return SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                    String.valueOf(seasonTvdbId));
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), getFlagValue());
            return values;
        }
    }

    public static class SeasonWatchedType extends SeasonType {

        private final long currentTime;

        public SeasonWatchedType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId, seasonTvdbId, season, episodeFlags,
                    Action.SEASON_WATCHED);
            currentTime = TimeTools.getCurrentTime(context);
        }

        @Override
        public String getDatabaseSelection() {
            if (EpisodeTools.isUnwatched(getFlagValue())) {
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
            episode.setWatchedFlag(getFlagValue());
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }

        private int getLastWatchedEpisodeTvdbId() {
            if (EpisodeTools.isUnwatched(getFlagValue())) {
                // unwatched season
                // just reset
                return 0;
            } else {
                // watched or skipped season
                int lastWatchedId = -1;

                // get the last flagged episode of the season
                final Cursor seasonEpisodes = getContext().getContentResolver().query(
                        SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                                String.valueOf(seasonTvdbId)),
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
        public void performPostExecute(int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            super.performPostExecute(getLastWatchedEpisodeTvdbId(),
                    !EpisodeTools.isUnwatched(getFlagValue()));

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());
        }

        @Override
        public String getConfirmationText() {
            if (EpisodeTools.isSkipped(getFlagValue())) {
                // skipping is not sent to trakt, no need for a message
                return null;
            }

            String number = TextTools.getEpisodeNumber(getContext(), season, -1);
            return getContext().getString(
                    EpisodeTools.isWatched(getFlagValue()) ? R.string.trakt_seen
                            : R.string.trakt_notseen,
                    number
            );
        }
    }

    public static class SeasonCollectedType extends SeasonType {

        public SeasonCollectedType(Context context, int showTvdbId, int seasonTvdbId, int season,
                int episodeFlags) {
            super(context, showTvdbId, seasonTvdbId, season, episodeFlags,
                    Action.SEASON_COLLECTED);
        }

        @Override
        public String getDatabaseSelection() {
            // include all episodes of season
            return null;
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // flag the whole season
            List<SyncSeason> seasons = new LinkedList<>();
            seasons.add(new SyncSeason().number(season));
            return seasons;
        }

        @Override
        public String getConfirmationText() {
            String number = TextTools.getEpisodeNumber(getContext(), season, -1);
            return getContext().getString(getFlagValue() == 1 ? R.string.trakt_collected
                    : R.string.trakt_notcollected, number);
        }
    }

    public static abstract class ShowType extends FlagType {

        public ShowType(Context context, int showTvdbId, int flagValue, Action action) {
            super(context, showTvdbId, flagValue, action);
        }

        @Override
        public Uri getDatabaseUri() {
            return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(
                    String.valueOf(getShowTvdbId()));
        }

        @Override
        protected ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put(getColumn(), getFlagValue());
            return values;
        }

        @Override
        public String getConfirmationText() {
            return null;
        }
    }

    public static class ShowWatchedType extends ShowType {

        private final long currentTime;

        public ShowWatchedType(Context context, int showTvdbId, int flagValue) {
            super(context, showTvdbId, flagValue, Action.SHOW_WATCHED);
            currentTime = TimeTools.getCurrentTime(context);
        }

        @Override
        public String getDatabaseSelection() {
            if (EpisodeTools.isUnwatched(getFlagValue())) {
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
            episode.setWatchedFlag(getFlagValue());
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }

        private int getLastWatchedEpisodeTvdbId() {
            if (EpisodeTools.isUnwatched(getFlagValue())) {
                // just reset
                return 0;
            } else {
                // we don't care
                return -1;
            }
        }

        @Override
        public void performPostExecute(int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            super.performPostExecute(getLastWatchedEpisodeTvdbId(),
                    !EpisodeTools.isUnwatched(getFlagValue()));

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());
        }
    }

    public static class ShowCollectedType extends ShowType {

        public ShowCollectedType(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId, episodeFlags, Action.SHOW_COLLECTED);
        }

        @Override
        public String getDatabaseSelection() {
            // only exclude specials (here will only affect database + hexagon)
            return SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
        }

        @Override
        protected void setEpisodeProperties(Episode episode) {
            episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
        }

        @Override
        protected String getColumn() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // send whole show
            return null;
        }
    }

    public static class EpisodeWatchedPreviousType extends FlagType {

        private long episodeAbsoluteNumber;

        public EpisodeWatchedPreviousType(Context context, int showTvdbId, int episodeAbsoluteNumber) {
            super(context, showTvdbId, EpisodeFlags.WATCHED,
                    Action.EPISODE_WATCHED_PREVIOUS);
            this.episodeAbsoluteNumber = episodeAbsoluteNumber;
        }

        @Override
        public Uri getDatabaseUri() {
            return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(
                    String.valueOf(getShowTvdbId()));
        }

        @Override
        public String getDatabaseSelection() {
            // must
            // - be released before current episode,
            // - have a release date,
            // - be unwatched or skipped
            return SeriesGuideContract.Episodes.ABSOLUTE_NUMBER + "<" + episodeAbsoluteNumber
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
        public void performPostExecute(int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
            // we don't care about the last watched episode value
            // always update last watched time, this type only marks as watched
            super.performPostExecute(-1, true);

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());
        }

        @Override
        public String getConfirmationText() {
            return null;
        }
    }
}
