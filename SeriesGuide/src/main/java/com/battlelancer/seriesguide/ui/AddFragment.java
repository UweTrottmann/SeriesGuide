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
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import java.util.List;

/**
 * Super class for fragments displaying a list of shows and allowing to add them to the database.
 */
public abstract class AddFragment extends Fragment {

    public static class AddShowEvent {
    }

    @Bind(R.id.containerAddContent) View contentContainer;
    @Bind(R.id.progressBarAdd) View progressBar;
    @Bind(android.R.id.list) GridView resultsGridView;
    @Bind(R.id.emptyViewAdd) TextView emptyView;

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

        ButterKnife.unbind(this);
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
    public void onEvent(AddShowEvent event) {
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
            ViewHolder holder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final SearchResult item = getItem(position);

            // display added indicator instead of add button if already added that show
            holder.addbutton.setVisibility(item.isAdded ? View.GONE : View.VISIBLE);
            holder.addedIndicator.setVisibility(item.isAdded ? View.VISIBLE : View.GONE);
            holder.addbutton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.isAdded = true;
                    EventBus.getDefault().post(new AddShowEvent());

                    TaskManager.getInstance(getContext()).performAddTask(item);
                }
            });

            // set text properties immediately
            holder.title.setText(item.title);
            holder.description.setText(item.overview);
            if (item.poster != null) {
                holder.poster.setVisibility(View.VISIBLE);
                ServiceUtils.loadWithPicasso(getContext(), item.poster).into(holder.poster);
            } else {
                holder.poster.setVisibility(View.GONE);
            }

            return convertView;
        }

        static class ViewHolder {

            public TextView title;
            public TextView description;
            public ImageView poster;
            public View addbutton;
            public View addedIndicator;

            public ViewHolder(View view) {
                title = (TextView) view.findViewById(R.id.textViewAddTitle);
                description = (TextView) view.findViewById(R.id.textViewAddDescription);
                poster = (ImageView) view.findViewById(R.id.imageViewAddPoster);
                addbutton = view.findViewById(R.id.viewAddButton);
                addedIndicator = view.findViewById(R.id.imageViewAddedIndicator);
            }
        }
    }
}
