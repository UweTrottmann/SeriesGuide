package com.battlelancer.seriesguide.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
        tableName = Tables.MOVIES,
        indices = {@Index(value = Movies.TMDB_ID, unique = true)}
)
public class SgMovie {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Movies._ID)
    public Integer id;

    @ColumnInfo(name = Movies.TMDB_ID)
    public int tmdbId;

    @ColumnInfo(name = Movies.IMDB_ID)
    public String imdbId;

    @ColumnInfo(name = Movies.TITLE)
    public String title;
    @ColumnInfo(name = Movies.TITLE_NOARTICLE)
    public String titleNoArticle;

    @ColumnInfo(name = Movies.POSTER)
    public String poster;
    @ColumnInfo(name = Movies.GENRES)
    public String genres;
    @ColumnInfo(name = Movies.OVERVIEW)
    public String overview;
    @ColumnInfo(name = Movies.RELEASED_UTC_MS)
    public Long releasedMs;
    @ColumnInfo(name = Movies.RUNTIME_MIN)
    public Integer runtimeMin = 0;
    @ColumnInfo(name = Movies.TRAILER)
    public String trailer;
    @ColumnInfo(name = Movies.CERTIFICATION)
    public String certification;

    @ColumnInfo(name = Movies.IN_COLLECTION)
    public Boolean inCollection = false;
    @ColumnInfo(name = Movies.IN_WATCHLIST)
    public Boolean inWatchlist = false;
    @ColumnInfo(name = Movies.PLAYS)
    public Integer plays = 0;
    @ColumnInfo(name = Movies.WATCHED)
    public Boolean watched = false;

    @ColumnInfo(name = Movies.RATING_TMDB)
    public Double ratingTmdb = 0.0;
    @ColumnInfo(name = Movies.RATING_VOTES_TMDB)
    public Integer ratingVotesTmdb = 0;
    @ColumnInfo(name = Movies.RATING_TRAKT)
    public Integer ratingTrakt = 0;
    @ColumnInfo(name = Movies.RATING_VOTES_TRAKT)
    public Integer ratingVotesTrakt = 0;
    @ColumnInfo(name = Movies.RATING_USER)
    public Integer ratingUser;

    @ColumnInfo(name = Movies.LAST_UPDATED)
    public Long lastUpdated;

    public long getReleasedMsOrDefault() {
        return releasedMs != null ? releasedMs : Long.MAX_VALUE;
    }

    public int getRuntimeMinOrDefault() {
        return runtimeMin != null ? runtimeMin : 0;
    }

    public long getLastUpdatedOrDefault() {
        return lastUpdated != null ? lastUpdated : 0;
    }
}
