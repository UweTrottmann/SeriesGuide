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
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;

/**
 * Loads a list of episodes released today and wraps them in a list of {@link
 * com.battlelancer.seriesguide.adapters.NowAdapter.NowItem} for use with {@link
 * com.battlelancer.seriesguide.adapters.NowAdapter}.
 */
public class ReleasedTodayLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    public ReleasedTodayLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        // go to start of current day
        long timeAtStartOfDay = new DateTime().withTimeAtStartOfDay().getMillis();
        // modify time to meet any user offset on episode release instants
        timeAtStartOfDay = TimeTools.applyUserOffsetInverted(getContext(), timeAtStartOfDay);

        Cursor query = getContext().getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                new String[] {
                        SeriesGuideDatabase.Tables.EPISODES + "." + Episodes._ID, // 0
                        Episodes.TITLE,
                        Episodes.NUMBER,
                        Episodes.SEASON,
                        Episodes.FIRSTAIREDMS, // 4
                        Shows.REF_SHOW_ID,
                        Shows.TITLE,
                        Shows.NETWORK,
                        Shows.POSTER // 8
                }, Episodes.FIRSTAIREDMS + ">=? AND " + Episodes.FIRSTAIREDMS + "<? AND "
                        + Shows.SELECTION_NO_HIDDEN,
                new String[] {
                        String.valueOf(timeAtStartOfDay),
                        String.valueOf(timeAtStartOfDay + DateUtils.DAY_IN_MILLIS)
                },
                Episodes.FIRSTAIREDMS + " DESC," + Shows.TITLE + " ASC,"
                        + Episodes.NUMBER + " DESC");
        if (query != null) {
            List<NowAdapter.NowItem> items = new ArrayList<>();

            // add header
            if (query.getCount() > 0) {
                items.add(new NowAdapter.NowItem().header(
                        getContext().getString(R.string.released_today)));
            }

            // add items
            while (query.moveToNext()) {
                NowAdapter.NowItem item = new NowAdapter.NowItem()
                        .displayData(
                                query.getLong(4),
                                query.getString(6),
                                Utils.getNextEpisodeString(getContext(), query.getInt(3),
                                        query.getInt(2),
                                        query.getString(1)),
                                query.getString(8)
                        )
                        .tvdbIds(query.getInt(0), query.getInt(5))
                        .releasedToday(query.getString(7));

                items.add(item);
            }

            query.close();

            return items;
        }

        return null;
    }
}
