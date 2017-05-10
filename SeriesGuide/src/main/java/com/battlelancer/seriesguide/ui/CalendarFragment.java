package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.CalendarAdapter;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.settings.CalendarSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays upcoming or recent episodes in a scrollable grid, by default grouped by day.
 */
public class CalendarFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener,
        OnSharedPreferenceChangeListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "Calendar";
    private static final int CONTEXT_FLAG_WATCHED_ID = 0;
    private static final int CONTEXT_FLAG_UNWATCHED_ID = 1;
    private static final int CONTEXT_CHECKIN_ID = 2;
    private static final int CONTEXT_COLLECTION_ADD_ID = 3;
    private static final int CONTEXT_COLLECTION_REMOVE_ID = 4;

    private StickyGridHeadersGridView gridView;
    private CalendarAdapter adapter;
    private Handler handler;
    private String type;

    /**
     * Data which has to be passed when creating {@link CalendarFragment}. All Bundle extras are
     * strings, except LOADER_ID and EMPTY_STRING_ID.
     */
    public interface InitBundle {
        String TYPE = "type";
        String ANALYTICS_TAG = "analyticstag";
        String LOADER_ID = "loaderid";
        String EMPTY_STRING_ID = "emptyid";
    }

    public interface CalendarType {
        String UPCOMING = "upcoming";
        String RECENT = "recent";
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getArguments().getString(InitBundle.TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

        TextView emptyView = (TextView) v.findViewById(R.id.emptyViewCalendar);
        emptyView.setText(getString(getArguments().getInt(InitBundle.EMPTY_STRING_ID)));

        gridView = (StickyGridHeadersGridView) v.findViewById(R.id.gridViewCalendar);
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher());
        gridView.setEmptyView(emptyView);
        gridView.setAreHeadersSticky(false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        adapter = new CalendarAdapter(getActivity());
        boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
        adapter.setIsShowingHeaders(!infiniteScrolling);

        // setup grid view
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
        gridView.setFastScrollEnabled(infiniteScrolling);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // prevent stale upcoming/recent episodes, also:
        /*
          Workaround for loader issues on config changes. For some reason the
          CursorLoader holds on to a cursor with old data. See
          https://github.com/UweTrottmann/SeriesGuide/issues/257.
         */
        boolean isLoaderExists = getLoaderManager().getLoader(getLoaderId()) != null;
        getLoaderManager().initLoader(getLoaderId(), null, this);
        if (isLoaderExists) {
            onRequery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // avoid CPU activity
        schedulePeriodicDataRefresh(false);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.calendar_menu, menu);

        VectorDrawableCompat visibilitySettingsIcon = ViewTools.createVectorIconWhite(
                getActivity(), getActivity().getTheme(), R.drawable.ic_visibility_black_24dp);
        menu.findItem(R.id.menu_action_calendar_visibility).setIcon(visibilitySettingsIcon);

        // set menu items to current values
        menu.findItem(R.id.menu_onlyfavorites)
                .setChecked(CalendarSettings.isOnlyFavorites(getActivity()));
        menu.findItem(R.id.menu_nospecials)
                .setChecked(DisplaySettings.isHidingSpecials(getActivity()));
        menu.findItem(R.id.menu_nowatched)
                .setChecked(DisplaySettings.isNoWatchedEpisodes(getActivity()));
        menu.findItem(R.id.menu_infinite_scrolling)
                .setChecked(CalendarSettings.isInfiniteScrolling(getActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_onlyfavorites) {
            toggleFilterSetting(item, CalendarSettings.KEY_ONLY_FAVORITE_SHOWS);
            Utils.trackAction(getActivity(), TAG, "Only favorite shows Toggle");
            return true;
        } else if (itemId == R.id.menu_nospecials) {
            toggleFilterSetting(item, DisplaySettings.KEY_HIDE_SPECIALS);
            Utils.trackAction(getActivity(), TAG, "Hide specials Toggle");
            return true;
        } else if (itemId == R.id.menu_nowatched) {
            toggleFilterSetting(item, DisplaySettings.KEY_NO_WATCHED_EPISODES);
            Utils.trackAction(getActivity(), TAG, "Hide watched Toggle");
            return true;
        } else if (itemId == R.id.menu_infinite_scrolling) {
            toggleFilterSetting(item, CalendarSettings.KEY_INFINITE_SCROLLING);
            Utils.trackAction(getActivity(), TAG, "Infinite Scrolling Toggle");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void checkInEpisode(int episodeTvdbId) {
        CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(), episodeTvdbId);
        if (f != null && isResumed()) {
            f.show(getFragmentManager(), "checkin-dialog");
        }
    }

    private void updateEpisodeCollectionState(int showTvdbId, int episodeTvdbId, int seasonNumber,
            int episodeNumber, boolean addToCollection) {
        EpisodeTools.episodeCollected(SgApp.from(getActivity()), showTvdbId, episodeTvdbId,
                seasonNumber, episodeNumber, addToCollection);
    }

    private void updateEpisodeWatchedState(int showTvdbId, int episodeTvdbId, int seasonNumber,
            int episodeNumber, boolean isWatched) {
        EpisodeTools.episodeWatched(SgApp.from(getActivity()), showTvdbId, episodeTvdbId,
                seasonNumber, episodeNumber,
                isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int episodeId = (int) id;

        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

        Utils.startActivityWithAnimation(getActivity(), intent, view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
            final long id) {
        if (!isResumed()) {
            // guard against being called after fragment is paged away (multi-touch)
            // adapter cursor might no longer have data
            return false;
        }

        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        Menu menu = popupMenu.getMenu();

        Cursor episode = adapter.getItem(position);
        if (episode == null) {
            return false;
        }

        // only display the action appropriate for the items current state
        menu.add(0, CONTEXT_CHECKIN_ID, 0, R.string.checkin);
        if (EpisodeTools.isWatched(episode.getInt(CalendarAdapter.Query.WATCHED))) {
            menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 1, R.string.action_unwatched);
        } else {
            menu.add(0, CONTEXT_FLAG_WATCHED_ID, 1, R.string.action_watched);
        }
        if (EpisodeTools.isCollected(episode.getInt(CalendarAdapter.Query.COLLECTED))) {
            menu.add(0, CONTEXT_COLLECTION_REMOVE_ID, 2, R.string.action_collection_remove);
        } else {
            menu.add(0, CONTEXT_COLLECTION_ADD_ID, 2, R.string.action_collection_add);
        }

        final int showTvdbId = episode.getInt(CalendarAdapter.Query.SHOW_ID);
        final int episodeTvdbId = episode.getInt(CalendarAdapter.Query._ID);
        final int seasonNumber = episode.getInt(CalendarAdapter.Query.SEASON);
        final int episodeNumber = episode.getInt(CalendarAdapter.Query.NUMBER);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case CONTEXT_CHECKIN_ID: {
                        checkInEpisode((int) id);
                        return true;
                    }
                    case CONTEXT_FLAG_WATCHED_ID: {
                        updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, true);
                        return true;
                    }
                    case CONTEXT_FLAG_UNWATCHED_ID: {
                        updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, false);
                        return true;
                    }
                    case CONTEXT_COLLECTION_ADD_ID: {
                        updateEpisodeCollectionState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, true);
                        return true;
                    }
                    case CONTEXT_COLLECTION_REMOVE_ID: {
                        updateEpisodeCollectionState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, false);
                        return true;
                    }
                }
                return false;
            }
        });

        popupMenu.show();

        return true;
    }

    public void onRequery() {
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    private int getLoaderId() {
        return getArguments().getInt("loaderid");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if ((CalendarType.UPCOMING.equals(type)
                && event.position == ShowsActivity.InitBundle.INDEX_TAB_UPCOMING) ||
                CalendarType.RECENT.equals(type)
                        && event.position == ShowsActivity.InitBundle.INDEX_TAB_RECENT) {
            gridView.smoothScrollToPosition(0);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        boolean isInfiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());

        // infinite or 30 days activity stream
        String[][] queryArgs = DBUtils.buildActivityQuery(getActivity(), type,
                isInfiniteScrolling ? -1 : 30);

        // prevent upcoming/recent episodes from becoming stale
        schedulePeriodicDataRefresh(true);

        return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                CalendarAdapter.Query.PROJECTION, queryArgs[0][0], queryArgs[1], queryArgs[2][0]);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private void schedulePeriodicDataRefresh(boolean enableRefresh) {
        if (handler == null) {
            handler = new Handler();
        }
        handler.removeCallbacks(mDataRefreshRunnable);
        if (enableRefresh) {
            handler.postDelayed(mDataRefreshRunnable, 5 * DateUtils.MINUTE_IN_MILLIS);
        }
    }

    private Runnable mDataRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                getLoaderManager().restartLoader(getLoaderId(), null, CalendarFragment.this);
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CalendarSettings.KEY_INFINITE_SCROLLING.equals(key)) {
            boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
            adapter.setIsShowingHeaders(!infiniteScrolling);
            gridView.setFastScrollEnabled(infiniteScrolling);
        }
        if (CalendarSettings.KEY_ONLY_FAVORITE_SHOWS.equals(key)
                || DisplaySettings.KEY_HIDE_SPECIALS.equals(key)
                || DisplaySettings.KEY_NO_WATCHED_EPISODES.equals(key)
                || CalendarSettings.KEY_INFINITE_SCROLLING.equals(key)) {
            onRequery();
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void toggleFilterSetting(MenuItem item, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(key, !item.isChecked()).apply();

        // refresh filter icon state
        getActivity().supportInvalidateOptionsMenu();
    }
}
