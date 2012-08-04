/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Super class for fragments displaying a list of shows and allowing to add them
 * to the database.
 */
public class AddFragment extends SherlockFragment {

    protected AddAdapter mAdapter;

    protected GridView mGrid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // so we don't have to do a network op each config change
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // basic setup of grid view
        mGrid = (GridView) getView().findViewById(android.R.id.list);
        mGrid.setFastScrollEnabled(true);
        mGrid.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                SearchResult result = mAdapter.getItem(position);
                AddDialogFragment.showAddDialog(result, getFragmentManager());
            }
        });
        View emptyView = getView().findViewById(android.R.id.empty);
        if (emptyView != null) {
            mGrid.setEmptyView(emptyView);
        }
    }

    @TargetApi(11)
    protected void setSearchResults(List<SearchResult> searchResults) {
        mAdapter.clear();
        if (AndroidUtils.isHoneycombOrHigher()) {
            mAdapter.addAll(searchResults);
        } else {
            for (SearchResult searchResult : searchResults) {
                mAdapter.add(searchResult);
            }
        }
        mGrid.setAdapter(mAdapter);
    }

    protected static class AddAdapter extends ArrayAdapter<SearchResult> {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        private ImageDownloader mImageDownloader;

        public AddAdapter(Context context, int layout, List<SearchResult> objects) {
            super(context, layout, objects);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
            mImageDownloader = ImageDownloader.getInstance(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.description = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            SearchResult item = getItem(position);
            viewHolder.title.setText(item.title);
            viewHolder.description.setText(item.overview);
            if (item.poster != null) {
                mImageDownloader.download(item.poster, viewHolder.poster, false);
            }

            return convertView;
        }

        static class ViewHolder {

            public TextView title;

            public TextView description;

            public ImageView poster;
        }
    }
}
