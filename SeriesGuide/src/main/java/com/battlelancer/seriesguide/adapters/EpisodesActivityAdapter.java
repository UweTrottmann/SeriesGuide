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
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.enumerations.ActivityAction;

/**
 * Creates a list of episodes from a list of {@link com.jakewharton.trakt.entities.ActivityItem},
 * displaying user name and avatar.
 */
public class EpisodesActivityAdapter extends SectionedStreamAdapter {

    public EpisodesActivityAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.friend, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.textViewFriendUsername);
            holder.show = (TextView) convertView.findViewById(R.id.textViewFriendShow);
            holder.episode = (TextView) convertView.findViewById(R.id.textViewFriendEpisode);
            holder.timestamp = (TextView) convertView.findViewById(
                    R.id.textViewFriendTimestamp);
            holder.poster = (ImageView) convertView.findViewById(R.id.imageViewFriendPoster);
            holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewFriendAvatar);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        ActivityItem activity = getItem(position);

        // show poster
        if (activity.show.images != null && !TextUtils.isEmpty(activity.show.images.poster)) {
            String posterPath = activity.show.images.poster.replace(
                    TraktSettings.POSTER_SIZE_SPEC_DEFAULT, TraktSettings.POSTER_SIZE_SPEC_138);
            ServiceUtils.getPicasso(getContext()).load(posterPath).into(holder.poster);
        }

        holder.name.setText(activity.user.username);
        ServiceUtils.getPicasso(getContext()).load(activity.user.avatar).into(holder.avatar);

        holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Small_Dim);

        CharSequence timestamp;
        // friend is watching something right now?
        if (activity.action == ActivityAction.Watching) {
            timestamp = getContext().getString(R.string.now);
            holder.timestamp.setTextAppearance(getContext(),
                    R.style.TextAppearance_Small_Highlight_Red);
        } else {
            timestamp = DateUtils.getRelativeTimeSpanString(
                    activity.timestamp.getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        }

        holder.show.setText(activity.show.title);
        holder.episode.setText(Utils.getNextEpisodeString(getContext(), activity.episode.season,
                activity.episode.number, activity.episode.title));
        holder.timestamp.setText(timestamp);

        return convertView;
    }

    static class ViewHolder {

        TextView name;

        TextView show;

        TextView episode;

        TextView timestamp;

        ImageView poster;

        ImageView avatar;
    }
}
