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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a list of recently watched episodes.
 */
public class RecentlyWatchedLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    public RecentlyWatchedLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        long timeDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;

        // get activity of the last 24 hours with the latest one first
        Cursor query = getContext().getContentResolver()
                .query(SeriesGuideContract.Activity.CONTENT_URI,
                        new String[] { SeriesGuideContract.Activity.TIMESTAMP,
                                SeriesGuideContract.Activity.EPISODE_TVDB_ID },
                        SeriesGuideContract.Activity.TIMESTAMP + ">" + timeDayAgo, null,
                        SeriesGuideContract.Activity.TIMESTAMP + " DESC");
        if (query == null) {
            return null;
        }

        List<NowAdapter.NowItem> items = new ArrayList<>();
        while (query.moveToNext()) {
            long timestamp = query.getLong(0);
            int episodeTvdbId = query.getInt(1);

            // get episode details
            Cursor episodeQuery = getContext().getContentResolver().query(
                    SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                    new String[] {
                            SeriesGuideDatabase.Tables.EPISODES + "."
                                    + SeriesGuideContract.Episodes._ID, // 0
                            SeriesGuideContract.Episodes.TITLE,
                            SeriesGuideContract.Episodes.NUMBER,
                            SeriesGuideContract.Episodes.SEASON, // 3
                            SeriesGuideContract.Episodes.FIRSTAIREDMS,
                            SeriesGuideContract.Shows.REF_SHOW_ID,
                            SeriesGuideContract.Shows.TITLE,
                            SeriesGuideContract.Shows.POSTER // 7
                    }, null, null, null);
            if (episodeQuery == null) {
                continue;
            }

            // add items
            if (episodeQuery.moveToFirst()) {
                NowAdapter.NowItem item = new NowAdapter.NowItem().displayData(
                        timestamp,
                        episodeQuery.getString(6),
                        Utils.getNextEpisodeString(getContext(), episodeQuery.getInt(3),
                                episodeQuery.getInt(2), episodeQuery.getString(1)),
                        episodeQuery.getString(7)
                ).tvdbIds(episodeTvdbId, episodeQuery.getInt(5)).recentlyWatchedLocal();
                items.add(item);
            }

            episodeQuery.close();
        }

        query.close();

        // add header
        if (items.size() > 0) {
            items.add(0, new NowAdapter.NowItem().header(
                    getContext().getString(R.string.recently_watched)));
        }

        return items;
    }
}
