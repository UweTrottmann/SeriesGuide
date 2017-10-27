package com.battlelancer.seriesguide.jobs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.MovieDetailsActivity;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;

public abstract class BaseNetworkMovieJob extends BaseNetworkJob {

    public BaseNetworkMovieJob(JobAction action, SgJobInfo jobInfo) {
        super(action, jobInfo);
    }

    @Nullable
    protected String getItemTitle(Context context) {
        int movieTmdbId = jobInfo.movieTmdbId();
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
                        SeriesGuideContract.Movies.PROJECTION_TITLE, null,
                        null, null);
        if (query == null) {
            return null;
        }
        if (!query.moveToFirst()) {
            query.close();
            return null;
        }
        String title = query.getString(
                query.getColumnIndexOrThrow(SeriesGuideContract.Movies.TITLE));
        query.close();
        return title;
    }

    @Nullable
    protected String getActionDescription(Context context) {
        switch (action) {
            case MOVIE_COLLECTION_ADD:
                return context.getString(R.string.action_collection_add);
            case MOVIE_COLLECTION_REMOVE:
                return context.getString(R.string.action_collection_remove);
            case MOVIE_WATCHLIST_ADD:
                return context.getString(R.string.watchlist_add);
            case MOVIE_WATCHLIST_REMOVE:
                return context.getString(R.string.watchlist_remove);
            case MOVIE_WATCHED_SET:
                return context.getString(R.string.action_watched);
            case MOVIE_WATCHED_REMOVE:
                return context.getString(R.string.action_unwatched);
            default:
                return null;
        }
    }

    @NonNull
    protected PendingIntent getErrorIntent(Context context) {
        // tapping the notification should open the affected movie
        return TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(new Intent(context, MoviesActivity.class))
                .addNextIntent(MovieDetailsActivity.intentMovie(context, jobInfo.movieTmdbId()))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
