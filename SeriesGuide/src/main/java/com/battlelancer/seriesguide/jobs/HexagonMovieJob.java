package com.battlelancer.seriesguide.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.sync.NetworkJobProcessor;
import com.google.api.client.http.HttpResponseException;
import com.uwetrottmann.seriesguide.backend.movies.Movies;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_COLLECTION_ADD;
import static com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_COLLECTION_REMOVE;
import static com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_WATCHLIST_ADD;
import static com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_WATCHLIST_REMOVE;

public class HexagonMovieJob extends NetworkJob {

    @NonNull private final HexagonTools hexagonTools;

    public HexagonMovieJob(@NonNull HexagonTools hexagonTools, JobAction action,
            SgJobInfo jobInfo) {
        super(action, jobInfo);
        this.hexagonTools = hexagonTools;
    }

    @NonNull
    @Override
    public NetworkJobProcessor.JobResult execute(Context context) {
        MovieList uploadWrapper = new MovieList();
        uploadWrapper.setMovies(getMovieForHexagon());

        try {
            Movies moviesService = hexagonTools.getMoviesService();
            if (moviesService == null) {
                return buildResult(context, NetworkJob.ERROR_HEXAGON_AUTH);
            }
            moviesService.save(uploadWrapper).execute();
        } catch (HttpResponseException e) {
            HexagonTools.trackFailedRequest(context, "save movie", e);
            int code = e.getStatusCode();
            if (code >= 400 && code < 500) {
                return buildResult(context, NetworkJob.ERROR_HEXAGON_CLIENT);
            } else {
                return buildResult(context, NetworkJob.ERROR_HEXAGON_SERVER);
            }
        } catch (IOException e) {
            HexagonTools.trackFailedRequest(context, "save movie", e);
            return buildResult(context, NetworkJob.ERROR_CONNECTION);
        }

        return buildResult(context, NetworkJob.SUCCESS);
    }

    @NonNull
    private List<Movie> getMovieForHexagon() {
        Movie movie = new Movie();
        movie.setTmdbId(jobInfo.movieTmdbId());

        if (action == MOVIE_COLLECTION_ADD) {
            movie.setIsInCollection(true);
        } else if (action == MOVIE_COLLECTION_REMOVE) {
            movie.setIsInCollection(false);
        } else if (action == MOVIE_WATCHLIST_ADD) {
            movie.setIsInWatchlist(true);
        } else if (action == MOVIE_WATCHLIST_REMOVE) {
            movie.setIsInWatchlist(false);
        } else {
            throw new IllegalArgumentException("Action " + action + " not supported.");
        }

        List<Movie> movies = new ArrayList<>();
        movies.add(movie);
        return movies;
    }
}
