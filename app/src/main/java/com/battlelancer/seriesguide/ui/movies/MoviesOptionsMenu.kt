package com.battlelancer.seriesguide.ui.movies

import android.app.Activity
import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.movies.MoviesDistillationSettings.MoviesSortOrder
import org.greenrobot.eventbus.EventBus

class MoviesOptionsMenu(val context: Context) {

    fun create(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.movies_lists_menu, menu)
        menu.findItem(R.id.menu_action_movies_sort_ignore_articles).isChecked =
            DisplaySettings.isSortOrderIgnoringArticles(context)
    }

    fun onItemSelected(item: MenuItem, activity: Activity): Boolean {
        val itemId = item.itemId
        when (itemId) {
            R.id.menu_action_movies_sort_title -> {
                if (MoviesDistillationSettings.getSortOrderId(context) == MoviesSortOrder.TITLE_ALPHABETICAL_ID) {
                    changeSortOrder(MoviesDistillationSettings.MoviesSortOrder.TITLE_REVERSE_ALHPABETICAL_ID)
                } else {
                    // was sorted title reverse or by release date
                    changeSortOrder(MoviesDistillationSettings.MoviesSortOrder.TITLE_ALPHABETICAL_ID)
                }
                return true
            }
            R.id.menu_action_movies_sort_release -> {
                if (MoviesDistillationSettings.getSortOrderId(context) == MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID) {
                    changeSortOrder(MoviesSortOrder.RELEASE_DATE_OLDEST_FIRST_ID)
                } else {
                    // was sorted by oldest first or by title
                    changeSortOrder(MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID)
                }
                return true
            }
            R.id.menu_action_movies_sort_ignore_articles -> {
                changeSortIgnoreArticles(
                    !DisplaySettings.isSortOrderIgnoringArticles(context),
                    activity
                )
                return true
            }
            else -> return false
        }
    }

    private fun changeSortOrder(sortOrderId: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(MoviesDistillationSettings.KEY_SORT_ORDER, sortOrderId)
            .apply()

        EventBus.getDefault().post(MoviesDistillationSettings.MoviesSortOrderChangedEvent())
    }

    private fun changeSortIgnoreArticles(value: Boolean, activity: Activity) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(DisplaySettings.KEY_SORT_IGNORE_ARTICLE, value)
            .apply()

        // refresh icon state
        activity.invalidateOptionsMenu()

        EventBus.getDefault().post(MoviesDistillationSettings.MoviesSortOrderChangedEvent())
    }

}