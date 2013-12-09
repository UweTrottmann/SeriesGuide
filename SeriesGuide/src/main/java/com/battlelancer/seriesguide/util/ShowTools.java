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
            uploadShow(show);
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
            uploadShow(show);
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
            uploadShow(show);
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
            uploadShow(show);
        }
    }

    public boolean isSignedIn() {
        return mCredential.getSelectedAccountName() != null;
    }

    private void uploadShow(Show show) {
        List<Show> shows = new LinkedList<>();
        shows.add(show);
        new ShowsUploadTask(mShowsService, shows).execute();
    }

    private static class ShowsUploadTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "ShowTools.Upload";

        private final Shows mShowsService;

        private final List<Show> mShows;

        public ShowsUploadTask(Shows showsService, List<Show> shows) {
            mShowsService = showsService;
            mShows = shows;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ShowList showList = new ShowList();
            showList.setShows(mShows);

            Log.d(TAG, "Uploading show(s)...");

            // upload shows
            try {
                mShowsService.save(showList).execute();
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
            }

            Log.d(TAG, "Uploading show(s)...DONE");

            return null;
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
                Log.w(TAG, e.getMessage(), e);
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
                // skip shows flagged as removed (null will fall back to false)
                if (show.getIsRemoved()) {
                    continue;
                }

                // skip shows not in local database
                if (!showsExisting.contains(show.getTvdbId())) {
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
