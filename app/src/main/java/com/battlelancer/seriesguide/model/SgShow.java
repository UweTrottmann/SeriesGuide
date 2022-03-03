package com.battlelancer.seriesguide.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

@Entity(tableName = Tables.SHOWS)
public class SgShow {

    @PrimaryKey
    @ColumnInfo(name = Shows._ID)
    public int tvdbId;

    @ColumnInfo(name = Shows.SLUG)
    public String slug = "";

    /**
     * Ensure this is NOT null (enforced through database constraint).
     */
    @ColumnInfo(name = Shows.TITLE)
    @NonNull
    public String title = "";

    /**
     * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
     */
    @ColumnInfo(name = Shows.TITLE_NOARTICLE)
    public String titleNoArticle;

    @ColumnInfo(name = Shows.OVERVIEW)
    public String overview = "";

    /**
     * Local release time. Encoded as integer (hhmm).
     *
     * <pre>
     * Example: 2035
     * Default: -1
     * </pre>
     */
    @ColumnInfo(name = Shows.RELEASE_TIME)
    public Integer releaseTime;
    /**
     * Local release week day. Encoded as integer.
     * <pre>
     * Range:   1-7
     * Daily:   0
     * Default: -1
     * </pre>
     */
    @ColumnInfo(name = Shows.RELEASE_WEEKDAY)
    public Integer releaseWeekDay;
    @ColumnInfo(name = Shows.RELEASE_COUNTRY)
    public String releaseCountry;
    @ColumnInfo(name = Shows.RELEASE_TIMEZONE)
    public String releaseTimeZone;

    @ColumnInfo(name = Shows.FIRST_RELEASE)
    public String firstRelease;

    @ColumnInfo(name = Shows.GENRES)
    public String genres = "";
    @ColumnInfo(name = Shows.NETWORK)
    public String network = "";

    @ColumnInfo(name = Shows.RATING_GLOBAL)
    public Double ratingGlobal;
    @ColumnInfo(name = Shows.RATING_VOTES)
    public Integer ratingVotes;
    @ColumnInfo(name = Shows.RATING_USER)
    public Integer ratingUser;

    @ColumnInfo(name = Shows.RUNTIME)
    public String runtime = "";
    @ColumnInfo(name = Shows.STATUS)
    public String status = "";
    @ColumnInfo(name = Shows.CONTENTRATING)
    public String contentRating = "";

    @ColumnInfo(name = Shows.NEXTEPISODE)
    public String nextEpisode = "";

    @ColumnInfo(name = Shows.POSTER)
    public String poster = "";

    @ColumnInfo(name = Shows.POSTER_SMALL)
    public String posterSmall = "";

    @ColumnInfo(name = Shows.NEXTAIRDATEMS)
    public Long nextAirdateMs;
    @ColumnInfo(name = Shows.NEXTTEXT)
    public String nextText = "";

    @ColumnInfo(name = Shows.IMDBID)
    public String imdbId = "";
    @ColumnInfo(name = Shows.TRAKT_ID)
    public Integer traktId = 0;

    @ColumnInfo(name = Shows.FAVORITE)
    public boolean favorite = false;

    @ColumnInfo(name = Shows.HEXAGON_MERGE_COMPLETE)
    public boolean hexagonMergeComplete = true;

    @ColumnInfo(name = Shows.HIDDEN)
    public boolean hidden = false;

    @ColumnInfo(name = Shows.LASTUPDATED)
    public long lastUpdatedMs = 0L;
    @ColumnInfo(name = Shows.LASTEDIT)
    public long lastEditedSec = 0L;

    @ColumnInfo(name = Shows.LASTWATCHEDID)
    public int lastWatchedEpisodeId = 0;
    @ColumnInfo(name = Shows.LASTWATCHED_MS)
    public long lastWatchedMs = 0L;

    @ColumnInfo(name = Shows.LANGUAGE)
    public String language = "";

    @ColumnInfo(name = Shows.UNWATCHED_COUNT)
    public int unwatchedCount = SgShow2.UNKNOWN_UNWATCHED_COUNT;

    @ColumnInfo(name = Shows.NOTIFY)
    public boolean notify = true;
}
