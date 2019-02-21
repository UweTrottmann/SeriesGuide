package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Videos;
import com.uwetrottmann.tmdb2.enumerations.VideoType;
import com.uwetrottmann.tmdb2.services.MoviesService;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads a YouTube movie trailer from TMDb. Tries to get a local trailer, if not falls back to
 * English.
 */
class MovieTrailersLoader extends GenericSimpleLoader<Videos.Video> {

    private int tmdbId;

    MovieTrailersLoader(Context context, int tmdbId) {
        super(context);
        this.tmdbId = tmdbId;
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
        MoviesService moviesService = SgApp.getServicesComponent(getContext()).moviesService();
        try {
            Response<Videos> response = moviesService.videos(tmdbId, language).execute();
            if (response.isSuccessful()) {
                return extractTrailer(response.body());
            } else {
                Errors.logAndReport(action, response);
            }
        } catch (Exception e) {
            Errors.logAndReport(action, e);
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
            if (video.type == VideoType.TRAILER && "YouTube".equals(video.site)
                    && !TextUtils.isEmpty(video.key)) {
                return video;
            }
        }

        return null;
    }
}
