package com.battlelancer.seriesguide.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.NextEpisodeUpdater;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("WeakerAccess")
public class SeriesGuideContract {

    public interface ShowsColumns {

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SHOW_ID = "series_id";

        /**
         * TheTVDB slug for this show to build URLs. Always a string, but may be a number string if
         * no slug is set (still safe to build URL with). May be null or empty.
         */
        String SLUG = "series_slug";

        /**
         * Ensure this is NOT null (enforced through database constraint).
         */
        String TITLE = "seriestitle";

        /**
         * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
         */
        String TITLE_NOARTICLE = "series_title_noarticle";

        String OVERVIEW = "overview";

        /**
         * A poster path. Needs to be prefixed with the poster server URL.
         */
        String POSTER = "poster";
        /**
         * Path to a small variant of the poster. Needs to be prefixed with the poster server URL.
         */
        String POSTER_SMALL = "series_poster_small";

        String CONTENTRATING = "contentrating";

        /**
         * Show status. Encoded as integer. See {@link ShowTools.Status}.
         */
        String STATUS = "status";

        String RUNTIME = "runtime";

        /**
         * Global rating. Encoded as double.
         * <pre>
         * Range:   0.0-10.0
         * Default: 0.0
         * </pre>
         */
        String RATING_GLOBAL = "rating";

        /**
         * Global rating votes. Encoded as integer.
         * <pre>
         * Example: 42
         * Default: 0
         * </pre>
         */
        String RATING_VOTES = "series_rating_votes";

        /**
         * User rating. Encoded as integer.
         * <pre>
         * Range:   1-10
         * Default: 0
         * </pre>
         */
        String RATING_USER = "series_rating_user";

        String NETWORK = "network";

        String GENRES = "genres";

        /**
         * Release date of the first episode. Encoded as ISO 8601 datetime string. Might be a legacy
         * value which only includes the date.
         *
         * <pre>
         * Example: "2008-01-20T02:00:00.000Z"
         * Default: ""
         * </pre>
         */
        String FIRST_RELEASE = "firstaired";

        /**
         * Local release time. Encoded as integer (hhmm).
         *
         * <pre>
         * Example: 2035
         * Default: -1
         * </pre>
         */
        String RELEASE_TIME = "airstime";

        /**
         * Local release week day. Encoded as integer.
         * <pre>
         * Range:   1-7
         * Daily:   0
         * Default: -1
         * </pre>
         */
        String RELEASE_WEEKDAY = "airsdayofweek";

        /**
         * Release time zone. Encoded as tzdata "Area/Location" string.
         *
         * <pre>
         * Example: "America/New_York"
         * Default: ""
         * </pre>
         *
         * <p> Added with {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase#DBVER_34_TRAKT_V2}.
         */
        String RELEASE_TIMEZONE = "series_timezone";

        /**
         * Release country. Encoded as ISO3166-1 alpha-2 string.
         *
         * <pre>
         * Example: "us"
         * Default: ""
         * </pre>
         *
         * <p> Previous use: Was added in db version 21 to store the air time in pure text.
         */
        String RELEASE_COUNTRY = "series_airtime";

        String IMDBID = "imdbid";

        /**
         * The trakt id of this show. Encoded as integer. Note: for simplification, the trakt id
         * might be handled as a String within the app.
         *
         * <pre>
         * Range:   integer
         * Default: 0 (unknown)
         * </pre>
         */
        String TRAKT_ID = "series_trakt_id";

        /**
         * Whether this show has been favorited.
         */
        String FAVORITE = "series_favorite";

        /**
         * Whether this show has been hidden. Added in db version 23.
         */
        String HIDDEN = "series_hidden";

        /**
         * Whether notifications for new episodes of this show should be shown. Added with {@link
         * SeriesGuideDatabase#DBVER_40_NOTIFY_PER_SHOW}.
         *
         * <pre>
         * Range: 0-1
         * Default: 1
         * </pre>
         */
        String NOTIFY = "series_notify";

        /**
         * Whether this show was merged with data on Hexagon after signing in the last time.
         *
         * <pre>
         * Range: 0-1
         * Default: 1
         * </pre>
         */
        String HEXAGON_MERGE_COMPLETE = "series_syncenabled";

        /**
         * Next episode TheTVDB id.
         *
         * <pre>
         * Example: "42"
         * Default: ""
         * </pre>
         */
        String NEXTEPISODE = "next";

        /**
         * Next episode text.
         *
         * <pre>
         * Example: "0x12 Episode Name"
         * Default: ""
         * </pre>
         */
        String NEXTTEXT = "nexttext";

        /**
         * Next episode release time instant. See {@link Episodes#FIRSTAIREDMS}.
         *
         * <pre>
         * Range:   long
         * Default: {@link NextEpisodeUpdater#UNKNOWN_NEXT_RELEASE_DATE}
         * </pre>
         *
         * <p> Added in db version 25 to allow correct sorting by next air date.
         */
        String NEXTAIRDATEMS = "series_nextairdate";

