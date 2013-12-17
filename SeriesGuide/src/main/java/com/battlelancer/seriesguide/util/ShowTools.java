package com.battlelancer.seriesguide.util;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;

import com.battlelancer.seriesguide.backend.CloudEndpointUtils;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.seriesguide.shows.Shows;
import com.uwetrottmann.seriesguide.shows.model.CollectionResponseShow;
import com.uwetrottmann.seriesguide.shows.model.Show;
import com.uwetrottmann.seriesguide.shows.model.ShowList;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static com.battlelancer.seriesguide.sync.SgSyncAdapter.UpdateResult;

/**
 * Common activities and tools useful when interacting with shows.
 */
public class ShowTools {

    private static ShowTools _instance;

    private final Context mContext;

    private final GoogleAccountCredential mCredential;

    private final Shows mShowsService;

    public static synchronized ShowTools get(Context context) {
        if (_instance == null) {
            _instance = new ShowTools(context);
        }
        return _instance;
    }

    private ShowTools(Context context) {
        mContext = context;

        // get registered Google account
        mCredential = GoogleAccountCredential.usingAudience(context, HexagonSettings.AUDIENCE);
        setShowsServiceAccountName(HexagonSettings.getAccountName(context));

        // build show service endpoint
        Shows.Builder builder = new Shows.Builder(
                AndroidHttp.newCompatibleTransport(), new JacksonFactory(), mCredential
        );
        mShowsService = CloudEndpointUtils.updateBuilder(builder).build();
    }

    public void setShowsServiceAccountName(String accountName) {
        mCredential.setSelectedAccountName(accountName);
    }

