package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.Utils;

class MovieClickListener implements MoviesAdapter.ItemClickListener {

    private Context context;

    MovieClickListener(Context context) {
        this.context = context;
    }

    Context getContext() {
        return context;
    }

    @Override
    public void onClickMovie(int movieTmdbId, ImageView posterView) {
        if (movieTmdbId == -1) return;

        // launch details activity
        Intent intent = MovieDetailsActivity.intentMovie(getContext(), movieTmdbId);
        Utils.startActivityWithAnimation(getContext(), intent, posterView);
    }

    @Override
    public void onClickMovieMoreOptions(final int movieTmdbId, View anchor) {
        if (movieTmdbId == -1) return;

        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.inflate(R.menu.movies_popup_menu);

        // check if movie is already in watchlist or collection
        boolean isInWatchlist = false;
        boolean isInCollection = false;
        Cursor movie = getContext().getContentResolver().query(
                SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
                new String[] { SeriesGuideContract.Movies.IN_WATCHLIST,
                        SeriesGuideContract.Movies.IN_COLLECTION }, null, null, null
        );
        if (movie != null) {
            if (movie.moveToFirst()) {
                isInWatchlist = movie.getInt(0) == 1;
                isInCollection = movie.getInt(1) == 1;
            }
            movie.close();
        }

        Menu menu = popupMenu.getMenu();
        menu.findItem(R.id.menu_action_movies_watchlist_add).setVisible(!isInWatchlist);
        menu.findItem(R.id.menu_action_movies_watchlist_remove).setVisible(isInWatchlist);
        menu.findItem(R.id.menu_action_movies_collection_add).setVisible(!isInCollection);
        menu.findItem(R.id.menu_action_movies_collection_remove).setVisible(isInCollection);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_action_movies_watchlist_add: {
                    MovieTools.addToWatchlist(getContext(), movieTmdbId);
                    return true;
                }
                case R.id.menu_action_movies_watchlist_remove: {
                    MovieTools.removeFromWatchlist(getContext(), movieTmdbId);
                    return true;
                }
                case R.id.menu_action_movies_collection_add: {
                    MovieTools.addToCollection(getContext(), movieTmdbId);
                    return true;
                }
                case R.id.menu_action_movies_collection_remove: {
                    MovieTools.removeFromCollection(getContext(), movieTmdbId);
                    return true;
                }
            }
            return false;
        });
        popupMenu.show();
    }
}
