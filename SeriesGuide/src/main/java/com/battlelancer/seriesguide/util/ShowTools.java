package com.battlelancer.seriesguide.util;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;

import com.battlelancer.seriesguide.backend.CloudEndpointUtils;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.uwetrottmann.seriesguide.shows.Shows;
import com.uwetrottmann.seriesguide.shows.model.Show;
import com.uwetrottmann.seriesguide.shows.model.ShowList;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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

    private boolean isSignedIn() {
        return mCredential.getSelectedAccountName() != null;
    }

    private void uploadShow(Show show) {
        List<Show> shows = new LinkedList<>();
        shows.add(show);
        new ShowsUploadTask(mShowsService, shows).execute();
    }

    private static class ShowsUploadTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "Show Upload";

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

            // upload shows
            try {
                mShowsService.save(showList).execute();
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
            }

            return null;
        }

    }

}
