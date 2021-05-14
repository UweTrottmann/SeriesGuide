package com.battlelancer.seriesguide.ui.search

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.shows.ShowTools2
import com.battlelancer.seriesguide.util.TaskManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    fun onEventAddingShow(event: AddFragment.OnAddingShowEvent) {
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

    protected val itemClickListener = object : AddFragment.AddAdapter.OnItemClickListener {
        override fun onItemClick(item: SearchResult?) {
            if (item != null && item.state != SearchResult.STATE_ADDING) {
                if (item.state == SearchResult.STATE_ADDED) {
                    // Already in library, open it.
                    lifecycleScope.launchWhenStarted {
                        val showId = withContext(Dispatchers.IO) {
                            SgRoomDatabase.getInstance(requireContext()).sgShow2Helper()
                                .getShowIdByTmdbId(item.tmdbId)
                        }
                        startActivity(OverviewActivity.intentShow(requireContext(), showId))
                    }
                } else {
                    // Display more details in a dialog.
                    AddShowDialogFragment.show(parentFragmentManager, item)
                }
            }
        }

        override fun onAddClick(item: SearchResult) {
            EventBus.getDefault().post(AddFragment.OnAddingShowEvent(item.tmdbId))
            TaskManager.getInstance().performAddTask(context, item)
        }

        override fun onMenuWatchlistClick(view: View?, showTvdbId: Int) {
            // Not used for this type of add fragment.
        }
    }
}