    /**
     * Removes a show and its seasons and episodes, including all images. Sends isRemoved flag to
     * Hexagon.
     *
     * @return One of {@link com.battlelancer.seriesguide.enums.Result}.
     */
    public int removeShow(int showTvdbId) {
        if (isSignedIn()) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return Result.OFFLINE;
            }
            // send to cloud
            sendIsRemoved(showTvdbId, true);
        }

        // remove database entries in last stage, so if an earlier stage fails, user can at least try again

        // IMAGES
        final ImageProvider imageProvider = ImageProvider.getInstance(mContext);

        // remove episode images
        final Cursor episodes = mContext.getContentResolver().query(
                SeriesContract.Episodes.buildEpisodesOfShowUri(showTvdbId), new String[]{
                SeriesContract.Episodes._ID, SeriesContract.Episodes.IMAGE
        }, null, null, null);
        if (episodes == null) {
            // failed
            return Result.GENERIC_ERROR;
        }
        List<String> episodeTvdbIds = new LinkedList<>(); // need those for search entries
        while (episodes.moveToNext()) {
            episodeTvdbIds.add(episodes.getString(0));
            String imageUrl = episodes.getString(1);
            if (!TextUtils.isEmpty(imageUrl)) {
                imageProvider.removeImage(imageUrl);
            }
        }
        episodes.close();

        // remove show poster
        final Cursor show = mContext.getContentResolver().query(
                SeriesContract.Shows.buildShowUri(showTvdbId),
                new String[]{
                        SeriesContract.Shows.POSTER
                }, null, null, null);
        if (show == null || !show.moveToFirst()) {
            // failed
            return Result.GENERIC_ERROR;
        }
        String posterPath = show.getString(0);
        if (!TextUtils.isEmpty(posterPath)) {
            imageProvider.removeImage(posterPath);
        }
        show.close();

        // DATABASE ENTRIES
        // apply batches early to save memory
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // remove episode search database entries
        for (String episodeTvdbId : episodeTvdbIds) {
            batch.add(ContentProviderOperation.newDelete(
                    SeriesContract.EpisodeSearch.buildDocIdUri(episodeTvdbId)).build());
        }
        DBUtils.applyInSmallBatches(mContext, batch);
        batch.clear();

        // remove episodes, seasons and show
        batch.add(ContentProviderOperation.newDelete(
                SeriesContract.Episodes.buildEpisodesOfShowUri(showTvdbId)).build());
        batch.add(ContentProviderOperation.newDelete(
                SeriesContract.Seasons.buildSeasonsOfShowUri(showTvdbId)).build());
        batch.add(ContentProviderOperation.newDelete(
                SeriesContract.Shows.buildShowUri(showTvdbId)).build());
        DBUtils.applyInSmallBatches(mContext, batch);

        return Result.SUCCESS;
    }

    /**
     * Sends new removed flag, if signed in, up into the cloud.
     */
    public void sendIsRemoved(int showTvdbId, boolean isRemoved) {
        if (isSignedIn()) {
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setIsRemoved(isRemoved);
            uploadShowAsync(show);
        }
    }

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsFavorite(int showTvdbId, boolean isFavorite) {
        if (isSignedIn()) {
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
        values.put(SeriesContract.Shows.FAVORITE, isFavorite);
        mContext.getContentResolver().update(
                SeriesContract.Shows.buildShowUri(showTvdbId), values, null, null);
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsHidden(int showTvdbId, boolean isHidden) {
        if (isSignedIn()) {
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
        values.put(SeriesContract.Shows.HIDDEN, isHidden);
        mContext.getContentResolver().update(
                SeriesContract.Shows.buildShowUri(showTvdbId), values, null, null);
    }

    /**
     * Saves new GetGlue id to the local database and, if signed in, up into the cloud as well.
     */
    public void storeGetGlueId(int showTvdbId, String getglueId) {
        if (isSignedIn()) {
            if (!Utils.isConnected(mContext, true)) {
                return;
            }
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setGetGlueId(getglueId);
            uploadShowAsync(show);
        }

        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesContract.Shows.GETGLUEID, getglueId);
        mContext.getContentResolver()
                .update(SeriesContract.Shows.buildShowUri(showTvdbId), values, null, null);
    }

    public boolean isSignedIn() {
        return mCredential.getSelectedAccountName() != null;
    }

    private void uploadShowAsync(Show show) {
        List<Show> shows = new LinkedList<>();
        shows.add(show);
        new ShowsUploadTask(mContext, shows).execute();
    }

    private static class ShowsUploadTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        private final List<Show> mShows;

        public ShowsUploadTask(Context context, List<Show> shows) {
            mContext = context;
            mShows = shows;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Upload.shows(mContext, mShows);

            return null;
        }

    }

    public static class Upload {

        public static final int SUCCESS = 0;

        public static final int FAILURE = -1;

        private static final String TAG = "ShowTools.Upload";

        /**
         * Tries to upload the given list of shows to Hexagon.
         *
         * @return 0 if successful, -1 if something went wrong.
         */
        public static int shows(Context context, List<Show> shows) {
            int resultCode = SUCCESS;

            // wrap into helper object
            ShowList showList = new ShowList();
            showList.setShows(shows);

            Log.d(TAG, "Uploading show(s)...");

            // upload shows
            try {
                ShowTools.get(context).mShowsService.save(showList).execute();
            } catch (IOException e) {
                Utils.trackExceptionAndLog(context, TAG, e);
                resultCode = FAILURE;
            }

            Log.d(TAG, "Uploading show(s)...DONE");

            return resultCode;
        }

        /**
         * Tries to upload all shows in the local database to Hexagon.
         *
         * @return 0 if successful, -1 if something went wrong.
         */
        public static int showsAll(Context context) {
            return shows(context, getLocalShowsAsList(context));
        }

        public static List<Show> getLocalShowsAsList(Context context) {
            return getSelectedLocalShowsAsList(context, null);
        }

        public static List<Show> getSelectedLocalShowsAsList(Context context,
                HashSet<Integer> showTvdbIds) {
            List<Show> shows = new LinkedList<>();

            Cursor query = context.getContentResolver()
                    .query(SeriesContract.Shows.CONTENT_URI, new String[]{
                            SeriesContract.Shows._ID, SeriesContract.Shows.FAVORITE,
                            SeriesContract.Shows.HIDDEN, SeriesContract.Shows.GETGLUEID,
                            SeriesContract.Shows.SYNCENABLED
                    }, null, null, null);
            if (query == null) {
                return null;
            }

            while (query.moveToNext()) {
                int showTvdbId = query.getInt(0);
                if (showTvdbIds != null && !showTvdbIds.contains(showTvdbId)) {
                    // skip this show
                    continue;
                }
                Show show = new Show();
                show.setTvdbId(showTvdbId);
                show.setIsFavorite(query.getInt(1) == 1);
                show.setIsHidden(query.getInt(2) == 1);
                show.setGetGlueId(query.getString(3));
                show.setIsSyncEnabled(query.getInt(4) == 1);
                shows.add(show);
            }

            query.close();

            return shows;
        }
    }

    public static class Download {

        private static final String TAG = "ShowTools.Download";

        /**
         * Downloads shows from Hexagon and updates existing shows with new property values. Any
         * shows not yet in the local database, determined by the given TVDb id set, will be added
         * to the given map.
         */
        public static UpdateResult syncRemoteShows(Context context,
                HashSet<Integer> showsExisting, HashMap<Integer, SearchResult> showsNew) {
            // download shows
            List<Show> shows = getRemoteShows(context);
            if (shows == null) {
                // response got screwed up
                return UpdateResult.INCOMPLETE;
            }
            if (shows.size() == 0) {
                // no shows on hexagon, nothing to do
                return UpdateResult.SUCCESS;
            }

            // update all received shows, ContentProvider will ignore those not added locally
            ArrayList<ContentProviderOperation> batch = buildShowUpdateOps(shows, showsExisting,
                    showsNew);
            DBUtils.applyInSmallBatches(context, batch);

            return UpdateResult.SUCCESS;
        }

        /**
         * Downloads a list of all shows on Hexagon.
         */
        public static List<Show> getRemoteShows(Context context) {
            // download shows
            CollectionResponseShow remoteShows = null;
            try {
                remoteShows = ShowTools.get(context).mShowsService.list().execute();
            } catch (IOException e) {
                Utils.trackExceptionAndLog(context, TAG, e);
            }

            // abort if no response
            if (remoteShows == null) {
                return null;
            }

            // extract list of remote shows
            return remoteShows.getItems();
        }

        private static ArrayList<ContentProviderOperation> buildShowUpdateOps(List<Show> shows,
                HashSet<Integer> showsExisting, HashMap<Integer, SearchResult> showsNew) {
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();

            ContentValues values = new ContentValues();
            for (Show show : shows) {
                // skip shows not in local database
                if (!showsExisting.contains(show.getTvdbId())) {
                    // skip shows flagged as removed
                    if (show.getIsRemoved() != null && show.getIsRemoved()) {
                        continue;
                    }

                    if (!showsNew.containsKey(show.getTvdbId())) {
                        // add show later
                        SearchResult item = new SearchResult();
                        item.tvdbid = show.getTvdbId();
                        item.title = "";
                        showsNew.put(show.getTvdbId(), item);
                    }

                    // do not create an update op for show
                    continue;
                }

                putSyncedShowPropertyValues(show, values);

                // build update op
                ContentProviderOperation op = ContentProviderOperation
                        .newUpdate(SeriesContract.Shows.buildShowUri(show.getTvdbId()))
                        .withValues(values).build();
                batch.add(op);

                // clean up for re-use
                values.clear();
            }

            return batch;
        }

        private static void putSyncedShowPropertyValues(Show show, ContentValues values) {
            putPropertyValueIfNotNull(values, SeriesContract.Shows.FAVORITE, show.getIsFavorite());
            putPropertyValueIfNotNull(values, SeriesContract.Shows.HIDDEN, show.getIsHidden());
            putPropertyValueIfNotNull(values, SeriesContract.Shows.SYNCENABLED,
                    show.getIsSyncEnabled());
            putPropertyValueIfNotNull(values, SeriesContract.Shows.GETGLUEID, show.getGetGlueId());
        }

        private static void putPropertyValueIfNotNull(ContentValues values, String key,
                Boolean value) {
            if (value != null) {
                values.put(key, value.booleanValue());
            }
        }

        private static void putPropertyValueIfNotNull(ContentValues values, String key,
                String value) {
            if (value != null) {
                values.put(key, value);
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

        Cursor shows = context.getContentResolver().query(SeriesContract.Shows.CONTENT_URI,
                new String[]{SeriesContract.Shows._ID}, null, null, null);
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
