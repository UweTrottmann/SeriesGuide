package com.battlelancer.seriesguide.movies;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentMoviesDiscoverBinding;
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity;
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MoviesDiscoverFragment extends Fragment {

    private FragmentMoviesDiscoverBinding binding;

    private MoviesDiscoverAdapter adapter;
    private GridLayoutManager layoutManager;

    public MoviesDiscoverFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentMoviesDiscoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.swipeRefreshLayoutMoviesDiscover.setOnRefreshListener(onRefreshListener);
        binding.swipeRefreshLayoutMoviesDiscover.setRefreshing(false);
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().getTheme(),
                binding.swipeRefreshLayoutMoviesDiscover);

        adapter = new MoviesDiscoverAdapter(requireContext(),
                new DiscoverItemClickListener(requireContext()));

        layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.movie_grid_columnWidth, 2, 6);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_LINK) {
                    return 3;
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_HEADER) {
                    return layoutManager.getSpanCount();
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_MOVIE) {
                    return 2;
                }
                return 0;
            }
        });

        binding.recyclerViewMoviesDiscover.setHasFixedSize(true);
        binding.recyclerViewMoviesDiscover.setLayoutManager(layoutManager);
        binding.recyclerViewMoviesDiscover.setAdapter(adapter);

        new ViewModelProvider(requireActivity()).get(MoviesActivityViewModel.class)
                .getScrollTabToTopLiveData()
                .observe(getViewLifecycleOwner(), event -> {
                    if (event != null
                            && event.getTabPosition() == MoviesActivity.TAB_POSITION_DISCOVER) {
                        binding.recyclerViewMoviesDiscover.smoothScrollToPosition(0);
                    }
                });

        LoaderManager.getInstance(this).initLoader(0, null, nowPlayingLoaderCallbacks);

        requireActivity().addMenuProvider(
                optionsMenuProvider,
                getViewLifecycleOwner(),
                Lifecycle.State.RESUMED
        );
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
        binding = null;
    }

    private final MenuProvider optionsMenuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.movies_discover_menu, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.menu_action_movies_search_change_language) {
                MovieLocalizationDialogFragment.show(getParentFragmentManager());
                return true;
            }
            return false;
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLanguageChanged(
            MovieLocalizationDialogFragment.LocalizationChangedEvent event) {
        LoaderManager.getInstance(this).restartLoader(0, null, nowPlayingLoaderCallbacks);
    }

    private static class DiscoverItemClickListener extends MovieClickListenerImpl
            implements MoviesDiscoverAdapter.ItemClickListener {

        DiscoverItemClickListener(Context context) {
            super(context);
        }

        @Override
        public void onClickLink(MoviesDiscoverLink link, View anchor) {
            Intent intent = new Intent(getContext(), MoviesSearchActivity.class);
            intent.putExtra(MoviesSearchActivity.EXTRA_ID_LINK, link.id);
            Utils.startActivityWithAnimation(getContext(), intent, anchor);
        }
    }

    private final LoaderManager.LoaderCallbacks<MoviesDiscoverLoader.Result> nowPlayingLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<MoviesDiscoverLoader.Result>() {
        @NonNull
        @Override
        public Loader<MoviesDiscoverLoader.Result> onCreateLoader(int id, Bundle args) {
            return new MoviesDiscoverLoader(requireContext());
        }

        @Override
        public void onLoadFinished(@NonNull Loader<MoviesDiscoverLoader.Result> loader,
                MoviesDiscoverLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            binding.swipeRefreshLayoutMoviesDiscover.setRefreshing(false);
            adapter.updateMovies(data.getResults());
        }

        @Override
        public void onLoaderReset(@NonNull Loader<MoviesDiscoverLoader.Result> loader) {
            adapter.updateMovies(null);
        }
    };

    private final SwipeRefreshLayout.OnRefreshListener onRefreshListener
            = () -> LoaderManager.getInstance(MoviesDiscoverFragment.this)
            .restartLoader(0, null, nowPlayingLoaderCallbacks);
}
