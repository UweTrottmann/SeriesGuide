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
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.View;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Adapter for show items.
 */
public class ShowsAdapter extends BaseShowsAdapter {

    public ShowsAdapter(Context context, OnContextMenuClickListener listener) {
        super(context, listener);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ShowViewHolder viewHolder = (ShowViewHolder) view.getTag();

        viewHolder.showTvdbId = cursor.getInt(Query._ID);
        viewHolder.isFavorited = cursor.getInt(Query.FAVORITE) == 1;

        // set text properties immediately
        viewHolder.name.setText(cursor.getString(Query.TITLE));

        // favorite label
        setFavoriteState(viewHolder.favorited, viewHolder.isFavorited);

        // next episode info
        String fieldValue = cursor.getString(Query.NEXTTEXT);
        if (TextUtils.isEmpty(fieldValue)) {
            // display show status if there is no next episode
            viewHolder.episodeTime.setText(
                    ShowTools.getStatus(context, cursor.getInt(Query.STATUS)));
            viewHolder.episode.setText("");
        } else {
            viewHolder.episode.setText(fieldValue);
            fieldValue = cursor.getString(Query.NEXTAIRDATETEXT);
            viewHolder.episodeTime.setText(fieldValue);
        }

        // network, day and time
        viewHolder.timeAndNetwork.setText(buildNetworkAndTimeString(context,
                cursor.getInt(Query.RELEASE_TIME),
                cursor.getInt(Query.RELEASE_WEEKDAY),
                cursor.getString(Query.RELEASE_TIMEZONE),
                cursor.getString(Query.RELEASE_COUNTRY),
                cursor.getString(Query.NETWORK)));

        // set poster
        Utils.loadTvdbShowPoster(context, viewHolder.poster,
                cursor.getString(Query.POSTER));

        // context menu
        viewHolder.isHidden = DBUtils.restoreBooleanFromInt(cursor.getInt(Query.HIDDEN));
        viewHolder.episodeTvdbId = cursor.getInt(Query.NEXTEPISODE);
    }

    public interface Query {

        String[] PROJECTION = {
                BaseColumns._ID, // 0
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.RELEASE_TIME,
                SeriesGuideContract.Shows.RELEASE_WEEKDAY,
                SeriesGuideContract.Shows.RELEASE_TIMEZONE,
                SeriesGuideContract.Shows.RELEASE_COUNTRY, // 5
                SeriesGuideContract.Shows.NETWORK,
                SeriesGuideContract.Shows.POSTER,
                SeriesGuideContract.Shows.STATUS,
                SeriesGuideContract.Shows.NEXTEPISODE,
                SeriesGuideContract.Shows.NEXTTEXT, // 10
                SeriesGuideContract.Shows.NEXTAIRDATETEXT,
                SeriesGuideContract.Shows.FAVORITE,
                SeriesGuideContract.Shows.HIDDEN // 13
        };

        int _ID = 0;
        int TITLE = 1;
        int RELEASE_TIME = 2;
        int RELEASE_WEEKDAY = 3;
        int RELEASE_TIMEZONE = 4;
        int RELEASE_COUNTRY = 5;
        int NETWORK = 6;
        int POSTER = 7;
        int STATUS = 8;
        int NEXTEPISODE = 9;
        int NEXTTEXT = 10;
        int NEXTAIRDATETEXT = 11;
        int FAVORITE = 12;
        int HIDDEN = 13;
    }
}
