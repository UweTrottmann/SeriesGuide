package com.battlelancer.seriesguide.traktapi

import android.content.Context
import android.text.format.DateUtils
import androidx.collection.LruCache
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Downloads Trakt ratings for an episode, stores them in the database.
 * The recent 50 downloads are cached for at most a day.
 */
object TraktRatingsFetcher {

    private const val HARD_CACHE_CAPACITY = 50
    private const val MAXIMUM_AGE = DateUtils.DAY_IN_MILLIS

    // Hard cache, with a fixed maximum capacity
    private val lruCache = LruCache<Long, Long>(HARD_CACHE_CAPACITY)

    @JvmStatic
    fun fetchEpisodeRatingsAsync(
        context: Context,
        episodeId: Long
    ): Job {
        return SgApp.coroutineScope.launch {
            fetchRating(context.applicationContext, episodeId)
        }
    }

    private suspend fun fetchRating(
        context: Context,
        episodeId: Long,
    ) = withContext(Dispatchers.IO) {
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()

        val episode = helper.getEpisodeNumbers(episodeId)
            ?: return@withContext

        // avoid saving ratings too frequently
        // (network requests are cached, but also avoiding database writes)
        val currentTimeMillis = System.currentTimeMillis()
        synchronized(lruCache) {
            val lastUpdateMillis = lruCache[episodeId]
            // if the ratings were just updated, do nothing
            if (lastUpdateMillis != null && lastUpdateMillis > currentTimeMillis - MAXIMUM_AGE) {
                Timber.d("Just loaded rating for %s, skip.", episodeId)
                return@withContext
            }
        }

        if (!isActive || !AndroidUtils.isNetworkConnected(context)) {
            return@withContext
        }

        // look up show trakt id
        val showTraktId = ShowTools.getShowTraktId(context, episode.showId)
        if (showTraktId == null) {
            Timber.d("Show %s has no trakt id, skip.", episode.showId)
            return@withContext
        }

        val showTraktIdString = showTraktId.toString()
        Timber.d("Updating rating for episode $episodeId")

        val ratings = TraktTools2.getEpisodeRatings(
            context,
            showTraktIdString,
            episode.season,
            episode.episodenumber
        )
        if (ratings != null) {
            helper.updateRating(episodeId, ratings.first, ratings.second)
        }

        // cache download time to avoid saving ratings too frequently
        synchronized(lruCache) {
            lruCache.put(episodeId, currentTimeMillis)
        }
    }

}