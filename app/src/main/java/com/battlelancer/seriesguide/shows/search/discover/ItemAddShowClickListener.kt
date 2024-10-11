// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.util.TaskManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

open class ItemAddShowClickListener(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val fragmentManager: FragmentManager
) : ItemAddShowViewHolder.ClickListener {

    override fun onItemClick(item: SearchResult) {
        if (item.state != SearchResult.STATE_ADDING) {
            if (item.state == SearchResult.STATE_ADDED) {
                // Already in library, open it.
                lifecycle.coroutineScope.launch {
                    val showId = withContext(Dispatchers.IO) {
                        SgRoomDatabase.getInstance(context).sgShow2Helper()
                            .getShowIdByTmdbId(item.tmdbId)
                    }
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        context.startActivity(OverviewActivity.intentShow(context, showId))
                    }
                }
            } else {
                // Display more details in a dialog.
                AddShowDialogFragment.show(fragmentManager, item)
            }
        }
    }

    override fun onAddClick(item: SearchResult) {
        EventBus.getDefault().post(AddFragment.OnAddingShowEvent(item.tmdbId))
        TaskManager.getInstance().performAddTask(context, item)
    }

    override fun onMoreOptionsClick(view: View, show: SearchResult) {
        val isTraktConnected = TraktCredentials.get(context).hasCredentials()
        AddShowPopupMenu(context, show, view).apply {
            // this does not know watchlist state, so if at all only show the add to item
            hideRemoveFromWatchlistAction()
            if (!isTraktConnected) hideAddToWatchlistAction()
        }.show()
    }

}