package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.tasks.RemoveShowTask
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays a list of popular shows to add that are paged in from the network.
 */
class ShowsPopularFragment : Fragment() {

    @BindView(R.id.swipeRefreshLayoutShowsPopular)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    @BindView(R.id.recyclerViewShowsPopular)
    lateinit var recyclerView: RecyclerView

    private lateinit var snackbar: Snackbar

    private lateinit var unbinder: Unbinder
    private lateinit var model: ShowsPopularViewModel
    private lateinit var adapter: ShowsPopularAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shows_popular, container, false).also {
            unbinder = ButterKnife.bind(this, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.apply {
            ViewTools.setSwipeRefreshLayoutColors(activity!!.theme, this)
            setOnRefreshListener { model.refresh() }
        }

        snackbar = Snackbar.make(swipeRefreshLayout, "", Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.action_try_again) { model.refresh() }

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth, 1, 1)
        }

        adapter = ShowsPopularAdapter(itemClickListener)
        recyclerView.adapter = adapter
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = ViewModelProviders.of(this).get(ShowsPopularViewModel::class.java)
        model.items.observe(this, Observer {
            adapter.submitList(it)
        })
        model.networkState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
            if (it?.status == Status.ERROR) {
                snackbar.setText(it.message!!)
                if (!snackbar.isShownOrQueued) snackbar.show()
            } else {
                if (snackbar.isShownOrQueued) snackbar.dismiss()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
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
            adapter.setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADDING)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AddShowTask.OnShowAddedEvent) {
        when {
            event.successful -> setShowAdded(event.showTvdbId)
            event.showTvdbId > 0 -> setShowNotAdded(event.showTvdbId)
            else -> adapter.setAllPendingNotAdded()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RemoveShowTask.OnShowRemovedEvent) {
        if (event.resultCode == NetworkResult.SUCCESS) {
            setShowNotAdded(event.showTvdbId)
        }
    }

    private fun setShowAdded(showTvdbId: Int) {
        adapter.setStateForTvdbId(showTvdbId, SearchResult.STATE_ADDED)
    }

    private fun setShowNotAdded(showTvdbId: Int) {
        adapter.setStateForTvdbId(showTvdbId, SearchResult.STATE_ADD)
    }

    private val itemClickListener = object : AddFragment.AddAdapter.OnItemClickListener {
        override fun onItemClick(item: SearchResult?) {
            if (item != null && item.state != SearchResult.STATE_ADDING) {
                if (item.state == SearchResult.STATE_ADDED) {
                    // already in library, open it
                    startActivity(OverviewActivity.intentShow(context, item.tvdbid))
                } else {
                    // display more details in a dialog
                    AddShowDialogFragment.show(context, fragmentManager!!, item)
                }
            }
        }

        override fun onAddClick(item: SearchResult) {
            EventBus.getDefault().post(AddFragment.OnAddingShowEvent(item.tvdbid))
            TaskManager.getInstance().performAddTask(context, item)
        }

        override fun onMenuWatchlistClick(view: View?, showTvdbId: Int) {
            // unused
        }
    }

}