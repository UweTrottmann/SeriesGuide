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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;

/**
 * Sectioned adapter displaying recently watched episodes, episodes released today and episodes
 * recently watched by trakt friends.
 */
public class NowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public HeaderViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.textViewNowHeader);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView description;

        public ItemViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.textViewNowTitle);
            description = (TextView) itemView.findViewById(R.id.textViewNowDescription);
        }
    }

    @Override
    public int getItemViewType(int position) {
        int recents = getRecentlyWatchedCount();
        int today = getReleasedTodayCount();

        // section headers (order is fixed!)
        // recents, today or friends (1 total)
        if (position == 0) {
            // first item is always a header
            return TYPE_HEADER;
        }
        // recents or friends first (2 total)
        if (recents != 0 && position == recents + 1) {
            return TYPE_HEADER;
        }
        if (recents == 0 && today != 0 && position == today + 1) {
            return TYPE_HEADER;
        }
        // recents, today and friends (3 total)
        if (recents != 0 && today != 0 && position == recents + 1 + today + 1) {
            return TYPE_HEADER;
        }

        // show/episode item otherwise
        return TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_now_header, parent, false);
            return new HeaderViewHolder(v);
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_now, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_HEADER: {
                HeaderViewHolder viewHolder = (HeaderViewHolder) holder;
                viewHolder.title.setText("Section header");
                break;
            }
            case TYPE_ITEM: {
                ItemViewHolder viewHolder = (ItemViewHolder) holder;
                viewHolder.title.setText("Item Title");
                viewHolder.description.setText("Item Description");
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        int recents = getRecentlyWatchedCount();
        int today = getReleasedTodayCount();
        int friends = getFriendsRecentlyWatchedCount();

        // +1 for each section header
        if (recents != 0) {
            recents += 1;
        }
        if (today != 0) {
            today += 1;
        }
        if (friends != 0) {
            friends += 1;
        }

        return recents + today + friends;
    }

    private int getRecentlyWatchedCount() {
        return 1;
    }

    private int getReleasedTodayCount() {
        return 2;
    }

    private int getFriendsRecentlyWatchedCount() {
        return 0;
    }
}
