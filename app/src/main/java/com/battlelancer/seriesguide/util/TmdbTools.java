package com.battlelancer.seriesguide.util;

import android.content.Context;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.uwetrottmann.tmdb2.entities.Genre;
import java.util.List;

public class TmdbTools {

    public enum ProfileImageSize {

        W45("w45"),
        W185("w185"),
        H632("h632"),
        ORIGINAL("original");

        private final String value;

        ProfileImageSize(String value) {
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
        Utils.launchWebsite(context, url, logTag, "TMDb");
    }

    public static String buildMovieUrl(int movieTmdbId) {
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
