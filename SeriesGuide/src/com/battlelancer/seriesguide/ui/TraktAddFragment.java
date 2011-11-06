
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.SimpleCrypto;
import com.battlelancer.thetvdbapi.SearchResult;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.TvShow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TraktAddFragment extends AddFragment {

    public static TraktAddFragment newInstance(int position) {
        TraktAddFragment f = new TraktAddFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("traktlisttype", position);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
         * never use this here (on config change the view needed before removing
         * the fragment)
         */
        // if (container == null) {
        // return null;
        // }
        return inflater.inflate(R.layout.traktaddfragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // only create and fill a new adapter if there is no previous one
        // (e.g. after config/page changed)
        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<SearchResult>(getActivity(), R.layout.add_searchresult,
                    R.id.TextViewAddSearchResult, new ArrayList<SearchResult>());

            int type = getArguments().getInt("traktlisttype");
            new GetTraktShowsTask(getActivity()).execute(type);
        }
    }

    public class GetTraktShowsTask extends AsyncTask<Integer, Void, List<SearchResult>> {

        private static final int TRENDING = 1;

        private static final int RECOMMENDED = 2;

        private static final int LIBRARY = 3;

        private Context mContext;

        public GetTraktShowsTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<SearchResult> doInBackground(Integer... params) {
            int type = params[0];
            List<SearchResult> showList = new ArrayList<SearchResult>();

            ServiceManager manager = new ServiceManager();
            manager.setApiKey(Constants.TRAKT_API_KEY);

            List<TvShow> shows = new ArrayList<TvShow>();

            if (type == TRENDING) {
                try {
                    shows = manager.showService().trending().fire();
                } catch (Exception e) {
                    // we don't care
                }
            } else {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext
                        .getApplicationContext());
                final String username = prefs.getString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
                String password = prefs.getString(SeriesGuidePreferences.PREF_TRAKTPWD, "");
                try {
                    password = SimpleCrypto.decrypt(password, mContext);
                    manager.setAuthentication(username, password);

                    switch (type) {
                        case RECOMMENDED:
                            shows = manager.recommendationsService().shows().fire();
                            break;
                        case LIBRARY:
                            shows = manager.userService().libraryShowsAll(username).fire();
                            break;
                    }
                } catch (Exception e) {
                    // we don't care
                }
            }

            parseTvShowsToSearchResults(shows, showList);

            return showList;
        }

        @Override
        protected void onPostExecute(List<SearchResult> result) {
            setSearchResults(result);
        }
    }

    /**
     * Parse a list of {@link TvShow} objects to a list of {@link SearchResult}
     * objects.
     * 
     * @param inputList
     * @param outputList
     * @return
     */
    private static List<SearchResult> parseTvShowsToSearchResults(List<TvShow> inputList,
            List<SearchResult> outputList) {
        Iterator<TvShow> trendingit = inputList.iterator();
        while (trendingit.hasNext()) {
            TvShow tvShow = (TvShow) trendingit.next();
            SearchResult show = new SearchResult();
            show.setId(tvShow.getTvdbId());
            show.setSeriesName(tvShow.getTitle());
            show.setOverview(tvShow.getOverview());
            outputList.add(show);
        }
        return outputList;
    }
}