        /**
         * Next episode release time formatted as text.
         *
         * <pre>
         * Example: "Apr 2 (Mon)"
         * Default: ""
         * </pre>
         *
         * @deprecated Use {@link #NEXTAIRDATEMS} and format.
         */
        String NEXTAIRDATETEXT = "series_nextairdatetext";

        /**
         * Added in db version 22 to store the last time a show was updated.
         */
        String LASTUPDATED = "series_lastupdate";

        /**
         * Last time show was edited on theTVDb.com (lastupdated field). Added in db version 27.
         */
        String LASTEDIT = "series_lastedit";

        /**
         * GetGlue object id, added in version 29 to support checking into shows without IMDb id.
         *
         * @deprecated Removed after tvtag (formerly GetGlue) shutdown end of 2014. Not added on new
         * installs.
         */
        String GETGLUEID = "series_getglueid";

        /**
         * Id of the last watched episode, used to calculate the next episode to watch. Added with
         * {@link SeriesGuideDatabase#DBVER_39_SHOW_LAST_WATCHED}.
         */
        String LASTWATCHEDID = "series_lastwatchedid";

        /**
         * Store the time an episode was last watched for this show. Added in
         */
        String LASTWATCHED_MS = "series_lastwatched_ms";

        /**
         * Language the show should be downloaded in, in two letter ISO 639-1 format,
         * plus optional ISO-3166-1 region tag.
         *
         * <pre>
         * Example: "de" or "pt-BR"
         * Default: "" (should fall back to English then)
         * </pre>
         */
        String LANGUAGE = "series_language";

        /**
         * The remaining number of episodes to watch for this show. Added with {@link
         * SeriesGuideDatabase#DBVER_39_SHOW_LAST_WATCHED}.
         */
        String UNWATCHED_COUNT = "series_unwatched_count";
    }

    public interface SeasonsColumns {

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SEASON_ID = "season_id";

        /**
         * The number of a season. Starting from 0 for Special Episodes.
         */
        String COMBINED = "combinednr";

        /**
         * Number of all episodes in a season.
         */
        String TOTALCOUNT = "season_totalcount";

        /**
         * Number of unwatched, aired episodes.
         */
        String WATCHCOUNT = "watchcount";

        /**
         * Number of unwatched, future episodes (not aired yet).
         */
        String UNAIREDCOUNT = "willaircount";

        /**
         * Number of unwatched episodes with no air date.
         */
        String NOAIRDATECOUNT = "noairdatecount";

        /**
         * Text tags for this season. <em>Repurposed.</em>.
         */
        String TAGS = "seasonposter";
    }

    interface EpisodesColumns {

        /**
         * Season number. A reference to the season id is stored separately.
         */
        String SEASON = "season";

        /**
         * Number of an episode within its season.
         */
        String NUMBER = "episodenumber";

        /**
         * Sometimes episodes are ordered differently when released on DVD. Uses decimal point
         * notation, e.g. 1.0, 1.5.
         */
        String DVDNUMBER = "dvdnumber";

        /**
         * Some shows, mainly anime, use absolute episode numbers instead of the season/episode
         * grouping. Added in db version 30.
         */
        String ABSOLUTE_NUMBER = "absolute_number";

        String TITLE = "episodetitle";

        String OVERVIEW = "episodedescription";

        String IMAGE = "episodeimage";

        String WRITERS = "writers";

        String GUESTSTARS = "gueststars";

        String DIRECTORS = "directors";

        /** See {@link ShowsColumns#RATING_GLOBAL}. */
        String RATING_GLOBAL = "rating";

        /** See {@link ShowsColumns#RATING_VOTES}. */
        String RATING_VOTES = "episode_rating_votes";

        /** See {@link ShowsColumns#RATING_USER}. */
        String RATING_USER = "episode_rating_user";

        /**
         * @deprecated Previously first release date in text as given by TVDb.com. Not created on
         * new installs.
         */
        String FIRSTAIRED = "epfirstaired";

        /**
         * First aired date in ms.
         *
         * <p>This date time is based on the shows release time and time zone at the time this
         * episode was last updated. It includes country and time zone specific offsets (currently
         * only for US western time zones). It does NOT include the user-set offset.
         *
         * <pre>
         * Range:   long
         * Default: {@link Constants#EPISODE_UNKNOWN_RELEASE}
         * </pre>
         */
        String FIRSTAIREDMS = "episode_firstairedms";

        /**
         * One of {@link EpisodeFlags}, whether an episode is watched, skipped or unwatched.
         */
        String WATCHED = "watched";

        /**
         * The number of times an episode was watched.
         */
        String PLAYS = "plays";

        /**
         * Whether an episode has been collected in digital, physical form.
         */
        String COLLECTED = "episode_collected";

        /**
         * IMDb id for a single episode. Added in db version 27.
         */
        String IMDBID = "episode_imdbid";

