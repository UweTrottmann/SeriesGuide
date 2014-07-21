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
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.enumerations.ActivityAction;

/**
 * Creates a list of episodes from a list of {@link com.jakewharton.trakt.entities.ActivityItem},
 * displaying user name and avatar.
 */
public class EpisodeStreamAdapter extends SectionedStreamAdapter {

    public EpisodeStreamAdapter(Context context) {
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
        ActivityItem activity = getItem(position);

        // show
        holder.show.setText(activity.show.title);
        if (activity.show.images != null && !TextUtils.isEmpty(activity.show.images.poster)) {
            String posterPath = activity.show.images.poster.replace(
                    TraktSettings.POSTER_SIZE_SPEC_DEFAULT, TraktSettings.POSTER_SIZE_SPEC_138);
            ServiceUtils.getPicasso(getContext()).load(posterPath).into(holder.poster);
        }

        // user
        holder.name.setText(activity.user.username);
        ServiceUtils.getPicasso(getContext()).load(activity.user.avatar).into(holder.avatar);

        // timestamp
        CharSequence timestamp;
        if (activity.action == ActivityAction.Watching) {
            timestamp = getContext().getString(R.string.now);
            holder.timestamp.setTextAppearance(getContext(),
                    R.style.TextAppearance_Small_Highlight_Red);
        } else {
            timestamp = DateUtils.getRelativeTimeSpanString(
                    activity.timestamp.getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Small_Dim);
        }
        holder.timestamp.setText(timestamp);

        // episode(s) and activity type indicator
        if (activity.action == ActivityAction.Seen) {
            // can be multiple episodes
            TvShowEpisode episode = activity.episodes.get(0);
            holder.episode.setText(Utils.getNextEpisodeString(getContext(), episode.season,
                    episode.number, episode.title));
            if (activity.episodes.size() > 1) {
                holder.more.setText(
                        getContext().getString(R.string.more, activity.episodes.size()));
            }
            holder.type.setImageResource(getResIdDrawableWatched());
        } else {
            // single episode (check-in, scrobble)
            holder.episode.setText(Utils.getNextEpisodeString(getContext(), activity.episode.season,
                    activity.episode.number, activity.episode.title));
            holder.more.setText(null);
            holder.type.setImageResource(getResIdDrawableCheckin());
        }

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
