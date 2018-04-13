package com.battlelancer.seriesguide.ui.episodes

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ListFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupMenu
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.ui.episodes.EpisodesAdapter.OnFlagEpisodeListener
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.util.Utils

/**
 * Displays a list of episodes of a season.
 */
class EpisodesFragment : ListFragment(), OnClickListener, OnFlagEpisodeListener,
        EpisodesAdapter.PopupMenuClickListener {

    private lateinit var sortOrder: Constants.EpisodeSorting
    private var isDualPane: Boolean = false
    private lateinit var adapter: EpisodesAdapter

    private var showTvdbId: Int = 0
    private var seasonTvdbId: Int = 0
    private var seasonNumber: Int = 0
    private var startingPosition: Int = 0
    private var lastCheckedItemId: Long = 0

    companion object {

        private const val TAG = "Episodes"
        private const val ARG_SHOW_TVDBID = "show_tvdbid"
        private const val ARG_SEASON_TVDBID = "season_tvdbid"
        private const val ARG_SEASON_NUMBER = "season_number"
        private const val ARG_STARTING_POSITION = "starting_position"

        @JvmStatic
        fun newInstance(showId: Int, seasonId: Int, seasonNumber: Int,
                startingPosition: Int): EpisodesFragment {
            return EpisodesFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SHOW_TVDBID, showId)
                    putInt(ARG_SEASON_TVDBID, seasonId)
                    putInt(ARG_SEASON_NUMBER, seasonNumber)
                    putInt(ARG_STARTING_POSITION, startingPosition)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            showTvdbId = it.getInt(ARG_SHOW_TVDBID)
            seasonTvdbId = it.getInt(ARG_SEASON_TVDBID)
            seasonNumber = it.getInt(ARG_SEASON_NUMBER)
            startingPosition = it.getInt(ARG_STARTING_POSITION)
        } ?: throw IllegalArgumentException("Missing arguments")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_episodes, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        loadSortOrder()

        // listen to changes to the sorting preference
        PreferenceManager.getDefaultSharedPreferences(requireActivity()).apply {
            registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        val pager = requireActivity().findViewById<View>(R.id.pagerEpisodes)
        isDualPane = pager != null && pager.visibility == View.VISIBLE

        if (isDualPane) {
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        } else {
            startingPosition = -1 // overwrite starting position
        }
        lastCheckedItemId = -1

        adapter = EpisodesAdapter(requireActivity(), this, this)
        listAdapter = adapter

        loaderManager.initLoader(EpisodesActivity.EPISODES_LOADER_ID, null,
                episodesLoaderCallbacks)

        setHasOptionsMenu(true)
    }

    /**
     * Display the episode at the given position in a detail pane or if not available in a new
     * activity.
     */
    private fun showDetails(view: View?, position: Int) {
        if (isDualPane) {
            val activity = requireActivity() as EpisodesActivity
            activity.setCurrentPage(position)
            setItemChecked(position)
        } else {
            val episodeId = listView.getItemIdAtPosition(position).toInt()

            val intent = Intent().apply {
                setClass(requireActivity(), EpisodesActivity::class.java)
                putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId)
            }

            Utils.startActivityWithAnimation(requireActivity(), intent, view)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSortOrder()
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop listening to sort pref changes
        PreferenceManager.getDefaultSharedPreferences(requireActivity()).apply {
            unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.episodelist_menu, menu)
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

    override fun onListItemClick(l: ListView?, view: View?, position: Int, id: Long) {
        showDetails(view, position)
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
                val isSkipped = EpisodeTools.isSkipped(watchedFlag)
                findItem(R.id.menu_action_episodes_skip).isVisible = !isWatched && !isSkipped
                findItem(R.id.menu_action_episodes_dont_skip).isVisible = isSkipped
            }
            setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_action_episodes_watched -> {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, true)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag watched")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_not_watched -> {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, false)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag unwatched")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_collection_add -> {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, true)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag collected")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_collection_remove -> {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, false)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag uncollected")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_skip -> {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, true)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag skipped")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_dont_skip -> {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, false)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag not skipped")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_watched_previous -> {
                        EpisodeTools.episodeWatchedPrevious(requireContext(), showTvdbId,
                                releaseTimeMs, episodeNumber)
                        Utils.trackContextMenu(requireContext(), TAG, "Flag previously aired")
                        return@OnMenuItemClickListener true
                    }
                    R.id.menu_action_episodes_manage_lists -> {
                        ManageListsDialogFragment.showListsDialog(episodeTvdbId,
                                ListItemTypes.EPISODE, fragmentManager)
                        Utils.trackContextMenu(requireContext(), TAG, "Manage lists")
                        return@OnMenuItemClickListener true
                    }
                }
                false
            })
            show()
        }
    }

    override fun onFlagEpisodeWatched(episodeTvdbId: Int, episode: Int, isWatched: Boolean) {
        EpisodeTools.episodeWatched(requireContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episode,
                if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED)
    }

    private fun onFlagEpisodeSkipped(episodeTvdbId: Int, episode: Int, isSkipped: Boolean) {
        EpisodeTools.episodeWatched(requireContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episode,
                if (isSkipped) EpisodeFlags.SKIPPED else EpisodeFlags.UNWATCHED)
    }

    private fun onFlagEpisodeCollected(episodeTvdbId: Int, episode: Int, isCollected: Boolean) {
        EpisodeTools.episodeCollected(requireContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episode, isCollected)
    }

    private fun loadSortOrder() {
        sortOrder = DisplaySettings.getEpisodeSortOrder(requireActivity())
    }

    private fun showSortDialog() {
        val sortDialog = SingleChoiceDialogFragment.newInstance(
                R.array.epsorting,
                R.array.epsortingData, sortOrder.index(),
                DisplaySettings.KEY_EPISODE_SORT_ORDER, R.string.pref_episodesorting)
        sortDialog.show(requireFragmentManager(), "fragment_sort")
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (DisplaySettings.KEY_EPISODE_SORT_ORDER == key) {
            onSortOrderChanged()
        }
    }

    private fun onSortOrderChanged() {
        loadSortOrder()

        lastCheckedItemId = listView.getItemIdAtPosition(listView.checkedItemPosition)
        loaderManager.restartLoader(EpisodesActivity.EPISODES_LOADER_ID, null,
                episodesLoaderCallbacks)

        Utils.trackCustomEvent(requireActivity(), TAG, "Sorting", sortOrder.name)
    }

    /**
     * Highlight the given episode in the list.
     */
    fun setItemChecked(position: Int) {
        with(listView) {
            setItemChecked(position, true)
            if (position <= firstVisiblePosition || position >= lastVisiblePosition) {
                smoothScrollToPosition(position)
            }
        }
    }

    override fun onClick(v: View) {
        requireActivity().openContextMenu(v)
    }

    private val episodesLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            return CursorLoader(requireContext(),
                    Episodes.buildEpisodesOfSeasonWithShowUri(seasonTvdbId.toString()),
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
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {
            adapter.swapCursor(null)
        }
    }

}
