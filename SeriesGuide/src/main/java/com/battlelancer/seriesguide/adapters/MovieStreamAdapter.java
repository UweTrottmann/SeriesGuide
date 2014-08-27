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
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.enumerations.ActivityAction;

/**
 * Creates a list of movies from a list of {@link com.jakewharton.trakt.entities.ActivityItem},
 * displaying user name and avatar.
 */
public class MovieStreamAdapter extends SectionedStreamAdapter {

    public MovieStreamAdapter(Context context) {
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
            holder.timestamp = (TextView) convertView.findViewById(
                    R.id.textViewFriendTimestamp);
            holder.movie = (TextView) convertView.findViewById(R.id.textViewFriendShow);
            holder.poster = (ImageView) convertView.findViewById(R.id.imageViewFriendPoster);
            holder.username = (TextView) convertView.findViewById(R.id.textViewFriendUsername);
            holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewFriendAvatar);
            holder.type = (ImageView) convertView.findViewById(R.id.imageViewFriendActionType);

            // no need for secondary text
            convertView.findViewById(R.id.textViewFriendEpisode).setVisibility(View.GONE);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        ActivityItem activity = getItem(position);

        // movie poster
        if (activity.movie.images != null && !TextUtils.isEmpty(activity.movie.images.poster)) {
            String posterPath = activity.movie.images.poster.replace(
                    TraktSettings.POSTER_SIZE_SPEC_DEFAULT, TraktSettings.POSTER_SIZE_SPEC_138);
            ServiceUtils.getPicasso(getContext()).load(posterPath).into(holder.poster);
        }

        holder.username.setText(activity.user.username);
        ServiceUtils.getPicasso(getContext()).load(activity.user.avatar).into(holder.avatar);

        holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Caption_Dim);

        CharSequence timestamp;
        // friend is watching something right now?
        if (activity.action == ActivityAction.Watching) {
            timestamp = getContext().getString(R.string.now);
            holder.timestamp.setTextAppearance(getContext(),
                    R.style.TextAppearance_Caption_Red);
        } else {
            timestamp = DateUtils.getRelativeTimeSpanString(
                    activity.timestamp.getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        }

        // activity type indicator
        if (activity.action == ActivityAction.Seen) {
            holder.type.setImageResource(getResIdDrawableWatched());
        } else {
            holder.type.setImageResource(getResIdDrawableCheckin());
        }

        holder.movie.setText(activity.movie.title);
        holder.timestamp.setText(timestamp);

        return convertView;
    }

    static class ViewHolder {

        TextView timestamp;

        TextView movie;

        ImageView poster;

        TextView username;

        ImageView avatar;

        ImageView type;
    }
}
