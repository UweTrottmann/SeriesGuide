package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Videos;
import com.uwetrottmann.tmdb2.services.MoviesService;
import java.io.IOException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads a YouTube movie trailer from TMDb. Tries to get a local trailer, if not falls back to
 * English.
 */
public class MovieTrailersLoader extends GenericSimpleLoader<Videos.Video> {

    private int mTmdbId;

    public MovieTrailersLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public Videos.Video loadInBackground() {
        MoviesService movieService = ServiceUtils.getTmdb(getContext()).moviesService();

        // try to get a local trailer
        Videos.Video trailer = getTrailer(movieService,
                DisplaySettings.getContentLanguage(getContext()), "get local movie trailer");
        if (trailer != null) {
            return trailer;
        }
        Timber.d("Did not find a local movie trailer.");

        // fall back to default language trailer
        return getTrailer(movieService, null, "get default movie trailer");
    }

    @Nullable
    private Videos.Video getTrailer(@NonNull MoviesService movieService, @Nullable String language,
            @NonNull String action) {
        try {
            Response<Videos> response = movieService.videos(mTmdbId, language).execute();
            if (response.isSuccessful()) {
                return extractTrailer(response.body());
            } else {
                SgTmdb.trackFailedRequest(getContext(), action, response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), action, e);
        }
        return null;
    }

    @Nullable
    private Videos.Video extractTrailer(@Nullable Videos videos) {
        if (videos == null || videos.results == null || videos.results.size() == 0) {
            return null;
        }

        // fish out the first YouTube trailer
        for (Videos.Video video : videos.results) {
            if ("Trailer".equals(video.type) && "YouTube".equals(video.site)
                    && !TextUtils.isEmpty(video.key)) {
                return video;
            }
        }

        return null;
    }
}
