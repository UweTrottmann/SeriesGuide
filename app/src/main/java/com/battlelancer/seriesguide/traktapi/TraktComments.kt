// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.Comment
import com.uwetrottmann.trakt5.entities.Episode
import com.uwetrottmann.trakt5.entities.EpisodeIds
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.awaitResponse
import java.util.regex.Pattern

/**
 * Post, edit or delete Trakt comments.
 */
class TraktComments(
    private val context: Context,
    private val trakt: TraktV2
) {

    private fun buildComment(comment: String, isSpoiler: Boolean) = Comment().also {
        it.comment = comment
        it.spoiler = isSpoiler
    }

    suspend fun postEpisodeComment(episodeTmdbId: Int, comment: String, isSpoiler: Boolean) {
        postComment(
            buildComment(comment, isSpoiler)
                .also {
                    it.episode = Episode().apply { ids = EpisodeIds.tmdb(episodeTmdbId) }
                }
        )
    }

    suspend fun postShowComment(showTraktId: Int, comment: String, isSpoiler: Boolean) {
        postComment(
            buildComment(comment, isSpoiler)
                .also {
                    it.show = Show().apply { ids = ShowIds.trakt(showTraktId) }
                }
        )
    }

    suspend fun postMovieComment(movieTmdbId: Int, comment: String, isSpoiler: Boolean) {
        postComment(
            buildComment(comment, isSpoiler)
                .also {
                    it.movie = Movie().apply { ids = MovieIds.tmdb(movieTmdbId) }
                }
        )
    }

    private suspend fun postComment(comment: Comment) {
        withContext(Dispatchers.IO) {
            executeCommentCall(trakt.comments().post(comment), "post comment")
        }
    }

    /**
     * Assumes the user connected to Trakt is the user of this comment.
     * Otherwise, the user is signed out.
     */
    suspend fun editComment(commentId: Int, comment: String, isSpoiler: Boolean) {
        val traktComment = buildComment(comment, isSpoiler).also {
            it.id = commentId
        }
        withContext(Dispatchers.IO) {
            executeCommentCall(trakt.comments().update(commentId, traktComment), "update comment")
        }
    }

    /**
     * Assumes the user connected to Trakt is the user of this comment.
     * Otherwise, the user is signed out.
     *
     * This may fail if the comment is too old or has replies.
     */
    suspend fun deleteComment(commentId: Int) {
        withContext(Dispatchers.IO) {
            executeCommentCall(trakt.comments().delete(commentId), "delete comment")
        }
    }

    private suspend fun <T> executeCommentCall(call: Call<T>, action: String) {
        var errorMessageOrNull: String? = null
        try {
            val response = call.awaitResponse()
            if (response.isSuccessful) {
                EventBus.getDefault()
                    .post(TraktTask.TraktActionCompleteEvent(TraktAction.COMMENT, true, null))
                return // Success
            } else {
                // https://trakt.docs.apiary.io/#reference/comments
                if (response.code() == 422) {
                    errorMessageOrNull = context.getString(R.string.shout_invalid)
                } else if (response.code() == 409) {
                    // Only when deleting is not allowed (too old or has replies)
                    errorMessageOrNull = context.getString(R.string.error_delete_comment)
                } else if (response.code() == 404) {
                    errorMessageOrNull = context.getString(R.string.trakt_error_not_exists)
                } else if (SgTrakt.isUnauthorized(response)) {
                    // for users banned from posting comments requests also return 401
                    // so do not sign out if an error header does not indicate the token is invalid
                    val authHeader = response.headers()["WWW-Authenticate"]
                    if (authHeader != null && !authHeader.contains("invalid_token")) {
                        val pattern = Pattern.compile("error_description=\"(.*)\"")
                        val matcher = pattern.matcher(authHeader)
                        errorMessageOrNull = if (matcher.find()) {
                            matcher.group(1)
                        } else {
                            context.getString(R.string.trakt_error_credentials)
                        }
                    } else {
                        TraktCredentials.get(context).setCredentialsInvalid()
                        errorMessageOrNull = context.getString(R.string.trakt_error_credentials)
                    }
                } else {
                    Errors.logAndReport(action, response)
                }
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }

        val errorMessage = errorMessageOrNull
            ?: context.getString(R.string.api_error_generic, context.getString(R.string.trakt))
        // In all other cases fail with error message
        EventBus.getDefault()
            .post(TraktTask.TraktActionCompleteEvent(TraktAction.COMMENT, false, errorMessage));
    }

}