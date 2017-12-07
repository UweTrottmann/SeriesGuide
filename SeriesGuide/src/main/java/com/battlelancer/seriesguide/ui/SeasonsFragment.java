package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.SeasonsAdapter;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.jobs.episodes.SeasonWatchedJob;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.service.UnwatchedUpdaterService;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import java.lang.ref.WeakReference;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays a list of seasons of one show.
 */
public class SeasonsFragment extends ListFragment {

    private static final int CONTEXT_WATCHED_SHOW_ALL_ID = 0;
    private static final int CONTEXT_WATCHED_SHOW_NONE_ID = 1;
    private static final int CONTEXT_COLLECTED_SHOW_ALL_ID = 2;
    private static final int CONTEXT_COLLECTED_SHOW_NONE_ID = 3;
    private static final String TAG = "Seasons";

    @BindView(R.id.textViewSeasonsRemaining) TextView textViewRemaining;
    @BindView(R.id.imageViewSeasonsCollectedToggle) ImageView buttonCollectedAll;
    @BindView(R.id.imageViewSeasonsWatchedToggle) ImageView buttonWatchedAll;
    private Unbinder unbinder;

    private SeasonsAdapter adapter;
    private boolean watchedAllEpisodes;
    private boolean collectedAllEpisodes;
    private RemainingUpdateTask remainingUpdateTask;
    private VectorDrawableCompat drawableWatchAll;
    private VectorDrawableCompat drawableCollectAll;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {

        String SHOW_TVDBID = "show_tvdbid";
    }

