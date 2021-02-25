package com.battlelancer.seriesguide.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.tasks.BaseShowActionTask;
import com.battlelancer.seriesguide.widgets.EmptyView;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Can display either the connected trakt user's watched, collected or watchlist-ed shows and offer
 * to add them.
 */
public class TraktAddFragment extends AddFragment {

    /**
     * Which trakt list should be shown. One of {@link TraktShowsLink}.
     */
    private final static String ARG_TYPE = "traktListType";

    public static TraktAddFragment newInstance(TraktShowsLink link) {
        TraktAddFragment f = new TraktAddFragment();

        Bundle args = new Bundle();
        args.putInt(TraktAddFragment.ARG_TYPE, link.id);
        f.setArguments(args);

        return f;
    }

    private Unbinder unbinder;
    private TraktShowsLink listType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        listType = TraktShowsLink.fromId(args != null ? args.getInt(ARG_TYPE) : -1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_trakt, container, false);
        unbinder = ButterKnife.bind(this, v);

        // set initial view states
        setProgressVisible(true, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter, enable context menu only for watchlist
        adapter = new AddAdapter(getActivity(), new ArrayList<>(), itemClickListener,
                listType == TraktShowsLink.WATCHLIST
        );

        // load data
        LoaderManager.getInstance(this)
                .initLoader(SearchActivity.TRAKT_BASE_LOADER_ID + listType.id, null,
                        traktAddCallbacks);

        // add menu options
        setHasOptionsMenu(true);
    }

    private final AddAdapter.OnItemClickListener itemClickListener
            = new AddAdapter.OnItemClickListener() {

        @Override
        public void onItemClick(SearchResult item) {
            if (item != null && item.getState() != SearchResult.STATE_ADDING) {
                if (item.getState() == SearchResult.STATE_ADDED) {
                    // already in library, open it
                    startActivity(OverviewActivity
                            .intentShowByTmdbId(requireContext(), item.getTmdbId()));
                } else {
                    // display more details in a dialog
                    AddShowDialogFragment.show(getParentFragmentManager(), item);
                }
            }
        }

        @Override
        public void onAddClick(SearchResult item) {
            EventBus.getDefault().post(new OnAddingShowEvent(item.getTmdbId()));
            TaskManager.getInstance().performAddTask(requireContext(), item);
        }

        @Override
        public void onMenuWatchlistClick(View view, int showTmdbId) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.add_dialog_popup_menu);

            // prevent adding shows to watchlist already on watchlist
            if (listType == TraktShowsLink.WATCHLIST) {
                popupMenu.getMenu().findItem(R.id.menu_action_show_watchlist_add).setVisible(false);
            }

            popupMenu.setOnMenuItemClickListener(
                    new AddItemMenuItemClickListener(requireContext(), showTmdbId));
            popupMenu.show();
        }
    };

    public static class AddItemMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        private final Context context;
        private final int showTmdbId;

        public AddItemMenuItemClickListener(Context context, int showTmdbId) {
            this.context = context;
            this.showTmdbId = showTmdbId;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_action_show_watchlist_add) {
                ShowTools.addToWatchlist(context, showTmdbId);
                return true;
            }
            if (itemId == R.id.menu_action_show_watchlist_remove) {
                ShowTools.removeFromWatchlist(context, showTmdbId);
                return true;
            }
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trakt_library_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add_all) {
            if (searchResults != null) {
                List<SearchResult> showsToAdd = new LinkedList<>();
                // only include shows not already added
                for (SearchResult result : searchResults) {
                    if (result.getState() == SearchResult.STATE_ADD) {
                        showsToAdd.add(result);
                        result.setState(SearchResult.STATE_ADDING);
                    }
                }
                EventBus.getDefault().post(new OnAddingShowEvent());
                TaskManager.getInstance().performAddTask(getContext(), showsToAdd, false, false);
            }
            // disable the item so the user has to come back
            item.setEnabled(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BaseShowActionTask.ShowChangedEvent event) {
        if (listType == TraktShowsLink.WATCHLIST) {
            // reload watchlist if a show was removed
            LoaderManager.getInstance(this)
                    .restartLoader(SearchActivity.TRAKT_BASE_LOADER_ID + listType.id, null,
                            traktAddCallbacks);
        }
    }

    @Override
    protected void setupEmptyView(EmptyView emptyView) {
        emptyView.setButtonClickListener(v -> {
            setProgressVisible(true, false);
            LoaderManager.getInstance(TraktAddFragment.this)
                    .restartLoader(SearchActivity.TRAKT_BASE_LOADER_ID + listType.id, null,
                            traktAddCallbacks);
        });
    }

    private final LoaderManager.LoaderCallbacks<TraktAddLoader.Result> traktAddCallbacks
            = new LoaderManager.LoaderCallbacks<TraktAddLoader.Result>() {
        @Override
        public Loader<TraktAddLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TraktAddLoader(getContext(), listType);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<TraktAddLoader.Result> loader,
                TraktAddLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            setSearchResults(data.results);
            setEmptyMessage(data.emptyText);
            setProgressVisible(false, true);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<TraktAddLoader.Result> loader) {
            // keep currently displayed data
        }
    };
}
