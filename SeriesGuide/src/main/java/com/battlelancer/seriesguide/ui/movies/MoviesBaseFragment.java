package com.battlelancer.seriesguide.ui.movies;

import static com.battlelancer.seriesguide.ui.movies.MoviesDistillationSettings.MoviesSortOrder;
import static com.battlelancer.seriesguide.ui.movies.MoviesDistillationSettings.MoviesSortOrderChangedEvent;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A shell for a fragment displaying a number of movies.
 */
public abstract class MoviesBaseFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        MoviesCursorAdapter.PopupMenuClickListener {

    private static final int LAYOUT = R.layout.fragment_movies;

    private GridView gridView;
    TextView emptyView;

    MoviesCursorAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(LAYOUT, container, false);

        gridView = v.findViewById(R.id.gridViewMovies);
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher());
        emptyView = v.findViewById(R.id.textViewMoviesEmpty);
        gridView.setEmptyView(emptyView);
        gridView.setOnItemClickListener(this);

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

        adapter = new MoviesCursorAdapter(getContext(), this, getLoaderId());
        gridView.setAdapter(adapter);

        getLoaderManager().initLoader(getLoaderId(), null, this);

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

        inflater.inflate(R.menu.movies_lists_menu, menu);

        menu.findItem(R.id.menu_action_movies_sort_ignore_articles)
                .setChecked(DisplaySettings.isSortOrderIgnoringArticles(getContext()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_movies_sort_title) {
            if (MoviesDistillationSettings.getSortOrderId(getContext())
                    == MoviesSortOrder.TITLE_ALPHABETICAL_ID) {
                changeSortOrder(MoviesSortOrder.TITLE_REVERSE_ALHPABETICAL_ID);
            } else {
                // was sorted title reverse or by release date
                changeSortOrder(MoviesSortOrder.TITLE_ALPHABETICAL_ID);
            }
            return true;
        }
        if (itemId == R.id.menu_action_movies_sort_release) {
            if (MoviesDistillationSettings.getSortOrderId(getContext())
                    == MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID) {
                changeSortOrder(MoviesSortOrder.RELEASE_DATE_OLDEST_FIRST_ID);
            } else {
                // was sorted by oldest first or by title
                changeSortOrder(MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID);
            }
            return true;
        }
        if (itemId == R.id.menu_action_movies_sort_ignore_articles) {
            changeSortIgnoreArticles(!DisplaySettings.isSortOrderIgnoringArticles(getContext()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void changeSortOrder(int sortOrderId) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putInt(MoviesDistillationSettings.KEY_SORT_ORDER, sortOrderId)
                .apply();

        EventBus.getDefault().post(new MoviesSortOrderChangedEvent());
    }

    private void changeSortIgnoreArticles(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putBoolean(DisplaySettings.KEY_SORT_IGNORE_ARTICLE, value)
                .apply();

        // refresh icon state
        getActivity().invalidateOptionsMenu();

        EventBus.getDefault().post(new MoviesSortOrderChangedEvent());
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MoviesSortOrderChangedEvent event) {
        getLoaderManager().restartLoader(getLoaderId(), null, this);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor movie = (Cursor) adapter.getItem(position);
        int tmdbId = movie.getInt(MoviesCursorAdapter.MoviesQuery.TMDB_ID);

        // launch movie details activity
        Intent i = MovieDetailsActivity.intentMovie(getActivity(), tmdbId);

        MoviesCursorAdapter.ViewHolder viewHolder
                = (MoviesCursorAdapter.ViewHolder) view.getTag();
        Utils.startActivityWithAnimation(getActivity(), i, viewHolder.poster);
    }

    @Override
    public abstract void onPopupMenuClick(View v, int movieTmdbId);

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    /**
     * Return a loader id different from any other used within {@link com.battlelancer.seriesguide.ui.MoviesActivity}.
     */
    abstract int getLoaderId();
}
