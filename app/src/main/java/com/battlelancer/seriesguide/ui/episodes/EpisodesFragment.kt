package com.battlelancer.seriesguide.ui.episodes

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentEpisodesBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.util.safeShow
import com.battlelancer.seriesguide.widgets.SgFastScroller

/**
 * Displays a list of episodes of a season.
 */
class EpisodesFragment : Fragment() {

    private var seasonId: Long = 0
    private var startingPosition: Int = 0
    private var scrollToCheckedItemOnDataRefresh = false
    private var watchedAllEpisodes: Boolean = false
    private var collectedAllEpisodes: Boolean = false

    private var binding: FragmentEpisodesBinding? = null
    private lateinit var adapter: EpisodesAdapter
    private val model by viewModels<EpisodesViewModel> {
        EpisodesViewModelFactory(requireActivity().application, seasonId)
    }

    companion object {

        private const val ARG_LONG_SEASON_ID = "season_id"
        private const val ARG_INT_STARTING_POSITION = "starting_position"

        private const val CONTEXT_WATCHED_ALL = 1
        private const val CONTEXT_WATCHED_NONE = 2
        private const val CONTEXT_COLLECTED_ALL = 3
        private const val CONTEXT_COLLECTED_NONE = 4

        @JvmStatic
        fun newInstance(
            seasonId: Long,
            startingPosition: Int
        ): EpisodesFragment {
            return EpisodesFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_LONG_SEASON_ID, seasonId)
                    putInt(ARG_INT_STARTING_POSITION, startingPosition)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            seasonId = it.getLong(ARG_LONG_SEASON_ID)
            startingPosition = it.getInt(ARG_INT_STARTING_POSITION)
        } ?: throw IllegalArgumentException("Missing arguments")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEpisodesBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.also { binding ->
            binding.imageViewEpisodesWatched.setImageResource(R.drawable.ic_watch_all_black_24dp)
            binding.imageViewEpisodesCollected.setImageResource(R.drawable.ic_collect_all_black_24dp)

            adapter = EpisodesAdapter(requireActivity(), episodesListClickListener)
            adapter.selectedItemId = model.selectedItemId

            binding.recyclerViewEpisodes.also {
                it.layoutManager = LinearLayoutManager(requireContext())
                it.adapter = adapter
                SgFastScroller(requireContext(), it)
            }
        }

        // listen to changes to the sorting preference
        PreferenceManager.getDefaultSharedPreferences(requireActivity()).apply {
            registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }

        model.episodeCounts.observe(viewLifecycleOwner) { result ->
            setWatchedToggleState(result.unwatchedEpisodes)
            setCollectedToggleState(result.uncollectedEpisodes)
        }
        model.episodes.observe(viewLifecycleOwner) { episodes ->
            adapter.submitList(episodes) {
                // set and scroll to an initial checked item
                if (savedInstanceState == null && startingPosition != -1) {
                    setItemChecked(startingPosition)
                    startingPosition = -1
                } else if (scrollToCheckedItemOnDataRefresh) {
                    val position = adapter.getPositionForId(adapter.selectedItemId)
                    if (position != -1) {
                        binding?.recyclerViewEpisodes?.smoothScrollToPosition(position)
                    }
                    scrollToCheckedItemOnDataRefresh = false
                }
            }
            // update count state every time data changes
            model.updateCounts()
        }

