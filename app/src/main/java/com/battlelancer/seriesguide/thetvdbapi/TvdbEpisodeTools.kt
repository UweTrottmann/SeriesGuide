package com.battlelancer.seriesguide.thetvdbapi

import android.content.ContentValues
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.thetvdb.entities.Episode

class TvdbEpisodeTools {

    companion object {

        @JvmStatic
        fun Episode.toContentValues(values: ContentValues,
                episodeTvdbId: Int, seasonTvdbId: Int, showTvdbId: Int,
                seasonNumber: Int, releaseDateTime: Long, forInsert: Boolean) {
            values.put(Episodes._ID, episodeTvdbId)
            values.put(Episodes.TITLE, episodeName ?: "")
            values.put(Episodes.OVERVIEW, overview)
            values.put(Episodes.NUMBER, airedEpisodeNumber ?: 0)
            values.put(Episodes.SEASON, seasonNumber)
            values.put(Episodes.DVDNUMBER, dvdEpisodeNumber)

            values.put(Episodes.DIRECTORS, TextTools.mendTvdbStrings(directors))
            values.put(Episodes.GUESTSTARS, TextTools.mendTvdbStrings(guestStars))
            values.put(Episodes.WRITERS, TextTools.mendTvdbStrings(writers))
            values.put(Episodes.IMAGE, filename ?: "")
            values.put(Episodes.IMDBID, imdbId ?: "")

            values.put(SeriesGuideContract.Seasons.REF_SEASON_ID, seasonTvdbId)
            values.put(SeriesGuideContract.Shows.REF_SHOW_ID, showTvdbId)

            values.put(Episodes.FIRSTAIREDMS, releaseDateTime)
            values.put(Episodes.ABSOLUTE_NUMBER, absoluteNumber)
            values.put(Episodes.LAST_EDITED, lastUpdated ?: 0)
            // TVDB again provides full details when getting series,
            // so immediately set to LAST_EDITED value (though value is ignored for now)
            values.put(Episodes.LAST_UPDATED, lastUpdated ?: 0)

            if (forInsert) {
                // set default values
                values.put(Episodes.WATCHED, 0)
                values.put(Episodes.PLAYS, 0)
                values.put(Episodes.COLLECTED, 0)
            }
        }

    }

}