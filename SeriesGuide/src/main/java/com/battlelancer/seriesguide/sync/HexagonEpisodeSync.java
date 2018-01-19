package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import timber.log.Timber;

public class HexagonEpisodeSync {

    public static final int MAX_BATCH_SIZE = 500;

    private Context context;
    private HexagonTools hexagonTools;

    public HexagonEpisodeSync(Context context, HexagonTools hexagonTools) {
        this.context = context;
        this.hexagonTools = hexagonTools;
    }

    /**
     * Downloads all episodes changed since the last time this was called and applies changes to
     * the database.
     */
    public boolean downloadChangedFlags() {
        HashSet<Integer> showTvdbIds = ShowTools.getShowTvdbIdsAsSet(context);
        if (showTvdbIds == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastEpisodesSyncTime(context));
        Timber.d("downloadChangedFlags: since %s", lastSyncTime);

        List<Episode> episodes;
        String cursor = null;
        boolean hasMoreEpisodes = true;
        SparseArrayCompat<Long> showsLastWatchedMs = new SparseArrayCompat<>();
        while (hasMoreEpisodes) {
            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
                if (episodesService == null) {
                    return false;
                }

                Episodes.Get request = episodesService.get()
                        .setUpdatedSince(lastSyncTime); // use default server limit
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                EpisodeList response = request.execute();
                if (response == null) {
                    // we're done here
                    Timber.d("downloadChangedFlags: response was null, done here");
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
                HexagonTools.trackFailedRequest(context, "get updated episodes", e);
                return false;
            }

            if (episodes == null || episodes.size() == 0) {
                // nothing to do here
                break;
            }

            // build batch of episode flag updates
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();
            for (Episode episode : episodes) {
                Integer showTvdbId = episode.getShowTvdbId();
                if (!showTvdbIds.contains(showTvdbId)) {
                    continue; // ignore, show not added on this device
                }

                ContentValues values = new ContentValues();
                Integer watchedFlag = episode.getWatchedFlag();
                if (watchedFlag != null) {
                    values.put(SeriesGuideContract.Episodes.WATCHED, watchedFlag);
                    // record the latest last watched time for a show
                    if (!EpisodeTools.isUnwatched(watchedFlag)) {
                        Long lastWatchedMs = showsLastWatchedMs.get(showTvdbId);
                        // episodes returned in reverse chrono order, so just get the first time
                        if (lastWatchedMs == null && episode.getUpdatedAt() != null) {
                            long updatedAtMs = episode.getUpdatedAt().getValue();
                            showsLastWatchedMs.put(showTvdbId, updatedAtMs);
                        }
                    }
                }
                if (episode.getIsInCollection() != null) {
                    values.put(SeriesGuideContract.Episodes.COLLECTED,
                            episode.getIsInCollection());
                }

                ContentProviderOperation op = ContentProviderOperation
                        .newUpdate(SeriesGuideContract.Episodes.CONTENT_URI)
                        .withSelection(SeriesGuideContract.Shows.REF_SHOW_ID + "="
                                + showTvdbId + " AND "
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
                Timber.e(e, "downloadChangedFlags: failed to apply updates");
                return false;
            }
        }

        if (!updateLastWatchedTimeOfShows(showsLastWatchedMs)) {
            return false;
        }

        // store new last sync time
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(HexagonSettings.KEY_LAST_SYNC_EPISODES, currentTime)
                .apply();

        return true;
    }

    /**
     * Downloads watched, skipped or collected episodes of this show from Hexagon and applies
     * those flags to episodes in the database.
     *
     * @return Whether the download was successful and all changes were applied to the database.
     */
    public boolean downloadFlags(int showTvdbId) {
        Timber.d("downloadFlags: for show %s", showTvdbId);
        List<Episode> episodes;
        boolean hasMoreEpisodes = true;
        String cursor = null;

        Uri episodesOfShowUri = SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId);
        Long lastWatchedMs = null;
        while (hasMoreEpisodes) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("downloadFlags: no network connection");
                return false;
            }

            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
                if (episodesService == null) {
                    return false;
                }

                // build request
                Episodes.Get request = episodesService.get()
                        .setShowTvdbId(showTvdbId); // use default server limit
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
                HexagonTools.trackFailedRequest(context, "get episodes of show", e);
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
                    // record last watched time by taking latest updatedAt of watched/skipped
                    DateTime updatedAt = episode.getUpdatedAt();
                    if (updatedAt != null) {
                        long lastWatchedMsNew = updatedAt.getValue();
                        if (lastWatchedMs == null || lastWatchedMs < lastWatchedMsNew) {
                            lastWatchedMs = lastWatchedMsNew;
                        }
                    }
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
                Timber.e(e, "downloadFlags: failed to apply updates for show %s", showTvdbId);
                return false;
            }
        }

        //noinspection RedundantIfStatement
        if (!updateLastWatchedTimeOfShow(showTvdbId, lastWatchedMs)) {
            return false; // failed to update last watched time
        }

        return true;
    }

    private boolean updateLastWatchedTimeOfShows(SparseArrayCompat<Long> showsLastWatchedMs) {
        if (showsLastWatchedMs.size() == 0) {
            return true; // no episodes were watched, no last watched time to update
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (int i = 0; i < showsLastWatchedMs.size(); i++) {
            int showTvdbId = showsLastWatchedMs.keyAt(i);
            long lastWatchedMsNew = showsLastWatchedMs.valueAt(i);
            if (!ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId,
                    lastWatchedMsNew)) {
                return false; // failed to query current last watched ms
            }
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "updateLastWatchedTimeOfShows: failed to apply updates");
            return false;
        }

        return true;
    }


    private boolean updateLastWatchedTimeOfShow(int showTvdbId, @Nullable Long lastWatchedMs) {
        if (lastWatchedMs == null) {
            return true; // no last watched time, nothing to update
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>(1);
        if (!ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId,
                lastWatchedMs)) {
            return false; // failed to query current last watched ms
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "updateLastWatchedTimeOfShow: failed to update for show %s", showTvdbId);
            return false;
        }

        return true;
    }

    /**
     * Uploads all watched, skipped or collected episodes of this show to Hexagon.
     *
     * @return Whether the upload was successful.
     */
    public boolean uploadFlags(int showTvdbId) {
        Timber.d("uploadFlags: for show %s", showTvdbId);

        // query for watched, skipped or collected episodes
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                        FlaggedEpisodesQuery.PROJECTION, FlaggedEpisodesQuery.SELECTION,
                        null, null
                );
        if (query == null) {
            Timber.e("uploadFlags: query was null");
            return false;
        }
        if (query.getCount() == 0) {
            Timber.d("uploadFlags: no flags to upload");
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
            if (episodes.size() == MAX_BATCH_SIZE || query.isLast()) {
                EpisodeList episodeList = new EpisodeList();
                episodeList.setEpisodes(episodes);
                episodeList.setShowTvdbId(showTvdbId);

                try {
                    // get service each time to check if auth was removed
                    Episodes episodesService = hexagonTools.getEpisodesService();
                    if (episodesService == null) {
                        return false;
                    }
                    episodesService.save(episodeList).execute();
                } catch (IOException e) {
                    // abort
                    HexagonTools.trackFailedRequest(context, "save episodes of show", e);
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

}