        setHasOptionsMenu(true)
    }

    /**
     * Display the episode at the given position in the detail pane, highlight it in the list.
     */
    private fun showDetails(position: Int) {
        val activity = requireActivity() as EpisodesActivity
        activity.setCurrentPage(position)
        // Note: page change listener will update checked episode.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop listening to sort pref changes
        PreferenceManager.getDefaultSharedPreferences(requireActivity()).apply {
            unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.episodelist_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_epsorting -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val episodesListClickListener = object : EpisodesAdapter.ClickListener {

        override fun onItemClick(position: Int) {
            showDetails(position)
        }

        override fun onWatchedBoxClick(episodeId: Long, isWatched: Boolean) {
            onFlagEpisodeWatched(episodeId, isWatched)
        }

        override fun onPopupMenuClick(
            v: View,
            episodeId: Long,
            episodeNumber: Int,
            releaseTimeMs: Long,
            watchedFlag: Int,
            isCollected: Boolean
        ) {
            PopupMenu(v.context, v).apply {
                inflate(R.menu.episodes_popup_menu)
                menu.apply {
                    findItem(R.id.menu_action_episodes_collection_add).isVisible = !isCollected
                    findItem(R.id.menu_action_episodes_collection_remove).isVisible = isCollected
                    val isWatched = EpisodeTools.isWatched(watchedFlag)
                    findItem(R.id.menu_action_episodes_watched).isVisible = !isWatched
                    findItem(R.id.menu_action_episodes_not_watched).isVisible = isWatched
                    findItem(R.id.menu_action_episodes_watched_up_to).isVisible = !isWatched
                    val isSkipped = EpisodeTools.isSkipped(watchedFlag)
                    findItem(R.id.menu_action_episodes_skip).isVisible = !isWatched && !isSkipped
                    findItem(R.id.menu_action_episodes_dont_skip).isVisible = isSkipped
                }
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_action_episodes_watched -> {
                            onFlagEpisodeWatched(episodeId, true)
                            true
                        }
                        R.id.menu_action_episodes_not_watched -> {
                            onFlagEpisodeWatched(episodeId, false)
                            true
                        }
                        R.id.menu_action_episodes_collection_add -> {
                            onFlagEpisodeCollected(episodeId, true)
                            true
                        }
                        R.id.menu_action_episodes_collection_remove -> {
                            onFlagEpisodeCollected(episodeId, false)
                            true
                        }
                        R.id.menu_action_episodes_skip -> {
                            onFlagEpisodeSkipped(episodeId, true)
                            true
                        }
                        R.id.menu_action_episodes_dont_skip -> {
                            onFlagEpisodeSkipped(episodeId, false)
                            true
                        }
                        R.id.menu_action_episodes_watched_up_to -> {
                            EpisodeWatchedUpToDialog.newInstance(
                                model.showId,
                                releaseTimeMs,
                                episodeNumber
                            ).safeShow(parentFragmentManager, "EpisodeWatchedUpToDialog")
                            true
                        }
                        else -> false
                    }
                }
                show()
            }

        }
    }

    private fun onFlagEpisodeWatched(episodeId: Long, isWatched: Boolean) {
        EpisodeTools.episodeWatched(
            requireContext(),
            episodeId,
            if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED
        )
    }

    private fun onFlagEpisodeSkipped(episodeId: Long, isSkipped: Boolean) {
        EpisodeTools.episodeWatched(
            requireContext(),
            episodeId,
            if (isSkipped) EpisodeFlags.SKIPPED else EpisodeFlags.UNWATCHED
        )
    }

    private fun onFlagEpisodeCollected(episodeId: Long, isCollected: Boolean) {
        EpisodeTools.episodeCollected(requireContext(), episodeId, isCollected)
    }

    private fun showSortDialog() {
        SingleChoiceDialogFragment.show(
            parentFragmentManager,
            R.array.epsorting,
            R.array.epsortingData, DisplaySettings.getEpisodeSortOrder(requireActivity()).index(),
            DisplaySettings.KEY_EPISODE_SORT_ORDER, R.string.pref_episodesorting,
            "episodeSortOrderDialog"
        )
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (DisplaySettings.KEY_EPISODE_SORT_ORDER == key) {
            scrollToCheckedItemOnDataRefresh = true
            model.updateOrder()
        }
    }

    /**
     * Highlight the given episode in the list.
     */
    fun setItemChecked(position: Int) {
        binding?.recyclerViewEpisodes?.also {
            model.selectedItemId = adapter.selectItem(position)
            it.smoothScrollToPosition(position)
        }
    }

    private fun setWatchedToggleState(unwatchedEpisodes: Int) {
        watchedAllEpisodes = unwatchedEpisodes == 0
        binding?.imageViewEpisodesWatched?.apply {
            // using vectors is safe because it will be an AppCompatImageView
            contentDescription = if (watchedAllEpisodes) {
                setImageResource(R.drawable.ic_watched_all_24dp)
                getString(R.string.unmark_all)
            } else {
                setImageResource(R.drawable.ic_watch_all_black_24dp)
                getString(R.string.mark_all)
            }
            // set onClick listener not before here to avoid unexpected actions
            setOnClickListener(watchedAllClickListener)
        }
    }

    private val watchedAllClickListener = View.OnClickListener { view ->
        PopupMenu(view.context, view).apply {
            if (!watchedAllEpisodes) {
                menu.add(0, CONTEXT_WATCHED_ALL, 0, R.string.mark_all)
            }
            menu.add(0, CONTEXT_WATCHED_NONE, 0, R.string.unmark_all)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_WATCHED_ALL -> {
                        EpisodeTools.seasonWatched(context, seasonId, EpisodeFlags.WATCHED)
                        true
                    }
                    CONTEXT_WATCHED_NONE -> {
                        EpisodeTools.seasonWatched(context, seasonId, EpisodeFlags.UNWATCHED)
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun setCollectedToggleState(uncollectedEpisodes: Int) {
        collectedAllEpisodes = uncollectedEpisodes == 0
        binding?.imageViewEpisodesCollected?.apply {
            // using vectors is safe because it will be an AppCompatImageView
            contentDescription = if (collectedAllEpisodes) {
                setImageResource(R.drawable.ic_collected_all_24dp)
                getString(R.string.uncollect_all)
            } else {
                setImageResource(R.drawable.ic_collect_all_black_24dp)
                getString(R.string.collect_all)
            }
            // set onClick listener not before here to avoid unexpected actions
            setOnClickListener(collectedAllClickListener)
        }
    }

    private val collectedAllClickListener = View.OnClickListener { view ->
        PopupMenu(view.context, view).apply {
            if (!collectedAllEpisodes) {
                menu.add(0, CONTEXT_COLLECTED_ALL, 0, R.string.collect_all)
            }
            menu.add(0, CONTEXT_COLLECTED_NONE, 0, R.string.uncollect_all)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_COLLECTED_ALL -> {
                        EpisodeTools.seasonCollected(context, seasonId, true)
                        true
                    }
                    CONTEXT_COLLECTED_NONE -> {
                        EpisodeTools.seasonCollected(context, seasonId, false)
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

}
