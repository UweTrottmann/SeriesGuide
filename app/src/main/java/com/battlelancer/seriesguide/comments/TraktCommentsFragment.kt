package com.battlelancer.seriesguide.comments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentCommentsBinding;
import com.battlelancer.seriesguide.traktapi.TraktAction;
import com.battlelancer.seriesguide.traktapi.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktLink;
import com.uwetrottmann.trakt5.entities.Comment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Displays show or episode comments and supports posting comments.
 */
public class TraktCommentsFragment extends Fragment {

    public interface InitBundle {
        String MOVIE_TMDB_ID = "movie";
        String SHOW_ID = "show";
        String EPISODE_ID = "episode";
    }

    private FragmentCommentsBinding binding;
    private TraktCommentsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentCommentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.swipeRefreshLayoutShouts.setSwipeableChildren(R.id.scrollViewComments,
                R.id.listViewShouts);
        binding.swipeRefreshLayoutShouts.setOnRefreshListener(
                this::refreshCommentsWithNetworkCheck);
        binding.swipeRefreshLayoutShouts.setProgressViewOffset(false,
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().getTheme(),
                binding.swipeRefreshLayoutShouts);

        binding.listViewShouts.setOnItemClickListener(onItemClickListener);
        binding.listViewShouts.setEmptyView(binding.textViewShoutsEmpty);

        binding.buttonShouts.setOnClickListener(v -> comment());

        // disable comment button by default, enable if comment entered
        binding.buttonShouts.setEnabled(false);
        binding.editTextShouts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.buttonShouts.setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // set initial view states
        showProgressBar(true);

        // setup adapter
        adapter = new TraktCommentsAdapter(getActivity());
        binding.listViewShouts.setAdapter(adapter);

        // load data
        LoaderManager.getInstance(this)
                .initLoader(TraktCommentsActivity.LOADER_ID_COMMENTS, getArguments(),
                        commentsLoaderCallbacks);

        // enable menu
        requireActivity().addMenuProvider(
                optionsMenuProvider,
                getViewLifecycleOwner(),
                Lifecycle.State.RESUMED
        );
    }

    private void comment() {
        // prevent empty comments
        String comment = binding.editTextShouts.getText().toString();
        if (TextUtils.isEmpty(comment)) {
            return;
        }

        // disable the comment button
        binding.buttonShouts.setEnabled(false);

        Bundle args = requireArguments();
        boolean isSpoiler = binding.checkBoxShouts.isChecked();

        // as determined by "science", episode comments are most likely, so check for them first
        // comment for an episode?
        long episodeId = args.getLong(InitBundle.EPISODE_ID);
        if (episodeId != 0) {
            new TraktTask(getContext()).commentEpisode(episodeId, comment, isSpoiler)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }

        // comment for a movie?
        int movieTmdbId = args.getInt(InitBundle.MOVIE_TMDB_ID);
        if (movieTmdbId != 0) {
            new TraktTask(getContext()).commentMovie(movieTmdbId, comment, isSpoiler)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }

        // comment for a show?
        long showId = args.getLong(InitBundle.SHOW_ID);
        if (showId != 0) {
            new TraktTask(getContext()).commentShow(showId, comment, isSpoiler)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // if all ids were 0, do nothing
        Timber.e("comment: did nothing, all possible ids were 0");
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
        binding = null;
    }

    private final MenuProvider optionsMenuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.comments_menu, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.menu_action_comments_refresh) {
                refreshCommentsWithNetworkCheck();
                return true;
            }
            return false;
        }
    };

    private final AdapterView.OnItemClickListener onItemClickListener
            = (parent, v, position, id) -> onListItemClick((ListView) parent, v, position);

    public void onListItemClick(ListView l, View v, int position) {
        final Comment comment = (Comment) l.getItemAtPosition(position);
        if (comment == null) {
            return;
        }

        if (comment.spoiler) {
            // if comment is a spoiler it is hidden, first click should reveal it
            comment.spoiler = false;
            TextView shoutText = v.findViewById(R.id.textViewComment);
            if (shoutText != null) {
                shoutText.setText(comment.comment);
            }
        } else {
            // open comment website
            Utils.launchWebsite(getContext(), TraktLink.comment(comment.id));
        }
    }

    private final LoaderCallbacks<TraktCommentsLoader.Result> commentsLoaderCallbacks
            = new LoaderCallbacks<TraktCommentsLoader.Result>() {
        @NonNull
        @Override
        public Loader<TraktCommentsLoader.Result> onCreateLoader(int id, Bundle args) {
            showProgressBar(true);
            return new TraktCommentsLoader(getContext(), args);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<TraktCommentsLoader.Result> loader,
                TraktCommentsLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            adapter.setData(data.results);
            setEmptyMessage(data.emptyText);
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<TraktCommentsLoader.Result> loader) {
            // keep existing data
        }
    };

    private void refreshCommentsWithNetworkCheck() {
        if (!AndroidUtils.isNetworkConnected(requireContext())) {
            // keep existing data, but update empty view anyhow
            showProgressBar(false);
            setEmptyMessage(getString(R.string.offline));
            Toast.makeText(requireContext(), R.string.offline, Toast.LENGTH_SHORT).show();
            return;
        }

        refreshComments();
    }

    private void refreshComments() {
        LoaderManager.getInstance(this)
                .restartLoader(TraktCommentsActivity.LOADER_ID_COMMENTS, getArguments(),
                        commentsLoaderCallbacks);
    }

    /**
     * Changes the empty message.
     */
    private void setEmptyMessage(String stringResourceId) {
        binding.textViewShoutsEmpty.setText(stringResourceId);
    }

    /**
     * Show or hide the progress bar of the {@link SwipeRefreshLayout}
     * wrapping the comments list.
     */
    protected void showProgressBar(boolean isShowing) {
        binding.swipeRefreshLayoutShouts.setRefreshing(isShowing);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(TraktTask.TraktActionCompleteEvent event) {
        if (event.traktAction != TraktAction.COMMENT || getView() == null) {
            return;
        }

        // reenable the shout button
        binding.buttonShouts.setEnabled(true);

        if (event.wasSuccessful) {
            // clear the text field and show recent shout
            binding.editTextShouts.setText("");
            refreshCommentsWithNetworkCheck();
        }
    }
}
