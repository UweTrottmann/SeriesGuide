package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.BaseShowsAdapter;
import com.battlelancer.seriesguide.adapters.ListItemsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.ListsDistillationSettings;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.ListsTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays one user created list which includes a mixture of shows, seasons and episodes.
 */
public class ListsFragment extends Fragment implements OnItemClickListener, View.OnClickListener {

    /** LoaderManager is created unique to fragment, so use same id for all of them */
    private static final int LOADER_ID = 1;
    private static final String TAG = "Lists";

    public static ListsFragment newInstance(String list_id) {
        ListsFragment f = new ListsFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.LIST_ID, list_id);
        f.setArguments(args);

        return f;
    }

    interface InitBundle {

        String LIST_ID = "list_id";
    }

    private ListItemsAdapter adapter;
    private TextView emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        emptyView = view.findViewById(android.R.id.empty);
        ViewTools.setVectorIconTop(emptyView.getContext().getTheme(), emptyView,
                R.drawable.ic_list_white_24dp);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ListItemsAdapter(getActivity(), onItemClickListener);

        if (getView() == null) {
            return;
        }

        // setup grid view
        GridView gridView = getView().findViewById(android.R.id.list);
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher());
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setEmptyView(emptyView);
        gridView.setFastScrollAlwaysVisible(false);
        gridView.setFastScrollEnabled(true);

        getLoaderManager().initLoader(LOADER_ID, getArguments(), loaderCallbacks);
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
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Cursor listItem = (Cursor) adapter.getItem(position);
        int itemType = listItem.getInt(ListItemsAdapter.Query.ITEM_TYPE);
        String itemRefId = listItem.getString(ListItemsAdapter.Query.ITEM_REF_ID);

        Intent intent = null;
        switch (itemType) {
            case 1: {
                // display show overview
                intent = new Intent(getActivity(), OverviewActivity.class);
                intent.putExtra(OverviewActivity.EXTRA_INT_SHOW_TVDBID,
                        Integer.valueOf(itemRefId));
                break;
            }
            case 2: {
                // display episodes of season
                intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID,
                        Integer.valueOf(itemRefId));
                break;
            }
            case 3: {
                // display episode details
                intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                        Integer.valueOf(itemRefId));
                break;
            }
        }

        if (intent != null) {
            Utils.startActivityWithAnimation(getActivity(), intent, view);
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ListsDistillationSettings.ListsSortOrderChangedEvent event) {
        // sort order has changed, reload lists
        getLoaderManager().restartLoader(LOADER_ID, getArguments(), loaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String listId = args.getString(InitBundle.LIST_ID);
            return new CursorLoader(getActivity(), ListItems.CONTENT_WITH_DETAILS_URI,
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
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private ListItemsAdapter.OnItemClickListener onItemClickListener
            = new ListItemsAdapter.OnItemClickListener() {
        @Override
        public void onClick(View view, BaseShowsAdapter.ShowViewHolder viewHolder) {
            if (viewHolder instanceof ListItemsAdapter.ListItemViewHolder) {
                ListItemsAdapter.ListItemViewHolder viewHolderActual
                        = (ListItemsAdapter.ListItemViewHolder) viewHolder;
                PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
                popupMenu.inflate(R.menu.lists_popup_menu);
                popupMenu.setOnMenuItemClickListener(
                        new PopupMenuItemClickListener(getContext(), getFragmentManager(),
                                viewHolderActual.itemId, viewHolderActual.itemTvdbId,
                                viewHolderActual.itemType));
                popupMenu.show();
            }
        }

        @Override
        public void onFavoriteClick(int showTvdbId, boolean isFavorite) {
            SgApp.getServicesComponent(getContext()).showTools()
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
                    ManageListsDialogFragment.showListsDialog(itemTvdbId, itemType,
                            fragmentManager);
                    Utils.trackContextMenu(context, TAG, "Manage lists");
                    return true;
                }
                case R.id.menu_action_lists_remove: {
                    ListsTools.removeListItem(context, itemId);
                    Utils.trackContextMenu(context, TAG, "Remove from list");
                    return true;
                }
            }
            return false;
        }
    }
}
