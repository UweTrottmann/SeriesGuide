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
    private static final String PATH_TV = "tv/";
    private static final String PATH_MOVIES = "movie/";
    private static final String PATH_PERSON = "person/";

    /**
     * Tries to display the TMDb website of the given movie through a view intent.
     */
    public static void openTmdbMovie(Context context, int movieTmdbId) {
        openTmdbUrl(context, buildMovieUrl(movieTmdbId));
    }

    /**
     * Tries to display the TMDb website of the given person through a view intent.
     */
    public static void openTmdbPerson(Context context, int personTmdbId) {
        openTmdbUrl(context, buildPersonUrl(personTmdbId));
    }

    private static void openTmdbUrl(Context context, String url) {
        Utils.launchWebsite(context, url);
    }

    public static String buildEpisodeUrl(int showTmdbId, int season, int episode) {
        return BASE_URL + PATH_TV + showTmdbId + "/season/" + season + "/episode/" + episode;
    }

    public static String buildShowUrl(int showTmdbId) {
        return BASE_URL + PATH_TV + showTmdbId;
    }

    public static String buildMovieUrl(int movieTmdbId) {
        return BASE_URL + PATH_MOVIES + movieTmdbId;
    }

    public static String buildPersonUrl(int personTmdbId) {
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
