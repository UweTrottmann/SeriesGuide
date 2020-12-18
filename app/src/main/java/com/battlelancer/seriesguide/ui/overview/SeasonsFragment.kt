package com.battlelancer.seriesguide.ui.overview

import android.content.Intent
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
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.SeasonWatchedJob
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays a list of seasons of one show.
 */
class SeasonsFragment : Fragment() {

    @BindView(R.id.listViewSeasons)
    lateinit var listViewSeasons: ListView
    @BindView(R.id.textViewSeasonsRemaining)
    lateinit var textViewRemaining: TextView
    @BindView(R.id.imageViewSeasonsCollectedToggle)
    lateinit var buttonCollectedAll: ImageView
    @BindView(R.id.imageViewSeasonsWatchedToggle)
    lateinit var buttonWatchedAll: ImageView
    private lateinit var unbinder: Unbinder

    private lateinit var model: SeasonsViewModel
    private lateinit var adapter: SeasonsAdapter
    private var watchedAllEpisodes: Boolean = false
    private var collectedAllEpisodes: Boolean = false

    private var showId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.run {
            showId = getInt(ARG_SHOW_TVDBID)
        } ?: throw IllegalArgumentException("Missing arguments")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_seasons, container, false)
        unbinder = ButterKnife.bind(this, view)

        buttonWatchedAll.setImageResource(R.drawable.ic_watch_all_black_24dp)
        buttonCollectedAll.setImageResource(R.drawable.ic_collect_all_black_24dp)

        listViewSeasons.onItemClickListener = listOnItemClickListener

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = ViewModelProvider(this).get(SeasonsViewModel::class.java).also {
            it.remainingCountData.observe(viewLifecycleOwner,
                    Observer { result -> handleRemainingCountUpdate(result) })
        }

        // populate list
        adapter = SeasonsAdapter(activity, popupMenuClickListener).also {
            listViewSeasons.adapter = it
        }
        // now let's get a loader or reconnect to existing one
        LoaderManager.getInstance(this)
            .initLoader(OverviewActivity.SEASONS_LOADER_ID, null, seasonsLoaderCallbacks)

        // listen to changes to the sorting preference
        PreferenceManager.getDefaultSharedPreferences(activity).apply {
            registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
        }

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()

        updateUnwatchedCounts()
        // FIXME
//        model.remainingCountData.load(showId)

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

    private val listOnItemClickListener = AdapterView.OnItemClickListener { _, view, _, id ->
        view?.let {
            val intent = Intent(activity, EpisodesActivity::class.java).apply {
                putExtra(EpisodesActivity.EXTRA_SEASON_TVDBID, id.toInt())
            }
            ActivityCompat.startActivity(requireActivity(), intent,
                ActivityOptionsCompat
                    .makeScaleUpAnimation(view, 0, 0, view.width, view.height)
                    .toBundle())
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
        // FIXME
//        model.remainingCountData.load(showId)
        if (event.flagJob is SeasonWatchedJob) {
            // If we can narrow it down to just one season...
            UnwatchedUpdateWorker.updateUnwatchedCountFor(
                requireContext(),
                showId,
                event.flagJob.seasonTvdbId
            )
        } else {
            updateUnwatchedCounts()
        }
    }

    private fun onFlagSeasonSkipped(seasonId: Long, seasonNumber: Int) {
        EpisodeTools.seasonWatched(context, showId, seasonId.toInt(),
                seasonNumber, EpisodeFlags.SKIPPED)
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of the season.
     */
    private fun onFlagSeasonWatched(seasonId: Long, seasonNumber: Int, isWatched: Boolean) {
        EpisodeTools.seasonWatched(context, showId, seasonId.toInt(),
                seasonNumber, if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED)
    }

    /**
     * Changes the seasons episodes collected flags.
     */
    private fun onFlagSeasonCollected(seasonId: Long, seasonNumber: Int, isCollected: Boolean) {
        EpisodeTools.seasonCollected(context, showId, seasonId.toInt(), seasonNumber, isCollected)
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
            textViewRemaining.text = null // failed to calculate
        } else {
            textViewRemaining.text = textViewRemaining.resources
                    .getQuantityString(R.plurals.remaining_episodes_plural, unwatched, unwatched)
        }
        setWatchedToggleState(unwatched)
        setCollectedToggleState(result.uncollectedEpisodes)
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
        val sortOrder = DisplaySettings.getSeasonSortOrder(activity)
        SingleChoiceDialogFragment.show(parentFragmentManager,
                R.array.sesorting,
                R.array.sesortingData, sortOrder.index(),
                DisplaySettings.KEY_SEASON_SORT_ORDER, R.string.pref_seasonsorting,
                "seasonSortOrderDialog")
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (DisplaySettings.KEY_SEASON_SORT_ORDER == key) {
            // reload seasons in new order
            LoaderManager.getInstance(this)
                .restartLoader(OverviewActivity.SEASONS_LOADER_ID, null,
                    seasonsLoaderCallbacks)
        }
    }

    private val seasonsLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            val sortOrder = DisplaySettings.getSeasonSortOrder(activity)
            // can use SELECTION_WITH_EPISODES as count is updated when this fragment runs
            return CursorLoader(requireActivity(),
                    Seasons.buildSeasonsOfShowUri(showId.toString()),
                    SeasonsAdapter.SeasonsQuery.PROJECTION,
                    Seasons.SELECTION_WITH_EPISODES, null, sortOrder.query())
        }

        override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
            // Swap the new cursor in. (The framework will take care of closing the
            // old cursor once we return.)
            adapter.swapCursor(data)
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed. We need to make sure we are no
            // longer using it.
            adapter.swapCursor(null)
        }
    }

    private val popupMenuClickListener = SeasonsAdapter.PopupMenuClickListener { v, seasonTvdbId, seasonNumber ->
        PopupMenu(v.context, v).apply {
            inflate(R.menu.seasons_popup_menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_action_seasons_watched_all -> {
                        onFlagSeasonWatched(seasonTvdbId.toLong(), seasonNumber, true)
                        true
                    }
                    R.id.menu_action_seasons_watched_none -> {
                        onFlagSeasonWatched(seasonTvdbId.toLong(), seasonNumber, false)
                        true
                    }
                    R.id.menu_action_seasons_collection_add -> {
                        onFlagSeasonCollected(seasonTvdbId.toLong(), seasonNumber, true)
                        true
                    }
                    R.id.menu_action_seasons_collection_remove -> {
                        onFlagSeasonCollected(seasonTvdbId.toLong(), seasonNumber, false)
                        true
                    }
                    R.id.menu_action_seasons_skip -> {
                        onFlagSeasonSkipped(seasonTvdbId.toLong(), seasonNumber)
                        true
                    }
                    R.id.menu_action_seasons_manage_lists -> {
                        ManageListsDialogFragment.show(
                            parentFragmentManager,
                            seasonTvdbId,
                            ListItemTypes.SEASON
                        )
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    companion object {

        /** Value is integer */
        const val ARG_SHOW_TVDBID = "show_tvdbid"

        private const val CONTEXT_WATCHED_SHOW_ALL_ID = 0
        private const val CONTEXT_WATCHED_SHOW_NONE_ID = 1
        private const val CONTEXT_COLLECTED_SHOW_ALL_ID = 2
        private const val CONTEXT_COLLECTED_SHOW_NONE_ID = 3

        @JvmStatic
        fun newInstance(showId: Int): SeasonsFragment {
            val f = SeasonsFragment()

            val args = Bundle()
            args.putInt(ARG_SHOW_TVDBID, showId)
            f.arguments = args

            return f
        }
    }
}
