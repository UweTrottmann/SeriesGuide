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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
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

/**
 * Common activities and tools useful when interacting with shows.
 */
public class ShowTools {

    private static final int SHOWS_MAX_BATCH_SIZE = 100;

    private static ShowTools _instance;

    private final Context mContext;

    public static synchronized ShowTools get(Context context) {
        if (_instance == null) {
            _instance = new ShowTools(context.getApplicationContext());
        }
        return _instance;
    }

    private ShowTools(Context context) {
        mContext = context;
    }

    /**
     * Removes a show and its seasons and episodes, including all images. Sends isRemoved flag to
     * Hexagon.
     *
     * @return One of {@link com.battlelancer.seriesguide.enums.NetworkResult}.
     */
    public int removeShow(int showTvdbId) {
        if (HexagonTools.isSignedIn(mContext)) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return NetworkResult.OFFLINE;
            }
            // send to cloud
            sendIsRemoved(showTvdbId, true);
        }

        // remove database entries in stages, so if an earlier stage fails, user can at least try again
        // also saves memory by applying batches early

        // SEARCH DATABASE ENTRIES
        final Cursor episodes = mContext.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId), new String[] {
                        SeriesGuideContract.Episodes._ID
                }, null, null, null
        );
        if (episodes == null) {
            // failed
            return Result.ERROR;
        }
        List<String> episodeTvdbIds = new LinkedList<>(); // need those for search entries
        while (episodes.moveToNext()) {
            episodeTvdbIds.add(episodes.getString(0));
        }
        episodes.close();

        // remove episode search database entries
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (String episodeTvdbId : episodeTvdbIds) {
            batch.add(ContentProviderOperation.newDelete(
                    SeriesGuideContract.EpisodeSearch.buildDocIdUri(episodeTvdbId)).build());
        }
        try {
            DBUtils.applyInSmallBatches(mContext, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Removing episode search entries failed");
            return Result.ERROR;
        }
        batch.clear();

        // ACTUAL ENTITY ENTRIES
        // remove episodes, seasons and show
        batch.add(ContentProviderOperation.newDelete(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId)).build());
        batch.add(ContentProviderOperation.newDelete(
                SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId)).build());
        batch.add(ContentProviderOperation.newDelete(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId)).build());
        try {
            DBUtils.applyInSmallBatches(mContext, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Removing episodes, seasons and show failed");
            return Result.ERROR;
        }

        // make sure other loaders (activity, overview, details) are notified
        mContext.getContentResolver().notifyChange(
                SeriesGuideContract.Episodes.CONTENT_URI_WITHSHOW, null);

        return Result.SUCCESS;
    }

    /**
     * Sets the isRemoved flag of the given show on Hexagon.
     *
     * @param isRemoved If true, the show will not be auto-added on any device connected to
     *                  Hexagon.
     */
    public void sendIsRemoved(int showTvdbId, boolean isRemoved) {
        Show show = new Show();
        show.setTvdbId(showTvdbId);
        show.setIsRemoved(isRemoved);
        uploadShowAsync(show);
    }

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsFavorite(int showTvdbId, boolean isFavorite) {
        if (HexagonTools.isSignedIn(mContext)) {
            if (!Utils.isConnected(mContext, true)) {
                return;
            }
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setIsFavorite(isFavorite);
            uploadShowAsync(show);
        }

        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.FAVORITE, isFavorite);
        mContext.getContentResolver().update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);

        // favorite status may determine eligibility for notifications
        Utils.runNotificationService(mContext);

        Toast.makeText(mContext, mContext.getString(isFavorite ?
                R.string.favorited : R.string.unfavorited), Toast.LENGTH_SHORT).show();
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsHidden(int showTvdbId, boolean isHidden) {
        if (HexagonTools.isSignedIn(mContext)) {
            if (!Utils.isConnected(mContext, true)) {
                return;
            }
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setIsHidden(isHidden);
            uploadShowAsync(show);
        }

        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.HIDDEN, isHidden);
        mContext.getContentResolver().update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);

        Toast.makeText(mContext, mContext.getString(isHidden ?
                R.string.hidden : R.string.unhidden), Toast.LENGTH_SHORT).show();
    }

    /**
     * Saves new GetGlue id to the local database and, if signed in, up into the cloud as well.
     */
    public void storeGetGlueId(int showTvdbId, String getglueId) {
        if (HexagonTools.isSignedIn(mContext)) {
            if (!Utils.isConnected(mContext, true)) {
                return;
            }
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setTvtagId(getglueId);
            uploadShowAsync(show);
        }

        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.GETGLUEID, getglueId);
        mContext.getContentResolver()
                .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);
    }

    private void uploadShowAsync(Show show) {
        AndroidUtils.executeOnPool(
                new ShowsUploadTask(mContext, show)
        );
    }

    private static class ShowsUploadTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        private final Show mShow;

        public ShowsUploadTask(Context context, Show show) {
            mContext = context;
            mShow = show;
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<Show> shows = new LinkedList<>();
            shows.add(mShow);

            Upload.toHexagon(mContext, shows);

            return null;
        }
    }

    public static class Upload {

        /**
         * Uploads all local shows to Hexagon.
         */
        public static boolean toHexagon(Context context) {
            Timber.d("toHexagon: uploading all shows");
            List<Show> shows = buildShowList(context);
            if (shows == null) {
                Timber.e("toHexagon: show query was null");
                return false;
            }
            if (shows.size() == 0) {
                Timber.d("toHexagon: no shows to upload");
                // nothing to upload
                return true;
            }
            return toHexagon(context, shows);
        }

        /**
         * Uploads the given list of shows to Hexagon.
         *
         * @return One of {@link com.battlelancer.seriesguide.enums.Result}.
         */
        public static boolean toHexagon(Context context, List<Show> shows) {
            // wrap into helper object
            ShowList showList = new ShowList();
            showList.setShows(shows);

            // upload shows
            try {
                HexagonTools.getShowsService(context).save(showList).execute();
            } catch (IOException e) {
                Timber.e(e, "toHexagon: failed to upload shows");
                return false;
            }

            return true;
        }

        private static List<Show> buildShowList(Context context) {
            List<Show> shows = new LinkedList<>();

            Cursor query = context.getContentResolver()
                    .query(SeriesGuideContract.Shows.CONTENT_URI, new String[] {
                            SeriesGuideContract.Shows._ID,
                            SeriesGuideContract.Shows.FAVORITE,
                            SeriesGuideContract.Shows.HIDDEN,
                            SeriesGuideContract.Shows.GETGLUEID
                    }, null, null, null);
            if (query == null) {
                return null;
            }

            while (query.moveToNext()) {
                Show show = new Show();
                show.setTvdbId(query.getInt(0));
                show.setIsFavorite(query.getInt(1) == 1);
                show.setIsHidden(query.getInt(2) == 1);
                show.setTvtagId(query.getString(3));
                shows.add(show);
            }

            query.close();

            return shows;
        }
    }

    public static class Download {

        /**
         * Downloads shows from Hexagon and updates existing shows with new property values. Any
         * shows not yet in the local database, determined by the given TVDb id set, will be added
         * to the given map.
         */
        public static boolean fromHexagon(Context context, HashSet<Integer> existingShows,
                HashMap<Integer, SearchResult> newShows, boolean hasMergedShows) {
            List<Show> shows;
            boolean hasMoreShows = true;
            String cursor = null;
            long currentTime = System.currentTimeMillis();
            DateTime lastSyncTime = new DateTime(HexagonSettings.getLastShowsSyncTime(context));

            if (hasMergedShows) {
                Timber.d("fromHexagon: downloading changed shows since " + lastSyncTime);
            } else {
                Timber.d("fromHexagon: downloading all shows");
            }

            while (hasMoreShows) {
                // abort if connection is lost
                if (!AndroidUtils.isNetworkConnected(context)) {
                    Timber.e("fromHexagon: no network connection");
                    return false;
                }

                try {
                    Shows.Get request = HexagonTools.getShowsService(context).get()
                            .setLimit(SHOWS_MAX_BATCH_SIZE);
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
                        Timber.d("fromHexagon: response was null, done here");
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
                    Timber.e(e, "fromHexagon: failed to download shows");
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
                    Timber.e(e, "fromHexagon: applying show updates failed");
                    return false;
                }
            }

            if (hasMergedShows) {
                // set new last sync time
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putLong(HexagonSettings.KEY_LAST_SYNC_SHOWS, currentTime)
                        .commit();
            }

            return true;
        }

        private static ArrayList<ContentProviderOperation> buildShowUpdateOps(List<Show> shows,
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
                        item.tvdbid = show.getTvdbId();
                        item.title = "";
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
         * @param mergeValues If set, only overwrites property if remote show property has a
         *                    certain value.
         */
        private static void buildShowPropertyValues(Show show, ContentValues values,
                boolean mergeValues) {
            if (show.getIsFavorite() != null) {
                // when merging, favorite shows, but never unfavorite them
                if (!mergeValues || show.getIsFavorite()) {
                    values.put(SeriesGuideContract.Shows.FAVORITE, show.getIsFavorite());
                }
            }
            if (show.getIsHidden() != null) {
                // when merging, un-hide shows, but never hide them
                if (!mergeValues || !show.getIsHidden()) {
                    values.put(SeriesGuideContract.Shows.HIDDEN, show.getIsHidden());
                }
            }
            if (show.getTvtagId() != null) {
                // always overwrite with the hexagon tvtag id
                values.put(SeriesGuideContract.Shows.GETGLUEID, show.getTvtagId());
            }
        }

        /**
         * If the given show exists on Hexagon, downloads and sets properties from Hexagon on the
         * given show entity.
         *
         * <p> <b>Note:</b> Ensure the given show has a valid TVDb id.
         */
        public static void showPropertiesFromHexagon(Context context,
                com.battlelancer.seriesguide.dataliberation.model.Show show) throws IOException {
            Show hexagonShow = HexagonTools.getShowsService(context)
                    .getShow()
                    .setShowTvdbId(show.tvdbId)
                    .execute();
            if (hexagonShow != null) {
                if (hexagonShow.getIsFavorite() != null) {
                    show.favorite = hexagonShow.getIsFavorite();
                }
                if (hexagonShow.getIsHidden() != null) {
                    show.hidden = hexagonShow.getIsHidden();
                }
                if (hexagonShow.getTvtagId() != null) {
                    show.checkInGetGlueId = hexagonShow.getTvtagId();
                }
            }
        }
    }

    /**
     * Returns a set of the TVDb ids of all shows in the local database.
     *
     * @return null if there was an error, empty list if there are no shows.
     */
    public static HashSet<Integer> getShowTvdbIdsAsSet(Context context) {
        HashSet<Integer> existingShows = new HashSet<>();

        Cursor shows = context.getContentResolver().query(SeriesGuideContract.Shows.CONTENT_URI,
                new String[] { SeriesGuideContract.Shows._ID }, null, null, null);
        if (shows == null) {
            return null;
        }

        while (shows.moveToNext()) {
            existingShows.add(shows.getInt(0));
        }

        shows.close();

        return existingShows;
    }
}
