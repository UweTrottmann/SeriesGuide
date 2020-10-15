package com.battlelancer.seriesguide.traktapi

import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import androidx.collection.LruCache
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.Ratings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Downloads Trakt ratings for a show or episode, stores them in the database.
 * Recent downloads are cached for some minutes.
 */
object TraktRatingsFetcher {

    private const val HARD_CACHE_CAPACITY = 50
    private const val MAXIMUM_AGE = 5 * DateUtils.MINUTE_IN_MILLIS
    
    // Hard cache, with a fixed maximum capacity
    private val lruCache = LruCache<Long, Long>(HARD_CACHE_CAPACITY)

    @JvmStatic
    fun fetchShowRatingsAsync(
        context: Context,
        showTvdbId: Int
    ): Job {
        return SgApp.coroutineScope.launch {
            fetchRating(context.applicationContext, showTvdbId)
        }
    }

    @JvmStatic
    fun fetchEpisodeRatingsAsync(
        context: Context,
        showTvdbId: Int,
        episodeTvdbId: Int,
        season: Int,
        episode: Int
    ): Job {
        return SgApp.coroutineScope.launch {
            fetchRating(context.applicationContext, showTvdbId, episodeTvdbId, season, episode)
        }
    }

    private suspend fun fetchRating(
        context: Context,
        showTvdbId: Int,
        episodeTvdbId: Int = 0,
        season: Int = 0,
        episode: Int = 0
    ) = withContext(Dispatchers.IO) {
        val ratingId: Long = createUniqueId(showTvdbId.toLong(), episodeTvdbId.toLong())

        // avoid saving ratings too frequently
        // (network requests are cached, but also avoiding database writes)
        val currentTimeMillis = System.currentTimeMillis()
        synchronized(lruCache) {
            val lastUpdateMillis = lruCache[ratingId]
            // if the ratings were just updated, do nothing
            if (lastUpdateMillis != null && lastUpdateMillis > currentTimeMillis - MAXIMUM_AGE) {
                Timber.d("Just loaded rating for %s, skip.", ratingId)
                return@withContext
            }
        }

        if (!isActive || !AndroidUtils.isNetworkConnected(context)) {
            return@withContext
        }

        // look up show trakt id
        val showTraktId = ShowTools.getShowTraktId(context, showTvdbId)
        if (showTraktId == null) {
            Timber.d("Show %s has no trakt id, skip.", showTvdbId)
            return@withContext
        }

        val showTraktIdString = showTraktId.toString()
        val isShowNotEpisode = episodeTvdbId == 0
        Timber.i(
            "Loading rating for %s (rating ID %s)",
            if (isShowNotEpisode) "show $showTvdbId" else "episode $episodeTvdbId", ratingId
        )

        val ratings: Ratings? = if (isShowNotEpisode) {
            SgTrakt.executeCall(
                SgApp.getServicesComponent(context).trakt()
                    .shows().ratings(showTraktIdString),
                "get show rating"
            )
        } else {
            SgTrakt.executeCall(
                SgApp.getServicesComponent(context).trakt()
                    .episodes().ratings(showTraktIdString, season, episode),
                "get episode rating"
            )
        }
        if (ratings?.rating != null && ratings.votes != null) {
            if (isShowNotEpisode) {
                saveShowRating(context, ratings, showTvdbId)
            } else {
                saveEpisodeRating(context, ratings, episodeTvdbId)
            }
        }

        // cache download time to avoid saving ratings too frequently
        synchronized(lruCache) {
            lruCache.put(ratingId, currentTimeMillis)
        }
    }

    /**
     * Creates a unique id using the 
     * [Cantor pairing](https://en.wikipedia.org/wiki/Cantor_pairing_function) function.
     */
    private fun createUniqueId(showTvdbId: Long, episodeTvdbId: Long): Long {
        return ((showTvdbId + episodeTvdbId) * (showTvdbId + episodeTvdbId + 1) / 2) + episodeTvdbId
    }

    private fun saveShowRating(context: Context, ratings: Ratings, showTvdbId: Int) {
        val values = ContentValues()
        values.put(SeriesGuideContract.Shows.RATING_GLOBAL, ratings.rating)
        values.put(SeriesGuideContract.Shows.RATING_VOTES, ratings.votes)
        context.contentResolver
            .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null)
    }

    private fun saveEpisodeRating(context: Context, ratings: Ratings, episodeTvdbId: Int) {
        val values = ContentValues()
        values.put(SeriesGuideContract.Episodes.RATING_GLOBAL, ratings.rating)
        values.put(SeriesGuideContract.Episodes.RATING_VOTES, ratings.votes)
        context.contentResolver
            .update(
                SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), values, null,
                null
            )

        // notify withshow uri as well (used by episode details view)
        context.contentResolver
            .notifyChange(
                SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                null
            )
    }

}