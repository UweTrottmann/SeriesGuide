package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewSortShowsBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings.ShowsSortOrder

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
            R.id.radio_shows_sort_title -> ShowsSortOrder.TITLE_ID
            R.id.radio_shows_sort_latest_episode -> ShowsSortOrder.LATEST_EPISODE_ID
            R.id.radio_shows_sort_oldest_episode -> ShowsSortOrder.OLDEST_EPISODE_ID
            R.id.radio_shows_sort_last_watched -> ShowsSortOrder.LAST_WATCHED_ID
            R.id.radio_shows_sort_remaining -> ShowsSortOrder.LEAST_REMAINING_EPISODES_ID
            R.id.radio_shows_sort_status -> ShowsSortOrder.STATUS
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
            ShowsSortOrder.TITLE_ID -> R.id.radio_shows_sort_title
            ShowsSortOrder.LATEST_EPISODE_ID -> R.id.radio_shows_sort_latest_episode
            ShowsSortOrder.OLDEST_EPISODE_ID -> R.id.radio_shows_sort_oldest_episode
            ShowsSortOrder.LAST_WATCHED_ID -> R.id.radio_shows_sort_last_watched
            ShowsSortOrder.LEAST_REMAINING_EPISODES_ID -> R.id.radio_shows_sort_remaining
            ShowsSortOrder.STATUS -> R.id.radio_shows_sort_status
            else -> R.id.radio_shows_sort_title // fall back to default
        }
        binding.radioGroupShowsSort.check(radioButtonId)
        binding.checkboxShowsSortFavorites.isChecked = showSortOrder.isSortFavoritesFirst
        binding.checkboxShowsSortIgnoreArticles.isChecked = showSortOrder.isSortIgnoreArticles
    }

    fun setSortOrderListener(sortOrderListener: SortOrderListener) {
        this.sortOrderListener = sortOrderListener
    }

    data class ShowSortOrder(
        val sortOrderId: Int,
        val isSortFavoritesFirst: Boolean,
        val isSortIgnoreArticles: Boolean,
        val changedIgnoreArticles: Boolean
    ) {
        companion object {
            @JvmStatic
            fun fromSettings(context: Context): ShowSortOrder {
                return ShowSortOrder(
                    ShowsDistillationSettings.getSortOrderId(context),
                    ShowsDistillationSettings.isSortFavoritesFirst(context),
                    DisplaySettings.isSortOrderIgnoringArticles(context),
                    false
                )
            }
        }
    }

    interface SortOrderListener {
        fun onSortOrderUpdate(showSortOrder: ShowSortOrder)
    }

}