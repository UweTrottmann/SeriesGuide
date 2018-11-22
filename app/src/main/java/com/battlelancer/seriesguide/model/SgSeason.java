package com.battlelancer.seriesguide.model;

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

    @PrimaryKey
    @ColumnInfo(name = Seasons._ID)
    public Integer tvdbId;

    @ColumnInfo(name = Seasons.COMBINED)
    public Integer number;

    @ColumnInfo(name = ShowsColumns.REF_SHOW_ID)
    public String showTvdbId;

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
