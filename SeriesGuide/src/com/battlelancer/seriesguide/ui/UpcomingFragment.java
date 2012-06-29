
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.WatchedBox;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class UpcomingFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MARK_WATCHED_ID = 0;

    private static final int MARK_UNWATCHED_ID = 1;

    private SimpleCursorAdapter mAdapter;

    private boolean mDualPane;

    /**
     * Data which has to be passed when creating {@link UpcomingFragment}. All
     * Bundle extras are strings, except LOADER_ID and EMPTY_STRING_ID.
     */
    public interface InitBundle {
        String QUERY = "query";

        String SORTORDER = "sortorder";

        String ANALYTICS_TAG = "analyticstag";

        String LOADER_ID = "loaderid";

        String EMPTY_STRING_ID = "emptyid";
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(getArguments().getInt(InitBundle.EMPTY_STRING_ID)));

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFragment = getActivity().findViewById(R.id.fragment_details);
        mDualPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;

        setupAdapter();

        getActivity().getSupportLoaderManager().initLoader(getLoaderId(), null, this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(SeriesGuidePreferences.KEY_ONLYFAVORITES)
                    || key.equals(SeriesGuidePreferences.KEY_ONLY_SEASON_EPISODES)
                    || key.equals(SeriesGuidePreferences.KEY_NOWATCHED)) {
                onRequery();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        final String tag = getArguments().getString("analyticstag");
        AnalyticsUtils.getInstance(getActivity()).trackPageView(tag);
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);

        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // only display the action appropiate for the items current state
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        WatchedBox watchedBox = (WatchedBox) info.targetView.findViewById(R.id.watchedBoxUpcoming);
        if (watchedBox.isChecked()) {
            menu.add(0, MARK_UNWATCHED_ID, 1, R.string.unmark_episode);
        } else {
            menu.add(0, MARK_WATCHED_ID, 0, R.string.mark_episode);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case MARK_WATCHED_ID: {
                onMarkEpisode(info, true);
                return true;
            }
            case MARK_UNWATCHED_ID: {
                onMarkEpisode(info, false);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void onMarkEpisode(AdapterContextMenuInfo info, boolean isWatched) {
        DBUtils.markEpisode(getActivity(), String.valueOf(info.id), isWatched);

        Cursor items = (Cursor) mAdapter.getItem(info.position);
        DBUtils.markSeenOnTrakt(getActivity(), items.getInt(UpcomingQuery.REF_SHOW_ID),
                items.getInt(UpcomingQuery.SEASON), items.getInt(UpcomingQuery.NUMBER), isWatched);
    }

    private void setupAdapter() {

        String[] from = new String[] {
                Episodes.TITLE, Episodes.WATCHED, Episodes.NUMBER, Episodes.FIRSTAIREDMS,
                Shows.TITLE, Shows.NETWORK, Shows.POSTER
        };
        int[] to = new int[] {
                R.id.textViewUpcomingEpisode, R.id.watchedBoxUpcoming, R.id.textViewUpcomingNumber,
                R.id.textViewUpcomingAirdate, R.id.textViewUpcomingShow,
                R.id.textViewUpcomingNetwork, R.id.poster
        };

        mAdapter = new SlowAdapter(getActivity(), R.layout.upcoming_row, null, from, to, 0);

        setListAdapter(mAdapter);

        final ListView list = getListView();
        list.setFastScrollEnabled(true);
        list.setDivider(null);
        list.setSelector(R.drawable.list_selector_holo_dark);
        list.setClipToPadding(Utils.isHoneycombOrHigher() ? false : true);
        final float scale = getResources().getDisplayMetrics().density;
        int layoutPadding = (int) (10 * scale + 0.5f);
        int defaultPadding = (int) (8 * scale + 0.5f);
        list.setPadding(layoutPadding, layoutPadding, layoutPadding, defaultPadding);
        registerForContextMenu(list);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails((int) id);
    }

    private void showDetails(int episodeId) {
        if (mDualPane) {
            // Check if fragment is shown, create new if needed.
            EpisodeDetailsFragment detailsFragment = (EpisodeDetailsFragment) getFragmentManager()
                    .findFragmentById(R.id.fragment_details);
            if (detailsFragment == null || detailsFragment.getEpisodeId() != episodeId) {
                // Make new fragment to show this selection.
                detailsFragment = EpisodeDetailsFragment.newInstance(episodeId, true);

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.fragment_slide_right_enter,
                        R.anim.fragment_slide_right_exit);
                ft.replace(R.id.fragment_details, detailsFragment, "fragmentDetails").commit();
            }

        } else {
            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodeDetailsActivity.class);
            intent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID, episodeId);
            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.fragment_slide_left_enter,
                    R.anim.fragment_slide_left_exit);
        }
    }

    public void onRequery() {
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    private int getLoaderId() {
        return getArguments().getInt("loaderid");
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO look to merge this with DBUtils.buildActivityQuery
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        long fakeNow = Utils.getFakeCurrentTime(prefs);
        // go an hour back in time, so episodes move to recent one hour late
        fakeNow -= DateUtils.HOUR_IN_MILLIS;
        final String recentThreshold = String.valueOf(fakeNow);

        final String sortOrder = getArguments().getString("sortorder");
        String query = getArguments().getString("query");

        boolean isOnlyFavorites = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLYFAVORITES, false);
        String[] selectionArgs;
        if (isOnlyFavorites) {
            query += UpcomingQuery.SELECTION_ONLYFAVORITES;
            selectionArgs = new String[] {
                    recentThreshold, "0", "1"
            };
        } else {
            selectionArgs = new String[] {
                    recentThreshold, "0"
            };
        }

        // append nospecials selection if necessary
        boolean isNoSpecials = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLY_SEASON_EPISODES,
                false);
        if (isNoSpecials) {
            query += UpcomingQuery.SELECTION_NOSPECIALS;
        }

        boolean isNoWatched = prefs.getBoolean(SeriesGuidePreferences.KEY_NOWATCHED, false);
        if (isNoWatched) {
            query += UpcomingQuery.SELECTION_NOWATCHED;
        }

        return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, query, selectionArgs, sortOrder);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public interface UpcomingQuery {
        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.WATCHED,
                Episodes.NUMBER, Episodes.SEASON, Episodes.FIRSTAIREDMS, Shows.TITLE,
                Shows.AIRSTIME, Shows.NETWORK, Shows.POSTER, Shows.REF_SHOW_ID
        };

        String QUERY_UPCOMING = Episodes.FIRSTAIREDMS + ">=? AND " + Shows.HIDDEN + "=?";

        String QUERY_RECENT = Episodes.FIRSTAIREDMS + "<? AND " + Episodes.FIRSTAIREDMS + ">0 AND "
                + Shows.HIDDEN + "=?";

        String SELECTION_ONLYFAVORITES = " AND " + Shows.FAVORITE + "=?";

        String SELECTION_NOWATCHED = " AND " + Episodes.WATCHED + "=0";

        String SELECTION_NOSPECIALS = " AND " + Episodes.SEASON + "!=0";

        String SORTING_UPCOMING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
                + Episodes.NUMBER + " ASC";

        String SORTING_RECENT = Episodes.FIRSTAIREDMS + " DESC," + Shows.TITLE + " ASC,"
                + Episodes.NUMBER + " DESC";

        int _ID = 0;

        int TITLE = 1;

        int WATCHED = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int FIRSTAIREDMS = 5;

        int SHOW_TITLE = 6;

        int SHOW_AIRSTIME = 7;

        int SHOW_NETWORK = 8;

        int SHOW_POSTER = 9;

        int REF_SHOW_ID = 10;

    }

    private class SlowAdapter extends SimpleCursorAdapter {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        private SharedPreferences mPrefs;

        public SlowAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);

            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            final ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.textViewUpcomingEpisode);
                viewHolder.show = (TextView) convertView.findViewById(R.id.textViewUpcomingShow);
                viewHolder.watchedBox = (WatchedBox) convertView
                        .findViewById(R.id.watchedBoxUpcoming);
                viewHolder.number = (TextView) convertView
                        .findViewById(R.id.textViewUpcomingNumber);
                viewHolder.airdate = (TextView) convertView
                        .findViewById(R.id.textViewUpcomingAirdate);
                viewHolder.network = (TextView) convertView
                        .findViewById(R.id.textViewUpcomingNetwork);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            viewHolder.episode.setText(mCursor.getString(UpcomingQuery.TITLE));
            viewHolder.show.setText(mCursor.getString(UpcomingQuery.SHOW_TITLE));

            // watched box
            // save rowid to hand over to OnClick event listener
            final int showId = mCursor.getInt(UpcomingQuery.REF_SHOW_ID);
            final int seasonNumber = mCursor.getInt(UpcomingQuery.SEASON);
            final String episodeId = mCursor.getString(UpcomingQuery._ID);
            final int episodeNumber = mCursor.getInt(UpcomingQuery.NUMBER);
            viewHolder.watchedBox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    WatchedBox checkBox = (WatchedBox) v;
                    checkBox.toggle();
                    DBUtils.markEpisode(getActivity(), episodeId, checkBox.isChecked());
                    DBUtils.markSeenOnTrakt(getActivity(), showId, seasonNumber, episodeNumber,
                            checkBox.isChecked());
                }
            });
            viewHolder.watchedBox.setChecked(mCursor.getInt(UpcomingQuery.WATCHED) > 0);

            // season and episode number
            final String number = Utils.getEpisodeNumber(mPrefs, seasonNumber, episodeNumber);
            viewHolder.number.setText(number);

            // airdate and time
            final long airtime = mCursor.getLong(UpcomingQuery.FIRSTAIREDMS);
            String network = "";
            if (airtime != -1) {
                String[] timeAndDay = Utils.formatToTimeAndDay(airtime, mContext);
                viewHolder.airdate.setText(timeAndDay[2]);
                network = timeAndDay[1] + " " + timeAndDay[0] + " ";
            } else {
                viewHolder.airdate.setText("");
            }

            // add network
            final String value = mCursor.getString(UpcomingQuery.SHOW_NETWORK);
            if (value.length() != 0) {
                network += getString(R.string.show_network) + " " + value;
            }
            viewHolder.network.setText(network);

            // set poster
            final String imagePath = mCursor.getString(UpcomingQuery.SHOW_POSTER);
            ImageProvider.getInstance(mContext).loadPosterThumb(viewHolder.poster, imagePath);

            return convertView;
        }
    }

    static class ViewHolder {

        public TextView show;

        public TextView episode;

        public WatchedBox watchedBox;

        public TextView number;

        public TextView airdate;

        public TextView network;

        public ImageView poster;
    }
}
