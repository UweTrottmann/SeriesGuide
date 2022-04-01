package com.battlelancer.seriesguide.provider

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.sync.ShowLastWatchedInfo
import com.battlelancer.seriesguide.ui.shows.ShowTools.Status

@Dao
interface SgShow2Helper {

    /**
     * Returns row ID.
     */
    @Insert
    fun insertShow(sgShow2: SgShow2): Long

    @Update(entity = SgShow2::class)
    fun updateShow(show: SgShow2Update): Int

    @Query("SELECT * FROM sg_show WHERE _id = :id")
    fun getShow(id: Long): SgShow2?

    @Query("SELECT * FROM sg_show WHERE _id=:id")
    fun getShowLiveData(id: Long): LiveData<SgShow2?>

    @Query("SELECT _id, series_tmdb_id, series_tvdb_id, series_title, series_poster_small FROM sg_show WHERE _id = :id")
    fun getShowMinimal(id: Long): SgShow2Minimal?

    @Query("SELECT _id, series_tmdb_id, series_tvdb_id, series_title, series_poster_small FROM sg_show")
    fun getShowsMinimal(): List<SgShow2Minimal>

    @Query("SELECT _id, series_tmdb_id, series_tvdb_id FROM sg_show WHERE _id=:id")
    suspend fun getShowIds(id: Long): SgShow2Ids?

    @Query("SELECT _id, series_tmdb_id, series_tvdb_id FROM sg_show")
    fun getShowIds(): List<SgShow2Ids>

    @Query("SELECT _id FROM sg_show")
    fun getShowIdsLong(): List<Long>

    @Query("SELECT _id, series_lastupdate, series_airsdayofweek FROM sg_show")
    fun getShowsUpdateInfo(): List<SgShow2UpdateInfo>

    @Query("SELECT _id FROM sg_show WHERE series_tmdb_id=:tmdbId")
    fun getShowIdByTmdbId(tmdbId: Int): Long

    @Query("SELECT _id FROM sg_show WHERE series_tvdb_id=:tvdbId")
    fun getShowIdByTvdbId(tvdbId: Int): Long

    @Query("SELECT _id FROM sg_show WHERE series_tvdb_id=:tvdbId AND series_tmdb_id IS NULL")
    fun getShowIdByTvdbIdWithNullTmdbId(tvdbId: Int): Long

    @Query("SELECT series_trakt_id FROM sg_show WHERE _id = :id")
    fun getShowTraktId(id: Long): Int

    @Query("SELECT series_tmdb_id FROM sg_show WHERE _id=:id")
    fun getShowTmdbId(id: Long): Int

    @Query("SELECT series_tmdb_id FROM sg_show WHERE series_tvdb_id = :showTvdbId")
    fun getShowTmdbIdByTvdbId(showTvdbId: Int): Int

    @Query("SELECT series_tvdb_id FROM sg_show WHERE _id=:id")
    fun getShowTvdbId(id: Long): Int

    @Query("SELECT series_title FROM sg_show WHERE _id=:id")
    fun getShowTitle(id: Long): String?

    @Query("SELECT series_lastwatchedid FROM sg_show WHERE _id = :id")
    fun getShowLastWatchedEpisodeId(id: Long): Long

    @Query("SELECT * FROM sg_show ORDER BY ${SgShow2Columns.SORT_TITLE}")
    fun getShowsForExport(): List<SgShow2>

    @RawQuery(observedEntities = [SgShow2::class])
    fun getShows(query: SupportSQLiteQuery): List<SgShow2ForLists>

    @RawQuery(observedEntities = [SgShow2::class])
    fun getShowsLiveData(query: SupportSQLiteQuery): LiveData<MutableList<SgShow2ForLists>>

