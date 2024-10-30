// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2019, 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.shows.tools.ShowSync

/**
 * Item click listener for show item popup menu.
 */
class ShowMenuItemClickListener(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val showId: Long,
    private val nextEpisodeId: Long
) : PopupMenu.OnMenuItemClickListener {

    private val showTools = SgApp.getServicesComponent(context).showTools()

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_action_shows_watched_next -> {
                EpisodeTools.episodeWatchedIfNotZero(context, nextEpisodeId)
                return true
            }
            R.id.menu_action_shows_favorites_add -> {
                showTools.storeIsFavorite(showId, true)
                return true
            }
            R.id.menu_action_shows_favorites_remove -> {
                showTools.storeIsFavorite(showId, false)
                return true
            }
            R.id.menu_action_shows_hide -> {
                showTools.storeIsHidden(showId, true)
                return true
            }
            R.id.menu_action_shows_unhide -> {
                showTools.storeIsHidden(showId, false)
                return true
            }
            R.id.menu_action_shows_manage_lists -> {
                ManageListsDialogFragment.show(fragmentManager, showId)
                return true
            }
            R.id.menu_action_shows_update -> {
                ShowSync.triggerDeltaSync(context, showId)
                return true
            }
            R.id.menu_action_shows_remove -> {
                RemoveShowDialogFragment.show(showId, fragmentManager, context)
                return true
            }
            else -> return false
        }
    }
}
