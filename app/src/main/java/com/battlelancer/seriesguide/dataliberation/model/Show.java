// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;
import java.util.List;

/**
 * Model to import/export show data.
 */
public class Show {

    /**
     * Required when importing.
     * <p>
     * It is currently still supported that a {@link #tvdb_id} is set instead, like in exports
     * created before the TMDB migration.
     */
    @Nullable public Integer tmdb_id;
    /**
     * If set and there is no {@link #tmdb_id}, will migrate the show to TMDB data when it is next
     * updated (if a show on TMDB has this TVDB ID set as an external ID).
     * <p>
     * May be exported for shows that were added before the TMDB migration.
     */
    @Nullable public Integer tvdb_id;
    /**
     * Used to link to IMDB.
     */
    public String imdb_id;
    /**
     * Enables certain Trakt features.
     */
    public Integer trakt_id;

    public String title;
    public String overview;

    /**
     * One of the TMDB language codes SeriesGuide supports (see {@code content_languages} in
     * {@code app/src/main/res/values/donottranslate.xml}).
     * <p>
     * May be a two-letter language code in legacy exports, it is mapped to the expected format.
     */
    public String language;

    /**
     * ISO 8601 datetime string.
     */
    public String first_aired;
    /**
     * Local release time. Encoded as integer (hhmm).
     *
     * <pre>
     * Example: 2035
     * Default: -1
     * </pre>
     */
    public int release_time;
    /**
     * Local release week day. Encoded as integer.
     * <pre>
     * Range:   1-7
     * Daily:   0
     * Default: -1
     * </pre>
     */
    public int release_weekday;
    /**
     * Release time zone. Encoded as tzdata "Area/Location" string.
     *
     * <pre>
     * Example: "America/New_York"
     * Default: ""
     * </pre>
     */
    public String release_timezone;
    /**
     * Release country. Encoded as ISO3166-1 alpha-2 string.
     *
     * <pre>
     * Example: "us"
     * Default: ""
     * </pre>
     */
    public String country;

    /**
     * Custom local release time to override the actual one. Encoded as integer (hhmm). This being
     * set also determines if the other custom release time values (day offset, time zone) should be
     * used.
     * <p>
     * Example: 2035
     * <p>
     * Default: -1
     */
    @Nullable public Integer custom_release_time;
    /**
     * Positive or negative day offset to shift the release day of episodes by.
     * <p>
     * Default: 0
     * <p>
     * Range: -28 to 28
     */
    @Nullable public Integer custom_release_day_offset;
    /**
     * Custom time zone for computing an episode release time.
     * <p>
     * Same format as {@link #release_timezone}.
     * <p>
     * Default: ""
     */
    @Nullable public String custom_release_timezone;

    /**
     * TMDB poster path.
     */
    public String poster;

    /**
     * An age classification for shows that were added before the TMDB migration.
     */
    public String content_rating;
    /**
     * Show status.
     * <p>
     * One of the values supported by TMDB:
     *
     * <pre>
     * in_production
     * pilot
     * canceled
     * upcoming
     * continuing
     * ended
     * unknown
     * </pre>
     */
    public String status;
    /**
     * Typical length of an episode in minutes. Used to calculate watch time.
     * <p>
     * Default: 45
     */
    public int runtime;
    /**
     * A pipe-separated list of genres.
     */
    public String genres;
    public String network;

    /**
     * TMDB rating. Encoded as double.
     *
     * <pre>
     * Range:   0.0-10.0
     * Default: not included
     * </pre>
     */
    @Nullable public Double rating_tmdb;
    /**
     * TMDB rating number of votes.
     *
     * <pre>
     * Example: 42
     * Default: not included
     * </pre>
     */
    @Nullable public Integer rating_tmdb_votes;
    /**
     * Trakt rating. Encoded as double.
     *
     * <pre>
     * Range:   0.0-10.0
     * Default: not included
     * </pre>
     */
    @Nullable public Double rating;
    /**
     * Trakt rating number of votes.
     *
     * <pre>
     * Example: 42
     * Default: not included
     * </pre>
     */
    @Nullable public Integer rating_votes;
    /**
     * Trakt user rating. Encoded as integer.
     *
     * <pre>
     * Range:   0-10
     * Default: not included
     * </pre>
     */
    @Nullable public Integer rating_user;

    /**
     * Whether this show has been added to favorites.
     * <p>
     * Default: false
     */
    public boolean favorite;
    /**
     * Whether notifications for new episodes of this show should be shown.
     * <p>
     * Default: true
     */
    public Boolean notify;
    /**
     * Whether this show has been hidden.
     * <p>
     * Default: false
     */
    public boolean hidden;

    /**
     * The time in milliseconds an episode was last watched for this show.
     * <p>
     * Used to sort shows by last watched.
     */
    public long last_watched_ms;

    /**
     * A user editable text note for this show.
     * <p>
     * Default: not included
     * <p>
     * Should be at most 500 characters.
     */
    @Nullable public String user_note;
    /**
     * Trakt ID for {@link #user_note}.
     * <p>
     * Default: not included
     */
    @Nullable public Long user_note_trakt_id;

    public List<Season> seasons;
}
