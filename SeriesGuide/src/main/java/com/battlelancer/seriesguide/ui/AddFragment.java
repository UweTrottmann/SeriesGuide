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

package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.List;

/**
 * Super class for fragments displaying a list of shows and allowing to add them to the database.
 */
public abstract class AddFragment extends Fragment {

    @InjectView(R.id.containerAddContent) View contentContainer;
    @InjectView(R.id.progressBarAdd) View progressBar;
    @InjectView(android.R.id.list) GridView resultsGridView;
    @InjectView(R.id.emptyViewAdd) TextView emptyView;

    protected List<SearchResult> searchResults;
    protected AddAdapter adapter;

    /**
     * Implementers should inflate their own layout and inject views with {@link
     * butterknife.ButterKnife#inject(Object, android.view.View)}.
     */
    @Override
    public abstract View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState);

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // basic setup of grid view
        resultsGridView.setEmptyView(emptyView);
        resultsGridView.setOnItemClickListener(mItemClickListener);

        // restore an existing adapter
        if (adapter != null) {
            resultsGridView.setAdapter(adapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    /**
     * Changes the empty message.
     */
    public void setEmptyMessage(int stringResourceId) {
        emptyView.setText(stringResourceId);
    }

    public void setSearchResults(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
        adapter.clear();
        if (AndroidUtils.isHoneycombOrHigher()) {
            adapter.addAll(searchResults);
        } else {
            for (SearchResult searchResult : searchResults) {
                adapter.add(searchResult);
            }
        }
        resultsGridView.setAdapter(adapter);
    }

    /**
     * Hides the content container and shows a progress bar.
     */
    public void setProgressVisible(boolean visible, boolean animate) {
        if (animate) {
            Animation out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            Animation in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            contentContainer.startAnimation(visible ? out : in);
            progressBar.startAnimation(visible ? in : out);
        }
        contentContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected AdapterView.OnItemClickListener mItemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // display more details in a dialog
            SearchResult show = adapter.getItem(position);
            AddShowDialogFragment.showAddDialog(show, getFragmentManager());
        }
    };

    /**
     * Called if the user adds a new show through the dialog.
     */
    public void onEvent(AddShowDialogFragment.AddShowEvent event) {
        adapter.notifyDataSetChanged();
    }

    protected static class AddAdapter extends ArrayAdapter<SearchResult> {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        public AddAdapter(Context context, int layout, List<SearchResult> objects) {
            super(context, layout, objects);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.addbutton = convertView.findViewById(R.id.addbutton);
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.description = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);

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
                ServiceUtils.getPicasso(getContext()).load(item.poster).into(viewHolder.poster);
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
        }
    }
}
