package com.battlelancer.seriesguide.jobs.episodes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class BaseJob implements EpisodeFlagJob {

    public static final String[] PROJECTION_EPISODE = new String[] {
            Episodes._ID
    };
    public static final String[] PROJECTION_SEASON_NUMBER = new String[] {
            Episodes.SEASON,
            Episodes.NUMBER
    };
    public static final String ORDER_SEASON_ASC_NUMBER_ASC =
            Episodes.SORT_SEASON_ASC + ", " + Episodes.SORT_NUMBER_ASC;

    private int showTvdbId;
    private int flagValue;
    private JobAction action;
    private List<EpisodeInfo> episodeInfos;

    public BaseJob(int showTvdbId, int flagValue, JobAction action) {
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
    public JobAction getAction() {
        return action;
    }

    protected abstract Uri getDatabaseUri();

    protected abstract String getDatabaseSelection();

    /**
     * Return the column which should get updated, either {@link Episodes}
     * .WATCHED or {@link Episodes}.COLLECTED.
     */
    protected abstract String getDatabaseColumnToUpdate();

    /**
     * Set watched or collection property.
     */
    protected abstract void setHexagonFlag(Episode episode);

    /**
     * Builds a list of episodes ready to upload to hexagon. However, the show TVDb id is not set.
     * It should be set in a wrapping {@link com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList}.
     */
    @Override
    public List<Episode> getEpisodesForHexagon(Context context) {
        List<Episode> episodes = new ArrayList<>();

        // determine uri
        Uri uri = getDatabaseUri();
        String selection = getDatabaseSelection();

        // query and add episodes to list
        final Cursor episodeCursor = context.getContentResolver().query(
                uri,
                new String[] {
                        Episodes.SEASON, Episodes.NUMBER
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

    @Nullable
    @Override
    public List<SyncSeason> getEpisodesForTrakt(Context context) {
        List<SyncSeason> seasons = new ArrayList<>();

        // determine uri
        Uri uri = getDatabaseUri();
        String selection = getDatabaseSelection();

        // query and add episodes to list
        // sort ascending by season, then number for trakt
        final Cursor episodeCursor = context.getContentResolver().query(
                uri,
                new String[] {
                        Episodes.SEASON, Episodes.NUMBER
                },
                selection,
                null,
                Episodes.SORT_SEASON_ASC + ", "
                        + Episodes.SORT_NUMBER_ASC
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
    public boolean applyLocalChanges(Context context) {
        // determine query uri
        Uri uri = getDatabaseUri();
        if (uri == null) {
            return false;
        }

        // TODO only if there will be a network op
        // store affected episodes for network part
        episodeInfos = new ArrayList<>();
        Cursor query = context.getContentResolver()
                .query(uri, PROJECTION_SEASON_NUMBER, getDatabaseSelection(), null,
                        ORDER_SEASON_ASC_NUMBER_ASC);
        if (query == null) {
            return false;
        }
        if (!query.moveToFirst()) {
            query.close();
            return false;
        }
        do {
            int season = query.getInt(0);
            int number = query.getInt(1);
            episodeInfos.add(new EpisodeInfo(season, number));
        } while (query.moveToNext());
        query.close();

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
                .notifyChange(Episodes.CONTENT_URI, null);
        context.getContentResolver()
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);

        return true;
    }

    /**
     * Set last watched episode and/or last watched time of a show, then update the episode shown as
     * next.
     *
     * @param lastWatchedEpisodeId The last watched episode for a show to save to the database. -1
     * for no-op.
     * @param setLastWatchedToNow Whether to set the last watched time of a show to now.
     */
    protected final void updateLastWatched(Context context,
            int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
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
        LatestEpisodeUpdateTask.updateLatestEpisodeFor(context, getShowTvdbId());
    }

    // TODO replace with flatbuffer class
    public static class EpisodeInfo {
        public int season;
        public int number;

        public EpisodeInfo(int season, int number) {
            this.season = season;
            this.number = number;
        }
    }
}