    @Query("SELECT sg_show._id, series_lastwatchedid, episode_number, episode_season_number, episode_firstairedms, episode_title, episode_plays FROM sg_show LEFT OUTER JOIN sg_episode ON series_lastwatchedid = sg_episode._id WHERE sg_show._id = :id")
    fun getShowWithLastWatchedEpisode(id: Long): SgShow2LastWatchedEpisode?

    @Query("SELECT sg_show._id, series_lastwatchedid, episode_number, episode_season_number, episode_firstairedms, episode_title, episode_plays FROM sg_show LEFT OUTER JOIN sg_episode ON series_lastwatchedid = sg_episode._id")
    fun getShowsWithLastWatchedEpisode(): List<SgShow2LastWatchedEpisode>

    @Query("SELECT _id, series_status, series_next, series_runtime FROM sg_show")
    fun getStats(): List<SgShow2Stats>

    @Query("SELECT count(series_id) FROM (SELECT series_id, series_status, sum(case when episode_watched = '0' then 1 else 0 end) as episodes_unwatched FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id = sg_show._id GROUP BY series_id) WHERE episodes_unwatched = 0 AND series_status IN (${Status.ENDED}, ${Status.CANCELED})")
    fun countShowsFinishedWatching(): Int

    @Query("SELECT count(series_id) FROM (SELECT series_id, series_status, sum(case when episode_watched = '0' then 1 else 0 end) as episodes_unwatched FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id = sg_show._id WHERE episode_season_number != 0 GROUP BY series_id) WHERE episodes_unwatched = 0 AND series_status IN (${Status.ENDED}, ${Status.CANCELED})")
    fun countShowsFinishedWatchingWithoutSpecials(): Int

    @Update(entity = SgShow2::class)
    fun updateShowNextEpisode(updates: List<SgShow2NextEpisodeUpdate>): Int

    @Query("UPDATE sg_show SET series_favorite = :isFavorite WHERE _id = :id")
    fun setShowFavorite(id: Long, isFavorite: Boolean): Int

    @Query("UPDATE sg_show SET series_notify = :isNotify WHERE _id = :id")
    fun setShowNotify(id: Long, isNotify: Boolean): Int

    @RawQuery(observedEntities = [SgShow2::class])
    fun getShowsNotifyStates(query: SupportSQLiteQuery): LiveData<List<SgShow2Notify>>

    @Query("SELECT count(_id) FROM sg_show WHERE series_notify = 1")
    fun countShowsNotifyEnabled(): Int

    @Query("UPDATE sg_show SET series_hidden = :isHidden WHERE _id = :id")
    fun setShowHidden(id: Long, isHidden: Boolean): Int

    @Query("SELECT count(_id) FROM sg_show WHERE series_hidden=1")
    fun countHiddenShows(): Int

    @Query("SELECT series_tmdb_id FROM sg_show WHERE series_hidden = 1")
    fun getHiddenShowsTmdbIds(): List<Int?>

    @Query("UPDATE sg_show SET series_hidden = 0 WHERE series_hidden = 1")
    fun makeHiddenVisible(): Int

    @Query("SELECT series_language FROM sg_show WHERE _id = :id")
    fun getLanguage(id: Long): String?

    @Query("UPDATE sg_show SET series_language = :languageCode WHERE _id = :id")
    fun updateLanguage(id: Long, languageCode: String)

    @Query("UPDATE sg_show SET series_tmdb_id = :tmdbId WHERE _id = :id")
    fun updateTmdbId(id: Long, tmdbId: Int): Int

    @Query("DELETE FROM sg_show")
    fun deleteAllShows()

    @Query("DELETE FROM sg_show WHERE _id = :showId")
    fun deleteShow(showId: Long): Int

    @Query("SELECT _id, series_tmdb_id, series_tvdb_id FROM sg_show WHERE series_syncenabled = 0")
    fun getHexagonMergeNotCompleted(): List<SgShow2Ids>

    @Query("UPDATE sg_show SET series_syncenabled = 0")
    fun setHexagonMergeNotCompletedForAll()

