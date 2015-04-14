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
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import java.util.Date;

/**
 * Base adapter for the show item layout.
 */
public abstract class BaseShowsAdapter extends CursorAdapter {

    public interface OnContextMenuClickListener {
        public void onClick(View view, ShowViewHolder viewHolder);
    }

    private OnContextMenuClickListener onContextMenuClickListener;
    private final int resIdStar;
    private final int resIdStarZero;

    public BaseShowsAdapter(Context context, OnContextMenuClickListener listener) {
        super(context, null, 0);
        this.onContextMenuClickListener = listener;

        resIdStar = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableStar);
        resIdStarZero = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableStar0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_show, parent, false);

        ShowViewHolder viewHolder = new ShowViewHolder(v, onContextMenuClickListener);
        v.setTag(viewHolder);

        return v;
    }

    public void setFavoriteState(ImageView view, boolean isFavorite) {
        view.setImageResource(isFavorite ? resIdStar : resIdStarZero);
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

    public static class ShowViewHolder {

        public TextView name;
        public TextView timeAndNetwork;
        public TextView episode;
        public TextView episodeTime;
        public ImageView poster;
        public ImageView favorited;
        public ImageView contextMenu;

        public int showTvdbId;
        public int episodeTvdbId;
        public boolean isFavorited;
        public boolean isHidden;

        public ShowViewHolder(View v, final OnContextMenuClickListener listener) {
            name = (TextView) v.findViewById(R.id.seriesname);
            timeAndNetwork = (TextView) v.findViewById(R.id.textViewShowsTimeAndNetwork);
            episode = (TextView) v.findViewById(R.id.TextViewShowListNextEpisode);
            episodeTime = (TextView) v.findViewById(R.id.episodetime);
            poster = (ImageView) v.findViewById(R.id.showposter);
            favorited = (ImageView) v.findViewById(R.id.favoritedLabel);
            contextMenu = (ImageView) v.findViewById(R.id.imageViewShowsContextMenu);

            // favorite star
            favorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowTools.get(v.getContext()).storeIsFavorite(showTvdbId, !isFavorited);
                }
            });
            // context menu
            contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onClick(v, ShowViewHolder.this);
                    }
                }
            });
        }
    }
}
