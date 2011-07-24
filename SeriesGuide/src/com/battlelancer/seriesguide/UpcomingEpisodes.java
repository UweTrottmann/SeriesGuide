
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Date;

public class UpcomingEpisodes extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MARK_WATCHED_ID = 0;

    private static final int MARK_UNWATCHED_ID = 1;

    private static final int LOADER_ID_UPCOMING = 0;

    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upcoming);

        setupAdapter();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MARK_WATCHED_ID, 0, R.string.mark_episode);
        menu.add(0, MARK_UNWATCHED_ID, 1, R.string.unmark_episode);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case MARK_WATCHED_ID:
                SeriesDatabase.markEpisode(this, String.valueOf(info.id), true);
                return true;

            case MARK_UNWATCHED_ID:
                SeriesDatabase.markEpisode(this, String.valueOf(info.id), false);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        onTrackPageView();
    }

    protected void onTrackPageView() {
        AnalyticsUtils.getInstance(this).trackPageView("/Upcoming");
    }

    protected void setupAdapter() {

        String[] from = new String[] {
                Episodes.TITLE, Episodes.WATCHED, Episodes.NUMBER, Episodes.FIRSTAIRED,
                Shows.TITLE, Shows.NETWORK
        };
        int[] to = new int[] {
                R.id.textViewUpcomingEpisode, R.id.watchedBoxUpcoming, R.id.textViewUpcomingNumber,
                R.id.textViewUpcomingAirdate, R.id.textViewUpcomingShow,
                R.id.textViewUpcomingNetwork
        };

        mAdapter = new SimpleCursorAdapter(this, R.layout.upcoming_row, null, from, to, 0);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
                // set WatchedBox state and add onClickListener
                if (columnIndex == UpcomingQuery.WATCHED) {
                    WatchedBox wb = (WatchedBox) view;

                    // save rowid to hand over to OnClick event listener
                    final String rowid = cursor.getString(UpcomingQuery._ID);

                    wb.setOnClickListener(new OnClickListener() {

                        public void onClick(View v) {
                            ((WatchedBox) v).toggle();
                            SeriesDatabase.markEpisode(UpcomingEpisodes.this, rowid,
                                    ((WatchedBox) v).isChecked());
                        }
                    });

                    wb.setChecked(cursor.getInt(columnIndex) > 0);
                    return true;
                } else if (columnIndex == UpcomingQuery.NUMBER) {
                    // set season and episode number
                    TextView text = (TextView) view;
                    String episodeString = cursor.getString(UpcomingQuery.SEASON);
                    String number = cursor.getString(UpcomingQuery.NUMBER);
                    if (number.length() == 1) {
                        episodeString += "x0" + number;
                    } else {
                        episodeString += "x" + number;
                    }
                    text.setText(episodeString);
                    return true;
                } else if (columnIndex == UpcomingQuery.FIRSTAIRED) {
                    TextView tv = (TextView) view;

                    // set airdate
                    String fieldValue = cursor.getString(UpcomingQuery.FIRSTAIRED);
                    if (fieldValue.length() != 0) {
                        tv.setText(SeriesGuideData.parseDateToLocalRelative(fieldValue,
                                cursor.getLong(UpcomingQuery.SHOW_AIRSTIME),
                                getApplicationContext()));
                    } else {
                        tv.setText("");
                    }

                    return true;
                } else if (columnIndex == UpcomingQuery.SHOW_NETWORK) {
                    TextView tv = (TextView) view;
                    String fieldValue = "";

                    // add airtime
                    long airtime = cursor.getLong(UpcomingQuery.SHOW_AIRSTIME);
                    String value = SeriesGuideData.parseMillisecondsToTime(airtime, null, false,
                            getApplicationContext())[0];
                    if (value.length() != 0) {
                        fieldValue += value + " ";
                    }
                    // add network
                    value = cursor.getString(UpcomingQuery.SHOW_NETWORK);
                    if (value.length() != 0) {
                        fieldValue += getString(R.string.show_network) + " " + value;
                    }

                    tv.setText(fieldValue);
                    return true;
                }
                return false;
            }
        });

        AbsListView list = (AbsListView) findViewById(android.R.id.list);
        if (android.os.Build.VERSION.SDK_INT < 11) {
            ((ListView) list).setAdapter(mAdapter);
        } else {
            // only possible since API level 11 (Honeycomb)
            list.setAdapter(mAdapter);
        }

        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(onItemClickListener);
        View emptyView = findViewById(android.R.id.empty);
        if (emptyView != null) {
            list.setEmptyView(emptyView);
        }

        registerForContextMenu(list);

        getSupportLoaderManager().initLoader(LOADER_ID_UPCOMING, null, this);
    }

    protected final OnItemClickListener onItemClickListener = new OnItemClickListener() {

        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Intent i = new Intent(UpcomingEpisodes.this, EpisodeDetailsActivity.class);
            i.putExtra(Episodes._ID, String.valueOf(id));
            startActivity(i);
        }
    };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Date date = new Date();
        String today = SeriesGuideData.theTVDBDateFormat.format(date);
        return new CursorLoader(this, Episodes.CONTENT_URI_WITHSHOW, UpcomingQuery.PROJECTION,
                Episodes.FIRSTAIRED + ">=?", new String[] {
                    today
                }, UpcomingQuery.sortOrder);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    interface UpcomingQuery {
        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.WATCHED,
                Episodes.NUMBER, Episodes.SEASON, Episodes.FIRSTAIRED, Shows.TITLE, Shows.AIRSTIME,
                Shows.NETWORK, Shows.POSTER
        };

        String sortOrder = Episodes.FIRSTAIRED + " ASC," + Shows.AIRSTIME + " ASC," + Shows.TITLE
                + " ASC";

        int _ID = 0;

        int TITLE = 1;

        int WATCHED = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int FIRSTAIRED = 5;

        int SHOW_TITLE = 6;

        int SHOW_AIRSTIME = 7;

        int SHOW_NETWORK = 8;

        int SHOW_POSTER = 9;
    }

}
