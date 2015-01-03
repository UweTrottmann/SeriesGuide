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
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;

/**
 * Creates a list of episodes from a list of trakt {@link com.uwetrottmann.trakt.v2.entities.HistoryEntry}.
 */
public class EpisodeHistoryAdapter extends SectionedHistoryAdapter {

    public EpisodeHistoryAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_friend, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.textViewFriendUsername);
            holder.show = (TextView) convertView.findViewById(R.id.textViewFriendShow);
            holder.episode = (TextView) convertView.findViewById(R.id.textViewFriendEpisode);
            holder.more = (TextView) convertView.findViewById(R.id.textViewFriendMore);
            holder.timestamp = (TextView) convertView.findViewById(
                    R.id.textViewFriendTimestamp);
            holder.poster = (ImageView) convertView.findViewById(R.id.imageViewFriendPoster);
            holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewFriendAvatar);
            holder.type = (ImageView) convertView.findViewById(R.id.imageViewFriendActionType);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        HistoryEntry item = getItem(position);

        // show title and poster
        holder.show.setText(item.show == null ? null : item.show.title);
        if (item.show.images != null && item.show.images.poster != null && !TextUtils.isEmpty(
                item.show.images.poster.thumb)) {
            ServiceUtils.getPicasso(getContext())
                    .load(item.show.images.poster.thumb)
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

        // episode
        holder.episode.setText(item.episode == null ? null
                : Utils.getNextEpisodeString(getContext(), item.episode.season, item.episode.number,
                        item.episode.title));
        holder.more.setText(null);

        return convertView;
    }

    static class ViewHolder {

        TextView name;

        TextView show;

        TextView episode;

        TextView more;

        TextView timestamp;

        ImageView poster;

        ImageView avatar;

        ImageView type;
    }
}
