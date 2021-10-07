package com.battlelancer.seriesguide.ui.overview

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentSeasonsBinding
import com.battlelancer.seriesguide.jobs.episodes.SeasonWatchedJob
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays a list of seasons of one show.
 */
class SeasonsFragment() : Fragment() {

    constructor(showId: Long) : this() {
        arguments = buildArgs(showId)
    }

    private var binding: FragmentSeasonsBinding? = null
    private lateinit var adapter: SeasonsAdapter
    private val model by viewModels<SeasonsViewModel> {
        SeasonsViewModelFactory(requireActivity().application, showId)
    }

    private var watchedAllEpisodes: Boolean = false
    private var collectedAllEpisodes: Boolean = false

    private var showId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.run {
            showId = getLong(ARG_LONG_SHOW_ROW_ID)
        } ?: throw IllegalArgumentException("Missing arguments")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSeasonsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SeasonsAdapter(
            requireContext(),
            listOnItemClickListener
        )

        binding?.apply {
            imageViewSeasonsWatchedToggle.setImageResource(R.drawable.ic_watch_all_black_24dp)
            imageViewSeasonsCollectedToggle.setImageResource(R.drawable.ic_collect_all_black_24dp)

            recyclerViewSeasons.also {
                it.setHasFixedSize(true)
                it.layoutManager = LinearLayoutManager(requireContext())
                it.adapter = adapter
            }
        }

        model.seasons.observe(viewLifecycleOwner) { seasons ->
            adapter.submitList(seasons)
        }
        model.remainingCountData.observe(viewLifecycleOwner) { result ->
            handleRemainingCountUpdate(result)
        }

