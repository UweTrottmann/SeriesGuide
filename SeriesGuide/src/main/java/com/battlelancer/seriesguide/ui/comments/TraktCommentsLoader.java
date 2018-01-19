package com.battlelancer.seriesguide.ui.comments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.Comment;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.services.Episodes;
import com.uwetrottmann.trakt5.services.Movies;
import com.uwetrottmann.trakt5.services.Shows;
import dagger.Lazy;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads up comments from trakt for a movie (tvdbId arg is 0), show (episode arg is 0) or episode.
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

    private Bundle args;
    @Inject MovieTools movieTools;
    @Inject Lazy<Episodes> traktEpisodes;
    @Inject Lazy<Movies> traktMovies;
    @Inject Lazy<Shows> traktShows;

    TraktCommentsLoader(Context context, Bundle args) {
        super(context);
        this.args = args;
        SgApp.getServicesComponent(context).inject(this);
    }

    @Override
    public Result loadInBackground() {
        // movie comments?
        int movieTmdbId = args.getInt(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID);
        if (movieTmdbId != 0) {
            Integer movieTraktId = movieTools.lookupTraktId(movieTmdbId);
            if (movieTraktId != null) {
                if (movieTraktId == -1) {
                    return buildResultFailure(R.string.trakt_error_not_exists);
                }
                try {
                    Response<List<Comment>> response = traktMovies.get()
                            .comments(String.valueOf(movieTraktId), 1, PAGE_SIZE, Extended.FULL)
                            .execute();
                    if (response.isSuccessful()) {
                        return buildResultSuccess(response.body());
                    } else {
                        SgTrakt.trackFailedRequest(getContext(), "get movie comments",
                                response);
                    }
                } catch (IOException e) {
                    SgTrakt.trackFailedRequest(getContext(), "get movie comments", e);
                }
            }
            return buildResultFailureWithOfflineCheck();
        }

        // episode comments?
        int episodeTvdbId = args.getInt(TraktCommentsFragment.InitBundle.EPISODE_TVDB_ID);
        if (episodeTvdbId != 0) {
            // look up episode number, season and show id
            Cursor query = getContext().getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId),
                            new String[] { SeriesGuideContract.Episodes.SEASON,
                                    SeriesGuideContract.Episodes.NUMBER,
                                    SeriesGuideContract.Shows.REF_SHOW_ID }, null, null, null);
            int season = -1;
            int episode = -1;
            int showTvdbId = -1;
            if (query != null) {
                if (query.moveToFirst()) {
                    season = query.getInt(0);
                    episode = query.getInt(1);
                    showTvdbId = query.getInt(2);
                }
                query.close();
            }

            if (season != -1 && episode != -1 && showTvdbId != -1) {
                // look up show trakt id
                Integer showTraktId = ShowTools.getShowTraktId(getContext(), showTvdbId);
                if (showTraktId == null) {
                    return buildResultFailure(R.string.trakt_error_not_exists);
                }
                try {
                    Response<List<Comment>> response = traktEpisodes.get()
                            .comments(String.valueOf(showTraktId), season, episode,
                                    1, PAGE_SIZE, Extended.FULL)
                            .execute();
                    if (response.isSuccessful()) {
                        return buildResultSuccess(response.body());
                    } else {
                        SgTrakt.trackFailedRequest(getContext(), "get episode comments", response);
                    }
                } catch (IOException e) {
                    SgTrakt.trackFailedRequest(getContext(), "get episode comments", e);
                }
                return buildResultFailureWithOfflineCheck();
            } else {
                Timber.e("loadInBackground: could not find episode in database");
                return buildResultFailure(R.string.unknown);
            }
        }

        // show comments!
        int showTvdbId = args.getInt(TraktCommentsFragment.InitBundle.SHOW_TVDB_ID);
        Integer showTraktId = ShowTools.getShowTraktId(getContext(), showTvdbId);
        if (showTraktId == null) {
            return buildResultFailure(R.string.trakt_error_not_exists);
        }
        try {
            Response<List<Comment>> response = traktShows.get()
                    .comments(String.valueOf(showTraktId), 1, PAGE_SIZE, Extended.FULL)
                    .execute();
            if (response.isSuccessful()) {
                return buildResultSuccess(response.body());
            } else {
                SgTrakt.trackFailedRequest(getContext(), "get show comments", response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(getContext(), "get show comments", e);
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
