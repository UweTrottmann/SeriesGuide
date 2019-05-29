package com.battlelancer.seriesguide.ui.movies;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.MoviesActivity;

/**
 * Displays a users collection of movies in a grid.
 */
public class MoviesCollectionFragment extends MoviesBaseFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        emptyView.setText(R.string.movies_collection_empty);

        return v;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(), Movies.CONTENT_URI,
                MoviesCursorAdapter.MoviesQuery.PROJECTION, Movies.SELECTION_COLLECTION, null,
                MoviesDistillationSettings.getSortQuery(getContext()));
    }

    @Override
    int getLoaderId() {
        return MoviesActivity.COLLECTION_LOADER_ID;
    }

    @Override
    int getTabPosition(boolean showingNowTab) {
        return showingNowTab
                ? MoviesActivity.TAB_POSITION_COLLECTION_WITH_NOW
                : MoviesActivity.TAB_POSITION_COLLECTION_DEFAULT;
    }
}
