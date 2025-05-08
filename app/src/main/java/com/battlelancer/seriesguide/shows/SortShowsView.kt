// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewSortShowsBinding
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowSortOrder

class SortShowsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewSortShowsBinding

    init {
        orientation = VERTICAL

        // can't do in onFinishInflate as that is only called when inflating from XML
        binding = ViewSortShowsBinding.inflate(LayoutInflater.from(context), this)

        binding.apply {
            radioShowsSortTitle.setOnClickListener { updateSortOrderListener() }
            radioShowsSortLatestEpisode.setOnClickListener { updateSortOrderListener() }
            radioShowsSortOldestEpisode.setOnClickListener { updateSortOrderListener() }
            radioShowsSortLastWatched.setOnClickListener { updateSortOrderListener() }
            radioShowsSortRemaining.setOnClickListener { updateSortOrderListener() }
            radioShowsSortStatus.setOnClickListener { updateSortOrderListener() }
            checkboxShowsSortFavorites.setOnClickListener { updateSortOrderListener() }
            checkboxShowsSortIgnoreArticles.setOnClickListener { updateSortOrderListener(true) }
        }
    }

    private var sortOrderListener: SortOrderListener? = null

    private fun updateSortOrderListener(changedIgnoreArticles: Boolean = false) {
        val sortOrderId = when (binding.radioGroupShowsSort.checkedRadioButtonId) {
            R.id.radio_shows_sort_title -> ShowSortOrder.TITLE_ID
            R.id.radio_shows_sort_latest_episode -> ShowSortOrder.LATEST_EPISODE_ID
            R.id.radio_shows_sort_oldest_episode -> ShowSortOrder.OLDEST_EPISODE_ID
            R.id.radio_shows_sort_last_watched -> ShowSortOrder.LAST_WATCHED_ID
            R.id.radio_shows_sort_remaining -> ShowSortOrder.LEAST_REMAINING_EPISODES_ID
            R.id.radio_shows_sort_status -> ShowSortOrder.STATUS
            else -> throw IllegalArgumentException("Unknown radio button id ${binding.radioGroupShowsSort.checkedRadioButtonId}")
        }
        sortOrderListener?.onSortOrderUpdate(
            ShowSortOrder(
                sortOrderId,
                binding.checkboxShowsSortFavorites.isChecked,
                binding.checkboxShowsSortIgnoreArticles.isChecked,
                changedIgnoreArticles
            )
        )
    }

    fun setInitialSort(showSortOrder: ShowSortOrder) {
        val radioButtonId = when (showSortOrder.sortOrderId) {
            ShowSortOrder.TITLE_ID -> R.id.radio_shows_sort_title
            ShowSortOrder.LATEST_EPISODE_ID -> R.id.radio_shows_sort_latest_episode
            ShowSortOrder.OLDEST_EPISODE_ID -> R.id.radio_shows_sort_oldest_episode
            ShowSortOrder.LAST_WATCHED_ID -> R.id.radio_shows_sort_last_watched
            ShowSortOrder.LEAST_REMAINING_EPISODES_ID -> R.id.radio_shows_sort_remaining
            ShowSortOrder.STATUS -> R.id.radio_shows_sort_status
            else -> R.id.radio_shows_sort_title // fall back to default
        }
        binding.radioGroupShowsSort.check(radioButtonId)
        binding.checkboxShowsSortFavorites.isChecked = showSortOrder.isSortFavoritesFirst
        binding.checkboxShowsSortIgnoreArticles.isChecked = showSortOrder.isSortIgnoreArticles
    }

    fun setSortOrderListener(sortOrderListener: SortOrderListener) {
        this.sortOrderListener = sortOrderListener
    }

    interface SortOrderListener {
        fun onSortOrderUpdate(showSortOrder: ShowSortOrder)
    }

}