        /**
         * Last time episode was edited on theTVDb.com (lastupdated field) in Unix time (seconds).
         * Added in {@link SeriesGuideDatabase#DBVER_27_IMDBIDSLASTEDIT}.
         *
         * <pre>
         * Range:   long (unix time)
         * Default: 0
         * </pre>
         */
        String LAST_EDITED = "episode_lastedit";

        /**
         * Stores the last edited time after fetching full episode data from TVDB.
         * Added in {@link SeriesGuideDatabase#DBVER_41_EPISODE_LAST_UPDATED}.
         *
         * <pre>
         * Range:   long (unix time)
         * Default: 0
         * </pre>
         */
        String LAST_UPDATED = "episode_lastupdate";
    }

    public interface SgShow2Columns extends BaseColumns {

        /**
         * SgShow2 table.
         * See {@link SeriesGuideProvider#SG_SHOW} and {@link SeriesGuideProvider#SG_SHOW_ID}.
         */
        Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SG_SHOW)
                .build();

        /**
         * Use if multiple items get returned
         */
        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.sgshow";

        /**
         * Use if a single item is returned
         */
        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.seriesguide.sgshow";

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SHOW_ID = "series_id";

        /**
         * The TMDB ID of a series, may be null for existing shows
         * (or for new shows until TMDB migration is complete).
         */
        String TMDB_ID = "series_tmdb_id";

        /**
         * The TVDB ID of a series, may be null for new shows after TMDB migration is complete.
         */
        String TVDB_ID = "series_tvdb_id";

        /**
         * TheTVDB slug for this show to build URLs. Always a string, but may be a number string if
         * no slug is set (still safe to build URL with). May be null or empty.
         */
        String SLUG = "series_slug";

        /**
         * Ensure this is NOT null (enforced through database constraint).
         */
        String TITLE = "series_title";

        /**
         * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
         */
        String TITLE_NOARTICLE = "series_title_noarticle";

        String OVERVIEW = "series_overview";

        /**
         * A poster path. Needs to be prefixed with the poster server URL.
         */
        String POSTER = "series_poster";
        /**
         * Path to a small variant of the poster. Needs to be prefixed with the poster server URL.
         */
        String POSTER_SMALL = "series_poster_small";

        String CONTENTRATING = "series_contentrating";

        /**
         * Show status. Encoded as integer. See {@link ShowTools.Status}.
         */
        String STATUS = "series_status";

        String RUNTIME = "series_runtime";

        /**
         * Global rating. Encoded as double.
         * <pre>
         * Range:   0.0-10.0
         * Default: 0.0
         * </pre>
         */
        String RATING_GLOBAL = "series_rating";

        /**
         * Global rating votes. Encoded as integer.
         * <pre>
         * Example: 42
         * Default: 0
         * </pre>
         */
        String RATING_VOTES = "series_rating_votes";

        /**
         * User rating. Encoded as integer.
         * <pre>
         * Range:   1-10
         * Default: 0
         * </pre>
         */
        String RATING_USER = "series_rating_user";

        String NETWORK = "series_network";

        String GENRES = "series_genres";

        /**
         * Release date of the first episode. Encoded as ISO 8601 datetime string. Might be a legacy
         * value which only includes the date.
         *
         * <pre>
         * Example: "2008-01-20T02:00:00.000Z"
         * Default: ""
         * </pre>
         */
        String FIRST_RELEASE = "series_firstaired";

        /**
         * Local release time. Encoded as integer (hhmm).
         *
         * <pre>
         * Example: 2035
         * Default: -1
         * </pre>
         */
        String RELEASE_TIME = "series_airstime";

        /**
         * Local release week day. Encoded as integer.
         * <pre>
         * Range:   1-7
         * Daily:   0
         * Default: -1
         * </pre>
         */
        String RELEASE_WEEKDAY = "series_airsdayofweek";

        /**
         * Release time zone. Encoded as tzdata "Area/Location" string.
         *
         * <pre>
         * Example: "America/New_York"
         * Default: ""
         * </pre>
         *
         * <p> Added with {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase#DBVER_34_TRAKT_V2}.
         */
        String RELEASE_TIMEZONE = "series_timezone";

        /**
         * Release country. Encoded as ISO3166-1 alpha-2 string.
         *
         * <pre>
         * Example: "us"
         * Default: ""
         * </pre>
         *
         * <p> Previous use: Was added in db version 21 to store the air time in pure text.
         */
        String RELEASE_COUNTRY = "series_country";

        String IMDBID = "series_imdbid";

        /**
         * The trakt id of this show. Encoded as integer. Note: for simplification, the trakt id
         * might be handled as a String within the app.
         *
         * <pre>
         * Range:   integer
         * Default: 0 (unknown)
         * </pre>
         */
        String TRAKT_ID = "series_trakt_id";

        /**
         * Whether this show has been favorited.
         */
        String FAVORITE = "series_favorite";

        /**
         * Whether this show has been hidden. Added in db version 23.
         */
        String HIDDEN = "series_hidden";

