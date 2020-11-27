package com.battlelancer.seriesguide.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

@Entity(
        tableName = Tables.SEASONS,
        foreignKeys = @ForeignKey(entity = SgShow.class,
                parentColumns = Shows._ID, childColumns = ShowsColumns.REF_SHOW_ID),
        indices = {@Index(ShowsColumns.REF_SHOW_ID)}
)
public class SgSeason {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Seasons._ID)
    public int id;

    @ColumnInfo(name = ShowsColumns.REF_SHOW_ID)
    public int showId;

    @ColumnInfo(name = Seasons.TMDB_ID)
    public String tmdbId;

    @ColumnInfo(name = Seasons.TVDB_ID)
    public Integer tvdbId;

    @ColumnInfo(name = Seasons.COMBINED)
    public Integer number;

    @Nullable
    @ColumnInfo(name = Seasons.NAME)
    public String name;

    @ColumnInfo(name = Seasons.ORDER)
    public int order;

    @ColumnInfo(name = Seasons.WATCHCOUNT)
    public Integer watchCount = 0;

    @ColumnInfo(name = Seasons.UNAIREDCOUNT)
    public Integer notReleasedCount = 0;

    @ColumnInfo(name = Seasons.NOAIRDATECOUNT)
    public Integer noReleaseDateCount = 0;

    @ColumnInfo(name = Seasons.TAGS)
    public String tags = "";

    @ColumnInfo(name = Seasons.TOTALCOUNT)
    public Integer totalCount = 0;
}
