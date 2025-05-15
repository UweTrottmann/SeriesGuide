// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class Episode {

    /**
     * Required when importing.
     * <p>
     * It is currently still supported that a {@link #tvdb_id} is set instead if a
     * {@link Show#tvdb_id} is set, like in exports created before the TMDB migration.
     */
    @Nullable public Integer tmdb_id;
    /**
     * May be set instead of a {@link #tmdb_id}, but only if the show has a {@link Show#tvdb_id}
     * set.
     * <p>
     * May be exported for shows that were added before the TMDB migration.
     */
    @Nullable
    @SerializedName("tvdb_id")
    public Integer tvdbId;

    /**
     * Number of the episode within its season.
     */
    public int episode;

    /**
     * Only exported for shows added before the TMBD migration.
     * <p>
     * Some shows, mainly anime, use absolute episode numbers instead of the season/episode
     * grouping.
     */
    @Nullable
    @SerializedName("episode_absolute")
    public Integer episodeAbsolute;

    @Nullable
    public String title;

    /**
     * First release date in milliseconds.
     * <p>
     * This date time is based on the shows (custom) release time and time zone at the time this
     * episode was last updated. It includes country and time zone specific offsets (currently only
     * for US western time zones).
     * <p>
     * Default: -1
     */
    @SerializedName("first_aired")
    public long firstAired;

    /**
     * If this episode is watched.
     * <p>
     * Default: false
     */
    public boolean watched;

    /**
     * The number of times an episode was watched.
     * <p>
     * Depending on {@link #watched}, defaults to 1 or 0.
     * <p>
     * If 1 or greater and {@link #watched} is not {@code true}, is ignored when importing.
     */
    public int plays;

    /**
     * If this episode is skipped.
     * <p>
     * If this is {@code true}, the value of {@link #watched} is ignored.
     * <p>
     * Default: false
     */
    public boolean skipped;

    /**
     * Whether an episode has been added to the collection.
     * <p>
     * Default: false
     */
    public boolean collected;

    @Nullable
    @SerializedName("imdb_id")
    public String imdbId;

    /*
     * #####################################################
     * The following values are only optionally exported.
     * #####################################################
     */

    /**
     * The episode number if released on disc.
     * <p>
     * Only exported for shows added before the TMDB migration.
     */
    @Nullable
    @SerializedName("episode_dvd")
    public Double episodeDvd;

    @Nullable
    public String overview;

    /**
     * A TMDB episode image (still) path.
     */
    @Nullable
    public String image;

    /**
     * A pipe-separated list of writers.
     */
    @Nullable
    public String writers;
    /**
     * A pipe-separated list of guest stars.
     */
    @Nullable
    public String gueststars;
    /**
     * A pipe-separated list of directors.
     */
    @Nullable
    public String directors;

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
}
