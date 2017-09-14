package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.CallSuper;
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

public class EpisodeFlagJobs {

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

    public static abstract class BaseJob implements EpisodeFlagJob {

        private Context context;
        private int showTvdbId;
        private int flagValue;
        private Action action;

        public BaseJob(Context context, int showTvdbId, int flagValue, Action action) {
            this.context = context.getApplicationContext();
            this.action = action;
            this.showTvdbId = showTvdbId;
            this.flagValue = flagValue;
        }

        @Override
        public int getShowTvdbId() {
            return showTvdbId;
        }

        @Override
        public int getFlagValue() {
            return flagValue;
        }

        @Override
        public Action getAction() {
            return action;
        }

        protected Context getContext() {
            return context;
        }

        protected abstract Uri getDatabaseUri();

        protected abstract String getDatabaseSelection();

        /**
         * Return the column which should get updated, either {@link SeriesGuideContract.Episodes}
         * .WATCHED or {@link SeriesGuideContract.Episodes}.COLLECTED.
         */
        protected abstract String getDatabaseColumnToUpdate();

        /**
         * Set watched or collection property.
         */
        protected abstract void setHexagonFlag(Episode episode);

        /**
         * Builds a list of episodes ready to upload to hexagon. However, the show TVDb id is not
         * set. It should be set in a wrapping {@link com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList}.
         */
        @Override
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
                    setHexagonFlag(episode);
                    episode.setSeasonNumber(episodeCursor.getInt(0));
                    episode.setEpisodeNumber(episodeCursor.getInt(1));
                    episodes.add(episode);
                }
                episodeCursor.close();
            }

            return episodes;
        }

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
         * Builds and executes the database op required to flag episodes in the local database,
         * notifies affected URIs, may update the list widget.
         */
        @Override
        @CallSuper
        public boolean applyLocalChanges() {
            // determine query uri
            Uri uri = getDatabaseUri();
            if (uri == null) {
                return false;
            }

            // build and execute query
            ContentValues values = new ContentValues();
            values.put(getDatabaseColumnToUpdate(), getFlagValue());
            int updated = context.getContentResolver()
                    .update(uri, values, getDatabaseSelection(), null);
            if (updated < 0) {
                return false; // -1 means error
            }

            // notify some other URIs for updates
            context.getContentResolver()
                    .notifyChange(SeriesGuideContract.Episodes.CONTENT_URI, null);
            context.getContentResolver()
                    .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);

            return true;
        }

        /**
         * Set last watched episode and/or last watched time of a show.
         *
         * @param lastWatchedEpisodeId The last watched episode for a show to save to the database.
         * -1 for no-op.
         * @param setLastWatchedToNow Whether to set the last watched time of a show to now.
         */
        protected final void updateLastWatched(int lastWatchedEpisodeId,
                boolean setLastWatchedToNow) {
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
    }

    /**
     * Flagging single episodes watched or collected.
     */
    public static abstract class SingleEpisodeJob extends BaseJob {

        protected int episodeTvdbId;
        protected int season;
        protected int episode;

        public SingleEpisodeJob(Context context, int showTvdbId, int episodeTvdbId, int season,
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
        public List<Episode> getEpisodesForHexagon() {
            List<Episode> episodes = new ArrayList<>();

            Episode episode = new Episode();
            setHexagonFlag(episode);
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

    public static class EpisodeWatchedJob extends SingleEpisodeJob {

        public EpisodeWatchedJob(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags,
                    Action.EPISODE_WATCHED);
        }

        @Override
        protected void setHexagonFlag(Episode episode) {
            episode.setWatchedFlag(getFlagValue());
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        private int getLastWatchedEpisodeTvdbId() {
            if (!EpisodeTools.isUnwatched(getFlagValue())) {
                return episodeTvdbId; // watched or skipped episode
            } else {
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
            }
        }

        @Override
        public boolean applyLocalChanges() {
            if (!super.applyLocalChanges()) {
                return false;
            }

            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            boolean unwatched = EpisodeTools.isUnwatched(getFlagValue());
            updateLastWatched(getLastWatchedEpisodeTvdbId(), !unwatched);

            if (EpisodeTools.isWatched(getFlagValue())) {
                // create activity entry for watched episode
                ActivityTools.addActivity(getContext(), episodeTvdbId, getShowTvdbId());
            } else if (unwatched) {
                // remove any previous activity entries for this episode
                // use case: user accidentally toggled watched flag
                ActivityTools.removeActivity(getContext(), episodeTvdbId);
            }

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());

            return true;
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

    public static class EpisodeCollectedJob extends SingleEpisodeJob {

        public EpisodeCollectedJob(Context context, int showTvdbId, int episodeTvdbId, int season,
                int episode, int episodeFlags) {
            super(context, showTvdbId, episodeTvdbId, season, episode, episodeFlags,
                    Action.EPISODE_COLLECTED);
        }

        @Override
        protected void setHexagonFlag(Episode episode) {
            episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
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
    public static abstract class SeasonJob extends BaseJob {

        protected int seasonTvdbId;
        protected int season;

        public SeasonJob(Context context, int showTvdbId, int seasonTvdbId, int season,
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
    }

    public static class SeasonWatchedJob extends SeasonJob {

        private final long currentTime;

        public SeasonWatchedJob(Context context, int showTvdbId, int seasonTvdbId, int season,
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
        protected void setHexagonFlag(Episode episode) {
            episode.setWatchedFlag(getFlagValue());
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
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
        public boolean applyLocalChanges() {
            if (!super.applyLocalChanges()) {
                return false;
            }

            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            updateLastWatched(getLastWatchedEpisodeTvdbId(),
                    !EpisodeTools.isUnwatched(getFlagValue()));

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());

            return true;
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

    public static class SeasonCollectedJob extends SeasonJob {

        public SeasonCollectedJob(Context context, int showTvdbId, int seasonTvdbId, int season,
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
        protected void setHexagonFlag(Episode episode) {
            episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
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

    public static abstract class ShowJob extends BaseJob {

        public ShowJob(Context context, int showTvdbId, int flagValue, Action action) {
            super(context, showTvdbId, flagValue, action);
        }

        @Override
        public Uri getDatabaseUri() {
            return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(
                    String.valueOf(getShowTvdbId()));
        }

        @Override
        public String getConfirmationText() {
            return null;
        }
    }

    public static class ShowWatchedJob extends ShowJob {

        private final long currentTime;

        public ShowWatchedJob(Context context, int showTvdbId, int flagValue) {
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
        protected void setHexagonFlag(Episode episode) {
            episode.setWatchedFlag(getFlagValue());
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }

        @Override
        public boolean applyLocalChanges() {
            if (!super.applyLocalChanges()) {
                return false;
            }

            int lastWatchedEpisodeTvdbId = EpisodeTools.isUnwatched(getFlagValue())
                    ? 0 /* just reset */
                    : -1 /* we don't care */;

            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            updateLastWatched(lastWatchedEpisodeTvdbId,
                    !EpisodeTools.isUnwatched(getFlagValue()));

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());

            return true;
        }
    }

    public static class ShowCollectedJob extends ShowJob {

        public ShowCollectedJob(Context context, int showTvdbId, int episodeFlags) {
            super(context, showTvdbId, episodeFlags, Action.SHOW_COLLECTED);
        }

        @Override
        public String getDatabaseSelection() {
            // only exclude specials (here will only affect database + hexagon)
            return SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
        }

        @Override
        protected void setHexagonFlag(Episode episode) {
            episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
            return SeriesGuideContract.Episodes.COLLECTED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            // send whole show
            return null;
        }
    }

    public static class EpisodeWatchedPreviousJob extends BaseJob {

        private long episodeFirstAired;

        public EpisodeWatchedPreviousJob(Context context, int showTvdbId, long episodeFirstAired) {
            super(context, showTvdbId, EpisodeFlags.WATCHED,
                    Action.EPISODE_WATCHED_PREVIOUS);
            this.episodeFirstAired = episodeFirstAired;
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
            return SeriesGuideContract.Episodes.FIRSTAIREDMS + "<" + episodeFirstAired
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_HAS_RELEASE_DATE
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_UNWATCHED_OR_SKIPPED;
        }

        @Override
        public List<SyncSeason> getEpisodesForTrakt() {
            return buildTraktEpisodeList();
        }

        @Override
        protected void setHexagonFlag(Episode episode) {
            episode.setWatchedFlag(EpisodeFlags.WATCHED);
        }

        @Override
        protected String getDatabaseColumnToUpdate() {
            return SeriesGuideContract.Episodes.WATCHED;
        }

        @Override
        public boolean applyLocalChanges() {
            if (!super.applyLocalChanges()) {
                return false;
            }

            // we don't care about the last watched episode value
            // always update last watched time, this type only marks as watched
            updateLastWatched(-1, true);

            ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());

            return true;
        }

        @Override
        public String getConfirmationText() {
            return null;
        }
    }
}
