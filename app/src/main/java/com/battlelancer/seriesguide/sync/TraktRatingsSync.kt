package com.battlelancer.seriesguide.sync

import android.content.ContentProviderOperation
import android.content.OperationApplicationException
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.entities.RatedEpisode
import com.uwetrottmann.trakt5.entities.RatedMovie
import com.uwetrottmann.trakt5.entities.RatedShow
import com.uwetrottmann.trakt5.enums.RatingsFilter
import org.threeten.bp.OffsetDateTime
import timber.log.Timber

/**
 * Downloads ratings for shows, episodes and movies from Trakt.
 */
class TraktRatingsSync(
    private val traktSync: TraktSync
) {
    private val context = traktSync.context

    /**
     * Downloads trakt show ratings and applies the latest ones to the database.
     *
     * To apply all ratings, set [TraktSettings.KEY_LAST_SHOWS_RATED_AT] to 0.
     */
    fun downloadForShows(ratedAt: OffsetDateTime?): Boolean {
        if (ratedAt == null) {
            Timber.e("downloadForShows: null rated_at")
            return false
        }

        val lastRatedAt = TraktSettings.getLastShowsRatedAt(context)
        if (!TimeTools.isAfterMillis(ratedAt, lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadForShows: no changes since %tF %tT", lastRatedAt, lastRatedAt)
            return true
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false
        }

        // download rated shows
        val ratedShows: List<RatedShow>?
        try {
            val response = traktSync.sync
                .ratingsShows(RatingsFilter.ALL, null, null, null)
                .execute()
            if (response.isSuccessful) {
                ratedShows = response.body()
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false
                }
                Errors.logAndReport("get show ratings", response)
                return false
            }
        } catch (e: Exception) {
            Errors.logAndReport("get show ratings", e)
            return false
        }
        if (ratedShows == null) {
            Timber.e("downloadForShows: null response")
            return false
        }
        if (ratedShows.isEmpty()) {
            Timber.d("downloadForShows: no ratings on trakt")
            return true
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        val ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS

        // go through ratings, latest first (trakt sends in that order)
        val tmdbIdsToRatings: MutableMap<Int, Int> = HashMap()
        for (show in ratedShows) {
            val rating = show.rating ?: continue
            val showTmdbId = show.show?.ids?.tmdb ?: continue
            val ratedAtTrakt = show.rated_at
            if (ratedAtTrakt != null
                && TimeTools.isBeforeMillis(ratedAtTrakt, ratedAtThreshold)) {
                // no need to apply older ratings again
                break
            }
            // if a show does not exist, this update will do nothing
            tmdbIdsToRatings[showTmdbId] = rating.value
        }

        // apply database updates
        SgRoomDatabase.getInstance(context).sgShow2Helper().updateUserRatings(tmdbIdsToRatings)

        // save last rated instant
        val ratedAtTime = ratedAt.toInstant().toEpochMilli()
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, ratedAtTime)
            .apply()

        Timber.d(
            "downloadForShows: success, last rated_at %tF %tT", ratedAtTime, ratedAtTime
        )
        return true
    }

    /**
     * Downloads trakt episode ratings and applies the latest ones to the database.
     *
     * To apply all ratings, set [TraktSettings.KEY_LAST_EPISODES_RATED_AT] to 0.
     */
    fun downloadForEpisodes(ratedAt: OffsetDateTime?): Boolean {
        if (ratedAt == null) {
            Timber.e("downloadForEpisodes: null rated_at")
            return false
        }

        val lastRatedAt = TraktSettings.getLastEpisodesRatedAt(context)
        if (!TimeTools.isAfterMillis(ratedAt, lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadForEpisodes: no changes since %tF %tT", lastRatedAt, lastRatedAt)
            return true
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false
        }

        // download rated episodes
        val ratedEpisodes: List<RatedEpisode>?
        try {
            val response = traktSync.sync
                .ratingsEpisodes(RatingsFilter.ALL, null, null, null)
                .execute()
            if (response.isSuccessful) {
                ratedEpisodes = response.body()
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false
                }
                Errors.logAndReport("get episode ratings", response)
                return false
            }
        } catch (e: Exception) {
            Errors.logAndReport("get episode ratings", e)
            return false
        }
        if (ratedEpisodes == null) {
            Timber.e("downloadForEpisodes: null response")
            return false
        }
        if (ratedEpisodes.isEmpty()) {
            Timber.d("downloadForEpisodes: no ratings on trakt")
            return true
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        val ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS

        val tmdbIdsToRatings: MutableMap<Int, Int> = HashMap()
        for (episode in ratedEpisodes) {
            val tmdbId = episode.episode?.ids?.tmdb ?: continue
            val rating = episode.rating ?: continue
            val ratedAtTrakt = episode.rated_at
            if (ratedAtTrakt != null
                && TimeTools.isBeforeMillis(ratedAtTrakt, ratedAtThreshold)) {
                // no need to apply older ratings again
                break
            }
            // if an episode does not exist, this update will do nothing
            tmdbIdsToRatings[tmdbId] = rating.value
        }

        // apply database updates
        SgRoomDatabase.getInstance(context).sgEpisode2Helper().updateUserRatings(tmdbIdsToRatings)

        // save last rated instant
        val ratedAtTime = ratedAt.toInstant().toEpochMilli()
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, ratedAtTime)
            .apply()

        Timber.d(
            "downloadForEpisodes: success, last rated_at %tF %tT", ratedAtTime, ratedAtTime
        )
        return true
    }

    /**
     * Downloads trakt movie ratings and applies the latest ones to the database.
     *
     * To apply all ratings, set [TraktSettings.KEY_LAST_MOVIES_RATED_AT] to 0.
     */
    fun downloadForMovies(ratedAt: OffsetDateTime?): Boolean {
        if (ratedAt == null) {
            Timber.e("downloadForMovies: null rated_at")
            return false
        }

        val lastRatedAt = TraktSettings.getLastMoviesRatedAt(context)
        if (!TimeTools.isAfterMillis(ratedAt, lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadForMovies: no changes since %tF %tT", lastRatedAt, lastRatedAt)
            return true
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false
        }

        // download rated shows
        val ratedMovies: List<RatedMovie>?
        try {
            val response = traktSync.sync
                .ratingsMovies(RatingsFilter.ALL, null, null, null)
                .execute()
            if (response.isSuccessful) {
                ratedMovies = response.body()
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false
                }
                Errors.logAndReport("get movie ratings", response)
                return false
            }
        } catch (e: Exception) {
            Errors.logAndReport("get movie ratings", e)
            return false
        }
        if (ratedMovies == null) {
            Timber.e("downloadForMovies: null response")
            return false
        }
        if (ratedMovies.isEmpty()) {
            Timber.d("downloadForMovies: no ratings on trakt")
            return true
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        val ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS

        // go through ratings, latest first (trakt sends in that order)
        val batch = ArrayList<ContentProviderOperation>()
        for (movie in ratedMovies) {
            val rating = movie.rating ?: continue
            val tmdbId = movie.movie?.ids?.tmdb ?: continue
            val ratedAtTrakt = movie.rated_at
            if (ratedAtTrakt != null &&
                TimeTools.isBeforeMillis(ratedAtTrakt, ratedAtThreshold)) {
                // no need to apply older ratings again
                break
            }

            // if a movie does not exist, this update will do nothing
            val op = ContentProviderOperation.newUpdate(Movies.buildMovieUri(tmdbId))
                .withValue(Movies.RATING_USER, rating.value)
                .build()
            batch.add(op)
        }

        // apply database updates
        try {
            DBUtils.applyInSmallBatches(context, batch)
        } catch (e: OperationApplicationException) {
            Timber.e(e, "downloadForMovies: database update failed")
            return false
        }

        // save last rated instant
        val ratedAtTime = ratedAt.toInstant().toEpochMilli()
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(TraktSettings.KEY_LAST_MOVIES_RATED_AT, ratedAtTime)
            .apply()

        Timber.d("downloadForMovies: success, last rated_at %tF %tT", ratedAtTime, ratedAtTime)
        return true
    }
}