    @Query("UPDATE sg_show SET series_syncenabled = 0 WHERE _id = :id")
    fun setHexagonMergeNotCompleted(id: Long)

    @Query("UPDATE sg_show SET series_syncenabled = 1 WHERE _id = :id")
    fun setHexagonMergeCompleted(id: Long)

    @Query("SELECT _id, series_tmdb_id, series_language, series_favorite, series_hidden, series_notify FROM sg_show WHERE _id = :id")
    fun getForCloudUpdate(id: Long): SgShow2CloudUpdate?

    @Query("SELECT _id, series_tmdb_id, series_language, series_favorite, series_hidden, series_notify FROM sg_show")
    fun getForCloudUpdate(): List<SgShow2CloudUpdate>

    @Update(entity = SgShow2::class)
    fun updateForCloudUpdate(updates: List<SgShow2CloudUpdate>)

    @Query("UPDATE sg_show SET series_lastwatched_ms = :lastWatchedMs WHERE _id = :id AND series_lastwatched_ms < :lastWatchedMs")
    fun updateLastWatchedMsIfLater(id: Long, lastWatchedMs: Long)

    @Transaction
    fun updateLastWatchedMsIfLater(showIdsToLastWatched: Map<Long, Long>) {
        showIdsToLastWatched.forEach {
            updateLastWatchedMsIfLater(it.key, it.value)
        }
    }

    @Query("UPDATE sg_show SET series_lastwatchedid = :episodeId WHERE _id = :id")
    fun updateLastWatchedEpisodeId(id: Long, episodeId: Long)

    @Transaction
    fun updateLastWatchedMsIfLaterAndLastWatchedEpisodeId(
        showIdsToLastWatched: Map<Long, ShowLastWatchedInfo>,
        episodeHelper: SgEpisode2Helper
    ) {
        showIdsToLastWatched.forEach {
            updateLastWatchedMsIfLater(it.key, it.value.lastWatchedMs)
            val episodeIdOrZero = episodeHelper.getEpisodeIdByNumber(
                it.key,
                it.value.episodeSeason,
                it.value.episodeNumber
            )
            if (episodeIdOrZero != 0L) {
                updateLastWatchedEpisodeId(it.key, episodeIdOrZero)
            }
        }
    }

    @Query("UPDATE sg_show SET series_lastupdate = :lastUpdatedMs WHERE _id = :id")
    fun setLastUpdated(id: Long, lastUpdatedMs: Long)

    @Query("SELECT series_lastupdate FROM sg_show WHERE _id = :id")
    fun getLastUpdated(id: Long): Long?

    @Query("UPDATE sg_show SET series_rating_user = :userRating WHERE _id = :showId")
    fun updateUserRating(showId: Long, userRating: Int): Int

    @Query("UPDATE sg_show SET series_rating_user = :userRating WHERE series_tmdb_id = :tmdbId")
    fun updateUserRatingByTmdbId(tmdbId: Int, userRating: Int): Int

    @Transaction
    fun updateUserRatings(tmdbIdsToRating: Map<Int, Int>) {
        tmdbIdsToRating.forEach {
            updateUserRatingByTmdbId(it.key, it.value)
        }
    }

    @Query("UPDATE sg_show SET series_status = ${Status.CANCELED} WHERE series_status = 3")
    fun migrateCanceledShowStatus()
}

data class SgShow2Ids(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val tvdbId: Int?
)

data class SgShow2UpdateInfo(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.LASTUPDATED) val lastUpdatedMs: Long,
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int,
)

data class SgShow2Minimal(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val posterSmall: String?
)

data class SgShow2Notify(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgShow2Columns.NOTIFY) val notify: Boolean
)

