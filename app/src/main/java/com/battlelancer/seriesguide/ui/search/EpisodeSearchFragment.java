package com.battlelancer.seriesguide.ui.search;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.Utils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays episode search results.
 */
public class EpisodeSearchFragment extends BaseSearchFragment {

    private EpisodeResultsAdapter adapter;

    public interface InitBundle {
        /** Set to pre-filter search results by show title. */
        String SHOW_TITLE = "title";
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // list items do not have right hand-side buttons, list may be long: enable fast scrolling
        gridView.setFastScrollAlwaysVisible(false);
        gridView.setFastScrollEnabled(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new EpisodeResultsAdapter(getActivity());
        gridView.setAdapter(adapter);

        // load for given query or restore last loader (ignoring args)
        getLoaderManager().initLoader(SearchActivity.EPISODES_LOADER_ID, loaderArgs,
                searchLoaderCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent i = new Intent(getActivity(), EpisodesActivity.class);
        i.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, (int) id);

        Utils.startActivityWithAnimation(getActivity(), i, view);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SearchActivity.SearchQueryEvent event) {
        getLoaderManager().restartLoader(SearchActivity.EPISODES_LOADER_ID, event.args,
                searchLoaderCallbacks);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == SearchActivity.TAB_POSITION_EPISODES) {
            gridView.smoothScrollToPosition(0);
        }
    }

    private LoaderCallbacks<Cursor> searchLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            loaderArgs = args;
            String selection = null;
            String query = args.getString(SearchManager.QUERY);
            if (TextUtils.isEmpty(query)) {
                query = "";
            }
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
                    SeriesGuideDatabase.EpisodeSearchQuery.PROJECTION, selection, selectionArgs,
                    null);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };
}
