package com.battlelancer.seriesguide.api;

import android.os.Bundle;
import java.util.Date;

public class Movie {
    private static final String KEY_TITLE = "title";
    private static final String KEY_IMDBID = "imdbid";
    private static final String KEY_TMDBID = "tmdbid";
    private static final String KEY_RELEASE_DATE = "releaseDate";

    private String title;
    private Integer tmdbId;
    private String imdbId;
    private Date releaseDate;

    private Movie() {
    }

    public String getTitle() {
        return title;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public String getImdbId() {
        return imdbId;
    }

    /**
     * The release date of the movie, or null if not known.
     */
    public Date getReleaseDate() {
        return releaseDate;
    }

    public static class Builder {
        private final Movie movie;

        public Builder() {
            movie = new Movie();
        }

        public Builder title(String movieTitle) {
            movie.title = movieTitle;
            return this;
        }

        public Builder tmdbId(Integer movieTmdbId) {
            movie.tmdbId = movieTmdbId;
            return this;
        }

        public Builder imdbId(String movieImdbId) {
            movie.imdbId = movieImdbId;
            return this;
        }

        public Builder releaseDate(Date releaseDate) {
            movie.releaseDate = releaseDate;
            return this;
        }

        public Movie build() {
            return movie;
        }
    }

    /**
     * Serializes this {@link Movie} object to a {@link Bundle} representation.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putInt(KEY_TMDBID, tmdbId);
        bundle.putString(KEY_IMDBID, imdbId);
        if (releaseDate != null) {
            bundle.putLong(KEY_RELEASE_DATE, releaseDate.getTime());
        }
        return bundle;
    }

    /**
     * Deserializes an {@link Movie} into a {@link Bundle} object.
     */
    public static Movie fromBundle(Bundle bundle) {
        long releaseDate = bundle.getLong(KEY_RELEASE_DATE, Long.MAX_VALUE);
        Builder builder = new Builder()
                .title(bundle.getString(KEY_TITLE))
                .tmdbId(bundle.getInt(KEY_TMDBID))
                .imdbId(bundle.getString(KEY_IMDBID))
                .releaseDate(releaseDate == Long.MAX_VALUE ? null : new Date(releaseDate));

        return builder.build();
    }
}
