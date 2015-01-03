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
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * Base adapter using the shows_row.xml layout with a ViewHolder.
 */
public abstract class BaseShowsAdapter extends CursorAdapter {

    protected LayoutInflater mLayoutInflater;

    private final int LAYOUT = R.layout.item_show;

    public BaseShowsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(LAYOUT, parent, false);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.name = (TextView) v.findViewById(R.id.seriesname);
        viewHolder.timeAndNetwork = (TextView) v.findViewById(
                R.id.textViewShowsTimeAndNetwork);
        viewHolder.episode = (TextView) v.findViewById(
                R.id.TextViewShowListNextEpisode);
        viewHolder.episodeTime = (TextView) v.findViewById(R.id.episodetime);
        viewHolder.poster = (ImageView) v.findViewById(R.id.showposter);
        viewHolder.favorited = (ImageView) v.findViewById(R.id.favoritedLabel);
        viewHolder.contextMenu = (ImageView) v.findViewById(
                R.id.imageViewShowsContextMenu);

        v.setTag(viewHolder);

        return v;
    }

    /**
     * Builds a network + release time string for a show formatted like "Network / Tue 08:00 PM".
     */
    public static String buildNetworkAndTimeString(Context context, int time, int weekday,
            String timeZone, String country, String network) {
        // network
        StringBuilder networkAndTime = new StringBuilder();
        networkAndTime.append(network);

        // time
        if (time != -1) {
            Date release = TimeTools.getShowReleaseDateTime(context,
                    TimeTools.getShowReleaseTime(time), weekday, timeZone, country);
            String dayString = TimeTools.formatToLocalDayOrDaily(context, release, weekday);
            String timeString = TimeTools.formatToLocalTime(context, release);
            if (networkAndTime.length() > 0) {
                networkAndTime.append(" / ");
            }
            networkAndTime.append(dayString).append(" ").append(timeString);
        }

        return networkAndTime.toString();
    }

    public static class ViewHolder {

        public TextView name;

        public TextView timeAndNetwork;

        public TextView episode;

        public TextView episodeTime;

        public ImageView poster;

        public ImageView favorited;

        public ImageView contextMenu;
    }
}
