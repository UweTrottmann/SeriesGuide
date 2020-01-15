package com.battlelancer.seriesguide.ui.episodes

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R

/**
 * Asks user to confirm before flagging all episodes
 * released up to (== including) the given one as watched,
 * excluding those with no release date.
 */
class EpisodeWatchedUpToDialog : AppCompatDialogFragment() {

    private var showTvdbId: Int = 0
    private var episodeReleaseTime: Long = 0
    private var episodeNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments!!.run {
            showTvdbId = getInt(ARG_SHOW_TVDB_ID)
            episodeReleaseTime = getLong(ARG_EPISODE_RELEASE_TIME)
            episodeNumber = getInt(ARG_EPISODE_NUMBER)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context!!)
            .setTitle(R.string.confirmation_watched_up_to)
            .setPositiveButton(R.string.action_watched_up_to) { _, _ ->
                EpisodeTools.episodeWatchedUpTo(
                    context, showTvdbId, episodeReleaseTime, episodeNumber
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()
    }

    companion object {
        private const val ARG_SHOW_TVDB_ID = "ARG_SHOW_TVDB_ID"
        private const val ARG_EPISODE_RELEASE_TIME = "ARG_EPISODE_RELEASE_TIME"
        private const val ARG_EPISODE_NUMBER = "ARG_EPISODE_NUMBER"

        @JvmStatic
        fun newInstance(
            showTvdbId: Int,
            episodeReleaseTime: Long,
            episodeNumber: Int
        ): EpisodeWatchedUpToDialog {
            return EpisodeWatchedUpToDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SHOW_TVDB_ID, showTvdbId)
                    putLong(ARG_EPISODE_RELEASE_TIME, episodeReleaseTime)
                    putInt(ARG_EPISODE_NUMBER, episodeNumber)
                }
            }
        }
    }

}