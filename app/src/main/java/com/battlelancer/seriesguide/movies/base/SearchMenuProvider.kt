// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.base

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity

/**
 * A [MenuProvider] that just adds a search action that opens [MoviesSearchActivity].
 */
class SearchMenuProvider(private val context: Context) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.add(0, MENU_ITEM_SEARCH_ID, 0, R.string.search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setIcon(R.drawable.ic_search_white_24dp)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            MENU_ITEM_SEARCH_ID -> {
                context.startActivity(MoviesSearchActivity.intentSearch(context))
                true
            }

            else -> false
        }
    }

    companion object {
        private const val MENU_ITEM_SEARCH_ID = 1
    }
}