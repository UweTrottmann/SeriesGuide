package com.battlelancer.seriesguide.comments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.comments.TraktCommentsFragment.InitBundle;
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.Comment;
import com.uwetrottmann.trakt5.enums.Extended;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads up comments from trakt for a movie, show or episode.
 */
public class TraktCommentsLoader extends GenericSimpleLoader<TraktCommentsLoader.Result> {

    public static class Result {
        public List<Comment> results;
        public String emptyText;

        public Result(List<Comment> results, String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }
    }

    private static final int PAGE_SIZE = 25;

    private final Bundle args;
    @Inject TraktV2 trakt;

    TraktCommentsLoader(Context context, Bundle args) {
        super(context);
        this.args = args;
        SgApp.getServicesComponent(context).inject(this);
    }

    @Override
    public Result loadInBackground() {
        // movie comments?
        int movieTmdbId = args.getInt(InitBundle.MOVIE_TMDB_ID);
        if (movieTmdbId != 0) {
            Integer movieTraktId = TraktTools.lookupMovieTraktId(trakt, movieTmdbId);
            if (movieTraktId != null) {
                if (movieTraktId == -1) {
                    return buildResultFailure(R.string.trakt_error_not_exists);
                }
                try {
                    Response<List<Comment>> response = trakt.movies()
                            .comments(String.valueOf(movieTraktId), 1, PAGE_SIZE, Extended.FULL)
                            .execute();
                    if (response.isSuccessful()) {
                        return buildResultSuccess(response.body());
                    } else {
                        Errors.logAndReport("get movie comments", response);
                    }
                } catch (Exception e) {
                    Errors.logAndReport("get movie comments", e);
                }
            }
            return buildResultFailureWithOfflineCheck();
        }

        // episode comments?
        long episodeId = args.getLong(InitBundle.EPISODE_ID);
        if (episodeId != 0) {
            // look up episode number, season and show id
            SgRoomDatabase database = SgRoomDatabase.getInstance(getContext());
            SgEpisode2Numbers episode = database.sgEpisode2Helper()
                    .getEpisodeNumbers(episodeId);
            if (episode == null) {
                Timber.e("Failed to get episode %d", episodeId);
                return buildResultFailure(R.string.unknown);
            }
            // look up show trakt id
            Integer showTraktId = SgApp.getServicesComponent(getContext())
                    .showTools()
                    .getShowTraktId(episode.getShowId());
            if (showTraktId == null) {
                Timber.e("Failed to get show %d", episode.getShowId());
                return buildResultFailure(R.string.trakt_error_not_exists);
            }
            try {
                Response<List<Comment>> response = trakt.episodes()
                        .comments(
                        String.valueOf(showTraktId),
                        episode.getSeason(),
                        episode.getEpisodenumber(),
                        1, PAGE_SIZE, Extended.FULL
                ).execute();
                if (response.isSuccessful()) {
                    return buildResultSuccess(response.body());
                } else {
                    Errors.logAndReport("get episode comments", response);
                }
            } catch (Exception e) {
                Errors.logAndReport("get episode comments", e);
            }
            return buildResultFailureWithOfflineCheck();
        }

        // show comments!
        long showId = args.getLong(InitBundle.SHOW_ID);
        Integer showTraktId = SgApp.getServicesComponent(getContext())
                .showTools()
                .getShowTraktId(showId);
        if (showTraktId == null) {
            return buildResultFailure(R.string.trakt_error_not_exists);
        }
        try {
            Response<List<Comment>> response = trakt.shows()
                    .comments(String.valueOf(showTraktId), 1, PAGE_SIZE, Extended.FULL)
                    .execute();
            if (response.isSuccessful()) {
                return buildResultSuccess(response.body());
            } else {
                Errors.logAndReport("get show comments", response);
            }
        } catch (Exception e) {
            Errors.logAndReport("get show comments", e);
        }
        return buildResultFailureWithOfflineCheck();
    }

    private Result buildResultSuccess(List<Comment> results) {
        return new Result(results, getContext().getString(R.string.no_shouts));
    }

    private Result buildResultFailure(@StringRes int emptyTextResId) {
        return new Result(null, getContext().getString(emptyTextResId));
    }

    private Result buildResultFailureWithOfflineCheck() {
        String emptyText;
        if (AndroidUtils.isNetworkConnected(getContext())) {
            emptyText = getContext().getString(R.string.api_error_generic,
                    getContext().getString(R.string.trakt));
        } else {
            emptyText = getContext().getString(R.string.offline);
        }
        return new Result(null, emptyText);
    }
}
