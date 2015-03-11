/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Adapter for show search result items.
 */
public class ShowResultsAdapter extends BaseShowsAdapter {

    public ShowResultsAdapter(Context context, OnContextMenuClickListener listener) {
        super(context, listener);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ShowViewHolder viewHolder = (ShowViewHolder) view.getTag();

        viewHolder.showTvdbId = cursor.getInt(Query.ID);
        viewHolder.isFavorited = cursor.getInt(Query.FAVORITE) == 1;

        // show title
        viewHolder.name.setText(cursor.getString(Query.TITLE));

        // favorited label
        setFavoriteState(viewHolder.favorited, viewHolder.isFavorited);

        // network, day and time
        viewHolder.timeAndNetwork.setText(buildNetworkAndTimeString(context,
                cursor.getInt(Query.RELEASE_TIME),
                cursor.getInt(Query.RELEASE_WEEKDAY),
                cursor.getString(Query.RELEASE_TIMEZONE),
                cursor.getString(Query.RELEASE_COUNTRY),
                cursor.getString(Query.NETWORK)));

        // poster
        Utils.loadTvdbShowPoster(context, viewHolder.poster, cursor.getString(Query.POSTER));

        // context menu
        viewHolder.isHidden = DBUtils.restoreBooleanFromInt(cursor.getInt(Query.HIDDEN));
    }

    public interface Query {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Shows._ID, // 0
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.POSTER,
                SeriesGuideContract.Shows.FAVORITE,
                SeriesGuideContract.Shows.HIDDEN, // 4
                SeriesGuideContract.Shows.RELEASE_TIME,
                SeriesGuideContract.Shows.RELEASE_WEEKDAY,
                SeriesGuideContract.Shows.RELEASE_TIMEZONE,
                SeriesGuideContract.Shows.RELEASE_COUNTRY,
                SeriesGuideContract.Shows.NETWORK // 9
        };

        int ID = 0;
        int TITLE = 1;
        int POSTER = 2;
        int FAVORITE = 3;
        int HIDDEN = 4;
        int RELEASE_TIME = 5;
        int RELEASE_WEEKDAY = 6;
        int RELEASE_TIMEZONE = 7;
        int RELEASE_COUNTRY = 8;
        int NETWORK = 9;
    }
}