        /**
         * Whether notifications for new episodes of this show should be shown. Added with {@link
         * SeriesGuideDatabase#DBVER_40_NOTIFY_PER_SHOW}.
         *
         * <pre>
         * Range: 0-1
         * Default: 1
         * </pre>
         */
        String NOTIFY = "series_notify";

        /**
         * Whether this show was merged with data on Hexagon after signing in the last time.
         *
         * <pre>
         * Range: 0-1
         * Default: 1
         * </pre>
         */
        String HEXAGON_MERGE_COMPLETE = "series_syncenabled";

        /**
         * Next episode row id.
         *
         * <pre>
         * Example: "42"
         * Default: ""
         * </pre>
         */
        String NEXTEPISODE = "series_next";

        /**
         * Next episode text.
         *
         * <pre>
         * Example: "0x12 Episode Name"
         * Default: ""
         * </pre>
         */
        String NEXTTEXT = "series_nexttext";

        /**
         * Next episode release time instant. See {@link Episodes#FIRSTAIREDMS}.
         *
         * <pre>
         * Range:   long
         * Default: {@link NextEpisodeUpdater#UNKNOWN_NEXT_RELEASE_DATE}
         * </pre>
         *
         * <p> Added in db version 25 to allow correct sorting by next air date.
         */
        String NEXTAIRDATEMS = "series_nextairdate";

        /**
         * Added in db version 22 to store the last time a show was updated.
         */
        String LASTUPDATED = "series_lastupdate";

        /**
         * Last time show was edited on theTVDb.com (lastupdated field). Added in db version 27.
         */
        String LASTEDIT = "series_lastedit";

        /**
         * Id of the last watched episode, used to calculate the next episode to watch. Added with
         * {@link SeriesGuideDatabase#DBVER_39_SHOW_LAST_WATCHED}.
         */
        String LASTWATCHEDID = "series_lastwatchedid";

        /**
         * Store the time an episode was last watched for this show. Added in
         */
        String LASTWATCHED_MS = "series_lastwatched_ms";

        /**
         * Language the show should be downloaded in, in two letter ISO 639-1 format,
         * plus optional ISO-3166-1 region tag.
         *
         * <pre>
         * Example: "de" or "pt-BR"
         * Default: "" (should fall back to English then)
         * </pre>
         */
        String LANGUAGE = "series_language";

        /**
         * The remaining number of episodes to watch for this show. Added with {@link
         * SeriesGuideDatabase#DBVER_39_SHOW_LAST_WATCHED}.
         */
        String UNWATCHED_COUNT = "series_unwatched_count";

        String SELECTION_FAVORITES = FAVORITE + "=1";
        String SELECTION_NOT_FAVORITES = FAVORITE + "=0";
        String SELECTION_HIDDEN = HIDDEN + "=1";
        String SELECTION_NO_HIDDEN = HIDDEN + "=0";
        /**
         * Technically continuing, upcoming, in production and pilot shows (as they might continue).
         * Using inverse of {@link SgShow2Columns#SELECTION_STATUS_NO_CONTINUING} as that is shorter.
         */
        String SELECTION_STATUS_CONTINUING = "("
                + STATUS + "!=" + ShowTools.Status.ENDED
                + " AND " + STATUS + "!=" + ShowTools.Status.CANCELED
                + " AND " + STATUS + "!=" + ShowTools.Status.UNKNOWN
                + ")";
        /**
         * Technically ended, canceled and unknown state shows.
         */
        String SELECTION_STATUS_NO_CONTINUING = "("
                + STATUS + "=" + ShowTools.Status.ENDED
                + " OR " + STATUS + "=" + ShowTools.Status.CANCELED
                + " OR " + STATUS + "=" + ShowTools.Status.UNKNOWN
                + ")";

        String SELECTION_HAS_NEXT_EPISODE =
                NEXTAIRDATEMS + "!=" + NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE;
        String SELECTION_NO_NEXT_EPISODE =
                NEXTAIRDATEMS + "=" + NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE;

        // Android provides the UNICODE collator,
        // use to correctly order characters with e.g. accents.
        // https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase
        String SORT_TITLE = TITLE + " COLLATE UNICODE ASC";
        String SORT_TITLE_NOARTICLE = TITLE_NOARTICLE + " COLLATE UNICODE ASC";
        String SORT_STATUS = STATUS + " DESC";
        String SORT_LATEST_EPISODE_THEN_STATUS = NEXTAIRDATEMS + " DESC," + SORT_STATUS;

