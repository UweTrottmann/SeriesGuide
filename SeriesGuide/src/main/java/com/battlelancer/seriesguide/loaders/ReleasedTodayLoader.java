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
        long currentTime = TimeTools.getCurrentTime(getContext());
        long timeAtStartOfDay = new DateTime(currentTime).withTimeAtStartOfDay().getMillis();

        Cursor query = getContext().getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                new String[] {
                        SeriesGuideDatabase.Tables.EPISODES + "." + Episodes._ID, // 0
                        Episodes.TITLE,
                        Episodes.NUMBER,
                        Episodes.SEASON,
                        Episodes.FIRSTAIREDMS,
                        Episodes.WATCHED, // 5
                        Episodes.COLLECTED,
                        Shows.REF_SHOW_ID,
                        Shows.TITLE,
                        Shows.NETWORK,
                        Shows.POSTER // 10
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

            while (query.moveToNext()) {
                NowAdapter.NowItem item = new NowAdapter.NowItem(
                        query.getInt(0),
                        query.getString(8),
                        Utils.getNextEpisodeString(getContext(), query.getInt(3), query.getInt(2),
                                query.getString(1)),
                        query.getString(10),
                        NowAdapter.NowType.RELEASED_TODAY
                );
                items.add(item);
            }

            query.close();

            return items;
        }

        return null;
    }
}
