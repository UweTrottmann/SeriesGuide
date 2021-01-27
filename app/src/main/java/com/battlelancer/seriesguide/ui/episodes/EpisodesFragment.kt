package com.battlelancer.seriesguide.ui.episodes

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.ui.episodes.EpisodesAdapter.OnFlagEpisodeListener
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.util.safeShow

/**
 * Displays a list of episodes of a season.
 */
class EpisodesFragment : Fragment(), OnFlagEpisodeListener, EpisodesAdapter.PopupMenuClickListener {

    private var seasonId: Long = 0
    private var startingPosition: Int = 0
    private var lastCheckedItemId: Long = 0
    private var watchedAllEpisodes: Boolean = false
    private var collectedAllEpisodes: Boolean = false

    private lateinit var sortOrder: Constants.EpisodeSorting
    private lateinit var adapter: EpisodesAdapter
    private val model by viewModels<EpisodesViewModel> {
        EpisodesViewModelFactory(requireActivity().application, seasonId)
    }

    @BindView(R.id.listViewEpisodes)
    lateinit var listViewEpisodes: ListView
    @BindView(R.id.imageViewEpisodesCollectedToggle)
    lateinit var buttonCollectedAll: ImageView
    @BindView(R.id.imageViewEpisodesWatchedToggle)
    lateinit var buttonWatchedAll: ImageView
    private lateinit var unbinder: Unbinder

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_episodes, container, false)
        unbinder = ButterKnife.bind(this, view)

        buttonWatchedAll.setImageResource(R.drawable.ic_watch_all_black_24dp)
        buttonCollectedAll.setImageResource(R.drawable.ic_collect_all_black_24dp)

        listViewEpisodes.onItemClickListener = listOnItemClickListener

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model.episodeCountLiveData.observe(viewLifecycleOwner) { result ->
            handleCountUpdate(result)
        }

        loadSortOrder()

        // listen to changes to the sorting preference
        PreferenceManager.getDefaultSharedPreferences(requireActivity()).apply {
            registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }

        listViewEpisodes.choiceMode = ListView.CHOICE_MODE_SINGLE
        lastCheckedItemId = -1

        adapter = EpisodesAdapter(requireActivity(), this, this)
        listViewEpisodes.adapter = adapter

        LoaderManager.getInstance(this)
            .initLoader(EpisodesActivity.EPISODES_LOADER_ID, null, episodesLoaderCallbacks)

        setHasOptionsMenu(true)
    }

    private fun handleCountUpdate(result: EpisodeCountLiveData.Result?) {
        if (result == null) {
            return
        }
        setWatchedToggleState(result.unwatchedEpisodes)
        setCollectedToggleState(result.uncollectedEpisodes)
    }

    /**
     * Display the episode at the given position in the detail pane, highlight it in the list.
     */
    private fun showDetails(position: Int) {
        val activity = requireActivity() as EpisodesActivity
        activity.setCurrentPage(position)
        setItemChecked(position)
    }

    private val listOnItemClickListener =
        AdapterView.OnItemClickListener { _, _, position, _ -> showDetails(position) }

    override fun onResume() {
        super.onResume()
        loadSortOrder()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
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

    override fun onPopupMenuClick(v: View, episodeTvdbId: Int, episodeNumber: Int,
            releaseTimeMs: Long, watchedFlag: Int, isCollected: Boolean) {
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
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, true)
                        true
                    }
                    R.id.menu_action_episodes_not_watched -> {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, false)
                        true
                    }
                    R.id.menu_action_episodes_collection_add -> {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, true)
                        true
                    }
                    R.id.menu_action_episodes_collection_remove -> {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, false)
                        true
                    }
                    R.id.menu_action_episodes_skip -> {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, true)
                        true
                    }
                    R.id.menu_action_episodes_dont_skip -> {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, false)
                        true
                    }
                    R.id.menu_action_episodes_watched_up_to -> {
                        EpisodeWatchedUpToDialog.newInstance(
                            model.showTvdbId,
                            releaseTimeMs,
                            episodeNumber
                        ).safeShow(parentFragmentManager, "EpisodeWatchedUpToDialog")
                        true
                    }
                    R.id.menu_action_episodes_manage_lists -> {
                        ManageListsDialogFragment.show(
                            parentFragmentManager,
                            episodeTvdbId,
                            ListItemTypes.EPISODE
                        )
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    override fun onFlagEpisodeWatched(episodeTvdbId: Int, episode: Int, isWatched: Boolean) {
        EpisodeTools.episodeWatched(requireContext(), model.showTvdbId, episodeTvdbId,
            model.seasonNumber, episode,
                if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED)
    }

    private fun onFlagEpisodeSkipped(episodeTvdbId: Int, episode: Int, isSkipped: Boolean) {
        EpisodeTools.episodeWatched(requireContext(), model.showTvdbId, episodeTvdbId,
            model.seasonNumber, episode,
                if (isSkipped) EpisodeFlags.SKIPPED else EpisodeFlags.UNWATCHED)
    }

    private fun onFlagEpisodeCollected(episodeTvdbId: Int, episode: Int, isCollected: Boolean) {
        EpisodeTools.episodeCollected(requireContext(), model.showTvdbId, episodeTvdbId,
            model.seasonNumber, episode, isCollected)
    }

    private fun loadSortOrder() {
        sortOrder = DisplaySettings.getEpisodeSortOrder(requireActivity())
    }

    private fun showSortDialog() {
        SingleChoiceDialogFragment.show(parentFragmentManager,
                R.array.epsorting,
                R.array.epsortingData, sortOrder.index(),
                DisplaySettings.KEY_EPISODE_SORT_ORDER, R.string.pref_episodesorting,
                "episodeSortOrderDialog")
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (DisplaySettings.KEY_EPISODE_SORT_ORDER == key) {
            onSortOrderChanged()
        }
    }

    private fun onSortOrderChanged() {
        loadSortOrder()

        lastCheckedItemId =
                listViewEpisodes.getItemIdAtPosition(listViewEpisodes.checkedItemPosition)
        LoaderManager.getInstance(this)
            .restartLoader(EpisodesActivity.EPISODES_LOADER_ID, null, episodesLoaderCallbacks)
    }

    /**
     * Highlight the given episode in the list.
     */
    fun setItemChecked(position: Int) {
        with(listViewEpisodes) {
            setItemChecked(position, true)
            if (position <= firstVisiblePosition || position >= lastVisiblePosition) {
                smoothScrollToPosition(position)
            }
        }
    }

    private fun setWatchedToggleState(unwatchedEpisodes: Int) {
        watchedAllEpisodes = unwatchedEpisodes == 0
        buttonWatchedAll.apply {
            // using vectors is safe because it will be an AppCompatImageView
            if (watchedAllEpisodes) {
                setImageResource(R.drawable.ic_watched_all_24dp)
                contentDescription = getString(R.string.unmark_all)
            } else {
                setImageResource(R.drawable.ic_watch_all_black_24dp)
                contentDescription = getString(R.string.mark_all)
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
                        EpisodeTools.seasonWatched(
                            context,
                            model.showTvdbId,
                            model.seasonTvdbId,
                            model.seasonNumber,
                            EpisodeFlags.WATCHED
                        )
                        true
                    }
                    CONTEXT_WATCHED_NONE -> {
                        EpisodeTools.seasonWatched(
                            context,
                            model.showTvdbId,
                            model.seasonTvdbId,
                            model.seasonNumber,
                            EpisodeFlags.UNWATCHED
                        )
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun setCollectedToggleState(uncollectedEpisodes: Int) {
        collectedAllEpisodes = uncollectedEpisodes == 0
        buttonCollectedAll.apply {
            // using vectors is safe because it will be an AppCompatImageView
            if (collectedAllEpisodes) {
                setImageResource(R.drawable.ic_collected_all_24dp)
                contentDescription = getString(R.string.uncollect_all)
            } else {
                setImageResource(R.drawable.ic_collect_all_black_24dp)
                contentDescription = getString(R.string.collect_all)
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
                        EpisodeTools.seasonCollected(
                            context,
                            model.showTvdbId,
                            model.seasonTvdbId,
                            model.seasonNumber,
                            true
                        )
                        true
                    }
                    CONTEXT_COLLECTED_NONE -> {
                        EpisodeTools.seasonCollected(
                            context,
                            model.showTvdbId,
                            model.seasonTvdbId,
                            model.seasonNumber,
                            false
                        )
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private val episodesLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            return CursorLoader(requireContext(),
                    Episodes.buildEpisodesOfSeasonWithShowUri(model.seasonTvdbId.toString()),
                    EpisodesAdapter.EpisodesQuery.PROJECTION,
                    null, null, sortOrder.query())
        }

        override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
            adapter.swapCursor(data)
            // set an initial checked item
            if (startingPosition != -1) {
                setItemChecked(startingPosition)
                startingPosition = -1
            }
            // correctly restore the last checked item
            else if (lastCheckedItemId != -1L) {
                setItemChecked(adapter.getItemPosition(lastCheckedItemId))
                lastCheckedItemId = -1
            }
            // update count state every time data changes
            model.episodeCountLiveData.load(model.seasonTvdbId)
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {
            adapter.swapCursor(null)
        }
    }

}
