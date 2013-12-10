package com.battlelancer.seriesguide.util;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;

import com.battlelancer.seriesguide.backend.CloudEndpointUtils;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.uwetrottmann.seriesguide.shows.Shows;
import com.uwetrottmann.seriesguide.shows.model.CollectionResponseShow;
import com.uwetrottmann.seriesguide.shows.model.Show;
import com.uwetrottmann.seriesguide.shows.model.ShowList;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
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
        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesContract.Shows.FAVORITE, isFavorite);
        mContext.getContentResolver().update(
                SeriesContract.Shows.buildShowUri(showTvdbId), values, null, null);

        if (isSignedIn()) {
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setIsFavorite(isFavorite);
            uploadShowAsync(show);
        }
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsHidden(int showTvdbId, boolean isHidden) {
        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesContract.Shows.HIDDEN, isHidden);
        mContext.getContentResolver().update(
                SeriesContract.Shows.buildShowUri(showTvdbId), values, null, null);

        if (isSignedIn()) {
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setIsHidden(isHidden);
            uploadShowAsync(show);
        }
    }

    /**
     * Saves new GetGlue id to the local database and, if signed in, up into the cloud as well.
     */
    public void storeGetGlueId(int showTvdbId, String getglueId) {
        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesContract.Shows.GETGLUEID, getglueId);
        mContext.getContentResolver()
                .update(SeriesContract.Shows.buildShowUri(showTvdbId), values, null, null);

        if (isSignedIn()) {
            // send to cloud
            Show show = new Show();
            show.setTvdbId(showTvdbId);
            show.setGetGlueId(getglueId);
            uploadShowAsync(show);
        }
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

        private static final String TAG = "ShowTools.Upload";

        /**
         * Tries to upload the given list of shows to Hexagon.
         */
        public static void shows(Context context, List<Show> shows) {
            // wrap into helper object
            ShowList showList = new ShowList();
            showList.setShows(shows);

            Log.d(TAG, "Uploading show(s)...");

            // upload shows
            try {
                ShowTools.get(context).mShowsService.save(showList).execute();
            } catch (IOException e) {
                Utils.trackExceptionAndLog(context, TAG, e);
            }

            Log.d(TAG, "Uploading show(s)...DONE");
        }

        /**
         * Tries to upload all shows in the local database to Hexagon.
         */
        public static void showsAllLocal(Context context) {
            shows(context, getLocalShowsAsList(context));
        }

        public static List<Show> getLocalShowsAsList(Context context) {
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
                Show show = new Show();
                show.setTvdbId(query.getInt(0));
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

        public static UpdateResult getAllRemoteShows(Context context,
                HashSet<Integer> showsExisting, HashMap<Integer, SearchResult> showsNew) {
            // download shows
            CollectionResponseShow remoteShows = null;
            try {
                remoteShows = ShowTools.get(context).mShowsService.list().execute();
            } catch (IOException e) {
                Utils.trackExceptionAndLog(context, TAG, e);
            }

            // abort if no response
            if (remoteShows == null) {
                return UpdateResult.INCOMPLETE;
            }

            // extract list of remote shows
            List<Show> shows = remoteShows.getItems();
            if (shows == null || shows.size() == 0) {
                return UpdateResult.SUCCESS;
            }

            // update all received shows, ContentProvider will ignore those not added locally
            ArrayList<ContentProviderOperation> batch = buildShowUpdateOps(shows, showsExisting,
                    showsNew);
            DBUtils.applyInSmallBatches(context, batch);

            return UpdateResult.SUCCESS;
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

}
