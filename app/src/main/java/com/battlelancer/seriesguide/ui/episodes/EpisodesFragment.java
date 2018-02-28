package com.battlelancer.seriesguide.ui.episodes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment;
import com.battlelancer.seriesguide.ui.episodes.EpisodesAdapter.OnFlagEpisodeListener;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Displays a list of episodes of a season.
 */
public class EpisodesFragment extends ListFragment
        implements OnClickListener, OnFlagEpisodeListener, EpisodesAdapter.PopupMenuClickListener {

    private static final String TAG = "Episodes";

    private Constants.EpisodeSorting sortOrder;
    private boolean isDualPane;
    private EpisodesAdapter adapter;

    private int showTvdbId;
    private int seasonTvdbId;
    private int seasonNumber;
    private int startingPosition;
    private long lastCheckedItemId;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {

        String SHOW_TVDBID = "show_tvdbid";

        String SEASON_TVDBID = "season_tvdbid";

        String SEASON_NUMBER = "season_number";

        String STARTING_POSITION = "starting_position";
    }

    public static EpisodesFragment newInstance(int showId, int seasonId, int seasonNumber,
            int startingPosition) {
        EpisodesFragment f = new EpisodesFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showId);
        args.putInt(InitBundle.SEASON_TVDBID, seasonId);
        args.putInt(InitBundle.SEASON_NUMBER, seasonNumber);
        args.putInt(InitBundle.STARTING_POSITION, startingPosition);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            showTvdbId = args.getInt(InitBundle.SHOW_TVDBID);
            seasonTvdbId = args.getInt(InitBundle.SEASON_TVDBID);
            seasonNumber = args.getInt(InitBundle.SEASON_NUMBER);
            startingPosition = args.getInt(InitBundle.STARTING_POSITION);
        } else {
            throw new IllegalArgumentException("Missing arguments");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episodes, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadSortOrder();

        // listen to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(requireActivity());
        prefs.registerOnSharedPreferenceChangeListener(onSortOrderChangedListener);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View pager = requireActivity().findViewById(R.id.pagerEpisodes);
        isDualPane = pager != null && pager.getVisibility() == View.VISIBLE;

        if (isDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } else {
            startingPosition = -1; // overwrite starting position
        }
        lastCheckedItemId = -1;

        adapter = new EpisodesAdapter(requireActivity(), this, this);
        setListAdapter(adapter);

        getLoaderManager().initLoader(EpisodesActivity.EPISODES_LOADER_ID, null,
                episodesLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    /**
     * Display the episode at the given position in a detail pane or if not available in a new
     * activity.
     */
    private void showDetails(View view, int position) {
        if (isDualPane) {
            EpisodesActivity activity = (EpisodesActivity) requireActivity();
            activity.setCurrentPage(position);
            setItemChecked(position);
        } else {
            int episodeId = (int) getListView().getItemIdAtPosition(position);

            Intent intent = new Intent();
            intent.setClass(requireActivity(), EpisodesActivity.class);
            intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

            Utils.startActivityWithAnimation(requireActivity(), intent, view);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSortOrder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // stop listening to sort pref changes
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(requireActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodelist_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_epsorting) {
            showSortDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        showDetails(view, position);
    }

    @Override
    public void onPopupMenuClick(View v, final int episodeTvdbId, final int episodeNumber,
            final long releaseTimeMs, final int watchedFlag, final boolean isCollected) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.inflate(R.menu.episodes_popup_menu);

        Menu menu = popupMenu.getMenu();
        menu.findItem(R.id.menu_action_episodes_collection_add).setVisible(!isCollected);
        menu.findItem(R.id.menu_action_episodes_collection_remove).setVisible(isCollected);
        boolean isWatched = EpisodeTools.isWatched(watchedFlag);
        menu.findItem(R.id.menu_action_episodes_watched).setVisible(!isWatched);
        menu.findItem(R.id.menu_action_episodes_not_watched).setVisible(isWatched);
        boolean isSkipped = EpisodeTools.isSkipped(watchedFlag);
        menu.findItem(R.id.menu_action_episodes_skip).setVisible(!isWatched && !isSkipped);
        menu.findItem(R.id.menu_action_episodes_dont_skip).setVisible(isSkipped);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_episodes_watched: {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, true);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag watched");
                        return true;
                    }
                    case R.id.menu_action_episodes_not_watched: {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, false);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag unwatched");
                        return true;
                    }
                    case R.id.menu_action_episodes_collection_add: {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, true);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag collected");
                        return true;
                    }
                    case R.id.menu_action_episodes_collection_remove: {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, false);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag uncollected");
                        return true;
                    }
                    case R.id.menu_action_episodes_skip: {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, true);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag skipped");
                        return true;
                    }
                    case R.id.menu_action_episodes_dont_skip: {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, false);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag not skipped");
                        return true;
                    }
                    case R.id.menu_action_episodes_watched_previous: {
                        EpisodeTools.episodeWatchedPrevious(requireContext(), showTvdbId,
                                releaseTimeMs, episodeNumber);
                        Utils.trackContextMenu(requireContext(), TAG, "Flag previously aired");
                        return true;
                    }
                    case R.id.menu_action_episodes_manage_lists: {
                        ManageListsDialogFragment.showListsDialog(episodeTvdbId,
                                ListItemTypes.EPISODE,
                                getFragmentManager());
                        Utils.trackContextMenu(requireContext(), TAG, "Manage lists");
                        return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onFlagEpisodeWatched(int episodeTvdbId, int episode, boolean isWatched) {
        EpisodeTools.episodeWatched(requireContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episode,
                isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    public void onFlagEpisodeSkipped(int episodeTvdbId, int episode, boolean isSkipped) {
        EpisodeTools.episodeWatched(requireContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episode,
                isSkipped ? EpisodeFlags.SKIPPED : EpisodeFlags.UNWATCHED);
    }

    public void onFlagEpisodeCollected(int episodeTvdbId, int episode, boolean isCollected) {
        EpisodeTools.episodeCollected(requireContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episode, isCollected);
    }

    private LoaderManager.LoaderCallbacks<Cursor> episodesLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(requireContext(),
                    Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(seasonTvdbId)),
                    EpisodesAdapter.EpisodesQuery.PROJECTION, null, null, sortOrder.query());
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
            // set an initial checked item
            if (startingPosition != -1) {
                setItemChecked(startingPosition);
                startingPosition = -1;
            }
            // correctly restore the last checked item
            else if (lastCheckedItemId != -1) {
                setItemChecked(adapter.getItemPosition(lastCheckedItemId));
                lastCheckedItemId = -1;
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private void loadSortOrder() {
        sortOrder = DisplaySettings.getEpisodeSortOrder(requireActivity());
    }

    private void showSortDialog() {
        FragmentManager fm = requireFragmentManager();
        SingleChoiceDialogFragment sortDialog = SingleChoiceDialogFragment.newInstance(
                R.array.epsorting,
                R.array.epsortingData, sortOrder.index(),
                DisplaySettings.KEY_EPISODE_SORT_ORDER, R.string.pref_episodesorting);
        sortDialog.show(fm, "fragment_sort");
    }

    private final OnSharedPreferenceChangeListener onSortOrderChangedListener
            = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DisplaySettings.KEY_EPISODE_SORT_ORDER.equals(key)) {
                onSortOrderChanged();
            }
        }
    };

    private void onSortOrderChanged() {
        loadSortOrder();

        lastCheckedItemId = getListView().getItemIdAtPosition(
                getListView().getCheckedItemPosition());
        getLoaderManager().restartLoader(EpisodesActivity.EPISODES_LOADER_ID, null,
                episodesLoaderCallbacks);

        Utils.trackCustomEvent(requireActivity(), TAG, "Sorting", sortOrder.name());
    }

    /**
     * Highlight the given episode in the list.
     */
    public void setItemChecked(int position) {
        ListView list = getListView();
        list.setItemChecked(position, true);
        if (position <= list.getFirstVisiblePosition()
                || position >= list.getLastVisiblePosition()) {
            list.smoothScrollToPosition(position);
        }
    }

    @Override
    public void onClick(View v) {
        requireActivity().openContextMenu(v);
    }
}
