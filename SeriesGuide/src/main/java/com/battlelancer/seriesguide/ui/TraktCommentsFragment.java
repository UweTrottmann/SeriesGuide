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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TraktCommentsAdapter;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.loaders.TraktCommentsLoader;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.EmptyViewSwipeRefreshLayout;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktLink;
import com.uwetrottmann.trakt.v2.entities.Comment;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * A custom {@link ListFragment} to display show or episode shouts and for posting own shouts.
 */
public class TraktCommentsFragment extends Fragment {

    public interface InitBundle {
        String MOVIE_TMDB_ID = "movie";
        String SHOW_TVDB_ID = "show";
        String EPISODE_TVDB_ID = "episode";
    }

    @Bind(R.id.listViewShouts) ListView mList;
    @Bind(R.id.textViewShoutsEmpty) TextView mEmptyView;
    @Bind(R.id.swipeRefreshLayoutShouts) EmptyViewSwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.buttonShouts) Button mButtonShout;
    @Bind(R.id.editTextShouts) EditText mEditTextShout;
    @Bind(R.id.checkBoxShouts) CheckBox mCheckBoxIsSpoiler;

    private TraktCommentsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_comments, container, false);
        ButterKnife.bind(this, v);

        mSwipeRefreshLayout.setSwipeableChildren(R.id.scrollViewComments, R.id.listViewShouts);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshCommentsWithNetworkCheck();
            }
        });
        mSwipeRefreshLayout.setProgressViewOffset(false, getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));
        int accentColorResId = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.colorAccent);
        mSwipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.teal_500);

        mList.setOnItemClickListener(mOnClickListener);
        mList.setEmptyView(mEmptyView);

        mButtonShout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                comment();
            }
        });

        // disable comment button by default, enable if comment entered
        mButtonShout.setEnabled(false);
        mEditTextShout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mButtonShout.setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // set initial view states
        showProgressBar(true);

        return v;
    }

    private void comment() {
        // prevent empty comments
        String comment = mEditTextShout.getText().toString();
        if (TextUtils.isEmpty(comment)) {
            return;
        }

        // disable the comment button
        mButtonShout.setEnabled(false);

        Bundle args = getArguments();
        boolean isSpoiler = mCheckBoxIsSpoiler.isChecked();

        // as determined by "science", episode comments are most likely, so check for them first
        // comment for an episode?
        int episodeTvdbId = args.getInt(InitBundle.EPISODE_TVDB_ID);
        if (episodeTvdbId != 0) {
            AndroidUtils.executeOnPool(
                    new TraktTask(getActivity()).commentEpisode(episodeTvdbId, comment, isSpoiler)
            );
            return;
        }

        // comment for a movie?
        int movieTmdbId = args.getInt(InitBundle.MOVIE_TMDB_ID);
        if (movieTmdbId != 0) {
            AndroidUtils.executeOnPool(
                    new TraktTask(getActivity()).commentMovie(movieTmdbId, comment, isSpoiler)
            );
            return;
        }

        // comment for a show?
        int showTvdbId = args.getInt(InitBundle.SHOW_TVDB_ID);
        if (showTvdbId != 0) {
            AndroidUtils.executeOnPool(
                    new TraktTask(getActivity()).commentShow(showTvdbId, comment, isSpoiler)
            );
        }

        // if all ids were 0, do nothing
        Timber.e("comment: did nothing, all possible ids were 0");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        mAdapter = new TraktCommentsAdapter(getActivity());
        mList.setAdapter(mAdapter);

        // load data
        getLoaderManager().initLoader(TraktCommentsActivity.LOADER_ID_COMMENTS, getArguments(),
                mCommentsCallbacks);

        // enable menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    /**
     * Detach from list view.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.comments_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_comments_refresh) {
            refreshCommentsWithNetworkCheck();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final AdapterView.OnItemClickListener mOnClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    public void onListItemClick(ListView l, View v, int position, long id) {
        final Comment comment = (Comment) l.getItemAtPosition(position);
        if (comment == null) {
            return;
        }

        if (comment.spoiler) {
            // if comment is a spoiler it is hidden, first click should reveal it
            comment.spoiler = false;
            TextView shoutText = (TextView) v.findViewById(R.id.shout);
            if (shoutText != null) {
                shoutText.setText(comment.comment);
            }
        } else {
            // open shout or review page
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(TraktLink.comment(comment.id)));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            Utils.tryStartActivity(getActivity(), intent, true);
        }
    }

    private LoaderCallbacks<TraktCommentsLoader.Result> mCommentsCallbacks
            = new LoaderCallbacks<TraktCommentsLoader.Result>() {
        @Override
        public Loader<TraktCommentsLoader.Result> onCreateLoader(int id, Bundle args) {
            showProgressBar(true);
            return new TraktCommentsLoader(getActivity(), args);
        }

        @Override
        public void onLoadFinished(Loader<TraktCommentsLoader.Result> loader,
                TraktCommentsLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            mAdapter.setData(data.results);
            setEmptyMessage(data.emptyTextResId);
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(Loader<TraktCommentsLoader.Result> loader) {
            // keep existing data
        }
    };

    private void refreshCommentsWithNetworkCheck() {
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            // keep existing data, but update empty view anyhow
            showProgressBar(false);
            setEmptyMessage(R.string.offline);
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_SHORT).show();
            return;
        }

        refreshComments();
    }

    private void refreshComments() {
        getLoaderManager().restartLoader(TraktCommentsActivity.LOADER_ID_COMMENTS, getArguments(),
                mCommentsCallbacks);
    }

    /**
     * Changes the empty message.
     */
    private void setEmptyMessage(int stringResourceId) {
        mEmptyView.setText(stringResourceId);
    }

    /**
     * Show or hide the progress bar of the {@link android.support.v4.widget.SwipeRefreshLayout}
     * wrapping the comments list.
     */
    protected void showProgressBar(boolean isShowing) {
        mSwipeRefreshLayout.setRefreshing(isShowing);
    }

    public void onEventMainThread(TraktTask.TraktActionCompleteEvent event) {
        if (event.mTraktAction != TraktAction.COMMENT || getView() == null) {
            return;
        }

        // reenable the shout button
        mButtonShout.setEnabled(true);

        if (event.mWasSuccessful) {
            // clear the text field and show recent shout
            mEditTextShout.setText("");
            refreshCommentsWithNetworkCheck();
        }
    }
}
