// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.os.AsyncTask
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.tasks.AddShowToWatchlistTask
import com.battlelancer.seriesguide.util.tasks.RemoveShowFromWatchlistTask
import org.greenrobot.eventbus.EventBus

/**
 * A [PopupMenu] with menu items to add a show and add to or remove a show from the Trakt watchlist.
 *
 * Hides add show item by default based on [show] state.
 * Use methods to hide not useful watchlist options.
 */
class AddShowPopupMenu(
    private val context: Context,
    private val show: SearchResult,
    anchor: View
) : PopupMenu(anchor.context, anchor), PopupMenu.OnMenuItemClickListener {

    init {
        inflate(R.menu.add_show_popup_menu)
        if (show.state != SearchResult.STATE_ADD) {
            menu.findItem(R.id.menu_action_add_show_add).isVisible = false
        }
        setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_add_show_add -> {
                // post so other fragments can display a progress indicator for that show
                EventBus.getDefault().post(OnAddingShowEvent(show.tmdbId))
                TaskManager.getInstance().performAddTask(context, show)
                true
            }

            R.id.menu_action_add_show_watchlist_add -> {
                @Suppress("DEPRECATION") // AsyncTask
                AddShowToWatchlistTask(context, show.tmdbId)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                true
            }

            R.id.menu_action_add_show_watchlist_remove -> {
                @Suppress("DEPRECATION") // AsyncTask
                RemoveShowFromWatchlistTask(context, show.tmdbId)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                true
            }

            else -> false
        }
    }

    fun hideAddToWatchlistAction() {
        menu.findItem(R.id.menu_action_add_show_watchlist_add).isVisible = false
    }

    fun hideRemoveFromWatchlistAction() {
        menu.findItem(R.id.menu_action_add_show_watchlist_remove).isVisible = false
    }

}