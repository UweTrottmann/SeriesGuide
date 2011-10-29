
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.SeriesGuideData.SeasonSorting;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SupportActivity;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SeasonsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ID_MARK_ALL_WATCHED = 0;

    private static final int ID_MARK_ALL_UNWATCHED = 1;

    private static final int LOADER_ID = 1;

    private SeasonSorting sorting;

    private SimpleCursorAdapter mAdapter;

    public static SeasonsFragment newInstance(String showId) {
        SeasonsFragment f = new SeasonsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(BaseColumns._ID, showId);
        f.setArguments(args);

        return f;
    }

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent("Seasons", "Click", label, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Seasons");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setSelector(R.drawable.list_selector_holo_dark);

        updatePreferences();

        // populate list
        fillData();

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
        updateUnwatchedCounts(false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, ID_MARK_ALL_WATCHED, 0, R.string.mark_all);
        menu.add(0, ID_MARK_ALL_UNWATCHED, 1, R.string.unmark_all);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case ID_MARK_ALL_WATCHED:
                markSeasonEpisodes(info.id, true);
                return true;

            case ID_MARK_ALL_UNWATCHED:
                markSeasonEpisodes(info.id, false);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, android.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.seasonlist_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT >= 11) {
            final CharSequence[] items = getResources().getStringArray(R.array.sesorting);
            menu.findItem(R.id.menu_sesortby).setTitle(
                    getString(R.string.sort) + ": " + items[sorting.index()]);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_markall:
                fireTrackerEvent("Mark all seasons");

                markAllEpisodes(true);
                return true;
            case R.id.menu_unmarkall:
                fireTrackerEvent("Unmark all seasons");

                markAllEpisodes(false);
                return true;
            case R.id.menu_sesortby:
                fireTrackerEvent("Sort seasons");

                // Create and show the dialog.
                SortDialogFragment newFragment = SortDialogFragment.newInstance(sorting.index(),
                        R.array.sesorting);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                newFragment.show(ft, "sortSeasonsDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showEpisodes(position, String.valueOf(id));
    }

    private void fillData() {
        String[] from = new String[] {
                Seasons.COMBINED, Seasons.WATCHCOUNT, Seasons.TOTALCOUNT
        };
        int[] to = new int[] {
                R.id.TextViewSeasonListTitle, R.id.TextViewSeasonListWatchCount,
                R.id.seasonProgress
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.season_row, null, from, to,
                CursorAdapter.NO_SELECTION);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == SeasonsQuery.WATCHCOUNT) {
                    final TextView watchcount = (TextView) view;
                    String episodeCount = "";
                    final int count = cursor.getInt(SeasonsQuery.WATCHCOUNT);
                    final int unairedCount = cursor.getInt(SeasonsQuery.UNAIREDCOUNT);
                    final int noairdateCount = cursor.getInt(SeasonsQuery.NOAIRDATECOUNT);

                    // add strings for unwatched episodes
                    if (count == 0) {
                        // make sure there are no unchecked episodes that happen
                        // to have no airdate
                        if (noairdateCount == 0) {
                            episodeCount += getString(R.string.season_allwatched);
                        } else {
                            episodeCount += noairdateCount + " ";
                            if (noairdateCount == 1) {
                                episodeCount += getString(R.string.oneotherepisode);
                            } else {
                                episodeCount += getString(R.string.otherepisodes);
                            }
                        }
                        watchcount.setTextColor(Color.GRAY);
                    } else if (count == 1) {
                        episodeCount += count + " " + getString(R.string.season_onenotwatched);
                        watchcount.setTextColor(Color.WHITE);
                    } else {
                        episodeCount += count + " " + getString(R.string.season_watchcount);
                        watchcount.setTextColor(Color.WHITE);
                    }

                    // add strings for unaired episodes
                    if (unairedCount > 0) {
                        episodeCount += " (+" + unairedCount + " "
                                + getString(R.string.season_unaired) + ")";
                    }
                    watchcount.setText(episodeCount);

                    return true;
                }
                if (columnIndex == SeasonsQuery.TOTALCOUNT) {
                    final int count = cursor.getInt(SeasonsQuery.WATCHCOUNT);
                    final int unairedCount = cursor.getInt(SeasonsQuery.UNAIREDCOUNT);
                    final int noairdateCount = cursor.getInt(SeasonsQuery.NOAIRDATECOUNT);
                    final int max = cursor.getInt(SeasonsQuery.TOTALCOUNT);
                    final int progress = max - count - unairedCount - noairdateCount;
                    final ProgressBar bar = (ProgressBar) view.findViewById(R.id.seasonProgressBar);
                    final TextView text = (TextView) view.findViewById(R.id.seasonProgressText);
                    bar.setMax(max);
                    bar.setProgress(progress);
                    text.setText(progress + "/" + max);
                    return true;
                }
                if (columnIndex == SeasonsQuery.COMBINED) {
                    final TextView seasonNameTextView = (TextView) view;
                    final String seasonNumber = cursor.getString(SeasonsQuery.COMBINED);
                    final String seasonName;
                    if (seasonNumber.equals("0") || seasonNumber.length() == 0) {
                        seasonName = getString(R.string.specialseason);
                    } else {
                        seasonName = getString(R.string.season) + " " + seasonNumber;
                    }
                    seasonNameTextView.setText(seasonName);

                    return true;
                }
                return false;
            }
        });
        setListAdapter(mAdapter);

        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    private String getShowId() {
        return getArguments().getString(Shows._ID);
    }

    private void showEpisodes(int position, String seasonid) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(Seasons._ID, seasonid);
        intent.putExtra(Shows.REF_SHOW_ID, getShowId());

        Cursor item = (Cursor) (getListView().getItemAtPosition(position));
        String season = item.getString(item.getColumnIndexOrThrow(Seasons.COMBINED));
        if (season.equals("0") || season.length() == 0) {
            season = getString(R.string.specialseason);
        } else {
            season = getString(R.string.season) + " " + season;
        }

        intent.putExtra(Intent.EXTRA_TITLE, season);
        startActivity(intent);
    }

    /**
     * Mark all episodes of the given season, updates the status label of the
     * season.
     * 
     * @param seasonid
     * @param state
     */
    private void markSeasonEpisodes(long seasonid, boolean state) {
        SeriesDatabase.markSeasonEpisodes(getActivity(), String.valueOf(seasonid), state);
        Thread t = new UpdateUnwatchThread(getShowId(), String.valueOf(seasonid), true);
        t.start();
    }

    /**
     * Mark all episodes of the given show, updates the status labels of the
     * season.
     * 
     * @param seasonid
     * @param state
     */
    private void markAllEpisodes(boolean state) {
        ContentValues values = new ContentValues();
        values.put(Episodes.WATCHED, state);
        getActivity().getContentResolver().update(Episodes.buildEpisodesOfShowUri(getShowId()),
                values, null, null);
        updateUnwatchedCounts(true);
    }

    /**
     * Update unwatched stats for all seasons of this fragments show. Requeries
     * the list afterwards.
     */
    protected void updateUnwatchedCounts(boolean updateOverview) {
        Thread t = new UpdateUnwatchThread(getShowId(), updateOverview);
        t.start();
    }

    private class UpdateUnwatchThread extends Thread {
        private String mSeasonId;

        private String mShowId;

        private boolean mUpdateOverview;

        public UpdateUnwatchThread(String showId, String seasonid, boolean updateOverview) {
            this(showId, updateOverview);
            mSeasonId = seasonid;
        }

        public UpdateUnwatchThread(String showId, boolean updateOverview) {
            mShowId = showId;
            mUpdateOverview = updateOverview;
            this.setName("UpdateWatchStatsThread");
        }

        public void run() {
            final SupportActivity context = getSupportActivity();
            if (context == null) {
                return;
            }

            if (mSeasonId != null) {
                // update one season
                SeriesDatabase.updateUnwatchedCount(context.asActivity(), mSeasonId);
            } else {
                // update all seasons of this show
                final Cursor seasons = context.getContentResolver().query(
                        Seasons.buildSeasonsOfShowUri(mShowId), new String[] {
                            Seasons._ID
                        }, null, null, null);
                while (seasons.moveToNext()) {
                    String seasonId = seasons.getString(0);
                    SeriesDatabase.updateUnwatchedCount(context.asActivity(), seasonId);
                }
                seasons.close();
            }

            context.getContentResolver().notifyChange(Seasons.buildSeasonsOfShowUri(mShowId), null);

            if (mUpdateOverview) {
                OverviewFragment overview = (OverviewFragment) context.getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_overview);
                if (overview != null) {
                    overview.onLoadEpisode();
                }
            }
        }
    }

    private void updatePreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity()
                .getApplicationContext());
        if (prefs.getString("seasonSorting", "latestfirst").equals("latestfirst")) {
            sorting = SeriesGuideData.SeasonSorting.LATEST_FIRST;
        } else {
            sorting = SeriesGuideData.SeasonSorting.OLDEST_FIRST;
        }
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Seasons.buildSeasonsOfShowUri(getShowId()),
                SeasonsQuery.PROJECTION, null, null, sorting.query());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private interface SeasonsQuery {
        String[] PROJECTION = {
                BaseColumns._ID, Seasons.COMBINED, Seasons.WATCHCOUNT, Seasons.UNAIREDCOUNT,
                Seasons.NOAIRDATECOUNT, Seasons.TOTALCOUNT
        };

        // int _ID = 0;

        int COMBINED = 1;

        int WATCHCOUNT = 2;

        int UNAIREDCOUNT = 3;

        int NOAIRDATECOUNT = 4;

        int TOTALCOUNT = 5;
    }

    public static class SortDialogFragment extends DialogFragment {

        /**
         * Creates a new sorting {@link DialogFragment} with posibilities of
         * {@code sortingArray} and selected value set to {@code index}.
         * 
         * @param index
         * @param sortingArray
         * @return
         */
        public static SortDialogFragment newInstance(int index, int sortingArray) {
            SortDialogFragment f = new SortDialogFragment();
            Bundle args = new Bundle();
            args.putInt("index", index);
            args.putInt("sortingarray", sortingArray);
            f.setArguments(args);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final CharSequence[] items = getResources().getStringArray(
                    getArguments().getInt("sortingarray"));

            return new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.pref_seasonsorting))
                    .setSingleChoiceItems(items, getArguments().getInt("index"),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int item) {
                                    SeasonsFragment f = (SeasonsFragment) getFragmentManager()
                                            .findFragmentById(R.id.fragment_seasons);
                                    if (f == null) {
                                        f = (SeasonsFragment) getFragmentManager()
                                                .findFragmentById(R.id.root_container);
                                    }
                                    f.updateSorting(item);
                                    dismiss();
                                }
                            }).create();
        }
    }

    private void updateSorting(int item) {
        sorting = (SeriesGuideData.SeasonSorting.values())[item];
        AnalyticsUtils.getInstance(getActivity()).trackEvent("Seasons", "Sorting", sorting.name(),
                0);

        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(
                getActivity()).edit();
        prefEditor.putString("seasonSorting",
                (getResources().getStringArray(R.array.sesortingData))[item]);
        prefEditor.commit();
        getLoaderManager().restartLoader(LOADER_ID, null, SeasonsFragment.this);

        if (Build.VERSION.SDK_INT >= 11) {
            getActivity().invalidateOptionsMenu();
        }
    }
}
