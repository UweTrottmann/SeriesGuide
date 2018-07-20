
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Season {

    @SerializedName("tvdb_id")
    public int tvdbId;

    public int season;

    public List<Episode> episodes;

    public ContentValues toContentValues(int showTvdbId) {
        ContentValues values = new ContentValues();
        values.put(Seasons._ID, tvdbId);
        values.put(Shows.REF_SHOW_ID, showTvdbId);
        values.put(Seasons.COMBINED, season >= 0 ? season : 0);
        // set default values
        values.put(Seasons.WATCHCOUNT, 0);
        values.put(Seasons.UNAIREDCOUNT, 0);
        values.put(Seasons.NOAIRDATECOUNT, 0);
        values.put(Seasons.TOTALCOUNT, 0);
        return values;
    }

}
