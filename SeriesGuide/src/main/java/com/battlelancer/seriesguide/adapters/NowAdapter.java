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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.model.HeaderData;
import com.battlelancer.seriesguide.util.Utils;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sectioned adapter displaying recently watched episodes, episodes released today and episodes
 * recently watched by trakt friends.
 */
public class NowAdapter extends ArrayAdapter<NowAdapter.NowItem>
        implements StickyGridHeadersBaseAdapter {

    private List<HeaderData> mHeaders;

    public enum NowType {
        RECENTLY_WATCHED,
        RELEASED_TODAY,
        FRIENDS
    }

    public static class NowItem {
        public String title;
        public String description;
        public String poster;
        public NowType type;

        public NowItem(String title, String description, String poster, NowType type) {
            this.title = title;
            this.description = description;
            this.poster = poster;
            this.type = type;
        }
    }

    public NowAdapter(Context context) {
        super(context, 0);
    }

    public class HeaderViewHolder {
        public TextView title;

        public HeaderViewHolder(View itemView) {
            title = (TextView) itemView.findViewById(R.id.textViewNowHeader);
        }
    }

    public class ItemViewHolder {
        public TextView title;
        public TextView description;
        public ImageView poster;

        public ItemViewHolder(View itemView) {
            title = (TextView) itemView.findViewById(R.id.textViewNowTitle);
            description = (TextView) itemView.findViewById(R.id.textViewNowDescription);
            poster = (ImageView) itemView.findViewById(R.id.imageViewNowPoster);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_now, parent, false);
            holder = new ItemViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        NowItem item = getItem(position);
        holder.title.setText(item.title);
        holder.description.setText(item.description);
        Utils.loadPosterThumbnail(getContext(), holder.poster, item.poster);

        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        // get header position for item position
        position = mHeaders.get(position).getRefPosition();

        NowItem item = getItem(position);
        if (item == null) {
            return null;
        }

        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_now_header, parent, false);
            holder = new HeaderViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        int titleResId;
        if (item.type == NowType.RECENTLY_WATCHED) {
            titleResId = R.string.recently_watched;
        } else if (item.type == NowType.RELEASED_TODAY) {
            titleResId = R.string.released_today;
        } else {
            titleResId = R.string.friends_recently;
        }
        holder.title.setText(titleResId);

        return convertView;
    }

    @Override
    public int getCountForHeader(int position) {
        if (mHeaders != null) {
            return mHeaders.get(position).getCount();
        }
        return 0;
    }

    @Override
    public int getNumHeaders() {
        if (mHeaders != null) {
            return mHeaders.size();
        }
        return 0;
    }

    @Override
    public void notifyDataSetChanged() {
        // re-create headers before letting notifyDataSetChanged reach the AdapterView
        mHeaders = generateHeaderList();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        // remove headers before letting notifyDataSetChanged reach the AdapterView
        mHeaders = null;
        super.notifyDataSetInvalidated();
    }

    public void setReleasedTodayData(List<NowItem> items) {
        clear();
        addAll(items);
    }

    private List<HeaderData> generateHeaderList() {
        if (getCount() == 0) {
            return null;
        }

        Map<Long, HeaderData> mapping = new HashMap<>();
        List<HeaderData> headers = new ArrayList<>();

        for (int i = 0; i < getCount(); i++) {
            long headerId = getHeaderId(i);
            HeaderData headerData = mapping.get(headerId);
            if (headerData == null) {
                headerData = new HeaderData(i);
                headers.add(headerData);
            }
            headerData.incrementCount();
            mapping.put(headerId, headerData);
        }

        return headers;
    }

    /**
     * Maps items to their section.
     */
    private long getHeaderId(int position) {
        NowItem item = getItem(position);
        if (item != null) {
            return item.type.ordinal();
        }
        return 0;
    }
}
