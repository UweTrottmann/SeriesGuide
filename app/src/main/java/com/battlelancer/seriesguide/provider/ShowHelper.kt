package com.battlelancer.seriesguide.provider

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.model.SgShowMinimal

/**
 * Data Access Object for the series table.
 */
@Dao
interface ShowHelper {
    /**
     * For testing: Get the first show from the table.
     */
    @Query("SELECT * FROM series LIMIT 1")
    fun getShow(): SgShow?

    /**
     * Returns a [SgShowMinimal] object with only title and poster populated.
     * Might return `null` if there is no show with that TVDb id.
     */
    @Query("SELECT series_title, series_poster_small FROM series WHERE _id = :tvdbId LIMIT 1")
    fun getShowMinimal(tvdbId: Long): SgShowMinimal?

    @Query("SELECT series_title FROM series WHERE _id = :tvdbId")
    fun getShowTitle(tvdbId: Long): String?

    @Query("SELECT _id FROM series WHERE series_tvdb_id=:tvdbId")
    fun getShowId(tvdbId: Long): Long

    @RawQuery(observedEntities = [SgShow::class])
    fun queryShows(query: SupportSQLiteQuery): LiveData<List<SgShow>>

    @Query("SELECT count(_id) FROM series WHERE series_hidden=1")
    fun countHiddenShows(): Int

    @Query("SELECT _id FROM series WHERE series_hidden=1")
    fun getHiddenShowsTvdbIds(): List<Int?>

    @Query("UPDATE series SET series_hidden=0 WHERE series_hidden=1")
    fun makeHiddenVisible(): Int
}