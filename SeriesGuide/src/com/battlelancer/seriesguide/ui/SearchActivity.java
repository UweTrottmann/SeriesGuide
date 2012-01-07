
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import android.app.ListActivity;
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

public class SearchActivity extends ListActivity {

    private static final String TAG = "SearchSeriesGuide";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        handleIntent(getIntent());

        setTitle(getString(R.string.search_title));
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
                // disabled because it causes to much CPU stress
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
