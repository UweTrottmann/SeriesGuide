package com.battlelancer.seriesguide.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.SearchResultsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.Utils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays episode search results.
 */
public class EpisodeSearchFragment extends ListFragment {

    private SearchResultsAdapter adapter;

    interface InitBundle {
        /** Set to pre-filter search results by show title. */
        String SHOW_TITLE = "title";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new SearchResultsAdapter(getActivity());
        setListAdapter(adapter);
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
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), EpisodesActivity.class);
        i.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, (int) id);

        Utils.startActivityWithAnimation(getActivity(), i, v);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SearchActivity.SearchQueryEvent event) {
        search(event.args);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == SearchActivity.TAB_POSITION_EPISODES) {
            getListView().smoothScrollToPosition(0);
        }
    }

    public void search(Bundle args) {
        getLoaderManager().restartLoader(SearchActivity.EPISODES_LOADER_ID, args,
                searchLoaderCallbacks);
    }

    private LoaderCallbacks<Cursor> searchLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String selection = null;
            final String query = args.getString(SearchManager.QUERY);
            String[] selectionArgs = new String[] {
                    query
            };

            Bundle appData = args.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                String showtitle = appData.getString(InitBundle.SHOW_TITLE);
                // set show filter instead
                if (showtitle != null) {
                    selection = Shows.TITLE + "=?";
                    selectionArgs = new String[] {
                            query, showtitle
                    };
                }
            }

            return new CursorLoader(getActivity(), EpisodeSearch.CONTENT_URI_SEARCH,
                    SearchQuery.PROJECTION, selection, selectionArgs, null);
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

    public interface SearchQuery {
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
