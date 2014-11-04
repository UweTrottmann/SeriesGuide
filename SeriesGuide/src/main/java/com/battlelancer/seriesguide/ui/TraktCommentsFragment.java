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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TraktCommentsAdapter;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.loaders.TraktCommentsLoader;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.entities.Comment;
import de.greenrobot.event.EventBus;
import java.util.List;

/**
 * A custom {@link ListFragment} to display show or episode shouts and for posting own shouts.
 */
public class TraktCommentsFragment extends Fragment implements
        LoaderCallbacks<List<Comment>>, SwipeRefreshLayout.OnRefreshListener {

    /**
     * Build a {@link TraktCommentsFragment} for shouts of an episode.
     */
    public static TraktCommentsFragment newInstanceEpisode(int showTvdbId, int seasonNumber,
            int episodeNumber) {
        TraktCommentsFragment f = new TraktCommentsFragment();
        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDB_ID, showTvdbId);
        args.putInt(InitBundle.SEASON_NUMBER, seasonNumber);
        args.putInt(InitBundle.EPISODE_NUMBER, episodeNumber);
        f.setArguments(args);
        return f;
    }

    /**
     * Build a {@link TraktCommentsFragment} for shouts of a show.
     */
    public static TraktCommentsFragment newInstanceShow(int tvdbId) {
        TraktCommentsFragment f = new TraktCommentsFragment();
        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDB_ID, tvdbId);
        f.setArguments(args);
        return f;
    }

    /**
     * Build a {@link TraktCommentsFragment} for shouts of a movie.
     */
    public static TraktCommentsFragment newInstanceMovie(int tmdbId) {
        TraktCommentsFragment f = new TraktCommentsFragment();
        Bundle args = new Bundle();
        args.putInt(InitBundle.MOVIE_TMDB_ID, tmdbId);
        f.setArguments(args);
        return f;
    }

    public interface InitBundle {
        String MOVIE_TMDB_ID = "tmdbid";
        String SHOW_TVDB_ID = "tvdbid";
        String EPISODE_NUMBER = "episode_number";
        String SEASON_NUMBER = "season_number";
    }

    private static final String TRAKT_MOVIE_COMMENT_PAGE_URL = "https://trakt.tv/comment/movie/";

    private static final String TRAKT_EPISODE_COMMENT_PAGE_URL
            = "https://trakt.tv/comment/episode/";

    private static final String TRAKT_SHOW_COMMENT_PAGE_URL = "https://trakt.tv/comment/show/";

    private final AdapterView.OnItemClickListener mOnClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    @InjectView(R.id.listViewShouts) ListView mList;

    @InjectView(R.id.textViewShoutsEmpty) TextView mEmptyView;

    @InjectView(R.id.swipeRefreshLayoutShouts) SwipeRefreshLayout mSwipeRefreshLayout;

    @InjectView(R.id.imageButtonShouts) ImageButton mButtonShout;

    @InjectView(R.id.editTextShouts) EditText mEditTextShout;

    @InjectView(R.id.checkBoxShouts) CheckBox mCheckBoxIsSpoiler;

    private TraktCommentsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_comments, container, false);
        ButterKnife.inject(this, v);

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressViewOffset(false, getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));
        int accentColorResId = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.colorAccent);
        mSwipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.teal_dark);

        mList.setOnItemClickListener(mOnClickListener);
        mList.setEmptyView(mEmptyView);

        mButtonShout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shout();
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

        return v;
    }

    private void shout() {
        // prevent empty shouts
        String shout = mEditTextShout.getText().toString();
        if (TextUtils.isEmpty(shout)) {
            return;
        }

        // disable the shout button
        mButtonShout.setEnabled(false);

        Bundle args = getArguments();
        boolean isSpoiler = mCheckBoxIsSpoiler.isChecked();

        // shout for a movie?
        int movieTmdbId = args.getInt(InitBundle.MOVIE_TMDB_ID);
        if (movieTmdbId != 0) {
            AndroidUtils.executeOnPool(
                    new TraktTask(getActivity()).shoutMovie(movieTmdbId, shout, isSpoiler)
            );
            return;
        }

        // shout for an episode?
        int showTvdbId = args.getInt(InitBundle.SHOW_TVDB_ID);
        int episodeNumber = args.getInt(InitBundle.EPISODE_NUMBER);
        if (episodeNumber != 0) {
            int seasonNumber = args.getInt(InitBundle.SEASON_NUMBER);
            AndroidUtils.executeOnPool(
                    new TraktTask(getActivity())
                            .shoutEpisode(showTvdbId, seasonNumber, episodeNumber, shout, isSpoiler)
            );
            return;
        }

        // shout for a show!
        AndroidUtils.executeOnPool(
                new TraktTask(getActivity()).shoutShow(showTvdbId, shout, isSpoiler)
        );
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // change empty message if we are offline
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            mEmptyView.setText(R.string.offline);
        } else {
            mEmptyView.setText(getEmptyMessageResId());
            showProgressBar(true);
        }

        mAdapter = new TraktCommentsAdapter(getActivity());
        mList.setAdapter(mAdapter);

        getLoaderManager().initLoader(TraktCommentsActivity.LOADER_ID_COMMENTS, getArguments(),
                this);

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
        ButterKnife.reset(this);
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
            int showTvdbId = getArguments().getInt(InitBundle.SHOW_TVDB_ID);
            int episodeNumber = getArguments().getInt(InitBundle.EPISODE_NUMBER);
            String typeUrl;
            if (showTvdbId == 0) {
                typeUrl = TRAKT_MOVIE_COMMENT_PAGE_URL;
            } else if (episodeNumber == 0) {
                typeUrl = TRAKT_SHOW_COMMENT_PAGE_URL;
            } else {
                typeUrl = TRAKT_EPISODE_COMMENT_PAGE_URL;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(typeUrl + comment.id));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            Utils.tryStartActivity(getActivity(), intent, true);
        }
    }

    @Override
    public Loader<List<Comment>> onCreateLoader(int id, Bundle args) {
        showProgressBar(true);
        return new TraktCommentsLoader(getActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> data) {
        mAdapter.setData(data);

        showProgressBar(false);
    }

    @Override
    public void onLoaderReset(Loader<List<Comment>> data) {
        mAdapter.setData(null);
    }

    private int getEmptyMessageResId() {
        return R.string.no_shouts;
    }

    @Override
    public void onRefresh() {
        refreshCommentsWithNetworkCheck();
    }

    private void refreshCommentsWithNetworkCheck() {
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            // keep existing data, but update empty view anyhow
            showProgressBar(false);
            mEmptyView.setText(R.string.offline);
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_SHORT).show();
            return;
        }
        showProgressBar(true);
        mEmptyView.setText(getEmptyMessageResId());
        refreshComments();
    }

    private void refreshComments() {
        getLoaderManager().restartLoader(TraktCommentsActivity.LOADER_ID_COMMENTS, getArguments(),
                this);
    }

    /**
     * Show or hide the progress bar of the {@link android.support.v4.widget.SwipeRefreshLayout}
     * wrapping the comments list.
     */
    protected void showProgressBar(boolean isShowing) {
        mSwipeRefreshLayout.setRefreshing(isShowing);
    }

    public void onEventMainThread(TraktTask.TraktActionCompleteEvent event) {
        if (event.mTraktAction != TraktAction.SHOUT || getView() == null) {
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
