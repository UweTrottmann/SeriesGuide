
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.List;

/**
 * @see com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns ShowsColumns
 */
public class Show {

    public int tvdb_id;
    public String imdb_id;
    public Integer trakt_id;

    public String title;
    public String overview;

    public String language;

    public String first_aired;
    public int release_time;
    public int release_weekday;
    public String release_timezone;
    public String country;

    public String poster;
    public String content_rating;
    public String status;
    public int runtime;
    public String genres;
    public String network;

    public double rating;
    public int rating_votes;
    public int rating_user;

    public long last_edited;

    /** SeriesGuide specific values */
    public boolean favorite;
    public Boolean notify;
    public boolean hidden;

    public long last_updated;
    public int last_watched_episode;
    public long last_watched_ms;

    public List<Season> seasons;
    
    public ContentValues toContentValues(Context context, boolean forInsert) {
        // note: if a value is explicitly inserted as NULL the DEFAULT value is not used
        // so ensure a NULL is never inserted if a DEFAULT constraint exists

        ContentValues values = new ContentValues();
        // values for new and existing shows
        // if in any case the title is empty, show a place holder
        values.put(Shows.TITLE, TextUtils.isEmpty(title) 
                ? context.getString(R.string.no_translation_title) : title);
        values.put(Shows.TITLE_NOARTICLE, DBUtils.trimLeadingArticle(title));
        values.put(Shows.OVERVIEW, overview != null ? overview : "");
        values.put(Shows.ACTORS, "");
        values.put(Shows.POSTER, poster != null ? poster : "");
        values.put(Shows.CONTENTRATING, content_rating != null ? content_rating : "");
        values.put(Shows.STATUS, DataLiberationTools.encodeShowStatus(status));
        values.put(Shows.RUNTIME, runtime >= 0 ? runtime : 0);
        values.put(Shows.RATING_GLOBAL, (rating >= 0 && rating <= 10) ? rating : 0);
        values.put(Shows.NETWORK, network != null ? network : "");
        values.put(Shows.GENRES, genres != null ? genres : "");
        values.put(Shows.FIRST_RELEASE, first_aired);
        values.put(Shows.RELEASE_TIME, release_time);
        values.put(Shows.RELEASE_WEEKDAY, (release_weekday >= -1 && release_weekday <= 7)
                ? release_weekday : TimeTools.RELEASE_WEEKDAY_UNKNOWN);
        values.put(Shows.RELEASE_TIMEZONE, release_timezone);
        values.put(Shows.RELEASE_COUNTRY, country);
        values.put(Shows.IMDBID, imdb_id != null ? imdb_id : "");
        values.put(Shows.TRAKT_ID, (trakt_id != null && trakt_id > 0) ? trakt_id : 0);
        values.put(Shows.LASTUPDATED, last_updated);
        values.put(Shows.LASTEDIT, last_edited);
        if (forInsert) {
            values.put(Shows._ID, tvdb_id);
            values.put(Shows.LANGUAGE, language != null ? language : DisplaySettings.LANGUAGE_EN);

            values.put(Shows.FAVORITE, favorite ? 1 : 0);
            values.put(Shows.NOTIFY, notify != null ? (notify ? 1 : 0) : 1);
            values.put(Shows.HIDDEN, hidden ? 1 : 0);

            values.put(Shows.RATING_VOTES, rating_votes >= 0 ? rating_votes : 0);
            values.put(Shows.RATING_USER, (rating_user >= 0 && rating_user <= 10)
                    ? rating_user : 0);

            values.put(Shows.LASTWATCHEDID, last_watched_episode);
            values.put(Shows.LASTWATCHED_MS, last_watched_ms);

            values.put(Shows.HEXAGON_MERGE_COMPLETE, 1);
            values.put(Shows.NEXTEPISODE, "");
            values.put(Shows.NEXTTEXT, "");
            values.put(Shows.NEXTAIRDATEMS, DBUtils.UNKNOWN_NEXT_RELEASE_DATE);
            values.put(Shows.UNWATCHED_COUNT, DBUtils.UNKNOWN_UNWATCHED_COUNT);
        }
        return values;
    }
}
