
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.TvShow;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
            mAdapter = new AddAdapter(getActivity(), R.layout.add_searchresult,
                    new ArrayList<SearchResult>());

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

            List<TvShow> shows = new ArrayList<TvShow>();

            if (type == TRENDING) {
                try {
                    shows = Utils.getServiceManager(mContext).showService().trending().fire();
                } catch (Exception e) {
                    // we don't care
                }
            } else {
                try {
                    ServiceManager manager = Utils.getServiceManagerWithAuth(mContext, false);

                    switch (type) {
                        case RECOMMENDED:
                            shows = manager.recommendationsService().shows().fire();
                            break;
                        case LIBRARY:
                            shows = manager.userService()
                                    .libraryShowsAll(Utils.getTraktUsername(mContext)).fire();
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
            show.tvdbid = tvShow.tvdbId;
            show.title = tvShow.title;
            show.overview = tvShow.overview;
            outputList.add(show);
        }
        return outputList;
    }
}
