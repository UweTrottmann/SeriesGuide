package com.battlelancer.seriesguide.ui.streams

import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import retrofit2.Call

/**
 * Loads the last few episodes watched on Trakt.
 */
internal open class TraktEpisodeHistoryLoader(context: Context)
    : GenericSimpleLoader<TraktEpisodeHistoryLoader.Result>(context) {

    class Result(
        var results: List<HistoryEntry?>?,
        var emptyText: String
    )

    override fun loadInBackground(): Result {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials)
        }

        var history: List<HistoryEntry?>? = null
        try {
            val response = buildCall().execute()
            if (response.isSuccessful) {
                history = response.body()
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return buildResultFailure(R.string.trakt_error_credentials)
                }
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultFailure()
            } else {
                buildResultFailure(R.string.offline)
            }
        }

        return if (history == null) {
            buildResultFailure()
        } else {
            Result(
                history,
                context.getString(emptyText)
            )
        }
    }

    open val action: String
        get() = "get user episode history"

    @get:StringRes
    open val emptyText: Int
        get() = R.string.user_stream_empty

    protected open fun buildCall(): Call<List<HistoryEntry?>?> {
        val traktUsers = SgApp.getServicesComponent(context).traktUsers()
        return traktUsers.history(
            UserSlug.ME,
            HistoryType.EPISODES,
            1,
            MAX_HISTORY_SIZE,
            null,
            null,
            null
        )
    }

    private fun buildResultFailure(): Result {
        return Result(
            null, context.getString(
                R.string.api_error_generic,
                context.getString(R.string.trakt)
            )
        )
    }

    private fun buildResultFailure(@StringRes emptyTextResId: Int): Result {
        return Result(
            null,
            context.getString(emptyTextResId)
        )
    }

    companion object {
        const val MAX_HISTORY_SIZE = 50
    }
}