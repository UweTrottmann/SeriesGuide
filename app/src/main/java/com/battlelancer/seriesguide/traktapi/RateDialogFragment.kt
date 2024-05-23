// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogTraktRateBinding
import com.battlelancer.seriesguide.util.safeShow
import com.battlelancer.seriesguide.util.tasks.BaseRateItemTask
import com.battlelancer.seriesguide.util.tasks.RateEpisodeTask
import com.battlelancer.seriesguide.util.tasks.RateMovieTask
import com.battlelancer.seriesguide.util.tasks.RateShowTask
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uwetrottmann.trakt5.enums.Rating

/**
 * If connected to Trakt, allows rating a show, episode or movie on a 10 value rating scale or to
 * remove the rating. If not connected, asks the user to connect Trakt.
 *
 * Use via [safeShow].
 */
class RateDialogFragment : AppCompatDialogFragment() {

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

        val ratingButtons: MutableList<MaterialButton> = ArrayList()
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

        // Rating may be null or 0 if not set
        val currentRatingOrNull = requireArguments().getInt(ARG_CURRENT_RATING)
            .let { if (it in 1..10) it else null }

        if (currentRatingOrNull != null) {
            // display indicator on current rating
            ratingButtons[currentRatingOrNull - 1].apply {
                setIconResource(R.drawable.ic_radio_button_checked_control_24dp)
                setIconTintResource(R.color.sg_white)
                iconGravity = MaterialButton.ICON_GRAVITY_END
            }
            binding.ratingDelete.apply {
                isGone = false
                setOnClickListener { rate(null) }
            }
        } else {
            // Hide delete button if no rating is set
            binding.ratingDelete.isGone = true
        }

        builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding.root)

        return builder.create()
    }

    private fun rate(rating: Rating?) {
        val args = requireArguments()

        val itemType = args.getString(ITEM_TYPE)
            ?: return
        val itemId = args.getLong(ITEM_ID)
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
        private const val ITEM_TYPE: String = "item-type"
        private const val ITEM_ID: String = "item-id"
        private const val ARG_CURRENT_RATING = "current-rating"
        private const val ITEM_SHOW = "show"
        private const val ITEM_EPISODE = "episode"
        private const val ITEM_MOVIE = "movie"

        private fun newInstance(
            itemType: String,
            itemId: Long,
            currentRating: Int?
        ): RateDialogFragment {
            val args = bundleOf(
                ITEM_TYPE to itemType,
                ITEM_ID to itemId
            )
            if (currentRating != null) {
                args.putInt(ARG_CURRENT_RATING, currentRating)
            }
            return RateDialogFragment().apply { arguments = args }
        }

        /**
         * Create [RateDialogFragment] to rate a show.
         */
        fun newInstanceShow(showId: Long, currentRating: Int?) =
            newInstance(ITEM_SHOW, showId, currentRating)

        /**
         * Create [RateDialogFragment] to rate an episode.
         */
        fun newInstanceEpisode(episodeId: Long, currentRating: Int?) =
            newInstance(ITEM_EPISODE, episodeId, currentRating)

        /**
         * Create [RateDialogFragment] to rate a movie.
         */
        fun newInstanceMovie(movieTmdbId: Int, currentRating: Int?) =
            newInstance(ITEM_MOVIE, movieTmdbId.toLong(), currentRating)
    }
}
