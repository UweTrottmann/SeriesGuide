
package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.adapters.MoviesWatchListAdapter;
import com.battlelancer.seriesguide.loaders.TraktMoviesWatchlistLoader;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

public class MoviesWatchListFragment extends SherlockFragment implements
        LoaderCallbacks<List<Movie>> {

    private MoviesWatchListAdapter mAdapter;
    private GridView mGridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.movies_watchlist_fragment, container, false);

        mGridView = (GridView) v.findViewById(R.id.gridViewMoviesWatchlist);
        mGridView.setEmptyView(v.findViewById(R.id.textViewMoviesWatchlistEmpty));

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new MoviesWatchListAdapter(getActivity());

        mGridView.setAdapter(mAdapter);

        getLoaderManager().initLoader(R.layout.movies_watchlist_fragment, null, this);
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int loaderId, Bundle args) {
        return new TraktMoviesWatchlistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> data) {
        mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.setData(null);
    }
}
