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
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.androidutils.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import timber.log.Timber;

/**
 * Common activities and tools useful when interacting with shows.
 */
public class ShowTools {

    private static ShowTools _instance;

    private final Context mContext;

    public static synchronized ShowTools get(Context context) {
        if (_instance == null) {
            _instance = new ShowTools(context);
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
        // remove database entries in last stage, so if an earlier stage fails, user can at least try again

        // IMAGES
        final ImageProvider imageProvider = ImageProvider.getInstance(mContext);

        // remove episode images
        final Cursor episodes = mContext.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId), new String[]{
                SeriesGuideContract.Episodes._ID, SeriesGuideContract.Episodes.IMAGE
        }, null, null, null);
        if (episodes == null) {
            // failed
            return Result.ERROR;
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
                SeriesGuideContract.Shows.buildShowUri(showTvdbId),
                new String[]{
                        SeriesGuideContract.Shows.POSTER
                }, null, null, null);
        if (show == null || !show.moveToFirst()) {
            // failed
            return Result.ERROR;
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
                    SeriesGuideContract.EpisodeSearch.buildDocIdUri(episodeTvdbId)).build());
        }
        try {
            DBUtils.applyInSmallBatches(mContext, batch);
        } catch (OperationApplicationException e) {
            Timber.e("Removing episode search entries failed", e);
            return Result.ERROR;
        }
        batch.clear();

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
            Timber.e("Removing episodes, seasons and show failed", e);
            return Result.ERROR;
        }

        // make sure other loaders (activity, overview, details) are notified
        mContext.getContentResolver().notifyChange(
                SeriesGuideContract.Episodes.CONTENT_URI_WITHSHOW, null);

        return Result.SUCCESS;
    }

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsFavorite(int showTvdbId, boolean isFavorite) {
        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.FAVORITE, isFavorite);
        mContext.getContentResolver().update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);

        Toast.makeText(mContext, mContext.getString(isFavorite ?
                R.string.favorited : R.string.unfavorited), Toast.LENGTH_SHORT).show();
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsHidden(int showTvdbId, boolean isHidden) {
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
        // save to local database
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.GETGLUEID, getglueId);
        mContext.getContentResolver()
                .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);
    }

    /**
     * Returns a set of the TVDb ids of all shows in the local database.
     *
     * @return null if there was an error, empty list if there are no shows.
     */
    public static HashSet<Integer> getShowTvdbIdsAsSet(Context context) {
        HashSet<Integer> existingShows = new HashSet<>();

        Cursor shows = context.getContentResolver().query(SeriesGuideContract.Shows.CONTENT_URI,
                new String[]{SeriesGuideContract.Shows._ID}, null, null, null);
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
