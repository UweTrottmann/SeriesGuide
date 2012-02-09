
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class SearchActivity extends SherlockListActivity {

    private static final String TAG = "SearchActivity";

    /**
     * Google Analytics helper method for easy event tracking.
     * 
     * @param label
     */
    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent(TAG, "Click", label, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        handleIntent(getIntent());

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        setTitle(R.string.search_title);
        actionBar.setTitle(R.string.search_title);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            AnalyticsUtils.getInstance(this).trackEvent(TAG, "Search action", "Search", 0);
            String query = intent.getStringExtra(SearchManager.QUERY);
            getSupportActionBar().setSubtitle("\"" + query + "\"");
            doMySearch(query);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            AnalyticsUtils.getInstance(this).trackEvent(TAG, "Search action", "View", 0);
            Uri data = intent.getData();
            String id = data.getLastPathSegment();
            onShowEpisodeDetails(id);
            finish();
        }
    }

    private void doMySearch(String query) {
        Cursor searchResults = getContentResolver().query(EpisodeSearch.CONTENT_URI_SEARCH,
                SearchQuery.PROJECTION, null, new String[] {
                    query
                }, null);
        startManagingCursor(searchResults);
        String[] from = new String[] {
                Episodes.TITLE, Episodes.OVERVIEW, Episodes.NUMBER, Episodes.WATCHED, Shows.TITLE
        };
        int[] to = new int[] {
                R.id.TextViewSearchRow, R.id.TextViewSearchSnippet, R.id.TextViewSearchEpNumbers,
                R.id.TextViewSearchEpWatchedState, R.id.TextViewSearchSeriesName
        };
        SimpleCursorAdapter resultsAdapter = new SimpleCursorAdapter(getApplicationContext(),
                R.layout.search_row, searchResults, from, to);
        resultsAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // disabled because it causes too much CPU stress
                // if (columnIndex ==
                // cursor.getColumnIndexOrThrow(SeriesGuideData.EPISODE_OVERVIEW))
                // {
                // TextView overview = (TextView)view;
                // String snippet = cursor.getString(columnIndex);
                // if
                // (snippet.contentEquals(cursor.getString(cursor.getColumnIndexOrThrow(SeriesGuideData.EPISODE_TITLE))))
                // {
                // overview.setVisibility(View.GONE);
                // } else {
                // overview.setText(snippet);
                // overview.setVisibility(View.VISIBLE);
                // }
                // return true;
                // }
                if (columnIndex == SearchQuery.NUMBER) {
                    TextView numbers = (TextView) view;
                    String epnumber = getString(R.string.episode) + " "
                            + cursor.getString(columnIndex);
                    String senumber = getString(R.string.season) + " "
                            + cursor.getString(SearchQuery.SEASON);
                    numbers.setText(senumber + " " + epnumber);
                    return true;
                }
                if (columnIndex == SearchQuery.WATCHED) {
                    boolean isWatched = (1 == cursor.getInt(columnIndex));
                    TextView watchedState = (TextView) view;
                    watchedState.setText(isWatched ? getString(R.string.episode_iswatched)
                            : getString(R.string.episode_notwatched));
                    watchedState.setTextColor(isWatched ? Color.GREEN : Color.GRAY);
                    return true;
                }
                if (columnIndex == SearchQuery.OVERVIEW) {
                    TextView watchedState = (TextView) view;
                    // make matched term bold
                    watchedState.setText(Html.fromHtml(cursor.getString(SearchQuery.OVERVIEW)));
                    return true;
                }

                return false;
            }
        });
        setListAdapter(resultsAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        onShowEpisodeDetails(String.valueOf(id));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.search_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final Intent intent = new Intent(this, ShowsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.home_enter, R.anim.home_exit);
                return true;
            case R.id.menu_search:
                fireTrackerEvent("Search");

                onSearchRequested();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShowEpisodeDetails(String id) {
        Intent i = new Intent(this, EpisodeDetailsActivity.class);
        i.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_ID, id);
        startActivity(i);
    }

    interface SearchQuery {
        String[] PROJECTION = new String[] {
                Episodes._ID, Episodes.TITLE, Episodes.OVERVIEW, Episodes.NUMBER, Episodes.SEASON,
                Episodes.WATCHED, Shows.TITLE
        };

        int _ID = 0;

        int TITLE = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int SHOW_TITLE = 6;
    }

}
