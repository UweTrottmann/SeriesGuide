package com.battlelancer.seriesguide.ui.shows;

import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;

public interface CalendarQuery {

    String[] PROJECTION = new String[] {
            SeriesGuideDatabase.Tables.EPISODES + "." + SeriesGuideContract.Episodes._ID,
            SeriesGuideContract.Episodes.TITLE,
            SeriesGuideContract.Episodes.NUMBER,
            SeriesGuideContract.Episodes.SEASON,
            SeriesGuideContract.Episodes.FIRSTAIREDMS,
            SeriesGuideContract.Episodes.WATCHED,
            SeriesGuideContract.Episodes.COLLECTED,
            SeriesGuideContract.Shows.REF_SHOW_ID,
            SeriesGuideContract.Shows.TITLE,
            SeriesGuideContract.Shows.NETWORK,
            SeriesGuideContract.Shows.POSTER
    };

    String QUERY_UPCOMING = SeriesGuideContract.Episodes.FIRSTAIREDMS + ">=? AND "
            + SeriesGuideContract.Episodes.FIRSTAIREDMS
            + "<? AND " + SeriesGuideContract.Shows.SELECTION_NO_HIDDEN;

    String QUERY_RECENT = SeriesGuideContract.Episodes.SELECTION_HAS_RELEASE_DATE + " AND "
            + SeriesGuideContract.Episodes.FIRSTAIREDMS + "<? AND "
            + SeriesGuideContract.Episodes.FIRSTAIREDMS + ">? AND "
            + SeriesGuideContract.Shows.SELECTION_NO_HIDDEN;

    String SORTING_UPCOMING = SeriesGuideContract.Episodes.FIRSTAIREDMS + " ASC,"
            + SeriesGuideContract.Shows.SORT_TITLE + ","
            + SeriesGuideContract.Episodes.NUMBER + " ASC";

    String SORTING_RECENT = SeriesGuideContract.Episodes.FIRSTAIREDMS + " DESC,"
            + SeriesGuideContract.Shows.SORT_TITLE + ","
            + SeriesGuideContract.Episodes.NUMBER + " DESC";

    int _ID = 0;
    int TITLE = 1;
    int NUMBER = 2;
    int SEASON = 3;
    int RELEASE_TIME_MS = 4;
    int WATCHED = 5;
    int COLLECTED = 6;
    int SHOW_ID = 7;
    int SHOW_TITLE = 8;
    int SHOW_NETWORK = 9;
    int SHOW_POSTER_PATH = 10;
}
