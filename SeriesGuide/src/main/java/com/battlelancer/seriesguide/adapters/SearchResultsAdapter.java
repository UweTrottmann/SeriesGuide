
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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.EpisodeSearchFragment.SearchQuery;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;

/**
 * {@link CursorAdapter} displaying episode search results inside the {@link
 * com.battlelancer.seriesguide.ui.EpisodeSearchFragment}.
 */
public class SearchResultsAdapter extends CursorAdapter {

    private static final int LAYOUT = R.layout.item_search_result;

    private LayoutInflater mLayoutInflater;

    public SearchResultsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            viewHolder.showTitle = (TextView) convertView.findViewById(R.id.textViewShowTitle);
            viewHolder.episodeTitle = (TextView) convertView
                    .findViewById(R.id.textViewEpisodeTitle);
            viewHolder.searchSnippet = (TextView) convertView
                    .findViewById(R.id.textViewSearchSnippet);
            viewHolder.watchedStatus = (ImageView) convertView
                    .findViewById(R.id.imageViewWatchedStatus);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.showTitle.setText(mCursor.getString(SearchQuery.SHOW_TITLE));
        viewHolder.watchedStatus.setImageResource(
                EpisodeTools.isWatched(mCursor.getInt(SearchQuery.WATCHED))
                        ? Utils.resolveAttributeToResourceId(mContext.getTheme(),
                                R.attr.drawableWatched)
                        : Utils.resolveAttributeToResourceId(mContext.getTheme(),
                                R.attr.drawableWatch));

        // ensure matched term is bold
        viewHolder.searchSnippet.setText(Html.fromHtml(mCursor.getString(SearchQuery.OVERVIEW)));

        // episode
        int number = mCursor.getInt(SearchQuery.NUMBER);
        int season = mCursor.getInt(SearchQuery.SEASON);
        String title = mCursor.getString(SearchQuery.TITLE);
        viewHolder.episodeTitle
                .setText(Utils.getNextEpisodeString(mContext, season, number, title));

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

        TextView showTitle;

        TextView episodeTitle;

        TextView searchSnippet;

        ImageView watchedStatus;
    }
}
