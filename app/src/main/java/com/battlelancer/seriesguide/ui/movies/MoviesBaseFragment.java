package com.battlelancer.seriesguide.ui.movies;

import static com.battlelancer.seriesguide.ui.movies.MoviesDistillationSettings.MoviesSortOrderChangedEvent;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A shell for a fragment displaying a number of movies.
 */
public abstract class MoviesBaseFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LAYOUT = R.layout.fragment_movies;

    private GridView gridView;
    TextView emptyView;

    private MoviesCursorAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(LAYOUT, container, false);

        gridView = v.findViewById(R.id.gridViewMovies);
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true);
        emptyView = v.findViewById(R.id.textViewMoviesEmpty);
        gridView.setEmptyView(emptyView);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new MoviesCursorAdapter(getContext(), new MovieClickListener(requireContext()),
                getLoaderId());
        gridView.setAdapter(adapter);

        LoaderManager.getInstance(this).initLoader(getLoaderId(), null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        new MoviesOptionsMenu(requireContext()).create(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MoviesOptionsMenu menu = new MoviesOptionsMenu(requireContext());
        if (menu.onItemSelected(item, requireActivity())) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MoviesSortOrderChangedEvent event) {
        LoaderManager.getInstance(this).restartLoader(getLoaderId(), null, this);
    }

    /**
     * @return The current position in the tab strip.
     * @see MoviesActivity
     */
    abstract int getTabPosition(boolean showingNowTab);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(MoviesActivity.MoviesTabClickEvent event) {
        if (event.position == getTabPosition(event.showingNowTab)) {
            gridView.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    /**
     * Return a loader id different from any other used within {@link com.battlelancer.seriesguide.ui.MoviesActivity}.
     */
    abstract int getLoaderId();
}
