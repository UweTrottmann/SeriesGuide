// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.databinding.DialogTraktRateBinding
import com.battlelancer.seriesguide.util.safeShow
import com.battlelancer.seriesguide.util.tasks.BaseRateItemTask
import com.battlelancer.seriesguide.util.tasks.RateEpisodeTask
import com.battlelancer.seriesguide.util.tasks.RateMovieTask
import com.battlelancer.seriesguide.util.tasks.RateShowTask
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uwetrottmann.trakt5.enums.Rating

/**
 * If connected to Trakt, allows rating a show, episode or movie on a 10 value rating scale.
 * If not connected, asks the user to connect Trakt.
 *
 * Use via [safeShow].
 */
class RateDialogFragment : AppCompatDialogFragment() {
    private interface InitBundle {
        companion object {
            const val ITEM_TYPE: String = "item-type"
            const val ITEM_ID: String = "item-id"
        }
    }

    private var binding: DialogTraktRateBinding? = null

    /**
     * Checks and asks for missing Trakt credentials. Otherwise if they are valid shows the dialog.
     */
    fun safeShow(context: Context, fragmentManager: FragmentManager): Boolean {
        if (!TraktCredentials.ensureCredentials(context)) {
            return false
        }
        return this.safeShow(fragmentManager, "rateDialog")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder

        val binding = DialogTraktRateBinding.inflate(LayoutInflater.from(context))
            .also { this.binding = it }

        val ratingButtons: MutableList<Button> = ArrayList()
        ratingButtons.add(binding.rating1)
        ratingButtons.add(binding.rating2)
        ratingButtons.add(binding.rating3)
        ratingButtons.add(binding.rating4)
        ratingButtons.add(binding.rating5)
        ratingButtons.add(binding.rating6)
        ratingButtons.add(binding.rating7)
        ratingButtons.add(binding.rating8)
        ratingButtons.add(binding.rating9)
        ratingButtons.add(binding.rating10)

        for (i in ratingButtons.indices) {
            val ratingButton = ratingButtons[i]
            ratingButton.text = TraktTools.buildUserRatingString(context, i + 1)
        }

        // rating buttons from 1 (worst) to 10 (best)
        ratingButtons[0].setOnClickListener { rate(Rating.WEAKSAUCE) }
        ratingButtons[1].setOnClickListener { rate(Rating.TERRIBLE) }
        ratingButtons[2].setOnClickListener { rate(Rating.BAD) }
        ratingButtons[3].setOnClickListener { rate(Rating.POOR) }
        ratingButtons[4].setOnClickListener { rate(Rating.MEH) }
        ratingButtons[5].setOnClickListener { rate(Rating.FAIR) }
        ratingButtons[6].setOnClickListener { rate(Rating.GOOD) }
        ratingButtons[7].setOnClickListener { rate(Rating.GREAT) }
        ratingButtons[8].setOnClickListener { rate(Rating.SUPERB) }
        ratingButtons[9].setOnClickListener { rate(Rating.TOTALLYNINJA) }

        builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding.root)

        return builder.create()
    }

    private fun rate(rating: Rating) {
        val args = requireArguments()

        val itemType = args.getString(InitBundle.ITEM_TYPE)
            ?: return
        val itemId = args.getLong(InitBundle.ITEM_ID)
        val task: BaseRateItemTask = when (itemType) {
            ITEM_MOVIE -> {
                RateMovieTask(requireContext(), rating, itemId.toInt())
            }

            ITEM_SHOW -> {
                RateShowTask(requireContext(), rating, itemId)
            }

            ITEM_EPISODE -> {
                RateEpisodeTask(requireContext(), rating, itemId)
            }

            else -> throw IllegalArgumentException("Unknown item type $itemType")
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        // guard against onClick being called after onSaveInstanceState by allowing state loss
        dismissAllowingStateLoss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ITEM_SHOW = "show"
        private const val ITEM_EPISODE = "episode"
        private const val ITEM_MOVIE = "movie"

        /**
         * Create [RateDialogFragment] to rate a show.
         */
        fun newInstanceShow(showId: Long): RateDialogFragment {
            val f = RateDialogFragment()

            val args = Bundle()
            args.putString(InitBundle.ITEM_TYPE, ITEM_SHOW)
            args.putLong(InitBundle.ITEM_ID, showId)
            f.arguments = args

            return f
        }

        /**
         * Create [RateDialogFragment] to rate an episode.
         */
        fun newInstanceEpisode(episodeId: Long): RateDialogFragment {
            val f = RateDialogFragment()

            val args = Bundle()
            args.putString(InitBundle.ITEM_TYPE, ITEM_EPISODE)
            args.putLong(InitBundle.ITEM_ID, episodeId)
            f.arguments = args

            return f
        }

        /**
         * Create [RateDialogFragment] to rate a movie.
         */
        fun newInstanceMovie(movieTmdbId: Int): RateDialogFragment {
            val f = RateDialogFragment()

            val args = Bundle()
            args.putString(InitBundle.ITEM_TYPE, ITEM_MOVIE)
            args.putLong(InitBundle.ITEM_ID, movieTmdbId.toLong())
            f.arguments = args

            return f
        }
    }
}
