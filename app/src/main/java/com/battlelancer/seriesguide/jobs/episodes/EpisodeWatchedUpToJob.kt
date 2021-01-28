package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags

/**
 * Set episodes watched up to (== including) a specific one excluding those with no release date.
 */
class EpisodeWatchedUpToJob(
    private val showId: Long,
    private val episodeFirstAired: Long,
    private val episodeNumber: Int
) : BaseEpisodesJob(EpisodeFlags.WATCHED, JobAction.EPISODE_WATCHED_FLAG) {

    override fun getShowId(): Long {
        return showId
    }

    override fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean {
        if (!super.applyLocalChanges(context, requiresNetworkJob)) {
            return false
        }

        // we don't care about the last watched episode value
        // always update last watched time, this type only marks as watched
        updateLastWatched(context, -1, true)

        ListWidgetProvider.notifyDataChanged(context)

        return true
    }

    override fun applyDatabaseChanges(context: Context): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .setWatchedUpToAndAddPlay(showId, episodeFirstAired, episodeNumber)
        return rowsUpdated >= 0 // -1 means error
    }

    override fun getEpisodesForNetworkJob(context: Context): List<SgEpisode2Numbers> {
        return SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .getEpisodeNumbersForWatchedUpTo(showId, episodeFirstAired, episodeNumber)
    }

    /**
     * Note: this should mirror the planned database changes in [applyDatabaseChanges].
     */
    override fun getPlaysForNetworkJob(plays: Int): Int {
        return plays + 1
    }

    override fun getConfirmationText(context: Context): String {
        return context.getString(R.string.action_watched_up_to)
    }
}
