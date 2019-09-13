package com.battlelancer.seriesguide.ui.search

import android.view.View
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.tasks.RemoveShowTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Kotlin add show fragment listening to add events to update adapter items.
 */
abstract class BaseAddShowsFragment : Fragment() {

    abstract fun setAllPendingNotAdded()

    abstract fun setStateForTvdbId(showTvdbId: Int, newState: Int)

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
     *
     * @see onEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AddFragment.OnAddingShowEvent) {
        if (event.showTvdbId > 0) {
            setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADDING)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AddShowTask.OnShowAddedEvent) {
        when {
            event.successful -> setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADDED)
            event.showTvdbId > 0 -> setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADD)
            else -> setAllPendingNotAdded()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RemoveShowTask.OnShowRemovedEvent) {
        if (event.resultCode == NetworkResult.SUCCESS) {
            setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADD)
        }
    }

    protected val itemClickListener = object : AddFragment.AddAdapter.OnItemClickListener {
        override fun onItemClick(item: SearchResult?) {
            if (item != null && item.state != SearchResult.STATE_ADDING) {
                if (item.state == SearchResult.STATE_ADDED) {
                    // Already in library, open it.
                    startActivity(OverviewActivity.intentShow(context, item.tvdbid))
                } else {
                    // Display more details in a dialog.
                    AddShowDialogFragment.show(context, fragmentManager!!, item)
                }
            }
        }

        override fun onAddClick(item: SearchResult) {
            EventBus.getDefault().post(AddFragment.OnAddingShowEvent(item.tvdbid))
            TaskManager.getInstance().performAddTask(context, item)
        }

        override fun onMenuWatchlistClick(view: View?, showTvdbId: Int) {
            // Not used for this type of add fragment.
        }
    }
}