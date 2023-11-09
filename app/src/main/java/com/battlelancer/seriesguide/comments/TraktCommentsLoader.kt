// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.comments

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.Comment
import com.uwetrottmann.trakt5.enums.Extended
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads up comments from Trakt for a movie, show or episode.
 */
class TraktCommentsLoader(context: Context, private val args: Bundle) :
    GenericSimpleLoader<TraktCommentsLoader.Result>(context) {

    data class Result(val results: List<Comment>?, val emptyText: String)

    @Inject
    lateinit var trakt: TraktV2

    init {
        SgApp.getServicesComponent(context).inject(this)
    }

    override fun loadInBackground(): Result {
        // movie comments?
        val movieTmdbId = args.getInt(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID)
        if (movieTmdbId != 0) {
            val movieTraktId = TraktTools.lookupMovieTraktId(trakt, movieTmdbId)
            if (movieTraktId != null) {
                if (movieTraktId == -1) {
                    return buildResultFailure(R.string.trakt_error_not_exists)
                }
                try {
                    val response = trakt.movies()
                        .comments(movieTraktId.toString(), 1, PAGE_SIZE, Extended.FULL)
                        .execute()
                    if (response.isSuccessful) {
                        return buildResultSuccess(response.body()!!)
                    } else {
                        Errors.logAndReport("get movie comments", response)
                    }
                } catch (e: Exception) {
                    Errors.logAndReport("get movie comments", e)
                }
            }
            return buildResultFailureWithOfflineCheck()
        }

        // episode comments?
        val episodeId = args.getLong(TraktCommentsFragment.InitBundle.EPISODE_ID)
        if (episodeId != 0L) {
            // look up episode number, season and show id
            val database = SgRoomDatabase.getInstance(context)
            val episode = database.sgEpisode2Helper().getEpisodeNumbers(episodeId)
            if (episode == null) {
                Timber.e("Failed to get episode %d", episodeId)
                return buildResultFailure(R.string.unknown)
            }
            // look up show trakt id
            val showTraktId = SgApp.getServicesComponent(context)
                .showTools()
                .getShowTraktId(episode.showId)
            if (showTraktId == null) {
                Timber.e("Failed to get show %d", episode.showId)
                return buildResultFailure(R.string.trakt_error_not_exists)
            }
            try {
                val response = trakt.episodes()
                    .comments(
                        showTraktId.toString(),
                        episode.season,
                        episode.episodenumber,
                        1, PAGE_SIZE, Extended.FULL
                    ).execute()
                if (response.isSuccessful) {
                    return buildResultSuccess(response.body()!!)
                } else {
                    Errors.logAndReport("get episode comments", response)
                }
            } catch (e: Exception) {
                Errors.logAndReport("get episode comments", e)
            }
            return buildResultFailureWithOfflineCheck()
        }

        // show comments!
        val showId = args.getLong(TraktCommentsFragment.InitBundle.SHOW_ID)
        val showTraktId = SgApp.getServicesComponent(context)
            .showTools()
            .getShowTraktId(showId) ?: return buildResultFailure(R.string.trakt_error_not_exists)
        try {
            val response = trakt.shows()
                .comments(showTraktId.toString(), 1, PAGE_SIZE, Extended.FULL)
                .execute()
            if (response.isSuccessful) {
                return buildResultSuccess(response.body()!!)
            } else {
                Errors.logAndReport("get show comments", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get show comments", e)
        }
        return buildResultFailureWithOfflineCheck()
    }

    private fun buildResultSuccess(results: List<Comment>): Result {
        return Result(results, context.getString(R.string.no_shouts))
    }

    private fun buildResultFailure(@StringRes emptyTextResId: Int): Result {
        return Result(null, context.getString(emptyTextResId))
    }

    private fun buildResultFailureWithOfflineCheck(): Result {
        val emptyText: String = if (AndroidUtils.isNetworkConnected(context)) {
            context.getString(
                R.string.api_error_generic,
                context.getString(R.string.trakt)
            )
        } else {
            context.getString(R.string.offline)
        }
        return Result(null, emptyText)
    }

    companion object {
        private const val PAGE_SIZE = 25
    }
}