
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
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt.v2.entities.Comment;
import java.util.List;

/**
 * Custom ArrayAdapter which binds {@link Comment} items to views using the ViewHolder pattern.
 */
public class TraktCommentsAdapter extends ArrayAdapter<Comment> {

    private final LayoutInflater mInflater;

    public TraktCommentsAdapter(Context context) {
        super(context, R.layout.item_comment);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<Comment> data) {
        clear();
        if (data != null) {
            for (Comment item : data) {
                add(item);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid
        // unnecessary calls to findViewById() on each row.
        TraktCommentsAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_comment, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.shout = (TextView) convertView.findViewById(R.id.shout);
            holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
            holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);

            convertView.setTag(holder);
        } else {
            holder = (TraktCommentsAdapter.ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        final Comment comment = getItem(position);

        String name = null;
        String avatarPath = null;
        if (comment.user != null) {
            name = comment.user.username;
            if (comment.user.images != null && comment.user.images.avatar != null) {
                avatarPath = comment.user.images.avatar.full;
            }
        }
        holder.name.setText(name);
        ServiceUtils.loadWithPicasso(getContext(), avatarPath)
                .into(holder.avatar);

        if (comment.spoiler) {
            holder.shout.setText(R.string.isspoiler);
            holder.shout.setTextAppearance(getContext(), R.style.TextAppearance_Body_Highlight_Red);
        } else {
            holder.shout.setText(comment.comment);
            holder.shout.setTextAppearance(getContext(), R.style.TextAppearance_Body);
        }

        String timestamp = (String) DateUtils.getRelativeTimeSpanString(
                comment.created_at.getMillis(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        holder.timestamp.setText(timestamp);

        return convertView;
    }

    static class ViewHolder {
        TextView name;

        TextView shout;

        TextView timestamp;

        ImageView avatar;
    }
}
