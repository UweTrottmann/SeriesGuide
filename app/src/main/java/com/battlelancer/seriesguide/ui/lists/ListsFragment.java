package com.battlelancer.seriesguide.ui.lists;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.ui.shows.BaseShowsAdapter;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays one user created list which includes a mixture of shows, seasons and episodes.
 */
public class ListsFragment extends Fragment {

    /** LoaderManager is created unique to fragment, so use same id for all of them */
    private static final int LOADER_ID = 1;
    private static final String ARG_LIST_ID = "LIST_ID";
    private static final String ARG_LIST_POSITION = "LIST_POSITION";

    public static ListsFragment newInstance(String listId, int listPosition) {
        ListsFragment f = new ListsFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LIST_ID, listId);
        args.putInt(ARG_LIST_POSITION, listPosition);
        f.setArguments(args);

        return f;
    }

    private ListItemsAdapter adapter;
    private TextView emptyView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        emptyView = view.findViewById(R.id.emptyViewList);
        ViewTools.setVectorDrawableTop(emptyView, R.drawable.ic_list_white_24dp);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ListItemsAdapter(getActivity(), onItemClickListener);

        GridView gridView = view.findViewById(R.id.gridViewList);
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true);
        gridView.setAdapter(adapter);
        gridView.setEmptyView(emptyView);

        new ViewModelProvider(requireActivity()).get(ListsActivityViewModel.class)
                .getScrollTabToTopLiveData()
                .observe(getViewLifecycleOwner(), tabPosition -> {
                    if (tabPosition != null
                            && tabPosition == requireArguments().getInt(ARG_LIST_POSITION)) {
                        gridView.smoothScrollToPosition(0);
                    }
                });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(LOADER_ID, getArguments(), loaderCallbacks);
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

    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ListsDistillationSettings.ListsSortOrderChangedEvent event) {
        // sort order has changed, reload lists
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, getArguments(), loaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String listId = args.getString(ARG_LIST_ID);
            return new CursorLoader(requireContext(), ListItems.CONTENT_WITH_DETAILS_URI,
                    ListItemsAdapter.Query.PROJECTION,
                    // items of this list, but exclude any if show was removed from the database
                    // (the join on show data will fail, hence the show id will be 0/null)
                    ListItems.SELECTION_LIST + " AND " + Shows.REF_SHOW_ID + ">0",
                    new String[] {
                            listId
                    }, ListsDistillationSettings.getSortQuery(getActivity())
            );
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private ListItemsAdapter.OnItemClickListener onItemClickListener
            = new ListItemsAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View anchor, BaseShowsAdapter.ShowViewHolder showViewHolder) {
            ListItemsAdapter.ListItemViewHolder viewHolder = (ListItemsAdapter.ListItemViewHolder) showViewHolder;
            int itemType = viewHolder.itemType;
            int itemTvdbId = viewHolder.itemTvdbId;

            Intent intent = null;
            switch (itemType) {
                case 1: {
                    // display show overview
                    intent = OverviewActivity.intentShow(getActivity(), itemTvdbId);
                    break;
                }
                case 2: {
                    // display episodes of season
                    intent = new Intent(getActivity(), EpisodesActivity.class);
                    intent.putExtra(EpisodesActivity.EXTRA_SEASON_TVDBID, itemTvdbId);
                    break;
                }
                case 3: {
                    // display episode details
                    intent = new Intent(getActivity(), EpisodesActivity.class);
                    intent.putExtra(EpisodesActivity.EXTRA_EPISODE_TVDBID, itemTvdbId);
                    break;
                }
            }

            if (intent != null) {
                Utils.startActivityWithAnimation(getActivity(), intent, anchor);
            }
        }

        @Override
        public void onMenuClick(View view, BaseShowsAdapter.ShowViewHolder viewHolder) {
            if (viewHolder instanceof ListItemsAdapter.ListItemViewHolder) {
                ListItemsAdapter.ListItemViewHolder viewHolderActual
                        = (ListItemsAdapter.ListItemViewHolder) viewHolder;
                PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
                popupMenu.inflate(R.menu.lists_popup_menu);
                popupMenu.setOnMenuItemClickListener(
                        new PopupMenuItemClickListener(getContext(), getParentFragmentManager(),
                                viewHolderActual.itemId, viewHolderActual.itemTvdbId,
                                viewHolderActual.itemType));
                popupMenu.show();
            }
        }

        @Override
        public void onFavoriteClick(int showTvdbId, boolean isFavorite) {
            SgApp.getServicesComponent(requireContext()).showTools()
                    .storeIsFavorite(showTvdbId, isFavorite);
        }
    };

    private static class PopupMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        private final Context context;
        private final FragmentManager fragmentManager;
        private final String itemId;
        private final int itemTvdbId;
        private final int itemType;

        public PopupMenuItemClickListener(Context context, FragmentManager fm, String itemId,
                int itemTvdbId, int itemType) {
            this.context = context;
            this.fragmentManager = fm;
            this.itemId = itemId;
            this.itemTvdbId = itemTvdbId;
            this.itemType = itemType;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_action_lists_manage: {
                    ManageListsDialogFragment.show(fragmentManager, itemTvdbId, itemType);
                    return true;
                }
                case R.id.menu_action_lists_remove: {
                    ListsTools.removeListItem(context, itemId);
                    return true;
                }
            }
            return false;
        }
    }
}
