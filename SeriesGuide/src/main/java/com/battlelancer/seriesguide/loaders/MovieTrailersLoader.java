/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.text.TextUtils;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.entities.Videos;
import com.uwetrottmann.tmdb.services.MoviesService;
import retrofit.RetrofitError;
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

        Videos videos;
        try {
            // try local trailer first
            videos = movieService.videos(mTmdbId, DisplaySettings.getContentLanguage(getContext()));
            Videos.Video trailer = extractTrailer(videos);
            if (trailer != null) {
                return trailer;
            }

            // fall back to default
            videos = movieService.videos(mTmdbId, null);
            return extractTrailer(videos);
        } catch (RetrofitError e) {
            Timber.e(e, "Downloading movie trailers failed");
            return null;
        }
    }

    private Videos.Video extractTrailer(Videos videos) {
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
