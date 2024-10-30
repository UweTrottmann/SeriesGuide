// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.app.Activity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.movies.MoviesDistillationSettings.MoviesSortOrder
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.menus.ManualSyncMenu
import org.greenrobot.eventbus.EventBus

class MoviesOptionsMenu(val activity: Activity) :
    ManualSyncMenu(activity, R.menu.movies_lists_menu) {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)

        menu.findItem(R.id.menu_action_movies_sort_ignore_articles).isChecked =
            DisplaySettings.isSortOrderIgnoringArticles(activity)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_action_movies_sort_title -> {
                if (MoviesDistillationSettings.getSortOrderId(activity) == MoviesSortOrder.TITLE_ALPHABETICAL_ID) {
                    changeSortOrder(MoviesSortOrder.TITLE_REVERSE_ALHPABETICAL_ID)
                } else {
                    // was sorted title reverse or by release date
                    changeSortOrder(MoviesSortOrder.TITLE_ALPHABETICAL_ID)
                }
                true
            }

            R.id.menu_action_movies_sort_release -> {
                if (MoviesDistillationSettings.getSortOrderId(activity) == MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID) {
                    changeSortOrder(MoviesSortOrder.RELEASE_DATE_OLDEST_FIRST_ID)
                } else {
                    // was sorted by oldest first or by title
                    changeSortOrder(MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID)
                }
                true
            }

            R.id.menu_action_movies_sort_ignore_articles -> {
                changeSortIgnoreArticles(
                    !DisplaySettings.isSortOrderIgnoringArticles(activity),
                    activity
                )
                true
            }

            else -> super.onMenuItemSelected(menuItem)
        }
    }

    private fun changeSortOrder(sortOrderId: Int) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit()
            .putInt(MoviesDistillationSettings.KEY_SORT_ORDER, sortOrderId)
            .apply()

        EventBus.getDefault().post(MoviesDistillationSettings.MoviesSortOrderChangedEvent())
    }

    private fun changeSortIgnoreArticles(value: Boolean, activity: Activity) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit()
            .putBoolean(DisplaySettings.KEY_SORT_IGNORE_ARTICLE, value)
            .apply()

        // refresh icon state
        activity.invalidateOptionsMenu()

        EventBus.getDefault().post(MoviesDistillationSettings.MoviesSortOrderChangedEvent())
    }

}