        static Uri buildIdUri(long rowId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(rowId)).build();
        }

        static long getId(Uri uri) {
            String lastPathSegment = uri.getLastPathSegment();
            if (lastPathSegment == null) {
                throw new IllegalArgumentException("Path of URI is empty: " + uri);
            }
            return Long.parseLong(lastPathSegment);
        }
    }

    public interface SgSeason2Columns extends BaseColumns {

        /**
         * SgSeason2 table.
         * See {@link SeriesGuideProvider#SG_SEASON} and {@link SeriesGuideProvider#SG_SEASON_ID}.
         */
        Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SG_SEASON)
                .build();

        /**
         * Use if multiple items get returned
         */
        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.sgseason";

        /**
         * Use if a single item is returned
         */
        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.seriesguide.sgseason";

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SEASON_ID = "season_id";

        /**
         * The TMDB ID of a season (integer) or episode group (string),
         * may be null for existing shows (or for new shows until TMDB migration is complete).
         */
        String TMDB_ID = "season_tmdb_id";

        /**
         * The TVDB ID of a season, may be null for new shows after TMDB migration is complete.
         */
        String TVDB_ID = "season_tvdb_id";

        /**
         * The number of a season. Starting from 0 for Special Episodes.
         */
        String COMBINED = "season_number";

        /**
         * Number of all episodes in a season.
         */
        String TOTALCOUNT = "season_totalcount";

        /**
         * Number of unwatched, aired episodes.
         */
        String WATCHCOUNT = "season_watchcount";

        /**
         * Number of unwatched, future episodes (not aired yet).
         */
        String UNAIREDCOUNT = "season_willaircount";

        /**
         * Number of unwatched episodes with no air date.
         */
        String NOAIRDATECOUNT = "season_noairdatecount";

        /**
         * Text tags for this season.
         */
        String TAGS = "season_tags";

        /**
         * The name of a season, or null (in which case should fall back to number).
         */
        String NAME = "season_name";

        /**
         * Integer to order seasons by, typically equal to the season number.
         */
        String ORDER = "season_order";

        static long getId(Uri uri) {
            String lastPathSegment = uri.getLastPathSegment();
            if (lastPathSegment == null) {
                throw new IllegalArgumentException("Path of URI is empty: " + uri);
            }
            return Long.parseLong(lastPathSegment);
        }
    }

    public interface SgEpisode2Columns extends BaseColumns {

        /**
         * SgEpisode2 table.
         * See {@link SeriesGuideProvider#SG_EPISODE} and {@link SeriesGuideProvider#SG_EPISODE_ID}.
         */
        Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SG_EPISODE)
                .build();

        /**
         * Use if multiple items get returned
         */
        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.sgepisode";

        /**
         * Use if a single item is returned
         */
        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.seriesguide.sgepisode";

        /**
         * The TMDB ID of an episode, may be null for existing shows
         * (or for new shows until TMDB migration is complete).
         */
        String TMDB_ID = "episode_tmdb_id";

        /**
         * The TVDB ID of an episode, may be null for new shows after TMDB migration is complete.
         */
        String TVDB_ID = "episode_tvdb_id";

        /**
         * Season number. A reference to the season id is stored separately.
         */
        String SEASON = "episode_season_number";

        /**
         * Number of an episode within its season.
         */
        String NUMBER = "episode_number";

        /**
         * Integer to order episodes by, typically equal to the episode number.
         */
        String ORDER = "episode_order";

        /**
         * Sometimes episodes are ordered differently when released on DVD. Uses decimal point
         * notation, e.g. 1.0, 1.5.
         */
        String DVDNUMBER = "episode_dvd_number";

        /**
         * Some shows, mainly anime, use absolute episode numbers instead of the season/episode
         * grouping. Added in db version 30.
         */
        String ABSOLUTE_NUMBER = "episode_absolute_number";

        String TITLE = "episode_title";

        String OVERVIEW = "episode_description";

        String IMAGE = "episode_image";

        String WRITERS = "episode_writers";

        String GUESTSTARS = "episode_gueststars";

        String DIRECTORS = "episode_directors";

        /** See {@link ShowsColumns#RATING_GLOBAL}. */
        String RATING_GLOBAL = "episode_rating";

        /** See {@link ShowsColumns#RATING_VOTES}. */
        String RATING_VOTES = "episode_rating_votes";

        /** See {@link ShowsColumns#RATING_USER}. */
        String RATING_USER = "episode_rating_user";

        /**
         * First aired date in ms.
         *
         * <p>This date time is based on the shows release time and time zone at the time this
         * episode was last updated. It includes country and time zone specific offsets (currently
         * only for US western time zones). It does NOT include the user-set offset.
         *
         * <pre>
         * Range:   long
         * Default: {@link Constants#EPISODE_UNKNOWN_RELEASE}
         * </pre>
         */
        String FIRSTAIREDMS = "episode_firstairedms";

        /**
         * One of {@link EpisodeFlags}, whether an episode is watched, skipped or unwatched.
         */
        String WATCHED = "episode_watched";

        /**
         * The number of times an episode was watched.
         */
        String PLAYS = "episode_plays";

        /**
         * Whether an episode has been collected in digital, physical form.
         */
        String COLLECTED = "episode_collected";

        /**
         * IMDb id for a single episode. Added in db version 27.
         */
        String IMDBID = "episode_imdbid";

        /**
         * Note: currently unused, TMDB does not provide this info.
         * See {@link com.battlelancer.seriesguide.ui.shows.ShowTools2}.
         *
         * Last time episode was edited (lastupdated field) in Unix time (seconds).
         * Added in {@link SeriesGuideDatabase#DBVER_27_IMDBIDSLASTEDIT}.
         *
         * <pre>
         * Range:   long (unix time)
         * Default: 0
         * </pre>
         */
        String LAST_EDITED = "episode_lastedit";

        /**
         * Note: currently last updated value is unused, all episodes are always updated.
         * See {@link com.battlelancer.seriesguide.ui.shows.ShowTools2}.
         *
         * Stores the last edited time after fetching full episode data from TVDB.
         * Added in {@link SeriesGuideDatabase#DBVER_41_EPISODE_LAST_UPDATED}.
         *
         * <pre>
         * Range:   long (unix time)
         * Default: 0
         * </pre>
         */
        String LAST_UPDATED = "episode_lastupdate";

        String SELECTION_UNWATCHED = WATCHED + "=" + EpisodeFlags.UNWATCHED;
        String SELECTION_NOT_SKIPPED = WATCHED + "!=" + EpisodeFlags.SKIPPED;
        String SELECTION_COLLECTED = COLLECTED + "=1";
        String SELECTION_NO_SPECIALS = SEASON + "!=0";
        String SELECTION_HAS_RELEASE_DATE = FIRSTAIREDMS + "!=" + Constants.EPISODE_UNKNOWN_RELEASE;
        String SELECTION_ONLY_PREMIERES = NUMBER + "=1";

        String SORT_UPCOMING = FIRSTAIREDMS + " ASC," + SgShow2Columns.SORT_TITLE + "," + NUMBER + " ASC";
        String SORT_RECENT = FIRSTAIREDMS + " DESC," + SgShow2Columns.SORT_TITLE + "," + NUMBER + " DESC";

    }

    interface EpisodeSearchColumns {

        String _DOCID = "docid";

    }

    interface ListsColumns {

        /**
         * Unique string identifier.
         */
        String LIST_ID = "list_id";

        String NAME = "list_name";

        /**
         * Helps determine list order in addition to the list name. Integer.
         * <pre>
         * Range: 0 to MAX INT
         * Default: 0
         * </pre>
         */
        String ORDER = "list_order";
    }

    interface ListItemsColumns {

        /**
         * Unique string identifier.
         */
        String LIST_ITEM_ID = "list_item_id";

        /**
         * NON-unique string identifier referencing internal ids of other tables.
         */
        String ITEM_REF_ID = "item_ref_id";

        /**
         * One of {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes}.
         */
        String TYPE = "item_type";
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ListItemTypes.TVDB_SHOW,
            ListItemTypes.SEASON,
            ListItemTypes.EPISODE,
            ListItemTypes.TMDB_SHOW
    })
    public @interface ListItemTypes {
        int TVDB_SHOW = 1;
        /**
         * Support for seasons on lists has been removed since the TMDB switch (version 58).
         */
        int SEASON = 2;
        /**
         * Support for episodes on lists has been removed since the TMDB switch (version 58).
         */
        int EPISODE = 3;
        int TMDB_SHOW = 4;
    }

    interface MoviesColumns {

        String TITLE = "movies_title";

        /**
         * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
         */
        String TITLE_NOARTICLE = "movies_title_noarticle";

        String IMDB_ID = "movies_imdbid";

        String TMDB_ID = "movies_tmdbid";

        String POSTER = "movies_poster";

        String GENRES = "movies_genres";

        String OVERVIEW = "movies_overview";

        /**
         * Release date in milliseconds. Store Long.MAX if unknown, as it is likely in the future
         * (also helps correctly sorting movies by release date).
         */
        String RELEASED_UTC_MS = "movies_released";

        String RUNTIME_MIN = "movies_runtime";

        String TRAILER = "movies_trailer";

        String CERTIFICATION = "movies_certification";

        /**
         * Whether a movie is in the collection. Encoded as integer.
         * <pre>
         * Range: 0-1
         * Default: 0
         * </pre>
         */
        String IN_COLLECTION = "movies_incollection";

        /**
         * Whether a movie is in the watchlist. Encoded as integer.
         * <pre>
         * Range: 0-1
         * Default: 0
         * </pre>
         */
        String IN_WATCHLIST = "movies_inwatchlist";

        /**
         * Whether a movie is watched. Encoded as integer.
         * <pre>
         * Range: 0-1
         * Default: 0
         * </pre>
         */
        String WATCHED = "movies_watched";

        /**
         * The number of times a movie was watched.
         */
        String PLAYS = "movies_plays";

        String RATING_TMDB = "movies_rating_tmdb";

        String RATING_VOTES_TMDB = "movies_rating_votes_tmdb";

        String RATING_TRAKT = "movies_rating_trakt";

        String RATING_VOTES_TRAKT = "movies_rating_votes_trakt";

        /** See {@link ShowsColumns#RATING_USER}. */
        String RATING_USER = "movies_rating_user";

        /**
         * Time in milliseconds a movie was last updated. Warning: may be null as only recently in use.
         */
        String LAST_UPDATED = "movies_last_updated";
    }

    interface ActivityColumns {

        String TIMESTAMP_MS = "activity_time";
        /**
         * A TVDB ID is stored as an int, a TMDB ID is stored as an int prefixed with `tmdb-`.
         */
        String EPISODE_TVDB_OR_TMDB_ID = "activity_episode";
        /**
         * A TVDB ID is stored as an int, a TMDB ID is stored as an int prefixed with `tmdb-`.
         */
        String SHOW_TVDB_OR_TMDB_ID = "activity_show";
    }

    interface JobsColumns {
        String CREATED_MS = "job_created_at";
        String TYPE = "job_type";
        String EXTRAS = "job_extras";
    }

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://"
            + SgApp.CONTENT_AUTHORITY);

    public static final String PATH_CLOSE = "close";

    public static final String PATH_SHOWS = "shows";

    public static final String PATH_SEASONS = "seasons";

    public static final String PATH_EPISODES = "episodes";

    public static final String PATH_SG_SHOW = "sg_show";
    public static final String PATH_SG_SEASON = "sg_season";
    public static final String PATH_SG_EPISODE = "sg_episode";

    public static final String PATH_OFSHOW = "ofshow";

    public static final String PATH_OFSEASON = "ofseason";

    public static final String PATH_WITHSHOW = "withshow";

    public static final String PATH_FILTER = "filter";

    public static final String PATH_LISTS = "lists";

    public static final String PATH_WITH_LIST_ITEM_ID = "with_list_item";

    public static final String PATH_LIST_ITEMS = "listitems";

    public static final String PATH_WITH_DETAILS = "with_details";

    public static final String PATH_WITH_NEXT_EPISODE = "with-next-episode";

    public static final String PATH_WITH_LAST_EPISODE = "with-last-episode";

    public static final String PATH_MOVIES = "movies";

    public static final String PATH_ACTIVITY = "activity";

    public static final String PATH_JOBS = "jobs";

    public static class Shows implements ShowsColumns, BaseColumns {

        /**
         * Shows table.
         * See {@link SeriesGuideProvider#SHOWS} and {@link SeriesGuideProvider#SHOWS_ID}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SHOWS)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.show";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.show";

        public static Uri buildShowUri(String showId) {
            return CONTENT_URI.buildUpon().appendPath(showId).build();
        }

        public static String getShowId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Episodes implements EpisodesColumns, BaseColumns {

        /**
         * Episodes table.
         * See {@link SeriesGuideProvider#EPISODES} and {@link SeriesGuideProvider#EPISODES_ID}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPISODES).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.episode";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.episode";

        public static Uri buildEpisodeUri(String episodeId) {
            return CONTENT_URI.buildUpon().appendPath(episodeId).build();
        }

        public static String getEpisodeId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Seasons implements SeasonsColumns, BaseColumns {

        /**
         * Seasons table.
         * See {@link SeriesGuideProvider#SEASONS} and {@link SeriesGuideProvider#SEASONS_ID}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEASONS)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.season";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.season";

        public static Uri buildSeasonUri(String seasonTvdbId) {
            return CONTENT_URI.buildUpon().appendPath(seasonTvdbId).build();
        }

        public static String getSeasonId(Uri uri) {
            return uri.getLastPathSegment();
        }

    }

    public static class Lists implements ListsColumns, BaseColumns {

        /**
         * Lists table.
         * See {@link SeriesGuideProvider#LISTS}
         * and {@link SeriesGuideProvider#LISTS_ID}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LISTS)
                .build();

        /**
         * Lists table joined with list items table (to get in which lists an item is in or not).
         * See {@link SeriesGuideProvider#LISTS_WITH_LIST_ITEM_ID}.
         */
        public static final Uri CONTENT_WITH_LIST_ITEM_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_WITH_LIST_ITEM_ID)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.list";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.list";

        public static final String SORT_ORDER_THEN_NAME = Lists.ORDER + " ASC," + Lists.NAME
                + " COLLATE UNICODE ASC";

        public static Uri buildListUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static Uri buildListsWithListItemUri(String itemId) {
            return CONTENT_WITH_LIST_ITEM_URI.buildUpon().appendPath(itemId).build();
        }

        public static String getId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateListId(@NonNull String name) {
            String uriEncodedId = Uri.encode(name);
            return TextUtils.isEmpty(uriEncodedId) ? "default" : uriEncodedId;
        }
    }

    public static class ListItems implements ListItemsColumns, BaseColumns {

        /**
         * List items table.
         * See {@link SeriesGuideProvider#LIST_ITEMS}
         * and {@link SeriesGuideProvider#LIST_ITEMS_ID}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_LIST_ITEMS)
                .build();

        /**
         * List items table joined with shows, seasons and episodes table (depending on list item
         * type). See {@link SeriesGuideProvider#LIST_ITEMS_WITH_DETAILS}.
         */
        public static final Uri CONTENT_WITH_DETAILS_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_WITH_DETAILS)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.listitem";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.listitem";

        public static final String SELECTION_LIST = Lists.LIST_ID + "=?";
        public static final String SELECTION_TVDB_SHOWS =
                ListItems.TYPE + "=" + ListItemTypes.TVDB_SHOW;
        public static final String SELECTION_TMDB_SHOWS =
                ListItems.TYPE + "=" + ListItemTypes.TMDB_SHOW;
        public static final String SELECTION_SEASONS =
                ListItems.TYPE + "=" + ListItemTypes.SEASON;
        public static final String SELECTION_EPISODES =
                ListItems.TYPE + "=" + ListItemTypes.EPISODE;

        public static final String SORT_TYPE = ListItems.TYPE + " ASC";

        public static Uri buildListItemUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static String getId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateListItemId(String itemStableId, int type, String listId) {
            return itemStableId + "-" + type + "-" + listId;
        }

        public static String generateListItemId(int itemStableId, int type, String listId) {
            return generateListItemId(String.valueOf(itemStableId), type, listId);
        }

        public static String generateListItemIdWildcard(int itemStableId, int type) {
            // The SQL % wildcard is added by the content provider
            return itemStableId + "-" + type + "-";
        }

        /**
         * Splits the id into the parts used to create it with {@link #generateListItemId(int, int,
         * String)}.
         */
        @Nullable
        public static String[] splitListItemId(@NonNull String listItemId) {
            String[] brokenUpId = listItemId.split("-", 3);
            if (brokenUpId.length != 3) {
                return null;
            }
            return brokenUpId;
        }

        /**
         * Checks if the given type index is one of {@link ListItemTypes}.
         */
        public static boolean isValidItemType(int type) {
            return type == ListItemTypes.TVDB_SHOW
                    || type == ListItemTypes.SEASON
                    || type == ListItemTypes.EPISODE
                    || type == ListItemTypes.TMDB_SHOW;
        }
    }

    public static class Movies implements MoviesColumns, BaseColumns {

        /**
         * Movies table.
         * See {@link SeriesGuideProvider#MOVIES}
         * and {@link SeriesGuideProvider#MOVIES_ID}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MOVIES)
                .build();

        public static final String SELECTION_COLLECTION = Movies.IN_COLLECTION + "=1";

        public static final String SELECTION_NOT_COLLECTION = Movies.IN_COLLECTION + "=0";

        public static final String SELECTION_WATCHLIST = Movies.IN_WATCHLIST + "=1";

        public static final String SELECTION_NOT_WATCHLIST = Movies.IN_WATCHLIST + "=0";

        public static final String SELECTION_WATCHED = Movies.WATCHED + "=1";
        public static final String SELECTION_UNWATCHED = Movies.WATCHED + "=0";

        // Android provides the UNICODE collator,
        // use to correctly order characters with e.g. accents.
        // https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase
        /** Default sort order. */
        public static final String SORT_TITLE_ALPHABETICAL = Movies.TITLE + " COLLATE UNICODE ASC";

        public static final String SORT_TITLE_ALPHABETICAL_NO_ARTICLE = Movies.TITLE_NOARTICLE
                + " COLLATE UNICODE ASC";

        public static final String SORT_TITLE_REVERSE_ALPHACETICAL = Movies.TITLE
                + " COLLATE UNICODE DESC";

        public static final String SORT_TITLE_REVERSE_ALPHACETICAL_NO_ARTICLE =
                Movies.TITLE_NOARTICLE + " COLLATE UNICODE DESC";

        public static final String SORT_RELEASE_DATE_NEWEST_FIRST = Movies.RELEASED_UTC_MS
                + " DESC," + SORT_TITLE_ALPHABETICAL;

        public static final String SORT_RELEASE_DATE_OLDEST_FIRST = Movies.RELEASED_UTC_MS + " ASC,"
                + SORT_TITLE_ALPHABETICAL;

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.movie";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.movie";

        public static Uri buildMovieUri(Integer tmdbId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(tmdbId)).build();
        }

        public static String getId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Activity implements ActivityColumns, BaseColumns {

        /**
         * Activity table.
         * See {@link SeriesGuideProvider#ACTIVITY}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_ACTIVITY)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.activity";

        public static Uri buildActivityUri(String episodeTvdbId) {
            return CONTENT_URI.buildUpon().appendPath(episodeTvdbId).build();
        }
    }

    public static class Jobs implements JobsColumns, BaseColumns {
        /**
         * Jobs table.
         * See {@link SeriesGuideProvider#JOBS}.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_JOBS)
                .build();

        public static final String[] PROJECTION = {
                Jobs._ID,
                Jobs.TYPE,
                Jobs.CREATED_MS,
                Jobs.EXTRAS
        };

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.jobs";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.jobs";

        public static final String SORT_OLDEST = Jobs.CREATED_MS + " ASC";

        public static Uri buildJobUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public static String getJobId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    private SeriesGuideContract() {
    }
}
