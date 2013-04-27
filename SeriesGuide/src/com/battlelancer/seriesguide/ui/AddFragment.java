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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

/**
 * Super class for fragments displaying a list of shows and allowing to add them
 * to the database.
 */
public class AddFragment extends SherlockFragment {

    protected List<SearchResult> mSearchResults;

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
        View emptyView = getView().findViewById(android.R.id.empty);
        if (emptyView != null) {
            mGrid.setEmptyView(emptyView);
        }

        // restore an existing adapter
        if (mAdapter != null) {
            mGrid.setAdapter(mAdapter);
        }
    }

    @TargetApi(11)
    protected void setSearchResults(List<SearchResult> searchResults) {
        mSearchResults = searchResults;
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

    protected OnClickListener mDetailsButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // display more details in a dialog
            int position = mGrid.getPositionForView(v);
            SearchResult show = mAdapter.getItem(position);
            AddDialogFragment.showAddDialog(show, getFragmentManager());
        }
    };

    protected static class AddAdapter extends ArrayAdapter<SearchResult> {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        private ImageDownloader mImageDownloader;

        private OnClickListener mDetailsButtonListener;

        public AddAdapter(Context context, int layout, List<SearchResult> objects,
                OnClickListener detailsButtonListener) {
            super(context, layout, objects);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
            mImageDownloader = ImageDownloader.getInstance(context);
            mDetailsButtonListener = detailsButtonListener;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.addbutton = convertView.findViewById(R.id.addbutton);
                viewHolder.details = convertView.findViewById(R.id.details);
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.description = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);

                // add button listeners
                viewHolder.details.setOnClickListener(mDetailsButtonListener);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final SearchResult item = getItem(position);

            // hide add button if already added that show
            viewHolder.addbutton.setVisibility(item.isAdded ? View.INVISIBLE : View.VISIBLE);
            viewHolder.addbutton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskManager.getInstance(getContext()).performAddTask(item);

                    item.isAdded = true;
                    v.setVisibility(View.INVISIBLE);
                }
            });

            // set text properties immediately
            viewHolder.title.setText(item.title);
            viewHolder.description.setText(item.overview);
            if (item.poster != null) {
                viewHolder.poster.setVisibility(View.VISIBLE);
                mImageDownloader.download(item.poster, viewHolder.poster, false);
            } else {
                viewHolder.poster.setVisibility(View.GONE);
            }

            return convertView;
        }

        static class ViewHolder {

            public TextView title;

            public TextView description;

            public ImageView poster;

            public View addbutton;

            public View details;
        }
    }
}
