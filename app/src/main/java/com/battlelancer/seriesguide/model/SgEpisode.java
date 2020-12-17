package com.battlelancer.seriesguide.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SeasonsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

@Entity(
        tableName = Tables.EPISODES,
        foreignKeys = {
                @ForeignKey(entity = SgShow.class,
                        parentColumns = Shows._ID, childColumns = ShowsColumns.REF_SHOW_ID)
        },
        indices = {
                @Index(SeasonsColumns.REF_SEASON_ID),
                @Index(ShowsColumns.REF_SHOW_ID)
        }
)
public class SgEpisode {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Episodes._ID)
    public int id;

    @ColumnInfo(name = SeasonsColumns.REF_SEASON_ID)
    public int seasonId;

    @ColumnInfo(name = ShowsColumns.REF_SHOW_ID)
    public int showId;

    @ColumnInfo(name = Episodes.TMDB_ID)
    public Integer tmdbId;

    @ColumnInfo(name = Episodes.TVDB_ID)
    public Integer tvdbId;

    @NonNull
    @ColumnInfo(name = Episodes.TITLE)
    public String title = "";
    @ColumnInfo(name = Episodes.OVERVIEW)
    public String overview;

    @ColumnInfo(name = Episodes.NUMBER)
    public int number = 0;
    @ColumnInfo(name = Episodes.SEASON)
    public int season = 0;
    @ColumnInfo(name = Episodes.ORDER)
    public int order;

    @ColumnInfo(name = Episodes.DVDNUMBER)
    public Double dvdNumber;

    @ColumnInfo(name = Episodes.WATCHED)
    public int watched = 0;
    @ColumnInfo(name = Episodes.PLAYS)
    public Integer plays = 0;

    @ColumnInfo(name = Episodes.DIRECTORS)
    public String directors = "";
    @ColumnInfo(name = Episodes.GUESTSTARS)
    public String guestStars = "";
    @ColumnInfo(name = Episodes.WRITERS)
    public String writers = "";
    @ColumnInfo(name = Episodes.IMAGE)
    public String image = "";

    @ColumnInfo(name = Episodes.FIRSTAIREDMS)
    public long firstReleasedMs = -1L;

    @ColumnInfo(name = Episodes.COLLECTED)
    public boolean collected = false;

    @ColumnInfo(name = Episodes.RATING_GLOBAL)
    public Double ratingGlobal;
    @ColumnInfo(name = Episodes.RATING_VOTES)
    public Integer ratingVotes;
    @ColumnInfo(name = Episodes.RATING_USER)
    public Integer ratingUser;

    @ColumnInfo(name = Episodes.IMDBID)
    public String imdbId = "";

    @ColumnInfo(name = Episodes.LAST_EDITED)
    public long lastEditedSec = 0L;

    @ColumnInfo(name = Episodes.ABSOLUTE_NUMBER)
    public Integer absoluteNumber;

    @ColumnInfo(name = Episodes.LAST_UPDATED)
    public long lastUpdatedSec = 0L;
}