    public static SeasonsFragment newInstance(int showId) {
        SeasonsFragment f = new SeasonsFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showId);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seasons, container, false);
        unbinder = ButterKnife.bind(this, view);

        drawableWatchAll = ViewTools.vectorIconActive(buttonWatchedAll.getContext(),
                buttonWatchedAll.getContext().getTheme(),
                R.drawable.ic_watch_all_black_24dp);
        buttonWatchedAll.setImageDrawable(drawableWatchAll);
        drawableCollectAll = ViewTools.vectorIconActive(buttonCollectedAll.getContext(),
                buttonCollectedAll.getContext().getTheme(), R.drawable.ic_collect_all_black_24dp);
        buttonCollectedAll.setImageDrawable(drawableCollectAll);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // populate list
        adapter = new SeasonsAdapter(getActivity(), popupMenuClickListener);
        setListAdapter(adapter);
        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(OverviewActivity.SEASONS_LOADER_ID, null,
                seasonsLoaderCallbacks);

        // listen to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(onSortOrderChangedListener);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        updateUnwatchedCounts();
        updateRemainingCounter();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (remainingUpdateTask != null) {
            remainingUpdateTask.cancel(true);
        }
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // stop listening to sort pref changes
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.seasons_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_sesortby) {
            showSortDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, (int) id);

        ActivityCompat.startActivity(getActivity(), intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    /**
     * Updates the total remaining episodes counter, updates season counters after episode actions.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BaseNavDrawerActivity.ServiceCompletedEvent event) {
        if (event.flagJob == null || !event.isSuccessful) {
            return; // no changes applied
        }
        if (!isAdded()) {
            return; // no longer added to activity
        }
        updateRemainingCounter();
        if (event.flagJob instanceof SeasonWatchedJob) {
            // If we can narrow it down to just one season...
            SeasonWatchedJob seasonWatchedType = (SeasonWatchedJob) event.flagJob;
            getActivity().startService(UnwatchedUpdaterService.buildIntent(getContext(),
                    getShowId(), seasonWatchedType.getSeasonTvdbId()));
        } else {
            updateUnwatchedCounts();
        }
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onFlagSeasonSkipped(long seasonId, int seasonNumber) {
        EpisodeTools.seasonWatched(getContext(), getShowId(), (int) seasonId,
                seasonNumber, EpisodeFlags.SKIPPED);
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of the season.
     */
    private void onFlagSeasonWatched(long seasonId, int seasonNumber, boolean isWatched) {
        EpisodeTools.seasonWatched(getContext(), getShowId(), (int) seasonId,
                seasonNumber, isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    /**
     * Changes the seasons episodes collected flags.
     */
    private void onFlagSeasonCollected(long seasonId, int seasonNumber, boolean isCollected) {
        EpisodeTools.seasonCollected(getContext(), getShowId(), (int) seasonId,
                seasonNumber, isCollected);
    }

    /**
     * Changes the watched flag for all episodes of the given show, updates the status labels of all
     * seasons.
     */
    private void onFlagShowWatched(boolean isWatched) {
        EpisodeTools.showWatched(getContext(), getShowId(), isWatched);
    }

    /**
     * Changes the collected flag for all episodes of the given show, updates the status labels of
     * all seasons.
     */
    private void onFlagShowCollected(boolean isCollected) {
        EpisodeTools.showCollected(getContext(), getShowId(), isCollected);
    }

    /**
     * Update unwatched stats for all seasons of this fragments show. After service finishes
     * notifies provider causing the loader to reload.
     */
    private void updateUnwatchedCounts() {
        getActivity().startService(UnwatchedUpdaterService.buildIntent(getContext(), getShowId()));
    }

    private void updateRemainingCounter() {
        if (remainingUpdateTask == null
                || remainingUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            remainingUpdateTask = new RemainingUpdateTask(this);
            remainingUpdateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    String.valueOf(getShowId()));
        }
    }

    private static class RemainingUpdateTask extends AsyncTask<String, Void, int[]> {

        private final WeakReference<SeasonsFragment> seasonsFragment;
        @SuppressLint("StaticFieldLeak") // using application context
        private final Context context;

        RemainingUpdateTask(SeasonsFragment seasonsFragment) {
            this.seasonsFragment = new WeakReference<>(seasonsFragment);
            this.context = seasonsFragment.getContext().getApplicationContext();
        }

        @Override
        protected int[] doInBackground(String... params) {
            int[] counts = new int[2];
            counts[0] = DBUtils.getUnwatchedEpisodesOfShow(context, params[0]);
            counts[1] = DBUtils.getUncollectedEpisodesOfShow(context, params[0]);
            return counts;
        }

        @Override
        protected void onPostExecute(int[] result) {
            SeasonsFragment seasonsFragment = this.seasonsFragment.get();
            if (seasonsFragment == null) {
                return;
            }
            if (result[0] <= 0) {
                seasonsFragment.textViewRemaining.setText(null);
            } else {
                int unwatched = result[0];
                seasonsFragment.textViewRemaining.setText(
                        seasonsFragment.textViewRemaining.getResources()
                                .getQuantityString(R.plurals.remaining_episodes_plural,
                                        unwatched, unwatched));
            }
            seasonsFragment.setWatchedToggleState(result[0]);
            seasonsFragment.setCollectedToggleState(result[1]);
        }
    }

    private void setWatchedToggleState(int unwatchedEpisodes) {
        watchedAllEpisodes = unwatchedEpisodes == 0;
        // using vectors is safe because it will be an AppCompatImageView
        if (watchedAllEpisodes) {
            buttonWatchedAll.setImageResource(R.drawable.ic_watched_all_24dp);
        } else {
            buttonWatchedAll.setImageDrawable(drawableWatchAll);
        }
        buttonWatchedAll.setContentDescription(
                getString(watchedAllEpisodes ? R.string.unmark_all : R.string.mark_all));
        // set onClick listener not before here to avoid unexpected actions
        buttonWatchedAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                if (!watchedAllEpisodes) {
                    popupMenu.getMenu().add(0, CONTEXT_WATCHED_SHOW_ALL_ID, 0, R.string.mark_all);
                }
                popupMenu.getMenu().add(0, CONTEXT_WATCHED_SHOW_NONE_ID, 0, R.string.unmark_all);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case CONTEXT_WATCHED_SHOW_ALL_ID: {
                                onFlagShowWatched(true);
                                Utils.trackAction(getActivity(), TAG, "Flag all watched (inline)");
                                return true;
                            }
                            case CONTEXT_WATCHED_SHOW_NONE_ID: {
                                onFlagShowWatched(false);
                                Utils.trackAction(getActivity(), TAG,
                                        "Flag all unwatched (inline)");
                                return true;
                            }
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }

    private void setCollectedToggleState(int uncollectedEpisodes) {
        collectedAllEpisodes = uncollectedEpisodes == 0;
        // using vectors is safe because it will be an AppCompatImageView
        if (collectedAllEpisodes) {
            buttonCollectedAll.setImageResource(R.drawable.ic_collected_all_24dp);
        } else {
            buttonCollectedAll.setImageDrawable(drawableCollectAll);
        }
        buttonCollectedAll.setContentDescription(
                getString(collectedAllEpisodes ? R.string.uncollect_all : R.string.collect_all));
        // set onClick listener not before here to avoid unexpected actions
        buttonCollectedAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                if (!collectedAllEpisodes) {
                    popupMenu.getMenu()
                            .add(0, CONTEXT_COLLECTED_SHOW_ALL_ID, 0, R.string.collect_all);
                }
                popupMenu.getMenu()
                        .add(0, CONTEXT_COLLECTED_SHOW_NONE_ID, 0, R.string.uncollect_all);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case CONTEXT_COLLECTED_SHOW_ALL_ID: {
                                onFlagShowCollected(true);
                                Utils.trackAction(getActivity(), TAG,
                                        "Flag all collected (inline)");
                                return true;
                            }
                            case CONTEXT_COLLECTED_SHOW_NONE_ID: {
                                onFlagShowCollected(false);
                                Utils.trackAction(getActivity(), TAG,
                                        "Flag all uncollected (inline)");
                                return true;
                            }
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }

    private void showSortDialog() {
        Constants.SeasonSorting sortOrder = DisplaySettings.getSeasonSortOrder(getActivity());
        FragmentManager fm = getFragmentManager();
        SingleChoiceDialogFragment sortDialog = SingleChoiceDialogFragment.newInstance(
                R.array.sesorting,
                R.array.sesortingData, sortOrder.index(),
                DisplaySettings.KEY_SEASON_SORT_ORDER, R.string.pref_seasonsorting);
        sortDialog.show(fm, "fragment_sort");
    }

    private final OnSharedPreferenceChangeListener onSortOrderChangedListener
            = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DisplaySettings.KEY_SEASON_SORT_ORDER.equals(key)) {
                // reload seasons in new order
                getLoaderManager().restartLoader(OverviewActivity.SEASONS_LOADER_ID, null,
                        seasonsLoaderCallbacks);
            }
        }
    };

    public interface SeasonsQuery {

        String[] PROJECTION = {
                BaseColumns._ID,
                Seasons.COMBINED,
                Seasons.WATCHCOUNT,
                Seasons.UNAIREDCOUNT,
                Seasons.NOAIRDATECOUNT,
                Seasons.TOTALCOUNT,
                Seasons.TAGS
        };

        int _ID = 0;
        int COMBINED = 1;
        int WATCHCOUNT = 2;
        int UNAIREDCOUNT = 3;
        int NOAIRDATECOUNT = 4;
        int TOTALCOUNT = 5;
        int TAGS = 6;
    }

    private LoaderManager.LoaderCallbacks<Cursor> seasonsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Constants.SeasonSorting sortOrder = DisplaySettings.getSeasonSortOrder(getActivity());
            // can use SELECTION_WITH_EPISODES as count is updated when this fragment runs
            return new CursorLoader(getActivity(),
                    Seasons.buildSeasonsOfShowUri(String.valueOf(getShowId())),
                    SeasonsQuery.PROJECTION, Seasons.SELECTION_WITH_EPISODES, null, sortOrder
                    .query()
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // Swap the new cursor in. (The framework will take care of closing the
            // old cursor once we return.)
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed. We need to make sure we are no
            // longer using it.
            adapter.swapCursor(null);
        }
    };

    private SeasonsAdapter.PopupMenuClickListener popupMenuClickListener =
            new SeasonsAdapter.PopupMenuClickListener() {
                @Override
                public void onPopupMenuClick(View v, final int seasonTvdbId,
                        final int seasonNumber) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.seasons_popup_menu);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_action_seasons_watched_all: {
                                    onFlagSeasonWatched(seasonTvdbId, seasonNumber, true);
                                    Utils.trackContextMenu(getActivity(), TAG, "Flag all watched");
                                    return true;
                                }
                                case R.id.menu_action_seasons_watched_none: {
                                    onFlagSeasonWatched(seasonTvdbId, seasonNumber, false);
                                    Utils.trackContextMenu(getActivity(), TAG,
                                            "Flag all unwatched");
                                    return true;
                                }
                                case R.id.menu_action_seasons_collection_add: {
                                    onFlagSeasonCollected(seasonTvdbId, seasonNumber, true);
                                    Utils.trackContextMenu(getActivity(), TAG,
                                            "Flag all collected");
                                    return true;
                                }
                                case R.id.menu_action_seasons_collection_remove: {
                                    onFlagSeasonCollected(seasonTvdbId, seasonNumber, false);
                                    Utils.trackContextMenu(getActivity(), TAG,
                                            "Flag all uncollected");
                                    return true;
                                }
                                case R.id.menu_action_seasons_skip: {
                                    onFlagSeasonSkipped(seasonTvdbId, seasonNumber);
                                    Utils.trackContextMenu(getActivity(), TAG, "Flag all skipped");
                                    return true;
                                }
                                case R.id.menu_action_seasons_manage_lists: {
                                    ManageListsDialogFragment.showListsDialog(seasonTvdbId,
                                            ListItemTypes.SEASON,
                                            getFragmentManager());
                                    Utils.trackContextMenu(getActivity(), TAG, "Manage lists");
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
                }
            };
}
