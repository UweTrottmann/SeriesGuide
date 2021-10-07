package com.battlelancer.seriesguide.ui.search

import android.app.SearchManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays episode search results.
 */
class EpisodeSearchFragment : BaseSearchFragment() {

    private val model by viewModels<EpisodeSearchViewModel>()
    private lateinit var adapter: EpisodeSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // list items do not have right hand-side buttons, list may be long: enable fast scrolling
        gridView.apply {
            isFastScrollAlwaysVisible = false
            isFastScrollEnabled = true
        }
        adapter = EpisodeSearchAdapter(requireContext(), onItemClickListener).also {
            gridView.adapter = it
        }

        model.episodes.observe(viewLifecycleOwner) { episodes ->
            adapter.setData(episodes)
            updateEmptyState(
                episodes.isEmpty(),
                !model.searchData.value?.searchTerm.isNullOrEmpty()
            )
        }

        // load for given query (if just created)
        val args = initialSearchArgs
        if (args != null) {
            updateQuery(args)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchActivity.SearchQueryEvent) {
        updateQuery(event.args)
    }

    private fun updateQuery(args: Bundle) {
        model.searchData.value = EpisodeSearchViewModel.SearchData(
            args.getString(SearchManager.QUERY),
            args.getBundle(SearchManager.APP_DATA)?.getString(ARG_SHOW_TITLE)
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventTabClick(event: TabClickEvent) {
        if (event.position == SearchActivity.TAB_POSITION_EPISODES) {
            gridView.smoothScrollToPosition(0)
        }
    }

    private val onItemClickListener = object : EpisodeSearchAdapter.OnItemClickListener {
        override fun onItemClick(anchor: View, episodeId: Long) {
            EpisodesActivity.intentEpisode(episodeId, requireContext())
                .also { Utils.startActivityWithAnimation(activity, it, anchor) }
        }
    }

    companion object {
        const val ARG_SHOW_TITLE = "title"
    }
}
