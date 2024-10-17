// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.shows.tools.AddShowTask
import com.battlelancer.seriesguide.shows.tools.ShowTools2
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Kotlin add show fragment listening to add events to update adapter items.
 */
abstract class BaseAddShowsFragment : Fragment() {

    abstract fun setAllPendingNotAdded()

    abstract fun setStateForTmdbId(showTmdbId: Int, newState: Int)

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Called if the user triggers adding a single new show through the add dialog. The show is not
     * actually added, yet.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventAddingShow(event: OnAddingShowEvent) {
        if (event.showTmdbId > 0) {
            setStateForTmdbId(event.showTmdbId, SearchResult.STATE_ADDING)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventShowAdded(event: AddShowTask.OnShowAddedEvent) {
        when {
            event.successful -> setStateForTmdbId(event.showTmdbId, SearchResult.STATE_ADDED)
            event.showTmdbId > 0 -> setStateForTmdbId(event.showTmdbId, SearchResult.STATE_ADD)
            else -> setAllPendingNotAdded()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventShowRemoved(event: ShowTools2.OnShowRemovedEvent) {
        if (event.resultCode == NetworkResult.SUCCESS) {
            setStateForTmdbId(event.showTmdbId, SearchResult.STATE_ADD)
        }
    }

    protected val itemClickListener
        get() = ItemAddShowClickListener(requireContext(), lifecycle, parentFragmentManager)
}