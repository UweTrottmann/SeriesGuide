package com.battlelancer.seriesguide.ui.stats

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.shows.ShowTools.*
import kotlinx.coroutines.Dispatchers

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    val hideSpecials = MutableLiveData<Boolean>()
    val statsData = Transformations.switchMap(hideSpecials) { hideSpecials ->
        loadStats(hideSpecials)
    }

    init {
        hideSpecials.value = DisplaySettings.isHidingSpecials(application)
    }

    private fun loadStats(excludeSpecials: Boolean) = liveData(
        context = viewModelScope.coroutineContext + Dispatchers.IO
    ) {
        val stats = Stats()

        // movies
        countMovies(stats)
        emit(buildUpdate(stats))

        // shows
        val showRuntimes = countShows(stats, excludeSpecials)
        emit(buildUpdate(stats))

        // episodes
        countEpisodes(stats, excludeSpecials)
        emit(buildUpdate(stats))

        // calculate runtime of watched episodes per show
        var totalRuntimeMin: Long = 0
        var previewTime = System.currentTimeMillis() + PREVIEW_UPDATE_INTERVAL_MS
        for (showRuntime in showRuntimes) {
            val showId = showRuntime.key
            val runtimeOfShowMin = showRuntime.value
            val helper = SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
            val watchedEpisodesOfShowCount = if (excludeSpecials) {
                helper.countWatchedEpisodesOfShowWithoutSpecials(showId)
            } else {
                helper.countWatchedEpisodesOfShow(showId)
            }
            if (watchedEpisodesOfShowCount == -1) {
                // episode query failed, return what we have so far
                stats.episodesWatchedRuntime = totalRuntimeMin * DateUtils.MINUTE_IN_MILLIS
                emit(StatsUpdateEvent(
                    stats,
                    finalValues = false,
                    successful = false
                ))
                return@liveData
            }
            // make sure we calculate with long here (first arg is long) to avoid overflows
            val runtimeOfEpisodesMin = runtimeOfShowMin * watchedEpisodesOfShowCount
            totalRuntimeMin += runtimeOfEpisodesMin

            // post regular update of minimum
            val currentTime = System.currentTimeMillis()
            if (currentTime > previewTime) {
                previewTime = currentTime + PREVIEW_UPDATE_INTERVAL_MS
                stats.episodesWatchedRuntime = totalRuntimeMin * DateUtils.MINUTE_IN_MILLIS
                emit(buildUpdate(stats))
            }
        }

        stats.episodesWatchedRuntime = totalRuntimeMin * DateUtils.MINUTE_IN_MILLIS

        // return final values
        emit(StatsUpdateEvent(
            stats,
            finalValues = true,
            successful = true
        ))
    }

    private fun buildUpdate(stats: Stats): StatsUpdateEvent {
        return StatsUpdateEvent(
            stats,
            finalValues = false,
            successful = true
        )
    }

    private fun countMovies(stats: Stats) {
        val helper = SgRoomDatabase.getInstance(getApplication()).movieHelper()
        val countMovies = helper.countMovies()
        val statsWatched = helper.getStatsWatched()
        val statsInWatchlist = helper.getStatsInWatchlist()
        val statsInCollection = helper.getStatsInCollection()
        stats.movies = countMovies
        stats.moviesWatched = statsWatched?.count ?: 0
        stats.moviesWatchedRuntime = (statsWatched?.runtime ?: 0) * DateUtils.MINUTE_IN_MILLIS
        stats.moviesWatchlist = statsInWatchlist?.count ?: 0
        stats.moviesWatchlistRuntime = (statsInWatchlist?.runtime ?: 0) * DateUtils.MINUTE_IN_MILLIS
        stats.moviesCollection = statsInCollection?.count ?: 0
        stats.moviesCollectionRuntime =
            (statsInCollection?.runtime ?: 0) * DateUtils.MINUTE_IN_MILLIS
    }

    /**
     * Returns shows mapped to their runtime.
     */
    private fun countShows(stats: Stats, excludeSpecials: Boolean): Map<Long, Int> {
        val helper = SgRoomDatabase.getInstance(getApplication()).sgShow2Helper()
        val showStats = helper.getStats()

        var continuing = 0
        var withnext = 0
        val showRuntimes = mutableMapOf<Long, Int>()
        for (show in showStats) {
            // count continuing shows
            if (show.status == Status.RETURNING) {
                continuing++
            }
            // count shows that are planned to receive new episodes
            if (show.status == Status.RETURNING
                || show.status == Status.PLANNED
                || show.status == Status.IN_PRODUCTION) {
                withnext++
            }
            // map show to its runtime
            showRuntimes[show.id] = show.runtime
        }

        stats.shows = showStats.size
        stats.showsContinuing = continuing
        stats.showsWithNextEpisodes = withnext

        stats.showsFinished = if (excludeSpecials) {
            helper.countShowsFinishedWatchingWithoutSpecials()
        } else {
            helper.countShowsFinishedWatching()
        }

        return showRuntimes
    }

    private fun countEpisodes(stats: Stats, excludeSpecials: Boolean) {
        val helper = SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
        stats.episodes = if (excludeSpecials) {
            helper.countEpisodesWithoutSpecials()
        } else {
            helper.countEpisodes()
        }
        stats.episodesWatched = if (excludeSpecials) {
            helper.countWatchedEpisodesWithoutSpecials()
        } else {
            helper.countWatchedEpisodes()
        }
    }

    companion object {
        private const val PREVIEW_UPDATE_INTERVAL_MS = DateUtils.SECOND_IN_MILLIS
    }

}

data class Stats(
    var shows: Int = 0,
    var showsFinished: Int = 0,
    var showsContinuing: Int = 0,
    var showsWithNextEpisodes: Int = 0,
    var episodes: Int = 0,
    var episodesWatched: Int = 0,
    var episodesWatchedRuntime: Long = 0,
    var movies: Int = 0,
    var moviesWatchlist: Int = 0,
    var moviesWatchlistRuntime: Long = 0,
    var moviesWatched: Int = 0,
    var moviesWatchedRuntime: Long = 0,
    var moviesCollection: Int = 0,
    var moviesCollectionRuntime: Long = 0
)

data class StatsUpdateEvent(
    val stats: Stats,
    val finalValues: Boolean,
    val successful: Boolean
)
