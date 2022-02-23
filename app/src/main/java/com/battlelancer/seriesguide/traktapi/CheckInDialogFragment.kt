package com.battlelancer.seriesguide.traktapi

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.safeShow

/**
 * Allows to check into an episode. Launching activities should subscribe to
 * [TraktTask.TraktActionCompleteEvent] to display status toasts.
 */
class CheckInDialogFragment : GenericCheckInDialogFragment() {

    override fun checkInTrakt(message: String) {
        TraktTask(requireContext()).checkInEpisode(
            requireArguments().getLong(ARG_EPISODE_ID),
            requireArguments().getString(ARG_ITEM_TITLE),
            message
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    companion object {
        /**
         * Builds and shows a new [CheckInDialogFragment] setting all values based on the given
         * episode row ID.
         *
         * @return `false` if the fragment was not shown.
         */
        fun show(context: Context, fragmentManager: FragmentManager, episodeId: Long): Boolean {
            val episode = getInstance(context).sgEpisode2Helper().getEpisodeWithShow(episodeId)
                ?: return false

            val f = CheckInDialogFragment()

            val args = Bundle()
            args.putLong(ARG_EPISODE_ID, episodeId)
            val episodeTitleWithNumbers = (episode.seriestitle
                    + " "
                    + TextTools.getNextEpisodeString(
                context,
                episode.season,
                episode.episodenumber,
                episode.episodetitle
            ))
            args.putString(ARG_ITEM_TITLE, episodeTitleWithNumbers)
            f.arguments = args
            return f.safeShow(fragmentManager, "checkInDialog")
        }
    }
}