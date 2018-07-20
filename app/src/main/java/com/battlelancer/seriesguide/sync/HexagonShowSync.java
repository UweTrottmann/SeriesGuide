package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.shows.Shows;
import com.uwetrottmann.seriesguide.backend.shows.model.Show;
import com.uwetrottmann.seriesguide.backend.shows.model.ShowList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import timber.log.Timber;

public class HexagonShowSync {

    private Context context;
    private HexagonTools hexagonTools;

    public HexagonShowSync(Context context, HexagonTools hexagonTools) {
        this.context = context;
        this.hexagonTools = hexagonTools;
    }

    /**
     * Downloads shows from Hexagon and updates existing shows with new property values. Any
     * shows not yet in the local database, determined by the given TVDb id set, will be added
     * to the given map.
     */
    public boolean download(HashSet<Integer> existingShows, HashMap<Integer, SearchResult> newShows,
            boolean hasMergedShows) {
        List<Show> shows;
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

                Shows.Get request = showsService.get(); // use default server limit
                if (hasMergedShows) {
                    // only get changed shows (otherwise returns all)
                    request.setUpdatedSince(lastSyncTime);
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                ShowList response = request.execute();
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
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(context, "get shows", e);
                return false;
            }

            if (shows == null || shows.size() == 0) {
                // nothing to do here
                break;
            }

            // update all received shows, ContentProvider will ignore those not added locally
            ArrayList<ContentProviderOperation> batch = buildShowUpdateOps(shows, existingShows,
                    newShows, !hasMergedShows);

            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e(e, "download: applying show updates failed");
                return false;
            }
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

    private ArrayList<ContentProviderOperation> buildShowUpdateOps(List<Show> shows,
            HashSet<Integer> existingShows, HashMap<Integer, SearchResult> newShows,
            boolean mergeValues) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        ContentValues values = new ContentValues();
        for (Show show : shows) {
            // schedule to add shows not in local database
            if (!existingShows.contains(show.getTvdbId())) {
                // ...but do NOT add shows marked as removed
                if (show.getIsRemoved() != null && show.getIsRemoved()) {
                    continue;
                }

                if (!newShows.containsKey(show.getTvdbId())) {
                    SearchResult item = new SearchResult();
                    item.setTvdbid(show.getTvdbId());
                    item.setLanguage(show.getLanguage());
                    item.setTitle("");
                    newShows.put(show.getTvdbId(), item);
                }

                continue;
            }

            buildShowPropertyValues(show, values, mergeValues);

            // build update op
            if (values.size() > 0) {
                ContentProviderOperation op = ContentProviderOperation
                        .newUpdate(SeriesGuideContract.Shows.buildShowUri(show.getTvdbId()))
                        .withValues(values).build();
                batch.add(op);

                // clean up for re-use
                values.clear();
            }
        }

        return batch;
    }

    /**
     * @param mergeValues If set, only overwrites property if remote show property has a certain
     * value.
     */
    private void buildShowPropertyValues(Show show, ContentValues values, boolean mergeValues) {
        if (show.getIsFavorite() != null) {
            // when merging, favorite shows, but never unfavorite them
            if (!mergeValues || show.getIsFavorite()) {
                values.put(SeriesGuideContract.Shows.FAVORITE, show.getIsFavorite() ? 1 : 0);
            }
        }
        if (show.getNotify() != null) {
            // when merging, enable notifications, but never disable them
            if (!mergeValues || show.getNotify()) {
                values.put(SeriesGuideContract.Shows.NOTIFY, show.getNotify() ? 1 : 0);
            }
        }
        if (show.getIsHidden() != null) {
            // when merging, un-hide shows, but never hide them
            if (!mergeValues || !show.getIsHidden()) {
                values.put(SeriesGuideContract.Shows.HIDDEN, show.getIsHidden() ? 1 : 0);
            }
        }
        if (!TextUtils.isEmpty(show.getLanguage())) {
            // always overwrite with hexagon language value
            values.put(SeriesGuideContract.Shows.LANGUAGE, show.getLanguage());
        }
    }

    /**
     * Uploads all local shows to Hexagon.
     */
    public boolean uploadAll() {
        Timber.d("uploadAll: uploading all shows");
        List<Show> shows = buildShowList();
        if (shows == null) {
            Timber.e("uploadAll: show query was null");
            return false;
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
    public boolean upload(List<Show> shows) {
        // wrap into helper object
        ShowList showList = new ShowList();
        showList.setShows(shows);

        // upload shows
        try {
            // get service each time to check if auth was removed
            Shows showsService = hexagonTools.getShowsService();
            if (showsService == null) {
                return false;
            }
            showsService.save(showList).execute();
        } catch (IOException e) {
            HexagonTools.trackFailedRequest(context, "save shows", e);
            return false;
        }

        return true;
    }

    private List<Show> buildShowList() {
        List<Show> shows = new LinkedList<>();

        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.Shows.CONTENT_URI, new String[] {
                        SeriesGuideContract.Shows._ID, // 0
                        SeriesGuideContract.Shows.FAVORITE, // 1
                        SeriesGuideContract.Shows.NOTIFY, // 2
                        SeriesGuideContract.Shows.HIDDEN, // 3
                        SeriesGuideContract.Shows.LANGUAGE // 4
                }, null, null, null);
        if (query == null) {
            return null;
        }

        while (query.moveToNext()) {
            Show show = new Show();
            show.setTvdbId(query.getInt(0));
            show.setIsFavorite(query.getInt(1) == 1);
            show.setNotify(query.getInt(2) == 1);
            show.setIsHidden(query.getInt(3) == 1);
            show.setLanguage(query.getString(4));
            shows.add(show);
        }

        query.close();

        return shows;
    }
}
