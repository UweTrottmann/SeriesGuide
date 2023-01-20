package com.battlelancer.seriesguide.shows.history

import android.content.Context
import android.text.TextUtils
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.history.NowAdapter.NowItem
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType

/**
 * Loads Trakt friends, then returns the most recently watched episode for each friend.
 */
internal class TraktFriendsEpisodeHistoryLoader(context: Context) :
    GenericSimpleLoader<List<NowItem>?>(context) {

    override fun loadInBackground(): List<NowItem>? {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return null
        }

        // get all trakt friends
        val services = getServicesComponent(context)
        val traktUsers = services.traktUsers()!!
        val friends = SgTrakt.executeAuthenticatedCall(
            context,
            traktUsers.friends(UserSlug.ME, Extended.FULL),
            "get friends"
        ) ?: return null

        val size = friends.size
        if (size == 0) {
            return null // no friends, done.
        }

        // estimate list size
        val items: MutableList<NowItem> = ArrayList(size + 1)

        // add header
        items.add(
            NowItem().header(context.getString(R.string.friends_recently))
        )

        // add last watched episode for each friend
        val tmdbIdsToPoster = services.showTools().getTmdbIdsToPoster()
        val episodeHelper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val hideTitle = DisplaySettings.preventSpoilers(context)
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
                    UserSlug(userSlug), HistoryType.EPISODES, 1, 1,
                    null, null, null
                ), "get friend episode history"
            )
            if (history == null || history.size == 0) {
                continue  // no history
            }

            val entry = history[0]
            val watchedAt = entry.watched_at
            val episode = entry.episode
            val season = episode?.season
            val number = episode?.number
            val show = entry.show
            if (watchedAt == null
                || episode == null || season == null || number == null
                || show == null) {
                // missing required values
                continue
            }

            // look for a poster
            val showTmdbId = show.ids?.tmdb
            val posterUrl: String? = if (showTmdbId != null) {
                // prefer poster of already added show, fall back to first uploaded poster
                ImageTools.posterUrlOrResolve(
                    tmdbIdsToPoster[showTmdbId],
                    showTmdbId, LanguageTools.LANGUAGE_EN, context
                )
            } else {
                null
            }

            val avatar = user.images?.avatar?.full
            val episodeString = TextTools.getNextEpisodeString(
                context,
                season, number,
                if (hideTitle) null else episode.title
            )

            val episodeTmdbIdOrNull = episode.ids?.tmdb
            val localEpisodeIdOrZero = if (episodeTmdbIdOrNull != null) {
                episodeHelper.getEpisodeIdByTmdbId(episodeTmdbIdOrNull)
            } else 0

            val nowItem = NowItem()
                .displayData(
                    watchedAt.toInstant().toEpochMilli(),
                    show.title,
                    episodeString,
                    posterUrl
                )
                .episodeIds(localEpisodeIdOrZero, showTmdbId ?: 0)
                .friend(user.username, avatar, entry.action)
            items.add(nowItem)
        }

        // only have a header? return nothing
        return if (items.size == 1) {
            emptyList()
        } else items
    }
}