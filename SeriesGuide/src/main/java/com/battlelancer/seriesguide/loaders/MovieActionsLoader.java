package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.util.MovieTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tries returning existing actions for a movie. If no actions have been published, will ask
 * extensions to do so and returns an empty list.
 */
public class MovieActionsLoader extends GenericSimpleLoader<List<Action>> {

    private final int movieTmbdId;
    private Cursor query;

    public MovieActionsLoader(Context context, int movieTmdbId) {
        super(context);
        movieTmbdId = movieTmdbId;
    }

    @Override
    public List<Action> loadInBackground() {
        List<Action> actions = ExtensionManager.getInstance(getContext())
                .getLatestMovieActions(movieTmbdId);

        // no actions available yet, request extensions to publish them
        if (actions == null || actions.size() == 0) {
            actions = new ArrayList<>();

            query = getContext().getContentResolver().query(
                    Movies.buildMovieUri(movieTmbdId),
                    Query.PROJECTION, null, null, null);
            if (query == null) {
                return actions;
            }

            Movie movie = null;
            if (query.moveToFirst()) {
                movie = new Movie.Builder()
                        .tmdbId(query.getInt(Query.TMDB_ID))
                        .imdbId(query.getString(Query.IMDB_ID))
                        .title(query.getString(Query.TITLE))
                        .releaseDate(MovieTools.movieReleaseDateFrom(
                                query.getLong(Query.RELEASED_UTC_MS)))
                        .build();
            }
            // clean up query first
            query.close();
            query = null;

            if (movie != null) {
                ExtensionManager.getInstance(getContext()).requestMovieActions(movie);
            }
        }

        return actions;
    }

    @Override
    protected void onReleaseResources(List<Action> items) {
        if (query != null && !query.isClosed()) {
            query.close();
        }
    }

    private interface Query {
        String[] PROJECTION = {
                Movies.TMDB_ID,
                Movies.IMDB_ID,
                Movies.TITLE,
                Movies.RELEASED_UTC_MS
        };

        int TMDB_ID = 0;
        int IMDB_ID = 1;
        int TITLE = 2;
        int RELEASED_UTC_MS = 3;
    }
}