data class SgShow2ForLists(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIME) val releaseTime: Int,
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int,
    @ColumnInfo(name = SgShow2Columns.RELEASE_COUNTRY) val releaseCountry: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String?,
    @ColumnInfo(name = SgShow2Columns.STATUS) val status: Int?,
    @ColumnInfo(name = SgShow2Columns.NEXTEPISODE) val nextEpisode: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER) val poster: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val posterSmall: String?,
    @ColumnInfo(name = SgShow2Columns.NEXTAIRDATEMS) val nextAirdateMs: Long,
    @ColumnInfo(name = SgShow2Columns.NEXTTEXT) val nextText: String,
    @ColumnInfo(name = SgShow2Columns.UNWATCHED_COUNT) val unwatchedCount: Int,
    @ColumnInfo(name = SgShow2Columns.FAVORITE) val favorite: Boolean,
    @ColumnInfo(name = SgShow2Columns.HIDDEN) val hidden: Boolean
)

/**
 * Note: using LEFT OUTER JOIN, so episode table values may be null!
 */
data class SgShow2LastWatchedEpisode(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.LASTWATCHEDID) val lastWatchedEpisodeId: Long,
    @ColumnInfo(name = SgEpisode2Columns.NUMBER) val episodeNumber: Int?,
    @ColumnInfo(name = SgEpisode2Columns.SEASON) val seasonNumber: Int?,
    @ColumnInfo(name = SgEpisode2Columns.FIRSTAIREDMS) val episodeReleaseDateMs: Long?,
    @ColumnInfo(name = SgEpisode2Columns.TITLE) val episodeTitle: String?,
    @ColumnInfo(name = SgEpisode2Columns.PLAYS) val episodePlays: Int?,
)

data class SgShow2Stats(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.STATUS) val status: Int,
    @ColumnInfo(name = SgShow2Columns.NEXTEPISODE) val nextEpisode: String?,
    @ColumnInfo(name = SgShow2Columns.RUNTIME) val runtime: Int
)

data class SgShow2NextEpisodeUpdate(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.NEXTEPISODE) val nextEpisode: String,
    @ColumnInfo(name = SgShow2Columns.NEXTAIRDATEMS) val nextAirdateMs: Long,
    @ColumnInfo(name = SgShow2Columns.NEXTTEXT) val nextText: String,
    @ColumnInfo(name = SgShow2Columns.UNWATCHED_COUNT) val unwatchedCount: Int
)

data class SgShow2Update(
    @ColumnInfo(name = SgShow2Columns._ID) var id: Long = 0,
    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TRAKT_ID) val traktId: Int?,
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgShow2Columns.TITLE_NOARTICLE) val titleNoArticle: String?,
    @ColumnInfo(name = SgShow2Columns.OVERVIEW) val overview: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIME) val releaseTime: Int,
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int,
    @ColumnInfo(name = SgShow2Columns.RELEASE_COUNTRY) val releaseCountry: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.FIRST_RELEASE) val firstRelease: String?,
    @ColumnInfo(name = SgShow2Columns.GENRES) val genres: String?,
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String?,
    @ColumnInfo(name = SgShow2Columns.IMDBID) val imdbId: String?,
    @ColumnInfo(name = SgShow2Columns.RATING_GLOBAL) val ratingGlobal: Double,
    @ColumnInfo(name = SgShow2Columns.RATING_VOTES) val ratingVotes: Int,
    @ColumnInfo(name = SgShow2Columns.RUNTIME) val runtime: Int?,
    @ColumnInfo(name = SgShow2Columns.STATUS) val status: Int,
    @ColumnInfo(name = SgShow2Columns.POSTER) val poster: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val posterSmall: String?,
    @ColumnInfo(name = SgShow2Columns.LASTUPDATED) val lastUpdatedMs: Long
)

data class SgShow2CloudUpdate(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.LANGUAGE) var language: String?,
    @ColumnInfo(name = SgShow2Columns.FAVORITE) var favorite: Boolean,
    @ColumnInfo(name = SgShow2Columns.HIDDEN) var hidden: Boolean,
    @ColumnInfo(name = SgShow2Columns.NOTIFY) var notify: Boolean
)
