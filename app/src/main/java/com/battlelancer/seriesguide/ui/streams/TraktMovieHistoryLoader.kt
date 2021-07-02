package com.battlelancer.seriesguide.ui.streams

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import retrofit2.Call

/**
 * Loads the last few movies watched on Trakt.
 */
internal class TraktMovieHistoryLoader(context: Context) : TraktEpisodeHistoryLoader(context) {

    override val action: String
        get() = "get user movie history"

    override val emptyText: Int
        get() = R.string.now_movies_empty

    override fun buildCall(): Call<List<HistoryEntry?>?> {
        val traktUsers = SgApp.getServicesComponent(context).traktUsers()!!
        return traktUsers.history(
            UserSlug.ME,
            HistoryType.MOVIES,
            1,
            MAX_HISTORY_SIZE,
            null,
            null,
            null
        )
    }
}