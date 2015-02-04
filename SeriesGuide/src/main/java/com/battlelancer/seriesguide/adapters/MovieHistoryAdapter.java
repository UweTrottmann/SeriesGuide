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

package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;

/**
 * Creates a list of movies from a list of {@link com.uwetrottmann.trakt.v2.entities.HistoryEntry}.
 */
public class MovieHistoryAdapter extends SectionedHistoryAdapter {

    public MovieHistoryAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_history, parent, false);

            holder = new ViewHolder(convertView);
            // no need for secondary text
            holder.description.setVisibility(View.GONE);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        HistoryEntry item = getItem(position);

        // movie title and poster
        holder.title.setText(item.movie == null ? null : item.movie.title);
        if (item.movie.images != null && item.movie.images.poster != null && !TextUtils.isEmpty(
                item.movie.images.poster.thumb)) {
            ServiceUtils.getPicasso(getContext())
                    .load(item.movie.images.poster.thumb)
                    .into(holder.poster);
        }

        // timestamp
        CharSequence timestamp = DateUtils.getRelativeTimeSpanString(
                item.watched_at.getMillis(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Caption_Dim);
        holder.timestamp.setText(timestamp);

        // action type indicator
        if ("watch".equals(item.action)) {
            // marked watched
            holder.type.setImageResource(getResIdDrawableWatched());
        } else {
            // check-in, scrobble
            holder.type.setImageResource(getResIdDrawableCheckin());
        }

        return convertView;
    }
}
