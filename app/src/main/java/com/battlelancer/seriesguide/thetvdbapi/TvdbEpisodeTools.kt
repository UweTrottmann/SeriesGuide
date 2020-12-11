package com.battlelancer.seriesguide.thetvdbapi

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.thetvdb.entities.Episode
import com.uwetrottmann.thetvdb.entities.EpisodesResponse
import com.uwetrottmann.thetvdb.services.TheTvdbSeries
import dagger.Lazy
import java.util.ArrayList
import java.util.TimeZone

class TvdbEpisodeTools constructor(
        private val context: Context,
        private val tvdbSeries: Lazy<TheTvdbSeries>
) {

    /**
     * Loads and parses episodes for the given show and language to create an array of
     * [ContentValues] for new episodes.
     *
     * Adds update ops for updated episodes and delete ops for local orphaned episodes to the given
     * [ContentProviderOperation] batch.
     */
    @Throws(TvdbException::class)
    fun fetchEpisodes(
        batch: ArrayList<ContentProviderOperation>,
        show: Show,
        showId: Long,
        language: String
    ): ArrayList<ContentValues> {
        val showTvdbId = show.tvdb_id
        val newEpisodesValues = ArrayList<ContentValues>()

        val localEpisodesByTvdbId = DBUtils.getEpisodesByTvdbId(context, showId)
        // just copy episodes list, then remove valid ones
        val removableEpisodesByTvdbId = HashMap(localEpisodesByTvdbId)

        val localSeasonsByTvdbId = DBUtils.getSeasonsByTvdbId(context, showId)
        // Store new/updated seasons to avoid duplicate ops
        val addedOrUpdatedSeasonsByTvdbId = HashMap<Int, Long>()

        val dateLastMonthEpoch = (System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 30) / 1000
        val showTimeZone = TimeTools.getDateTimeZone(show.release_timezone)
        val showReleaseTime = TimeTools.getShowReleaseTime(show.release_time)
        val deviceTimeZone = TimeZone.getDefault().id

        val fallback = DisplaySettings.getShowsLanguageFallback(context)
        val fallbackLanguage: String? = if (fallback != language) fallback else null

        var page: Int? = 0
        while (page != null) {
            val response = getEpisodes(showTvdbId, page, language)
            val episodes = response.data
                ?: throw TvdbDataException("fetchEpisodes response is null") // No episode data returned, stop (likely API error).

            // fall back if no translation is available for some episodes
            // note: just checking errors is not enough as no error if just some are not translated
            val fallbackResponse = if (fallbackLanguage != null
                    && (response.errors?.invalidLanguage != null
                    || episodes.find { it.episodeName.isNullOrEmpty() || it.overview.isNullOrEmpty() } != null)) {
                // assumes that episode pages match between languages
                // worst case: no fallback title or overview
                getEpisodes(showTvdbId, page, fallbackLanguage)
            } else {
                null
            }

            val values = ContentValues()
            for (episode in episodes) {
                val episodeTvdbId = episode.id
                val seasonNumber = episode.airedSeason
                val seasonTvdbId = episode.airedSeasonID
                if (episodeTvdbId == null || episodeTvdbId <= 0
                        || seasonNumber == null || seasonNumber < 0 // season 0 allowed (specials)
                        || seasonTvdbId == null || seasonTvdbId <= 0) {
                    continue // invalid ids, skip
                }

                val seasonIdOrNull = addedOrUpdatedSeasonsByTvdbId[seasonTvdbId]
                val seasonId = if (seasonIdOrNull != null) {
                    seasonIdOrNull
                } else {
                    // Insert/update season, prevent it from getting cleaned.
                    val seasonId = insertOrUpdateSeason(
                        context.contentResolver,
                        showId,
                        seasonTvdbId,
                        seasonNumber,
                        localSeasonsByTvdbId[seasonTvdbId]?.id
                    )
                    addedOrUpdatedSeasonsByTvdbId[seasonTvdbId] = seasonId
                    seasonId
                }

                // don't clean up this episode
                removableEpisodesByTvdbId.remove(episodeTvdbId)

                val localEpisodeOrNull = localEpisodesByTvdbId[episodeTvdbId]
                if (localEpisodeOrNull != null) {
                    /*
                     * Update uses provider ops which take a long time. Only
                     * update if episode was edited on TVDb or is not older than
                     * a month (ensures show air time changes get stored).
                     */
                    val lastUpdatedEpoch = localEpisodeOrNull.lastUpdatedSec
                    val lastTvdbEditEpoch = episode.lastUpdated
                    if (lastTvdbEditEpoch != null
                        && (lastUpdatedEpoch < lastTvdbEditEpoch
                                || dateLastMonthEpoch < lastUpdatedEpoch)) {
                        // update episode
                    } else {
                        continue // not edited or too old to update, skip
                    }
                }

                // calculate release time
                val releaseDateTime = TimeTools.parseEpisodeReleaseDate(
                    context, showTimeZone,
                    episode.firstAired, showReleaseTime, show.country, show.network,
                    deviceTimeZone
                )
                // if name or overview are empty use fallback
                val hasName = !episode.episodeName.isNullOrEmpty()
                val hasOverview = !episode.overview.isNullOrEmpty()
                val fallbackEpisode = if (!hasName || !hasOverview) {
                    fallbackResponse?.data?.find { it.id == episodeTvdbId }
                } else {
                    null
                }
                if (!hasName) {
                    episode.episodeName = fallbackEpisode?.episodeName
                }
                if (!hasOverview) {
                    episode.overview = fallbackEpisode?.overview
                }

                episode.toContentValues(
                    values, episodeTvdbId, seasonId, showId,
                    seasonNumber, releaseDateTime, localEpisodeOrNull == null
                )

                if (localEpisodeOrNull == null) {
                    // episode does not exist, yet: insert
                    newEpisodesValues.add(ContentValues(values))
                } else {
                    // episode exists: update
                    batch.add(
                        ContentProviderOperation
                            .newUpdate(Episodes.buildIdUri(localEpisodeOrNull.id))
                            .withValues(values).build()
                    )
                }

                values.clear()
            }
            page = response.links?.next
        }

        // Add delete ops for leftover episodes in local db.
        removableEpisodesByTvdbId.values.mapTo(batch) {
            ContentProviderOperation.newDelete(Episodes.buildIdUri(it.id)).build()
        }

        // Add delete ops for leftover seasons in local db.
        localSeasonsByTvdbId.values
                .filterNot { addedOrUpdatedSeasonsByTvdbId.contains(it.tvdbId) }
                .forEach {
                    batch.add(
                        ContentProviderOperation.newDelete(Seasons.buildIdUri(it.id)).build()
                    )
                }

        return newEpisodesValues
    }

    @Throws(TvdbException::class)
    private fun getEpisodes(showTvdbId: Int, page: Int, language: String): EpisodesResponse {
        val response: retrofit2.Response<EpisodesResponse>
        try {
            response = tvdbSeries.get().episodes(showTvdbId, page, language).execute()
        } catch (e: Exception) {
            Errors.logAndReport("getEpisodes", e)
            throw TvdbException("getEpisodes", e)
        }

        if (response.code() == 404) {
            // Special case for TheTVDB: no results.
            return EpisodesResponse()
                .also { it.data = emptyList() }
        }

        Errors.throwAndReportIfNotSuccessfulTvdb("getEpisodes", response.raw())

        return response.body()!!
    }

    /**
     * Inserts season if `seasonId` is null, or updates it instead with the given values.
     * Returns the seasonId. Throws if the database op failed.
     */
    @Throws(TvdbException::class)
    fun insertOrUpdateSeason(
        resolver: ContentResolver,
        showId: Long,
        seasonTvdbId: Int,
        seasonNumber: Int,
        seasonId: Long?
    ): Long {
        val values = ContentValues()
        values.put(Seasons.COMBINED, seasonNumber)
        values.put(Seasons.ORDER, seasonNumber)

        return if (seasonId == null) {
            values.put(Seasons.TVDB_ID, seasonTvdbId)
            values.put(Shows.REF_SHOW_ID, showId)
            // set default values
            values.put(Seasons.WATCHCOUNT, 0)
            values.put(Seasons.UNAIREDCOUNT, 0)
            values.put(Seasons.NOAIRDATECOUNT, 0)
            values.put(Seasons.TOTALCOUNT, 0)
            val seasonUri = resolver.insert(Seasons.CONTENT_URI, values)
                ?: throw TvdbDataException("Failed to insert season")
            Seasons.getId(seasonUri).toLong()
        } else {
            resolver.update(Seasons.buildIdUri(seasonId), values, null, null)
            seasonId
        }
    }

    companion object {

        @JvmStatic
        fun Episode.toContentValues(
            values: ContentValues,
            episodeTvdbId: Int,
            seasonId: Long,
            showId: Long,
            seasonNumber: Int,
            releaseDateTime: Long,
            forInsert: Boolean
        ) {
            values.put(Episodes.TVDB_ID, episodeTvdbId)
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

            values.put(Seasons.REF_SEASON_ID, seasonId)
            values.put(Shows.REF_SHOW_ID, showId)

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