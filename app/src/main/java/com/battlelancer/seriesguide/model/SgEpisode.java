package com.battlelancer.seriesguide.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SeasonsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

@Entity(
        tableName = Tables.EPISODES,
        foreignKeys = {
                @ForeignKey(entity = SgSeason.class,
                        parentColumns = Seasons._ID, childColumns = SeasonsColumns.REF_SEASON_ID),
                @ForeignKey(entity = SgShow.class,
                        parentColumns = Shows._ID, childColumns = ShowsColumns.REF_SHOW_ID)
        },
        indices = {
                @Index(SeasonsColumns.REF_SEASON_ID),
                @Index(ShowsColumns.REF_SHOW_ID)
        }
)
public class SgEpisode {

    @PrimaryKey
    @ColumnInfo(name = Episodes._ID)
    public Integer tvdbId;

    @NonNull
    @ColumnInfo(name = Episodes.TITLE)
    public String title = "";
    @ColumnInfo(name = Episodes.OVERVIEW)
    public String overview;

    @ColumnInfo(name = Episodes.NUMBER)
    public Integer number = 0;
    @ColumnInfo(name = Episodes.SEASON)
    public Integer season = 0;
    @ColumnInfo(name = Episodes.DVDNUMBER)
    public Double dvdNumber;

    @ColumnInfo(name = SeasonsColumns.REF_SEASON_ID)
    public String seasonTvdbId;
    @ColumnInfo(name = ShowsColumns.REF_SHOW_ID)
    public String showTvdbId;

    @ColumnInfo(name = Episodes.WATCHED)
    public Integer watched = 0;

    @ColumnInfo(name = Episodes.DIRECTORS)
    public String directors = "";
    @ColumnInfo(name = Episodes.GUESTSTARS)
    public String guestStars = "";
    @ColumnInfo(name = Episodes.WRITERS)
    public String writers = "";
    @ColumnInfo(name = Episodes.IMAGE)
    public String image = "";

    @ColumnInfo(name = Episodes.FIRSTAIREDMS)
    public Long firstReleasedMs = -1L;

    @ColumnInfo(name = Episodes.COLLECTED)
    public Boolean collected = false;

    @ColumnInfo(name = Episodes.RATING_GLOBAL)
    public Double ratingGlobal;
    @ColumnInfo(name = Episodes.RATING_VOTES)
    public Integer ratingVotes;
    @ColumnInfo(name = Episodes.RATING_USER)
    public Integer ratingUser;

    @ColumnInfo(name = Episodes.IMDBID)
    public String imdbId = "";

    @ColumnInfo(name = Episodes.LAST_EDITED)
    public Long lastEditedSec = 0L;

    @ColumnInfo(name = Episodes.ABSOLUTE_NUMBER)
    public Integer absoluteNumber;

    @ColumnInfo(name = Episodes.LAST_UPDATED)
    public Long lastUpdatedSec = 0L;
}