        // listen to changes to the sorting preference
        PreferenceManager.getDefaultSharedPreferences(activity).apply {
            registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }
    }

    override fun onStart() {
        super.onStart()

        updateUnwatchedCounts()
        model.remainingCountData.load(showId)

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop listening to sort pref changes
        PreferenceManager.getDefaultSharedPreferences(activity).apply {
            unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.seasons_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        return if (itemId == R.id.menu_sesortby) {
            showSortDialog()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private val listOnItemClickListener = object : SeasonsAdapter.ItemClickListener {
        override fun onItemClick(v: View, seasonRowId: Long) {
            val intent = EpisodesActivity.intentSeason(seasonRowId, requireActivity())
            ActivityCompat.startActivity(requireActivity(), intent,
                ActivityOptionsCompat
                    .makeScaleUpAnimation(v, 0, 0, v.width, v.height)
                    .toBundle())
        }

        override fun onPopupMenuClick(v: View, seasonRowId: Long) {
            PopupMenu(v.context, v).apply {
                inflate(R.menu.seasons_popup_menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_action_seasons_watched_all -> {
                            onFlagSeasonWatched(seasonRowId, true)
                            true
                        }
                        R.id.menu_action_seasons_watched_none -> {
                            onFlagSeasonWatched(seasonRowId, false)
                            true
                        }
                        R.id.menu_action_seasons_collection_add -> {
                            onFlagSeasonCollected(seasonRowId, true)
                            true
                        }
                        R.id.menu_action_seasons_collection_remove -> {
                            onFlagSeasonCollected(seasonRowId, false)
                            true
                        }
                        R.id.menu_action_seasons_skip -> {
                            onFlagSeasonSkipped(seasonRowId)
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }

    /**
     * Updates the total remaining episodes counter, updates season counters after episode actions.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BaseMessageActivity.ServiceCompletedEvent) {
        if (event.flagJob == null || !event.isSuccessful) {
            return  // no changes applied
        }
        if (!isAdded) {
            return  // no longer added to activity
        }
        model.remainingCountData.load(showId)
        if (event.flagJob is SeasonWatchedJob) {
            // If we can narrow it down to just one season...
            UnwatchedUpdateWorker.updateUnwatchedCountFor(
                requireContext(),
                showId,
                event.flagJob.seasonId
            )
        } else {
            updateUnwatchedCounts()
        }
    }

    private fun onFlagSeasonSkipped(seasonId: Long) {
        EpisodeTools.seasonWatched(context, seasonId, EpisodeFlags.SKIPPED)
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of the season.
     */
    private fun onFlagSeasonWatched(seasonId: Long, isWatched: Boolean) {
        EpisodeTools.seasonWatched(
            context,
            seasonId,
            if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED
        )
    }

    /**
     * Changes the seasons episodes collected flags.
     */
    private fun onFlagSeasonCollected(seasonId: Long, isCollected: Boolean) {
        EpisodeTools.seasonCollected(context, seasonId, isCollected)
    }

    /**
     * Changes the watched flag for all episodes of the given show, updates the status labels of all
     * seasons.
     */
    private fun onFlagShowWatched(isWatched: Boolean) {
        EpisodeTools.showWatched(context, showId, isWatched)
    }

    /**
     * Changes the collected flag for all episodes of the given show, updates the status labels of
     * all seasons.
     */
    private fun onFlagShowCollected(isCollected: Boolean) {
        EpisodeTools.showCollected(context, showId, isCollected)
    }

    /**
     * Update unwatched stats for all seasons of this fragments show. If finished worker
     * notifies provider causing the loader to reload.
     */
    private fun updateUnwatchedCounts() {
        UnwatchedUpdateWorker.updateUnwatchedCountFor(requireContext(), showId)
    }

    private fun handleRemainingCountUpdate(result: RemainingCountLiveData.Result?) {
        if (result == null) {
            return
        }
        val unwatched = result.unwatchedEpisodes
        if (unwatched <= 0) {
            binding?.textViewSeasonsRemaining?.text = null // failed to calculate
        } else {
            binding?.textViewSeasonsRemaining?.text =
                requireContext().resources.getQuantityString(
                    R.plurals.remaining_episodes_plural,
                    unwatched,
                    unwatched
                )
        }
        setWatchedToggleState(unwatched)
        setCollectedToggleState(result.uncollectedEpisodes)
    }

    private fun setWatchedToggleState(unwatchedEpisodes: Int) {
        watchedAllEpisodes = unwatchedEpisodes == 0
        binding?.imageViewSeasonsWatchedToggle?.apply {
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
                menu.add(0, CONTEXT_WATCHED_SHOW_ALL_ID, 0, R.string.mark_all)
            }
            menu.add(0, CONTEXT_WATCHED_SHOW_NONE_ID, 0, R.string.unmark_all)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_WATCHED_SHOW_ALL_ID -> {
                        onFlagShowWatched(true)
                        true
                    }
                    CONTEXT_WATCHED_SHOW_NONE_ID -> {
                        onFlagShowWatched(false)
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun setCollectedToggleState(uncollectedEpisodes: Int) {
        collectedAllEpisodes = uncollectedEpisodes == 0
        binding?.imageViewSeasonsCollectedToggle?.apply {
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
                menu.add(0, CONTEXT_COLLECTED_SHOW_ALL_ID, 0, R.string.collect_all)
            }
            menu.add(0, CONTEXT_COLLECTED_SHOW_NONE_ID, 0, R.string.uncollect_all)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_COLLECTED_SHOW_ALL_ID -> {
                        onFlagShowCollected(true)
                        true
                    }
                    CONTEXT_COLLECTED_SHOW_NONE_ID -> {
                        onFlagShowCollected(false)
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun showSortDialog() {
        val sortOrder = DisplaySettings.getSeasonSortOrder(requireContext())
        SingleChoiceDialogFragment.show(parentFragmentManager,
                R.array.sesorting,
                R.array.sesortingData, sortOrder.index,
                DisplaySettings.KEY_SEASON_SORT_ORDER, R.string.pref_seasonsorting,
                "seasonSortOrderDialog")
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (DisplaySettings.KEY_SEASON_SORT_ORDER == key) {
            // reload seasons in new order
            model.updateOrder()
        }
    }

    companion object {

        private const val ARG_LONG_SHOW_ROW_ID = "show_id"

        private const val CONTEXT_WATCHED_SHOW_ALL_ID = 0
        private const val CONTEXT_WATCHED_SHOW_NONE_ID = 1
        private const val CONTEXT_COLLECTED_SHOW_ALL_ID = 2
        private const val CONTEXT_COLLECTED_SHOW_NONE_ID = 3

        @JvmStatic
        fun buildArgs(showId: Long): Bundle {
            return bundleOf(ARG_LONG_SHOW_ROW_ID to showId)
        }
    }
}
