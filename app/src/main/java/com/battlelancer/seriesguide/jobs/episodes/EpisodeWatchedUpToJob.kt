package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import android.net.Uri
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags

/**
 * Set episodes watched up to (== including) a specific one excluding those with no release date.
 */
class EpisodeWatchedUpToJob(
    showTvdbId: Int,
    private val episodeFirstAired: Long,
    private val episodeNumber: Int
) : BaseEpisodesJob(showTvdbId, EpisodeFlags.WATCHED, JobAction.EPISODE_WATCHED_FLAG) {

    public override fun getDatabaseUri(): Uri {
        return Episodes.buildEpisodesOfShowUri(showTvdbId.toString())
    }

    public override fun getDatabaseSelection(): String {
        // Must
        // - be released before current episode,
        // - OR at the same time, but with same (itself) or lower (all released at same time) number
        // - have a release date,
        // - be unwatched or skipped.
        return ("("
                + Episodes.FIRSTAIREDMS + "<" + episodeFirstAired
                + " OR (" + Episodes.FIRSTAIREDMS + "=" + episodeFirstAired
                + " AND " + Episodes.NUMBER + "<=" + episodeNumber + ")"
                + ")"
                + " AND " + Episodes.SELECTION_HAS_RELEASE_DATE
                + " AND " + Episodes.SELECTION_UNWATCHED_OR_SKIPPED)
    }

    override fun getDatabaseColumnToUpdate(): String {
        return Episodes.WATCHED
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

    override fun applyDatabaseChanges(context: Context, uri: Uri): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).episodeHelper()
            .setWatchedUpToAndAddPlay(showTvdbId, episodeFirstAired, episodeNumber)
        return rowsUpdated >= 0 // -1 means error
    }

    override fun getConfirmationText(context: Context): String {
        return context.getString(R.string.action_watched_up_to)
    }
}
