
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

import com.battlelancer.seriesguide.ui.SeasonsFragment.SeasonsQuery;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SeasonsAdapter extends CursorAdapter {

    private static final int LAYOUT = R.layout.season_row;

    private LayoutInflater mLayoutInflater;

    private OnClickListener mOnClickListener;

    public SeasonsAdapter(Context context, Cursor c, int flags, OnClickListener listener) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mOnClickListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = newView(mContext, mCursor, parent);

            viewHolder = new ViewHolder();
            viewHolder.seasonTitle = (TextView) convertView.findViewById(R.id.textViewSeasonTitle);
            viewHolder.seasonProgress = (TextView) convertView
                    .findViewById(R.id.textViewSeasonProgress);
            viewHolder.seasonProgressBar = (ProgressBar) convertView
                    .findViewById(R.id.progressBarSeason);
            viewHolder.seasonWatchCount = (TextView) convertView
                    .findViewById(R.id.textViewSeasonWatchCount);
            viewHolder.seasonSkipped = convertView.findViewById(R.id.imageViewSeasonSkipped);
            viewHolder.contextMenu = (ImageView) convertView
                    .findViewById(R.id.imageViewContextMenu);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // title
        viewHolder.seasonTitle.setText(
                SeasonTools.getSeasonString(mContext, mCursor.getInt(SeasonsQuery.COMBINED)));

        // progress
        final int count = mCursor.getInt(SeasonsQuery.WATCHCOUNT);
        final int unairedCount = mCursor.getInt(SeasonsQuery.UNAIREDCOUNT);
        final int noairdateCount = mCursor.getInt(SeasonsQuery.NOAIRDATECOUNT);
        final int max = mCursor.getInt(SeasonsQuery.TOTALCOUNT);
        final int progress = max - count - unairedCount - noairdateCount;
        viewHolder.seasonProgressBar.setMax(max);
        viewHolder.seasonProgressBar.setProgress(progress);
        viewHolder.seasonProgress.setText(progress + "/" + max);

        // skipped label
        viewHolder.seasonSkipped
                .setVisibility(SeasonTools.hasSkippedTag(mCursor.getString(SeasonsQuery.TAGS))
                        ? View.VISIBLE : View.GONE);

        // episodes text
        String episodeCount = "";
        // add strings for unwatched episodes
        if (count == 0) {
            // make sure there are no unchecked episodes that happen
            // to have no airdate
            if (noairdateCount == 0) {
                episodeCount += mContext.getString(R.string.season_allwatched);
            } else {
                episodeCount += noairdateCount + " ";
                if (noairdateCount == 1) {
                    episodeCount += mContext.getString(R.string.oneotherepisode);
                } else {
                    episodeCount += mContext.getString(R.string.otherepisodes);
                }
            }

            viewHolder.seasonWatchCount.setTextAppearance(mContext,
                    R.style.TextAppearance_XSmall_Dim);
        } else if (count == 1) {
            episodeCount += count + " " + mContext.getString(R.string.season_onenotwatched);
            viewHolder.seasonWatchCount.setTextAppearance(mContext, R.style.TextAppearance_XSmall);
        } else {
            episodeCount += count + " " + mContext.getString(R.string.season_watchcount);
            viewHolder.seasonWatchCount.setTextAppearance(mContext, R.style.TextAppearance_XSmall);
        }

        // add strings for unaired episodes
        if (unairedCount > 0) {
            episodeCount += " (+" + unairedCount + " "
                    + mContext.getString(R.string.season_unaired) + ")";
        }
        viewHolder.seasonWatchCount.setText(episodeCount);

        // context menu
        viewHolder.contextMenu.setOnClickListener(mOnClickListener);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mLayoutInflater.inflate(LAYOUT, parent, false);
    }

    static class ViewHolder {

        TextView seasonTitle;

        TextView seasonProgress;

        ProgressBar seasonProgressBar;

        TextView seasonWatchCount;

        View seasonSkipped;

        ImageView contextMenu;
    }

}
