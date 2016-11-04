package com.battlelancer.seriesguide.loaders;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Videos;
import com.uwetrottmann.tmdb2.services.MoviesService;
import java.io.IOException;
import javax.inject.Inject;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads a YouTube movie trailer from TMDb. Tries to get a local trailer, if not falls back to
 * English.
 */
public class MovieTrailersLoader extends GenericSimpleLoader<Videos.Video> {

    @Inject MoviesService moviesService;
    private int mTmdbId;

    public MovieTrailersLoader(SgApp app, int tmdbId) {
        super(app);
        app.getServicesComponent().inject(this);
        mTmdbId = tmdbId;
    }

    @Override
    public Videos.Video loadInBackground() {
        // try to get a local trailer
        Videos.Video trailer = getTrailer(
                DisplaySettings.getMoviesLanguage(getContext()), "get local movie trailer");
        if (trailer != null) {
            return trailer;
        }
        Timber.d("Did not find a local movie trailer.");

        // fall back to default language trailer
        return getTrailer(null, "get default movie trailer");
    }

    @Nullable
    private Videos.Video getTrailer(@Nullable String language, @NonNull String action) {
        try {
            Response<Videos> response = moviesService.videos(mTmdbId, language).execute();
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
