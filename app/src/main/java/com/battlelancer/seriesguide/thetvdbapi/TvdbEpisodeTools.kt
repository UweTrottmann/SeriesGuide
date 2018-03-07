package com.battlelancer.seriesguide.thetvdbapi

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools.ensureSuccessfulResponse
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.thetvdb.entities.Episode
import com.uwetrottmann.thetvdb.entities.EpisodesResponse
import com.uwetrottmann.thetvdb.services.TheTvdbSeries
import dagger.Lazy
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
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
    fun fetchEpisodes(batch: ArrayList<ContentProviderOperation>, show: Show,
            language: String): ArrayList<ContentValues> {
        val showTvdbId = show.tvdb_id
        val newEpisodesValues = ArrayList<ContentValues>()

        val localEpisodeIds = DBUtils.getEpisodeMapForShow(context, showTvdbId)
        // just copy episodes list, then remove valid ones
        val removableEpisodeIds = HashMap(localEpisodeIds)

        val localSeasonIds = DBUtils.getSeasonIdsOfShow(context, showTvdbId)
        // store updated seasons to avoid duplicate ops
        val seasonsToAddOrUpdate = HashSet<Int>()

        val dateLastMonthEpoch = (System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 30) / 1000
        val showTimeZone = TimeTools.getDateTimeZone(show.release_timezone)
        val showReleaseTime = TimeTools.getShowReleaseTime(show.release_time)
        val deviceTimeZone = TimeZone.getDefault().id

        val fallback = DisplaySettings.getShowsLanguageFallback(context)
        val fallbackLanguage: String? = if (fallback != language) fallback else null

        var page: Int? = 0
        while (page != null) {
            val response = getEpisodes(showTvdbId, page, language)

            // fall back if no translation is available for some episodes
            // note: just checking errors is not enough as no error if just some are not translated
            val fallbackResponse = if (fallbackLanguage != null
                    && (response.errors?.invalidLanguage != null
                    || response.data.find { it.episodeName.isNullOrEmpty() || it.overview.isNullOrEmpty() } != null)) {
                // assumes that episode pages match between languages
                // worst case: no fallback title or overview
                getEpisodes(showTvdbId, page, fallbackLanguage)
            } else {
                null
            }

            val values = ContentValues()
            for (episode in response.data) {
                val episodeId = episode.id
                val seasonNumber = episode.airedSeason
                val seasonId = episode.airedSeasonID
                if (episodeId == null || episodeId <= 0
                        || seasonNumber == null || seasonNumber < 0 // season 0 allowed (specials)

                        || seasonId == null || seasonId <= 0) {
                    continue // invalid ids, skip
                }

                // add insert/update op for season, prevents it from getting cleaned
                if (!seasonsToAddOrUpdate.contains(seasonId)) {
                    batch.add(DBUtils.buildSeasonOp(showTvdbId, seasonId, seasonNumber,
                            !localSeasonIds.contains(seasonId)))
                    seasonsToAddOrUpdate.add(seasonId)
                }

                // don't clean up this episode
                removableEpisodeIds.remove(episodeId)

                var insert = true
                if (localEpisodeIds.containsKey(episodeId)) {
                    /*
                     * Update uses provider ops which take a long time. Only
                     * update if episode was edited on TVDb or is not older than
                     * a month (ensures show air time changes get stored).
                     */
                    val lastEditEpoch = localEpisodeIds[episodeId]
                    val lastEditEpochNew = episode.lastUpdated
                    if (lastEditEpoch != null && lastEditEpochNew != null
                            && (lastEditEpoch < lastEditEpochNew || dateLastMonthEpoch < lastEditEpoch)) {
                        // update episode
                        insert = false
                    } else {
                        continue // too old to update, skip
                    }
                }

                // calculate release time
                val releaseDateTime = TimeTools.parseEpisodeReleaseDate(context, showTimeZone,
                        episode.firstAired, showReleaseTime, show.country, show.network,
                        deviceTimeZone)
                // if name or overview are empty use fallback
                val hasName = !episode.episodeName.isNullOrEmpty()
                val hasOverview = !episode.overview.isNullOrEmpty()
                val fallbackEpisode = if (!hasName || !hasOverview) {
                    fallbackResponse?.data?.find { it.id == episodeId }
                } else {
                    null
                }
                if (!hasName) {
                    episode.episodeName = fallbackEpisode?.episodeName
                }
                if (!hasOverview) {
                    episode.overview = fallbackEpisode?.overview
                }

                episode.toContentValues(values, episodeId, seasonId, showTvdbId,
                        seasonNumber, releaseDateTime)

                if (insert) {
                    // episode does not exist, yet: insert
                    newEpisodesValues.add(ContentValues(values))
                } else {
                    // episode exists: update
                    batch.add(DBUtils.buildEpisodeUpdateOp(values))
                }

                values.clear()
            }
            page = response.links.next
        }

        // add delete ops for leftover episodeIds in our db
        removableEpisodeIds.keys.mapTo(batch) {
            ContentProviderOperation.newDelete(Episodes.buildEpisodeUri(it)).build()
        }

        // add delete ops for leftover seasonIds in our db
        localSeasonIds
                .filterNot { seasonsToAddOrUpdate.contains(it) }
                .mapTo(batch) {
                    ContentProviderOperation.newDelete(
                            SeriesGuideContract.Seasons.buildSeasonUri(it)).build()
                }

        return newEpisodesValues
    }

    @Throws(TvdbException::class)
    private fun getEpisodes(showTvdbId: Int, page: Int, language: String): EpisodesResponse {
        val response: retrofit2.Response<EpisodesResponse>
        try {
            response = tvdbSeries.get().episodes(showTvdbId, page, language).execute()
        } catch (e: IOException) {
            throw TvdbException("getEpisodes: " + e.message, e)
        }

        ensureSuccessfulResponse(response.raw(), "getEpisodes: ")

        return response.body()!!
    }


    companion object {

        @JvmStatic
        fun Episode.toContentValues(values: ContentValues,
                episodeTvdbId: Int, seasonTvdbId: Int, showTvdbId: Int,
                seasonNumber: Int, releaseDateTime: Long) {
            values.put(Episodes._ID, episodeTvdbId)
            values.put(Episodes.TITLE, episodeName ?: "")
            values.put(Episodes.OVERVIEW, overview)
            values.put(Episodes.NUMBER, airedEpisodeNumber ?: 0)
            values.put(Episodes.SEASON, seasonNumber)
            values.put(Episodes.DVDNUMBER, dvdEpisodeNumber)

            values.put(SeriesGuideContract.Seasons.REF_SEASON_ID, seasonTvdbId)
            values.put(SeriesGuideContract.Shows.REF_SHOW_ID, showTvdbId)

            values.put(Episodes.FIRSTAIREDMS, releaseDateTime)
            values.put(Episodes.ABSOLUTE_NUMBER, absoluteNumber)
            values.put(Episodes.LAST_EDITED, lastUpdated ?: 0)

            // set default values
            values.put(Episodes.WATCHED, 0)
            values.put(Episodes.DIRECTORS, "")
            values.put(Episodes.GUESTSTARS, "")
            values.put(Episodes.WRITERS, "")
            values.put(Episodes.IMAGE, "")
            values.put(Episodes.COLLECTED, 0)
            values.put(Episodes.IMDBID, "")
            values.put(Episodes.LAST_UPDATED, 0)
        }

    }

}