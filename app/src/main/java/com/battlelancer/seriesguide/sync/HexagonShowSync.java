package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgShow2CloudUpdate;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.util.Errors;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.shows.Shows;
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow;
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShowList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class HexagonShowSync {

    private final Context context;
    private final HexagonTools hexagonTools;

    public HexagonShowSync(Context context, HexagonTools hexagonTools) {
        this.context = context;
        this.hexagonTools = hexagonTools;
    }

    /**
     * Downloads shows from Hexagon and updates existing shows with new property values. Any
     * shows not yet in the local database, determined by the given TMDB ID map, will be added
     * to the given map.
     */
    public boolean download(Map<Integer, Long> tmdbIdsToShowIds,
            HashMap<Integer, SearchResult> newShows, boolean hasMergedShows) {
        List<SgCloudShow> shows;
        boolean hasMoreShows = true;
        String cursor = null;
        long currentTime = System.currentTimeMillis();
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastShowsSyncTime(context));

        if (hasMergedShows) {
            Timber.d("download: changed shows since %s", lastSyncTime);
        } else {
            Timber.d("download: all shows");
        }

        while (hasMoreShows) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("download: no network connection");
                return false;
            }

            try {
                // get service each time to check if auth was removed
                Shows showsService = hexagonTools.getShowsService();
                if (showsService == null) {
                    return false;
                }

                Shows.GetSgShows request = showsService.getSgShows(); // use default server limit
                if (hasMergedShows) {
                    // only get changed shows (otherwise returns all)
                    request.setUpdatedSince(lastSyncTime);
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                SgCloudShowList response = request.execute();
                if (response == null) {
                    // we're done
                    Timber.d("download: response was null, done here");
                    break;
                }

                shows = response.getShows();

                // check for more items
                if (response.getCursor() != null) {
                    cursor = response.getCursor();
                } else {
                    hasMoreShows = false;
                }
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get shows", e);
                return false;
            }

            if (shows == null || shows.size() == 0) {
                // nothing to do here
                break;
            }

            // update all received shows, will ignore those not added locally
            ArrayList<SgShow2CloudUpdate> batch = buildShowUpdates(shows, tmdbIdsToShowIds,
                    newShows, !hasMergedShows);
            SgRoomDatabase.getInstance(context).sgShow2Helper().updateForCloudUpdate(batch);
        }

        if (hasMergedShows) {
            // set new last sync time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(HexagonSettings.KEY_LAST_SYNC_SHOWS, currentTime)
                    .apply();
        }

        return true;
    }

    @NonNull
    private ArrayList<SgShow2CloudUpdate> buildShowUpdates(List<SgCloudShow> shows,
            Map<Integer, Long> tmdbIdsToShowIds, HashMap<Integer, SearchResult> newShows,
            boolean mergeValues) {
        ArrayList<SgShow2CloudUpdate> batch = new ArrayList<>();
        for (SgCloudShow show : shows) {
            // schedule to add shows not in local database
            Integer showTmdbId = show.getTmdbId();
            if (showTmdbId == null) continue; // Invalid data.

            Long showIdOrNull = tmdbIdsToShowIds.get(showTmdbId);
            if (showIdOrNull == null) {
                // ...but do NOT add shows marked as removed
                if (show.getIsRemoved() != null && show.getIsRemoved()) {
                    continue;
                }

                if (!newShows.containsKey(showTmdbId)) {
                    SearchResult item = new SearchResult();
                    item.setTmdbId(showTmdbId);
                    item.setLanguage(show.getLanguage());
                    item.setTitle("");
                    newShows.put(showTmdbId, item);
                }
            } else {
                SgShow2CloudUpdate update = SgRoomDatabase.getInstance(context)
                        .sgShow2Helper()
                        .getForCloudUpdate(showIdOrNull);
                if (update != null) {
                    boolean hasUpdates = false;
                    if (show.getIsFavorite() != null) {
                        // when merging, favorite shows, but never unfavorite them
                        if (!mergeValues || show.getIsFavorite()) {
                            update.setFavorite(show.getIsFavorite());
                            hasUpdates = true;
                        }
                    }
                    if (show.getNotify() != null) {
                        // when merging, enable notifications, but never disable them
                        if (!mergeValues || show.getNotify()) {
                            update.setNotify(show.getNotify());
                            hasUpdates = true;
                        }
                    }
                    if (show.getIsHidden() != null) {
                        // when merging, un-hide shows, but never hide them
                        if (!mergeValues || !show.getIsHidden()) {
                            update.setHidden(show.getIsHidden());
                            hasUpdates = true;
                        }
                    }
                    if (!TextUtils.isEmpty(show.getLanguage())) {
                        // always overwrite with hexagon language value
                        update.setLanguage(show.getLanguage());
                        hasUpdates = true;
                    }
                    if (hasUpdates) batch.add(update);
                }
            }
        }
        return batch;
    }

    /**
     * Uploads all local shows to Hexagon.
     */
    public boolean uploadAll() {
        Timber.d("uploadAll: uploading all shows");
        List<SgShow2CloudUpdate> forCloudUpdate = SgRoomDatabase.getInstance(context)
                .sgShow2Helper().getForCloudUpdate();

        List<SgCloudShow> shows = new LinkedList<>();
        for (SgShow2CloudUpdate localShow : forCloudUpdate) {
            if (localShow.getTmdbId() == null) continue;
            SgCloudShow show = new SgCloudShow();
            show.setTmdbId(localShow.getTmdbId());
            show.setIsFavorite(localShow.getFavorite());
            show.setNotify(localShow.getNotify());
            show.setIsHidden(localShow.getHidden());
            show.setLanguage(localShow.getLanguage());
            shows.add(show);
        }
        if (shows.size() == 0) {
            Timber.d("uploadAll: no shows to upload");
            // nothing to upload
            return true;
        }

        return upload(shows);
    }

    /**
     * Uploads the given list of shows to Hexagon.
     */
    public boolean upload(List<SgCloudShow> shows) {
        if (shows.isEmpty()) {
            Timber.d("upload: no shows to upload");
            return true;
        }

        // wrap into helper object
        SgCloudShowList showList = new SgCloudShowList();
        showList.setShows(shows);

        // upload shows
        try {
            // get service each time to check if auth was removed
            Shows showsService = hexagonTools.getShowsService();
            if (showsService == null) {
                return false;
            }
            showsService.saveSgShows(showList).execute();
        } catch (IOException e) {
            Errors.logAndReportHexagon("save shows", e);
            return false;
        }

        return true;
    }
}
