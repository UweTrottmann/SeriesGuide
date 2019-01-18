package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings.ShowsSortOrder

class SortShowsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_sort_shows, this)

        // can't do in onFinishInflate as that is only called when inflating from XML
        ButterKnife.bind(this)

        radioSortTitle.setOnClickListener { updateSortOrderListener() }
        radioSortLatestEpisode.setOnClickListener { updateSortOrderListener() }
        radioSortOldestEpisode.setOnClickListener { updateSortOrderListener() }
        radioSortLastWatched.setOnClickListener { updateSortOrderListener() }
        radioSortRemaining.setOnClickListener { updateSortOrderListener() }
        checkBoxFavoritesFirst.setOnClickListener { updateSortOrderListener() }
        checkBoxIgnoreArticles.setOnClickListener { updateSortOrderListener(true) }
    }

    @BindView(R.id.radio_group_shows_sort)
    internal lateinit var radioGroup: RadioGroup
    @BindView(R.id.radio_shows_sort_title)
    internal lateinit var radioSortTitle: RadioButton
    @BindView(R.id.radio_shows_sort_latest_episode)
    internal lateinit var radioSortLatestEpisode: RadioButton
    @BindView(R.id.radio_shows_sort_oldest_episode)
    internal lateinit var radioSortOldestEpisode: RadioButton
    @BindView(R.id.radio_shows_sort_last_watched)
    internal lateinit var radioSortLastWatched: RadioButton
    @BindView(R.id.radio_shows_sort_remaining)
    internal lateinit var radioSortRemaining: RadioButton
    @BindView(R.id.checkbox_shows_sort_favorites)
    internal lateinit var checkBoxFavoritesFirst: CheckBox
    @BindView(R.id.checkbox_shows_sort_ignore_articles)
    internal lateinit var checkBoxIgnoreArticles: CheckBox

    private var sortOrderListener: SortOrderListener? = null

    private fun updateSortOrderListener(changedIgnoreArticles: Boolean = false) {
        val sortOrderId = when (radioGroup.checkedRadioButtonId) {
            R.id.radio_shows_sort_title -> ShowsSortOrder.TITLE_ID
            R.id.radio_shows_sort_latest_episode -> ShowsSortOrder.LATEST_EPISODE_ID
            R.id.radio_shows_sort_oldest_episode -> ShowsSortOrder.OLDEST_EPISODE_ID
            R.id.radio_shows_sort_last_watched -> ShowsSortOrder.LAST_WATCHED_ID
            R.id.radio_shows_sort_remaining -> ShowsSortOrder.LEAST_REMAINING_EPISODES_ID
            else -> throw IllegalArgumentException("Unknown radio button id ${radioGroup.checkedRadioButtonId}")
        }
        sortOrderListener?.onSortOrderUpdate(
            ShowSortOrder(
                sortOrderId,
                checkBoxFavoritesFirst.isChecked,
                checkBoxIgnoreArticles.isChecked,
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
            else -> throw IllegalArgumentException("Unknown sort order id ${showSortOrder.sortOrderId}")
        }
        radioGroup.check(radioButtonId)
        checkBoxFavoritesFirst.isChecked = showSortOrder.isSortFavoritesFirst
        checkBoxIgnoreArticles.isChecked = showSortOrder.isSortIgnoreArticles
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