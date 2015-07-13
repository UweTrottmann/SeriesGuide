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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.uwetrottmann.tmdb.entities.Genre;
import java.util.List;

public class TmdbTools {

    public enum ProfileImageSize {

        W45("w45"),
        W185("w185"),
        H632("h632"),
        ORIGINAL("original");

        private final String value;

        private ProfileImageSize(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final String BASE_URL = "https://www.themoviedb.org/";
    private static final String PATH_MOVIES = "movie/";
    private static final String PATH_PERSON = "person/";

    /**
     * Tries to display the TMDb website of the given movie through a view intent.
     */
    public static void openTmdbMovie(Context context, int movieTmdbId, String logTag) {
        openTmdbUrl(context, buildMovieUrl(movieTmdbId), logTag);
    }

    /**
     * Tries to display the TMDb website of the given person through a view intent.
     */
    public static void openTmdbPerson(Context context, int personTmdbId, String logTag) {
        openTmdbUrl(context, buildPersonUrl(personTmdbId), logTag);
    }

    private static void openTmdbUrl(Context context, String url, String logTag) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, "TMDb");
    }

    private static String buildMovieUrl(int movieTmdbId) {
        return BASE_URL + PATH_MOVIES + movieTmdbId;
    }

    private static String buildPersonUrl(int personTmdbId) {
        return BASE_URL + PATH_PERSON + personTmdbId;
    }

    /**
     * Build url to a profile image using the given size spec and current TMDb image url (see {@link
     * com.battlelancer.seriesguide.settings.TmdbSettings#getImageBaseUrl(android.content.Context)}.
     */
    public static String buildProfileImageUrl(Context context, String path, ProfileImageSize size) {
        return TmdbSettings.getImageBaseUrl(context) + size + path;
    }

    /**
     * Builds a string listing all given genres by name, separated by comma.
     */
    public static String buildGenresString(List<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return null;
        }
        StringBuilder genresString = new StringBuilder();
        for (int i = 0; i < genres.size(); i++) {
            Genre genre = genres.get(i);
            genresString.append(genre.name);
            if (i + 1 < genres.size()) {
                genresString.append(", ");
            }
        }
        return genresString.toString();
    }
}
