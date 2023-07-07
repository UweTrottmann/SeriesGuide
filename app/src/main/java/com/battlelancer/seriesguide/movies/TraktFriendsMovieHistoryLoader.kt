package com.battlelancer.seriesguide.movies

import android.content.Context
import android.text.TextUtils
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.shows.history.NowAdapter.NowItem
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType

/**
 * Loads Trakt friends, then returns the most recently watched movie for each friend.
 */
internal class TraktFriendsMovieHistoryLoader(context: Context) :
    GenericSimpleLoader<List<NowItem>?>(context) {

    override fun loadInBackground(): List<NowItem>? {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return null
        }

        // get all trakt friends
        val traktUsers = SgApp.getServicesComponent(context).traktUsers()!!
        val friends = SgTrakt.executeAuthenticatedCall(
            context,
            traktUsers.friends(UserSlug.ME, Extended.FULL), "get friends"
        ) ?: return null

        val size = friends.size
        if (size == 0) {
            return null // no friends, done.
        }

        // estimate list size
        val items: MutableList<NowItem> = ArrayList(size + 1)

        // add header
        items.add(
            NowItem().header(context.getString(R.string.friends_recently), true)
        )

        // add last watched movie for each friend
        for (i in 0 until size) {
            val friend = friends[i]

            // at least need a userSlug
            val user = friend.user ?: continue
            val userSlug = user.ids?.slug
            if (TextUtils.isEmpty(userSlug)) {
                continue
            }

            // get last watched episode
            val history = SgTrakt.executeCall(
                traktUsers.history(
                    UserSlug(userSlug), HistoryType.MOVIES, 1, 1,
                    null, null, null
                ), "get friend movie history"
            )
            if (history == null || history.size == 0) {
                continue  // no history
            }

            val entry = history[0]
            val watchedAt = entry.watched_at
            val movie = entry.movie
            if (watchedAt == null || movie == null) {
                // missing required values
                continue
            }

            val avatar = user.images?.avatar?.full
            // Poster resolved on demand, see view holder binding.
            val nowItem = NowItem().displayData(
                watchedAt.toInstant().toEpochMilli(),
                movie.title,
                null,
                null
            )
                .tmdbId(movie.ids?.tmdb)
                .friend(user.username, avatar, entry.action)
            items.add(nowItem)
        }

        // only have a header? return nothing
        return if (items.size == 1) {
            emptyList()
        } else items
    }
}