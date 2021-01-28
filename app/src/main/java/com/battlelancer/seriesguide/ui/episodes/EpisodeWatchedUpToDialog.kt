package com.battlelancer.seriesguide.ui.episodes

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Asks user to confirm before flagging all episodes
 * released up to (== including) the given one as watched,
 * excluding those with no release date.
 */
class EpisodeWatchedUpToDialog : AppCompatDialogFragment() {

    private var showId: Long = 0
    private var episodeReleaseTime: Long = 0
    private var episodeNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().run {
            showId = getLong(ARG_SHOW_ID)
            episodeReleaseTime = getLong(ARG_EPISODE_RELEASE_TIME)
            episodeNumber = getInt(ARG_EPISODE_NUMBER)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirmation_watched_up_to)
            .setPositiveButton(R.string.action_watched_up_to) { _, _ ->
                EpisodeTools.episodeWatchedUpTo(
                    context, showId, episodeReleaseTime, episodeNumber
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()
    }

    companion object {
        private const val ARG_SHOW_ID = "ARG_SHOW_ID"
        private const val ARG_EPISODE_RELEASE_TIME = "ARG_EPISODE_RELEASE_TIME"
        private const val ARG_EPISODE_NUMBER = "ARG_EPISODE_NUMBER"

        @JvmStatic
        fun newInstance(
            showId: Long,
            episodeReleaseTime: Long,
            episodeNumber: Int
        ): EpisodeWatchedUpToDialog {
            return EpisodeWatchedUpToDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SHOW_ID, showId)
                    putLong(ARG_EPISODE_RELEASE_TIME, episodeReleaseTime)
                    putInt(ARG_EPISODE_NUMBER, episodeNumber)
                }
            }
        }
    }

}