package com.battlelancer.seriesguide.shows.search

import android.app.SearchManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentSearchBinding
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays episode search results.
 */
class EpisodeSearchFragment : BaseSearchFragment() {

    private var binding: FragmentSearchBinding? = null
    private val model by viewModels<EpisodeSearchViewModel>()
    private lateinit var adapter: EpisodeSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSearchBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override val emptyView: View
        get() = binding!!.textViewSearchEpisodesEmpty
    override val gridView: GridView
        get() = binding!!.gridViewSearchEpisodes

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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchActivityImpl.SearchQueryEvent) {
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
        if (event.position == SearchActivityImpl.TAB_POSITION_EPISODES) {
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
        val liftOnScrollTargetViewId = R.id.gridViewSearchEpisodes

        const val ARG_SHOW_TITLE = "title"
    }
}
