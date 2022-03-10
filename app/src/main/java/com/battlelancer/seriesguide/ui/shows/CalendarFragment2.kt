package com.battlelancer.seriesguide.ui.shows

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.widgets.SgFastScroller
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class CalendarFragment2 : Fragment() {

    enum class CalendarType(val id: Int) {
        UPCOMING(1),
        RECENT(2)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewEmpty: TextView
    private val viewModel: CalendarFragment2ViewModel by viewModels()

    private lateinit var adapter: CalendarAdapter2
    private lateinit var type: CalendarType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val argType = requireArguments().getInt(ARG_CALENDAR_TYPE)
        type = when (argType) {
            CalendarType.UPCOMING.id -> CalendarType.UPCOMING
            CalendarType.RECENT.id -> CalendarType.RECENT
            else -> throw IllegalArgumentException("Unknown calendar type $argType")
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar2, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewCalendar)
        textViewEmpty = view.findViewById(R.id.textViewCalendarEmpty)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PreferenceManager.getDefaultSharedPreferences(activity)
            .registerOnSharedPreferenceChangeListener(prefChangeListener)

        adapter = CalendarAdapter2(requireContext(), calendarItemClickListener)

        val layoutManager = AutoGridLayoutManager(
            context,
            R.dimen.showgrid_columnWidth, 1, 1,
            adapter
        )

        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = layoutManager
            it.adapter = adapter
        }
        SgFastScroller(requireContext(), recyclerView)

        textViewEmpty.setText(
            if (type == CalendarType.UPCOMING) {
                R.string.noupcoming
            } else {
                R.string.norecent
            }
        )

        ViewModelProvider(requireActivity()).get(ShowsActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) { tabPosition: Int? ->
                if (tabPosition != null) {
                    if (CalendarType.UPCOMING == type
                        && tabPosition == ShowsActivity.INDEX_TAB_UPCOMING
                        || CalendarType.RECENT == type
                        && tabPosition == ShowsActivity.INDEX_TAB_RECENT) {
                        recyclerView.smoothScrollToPosition(0)
                    }
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collectLatest {
                    adapter.submitData(it)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.onPagesUpdatedFlow.conflate().collectLatest {
                    updateEmptyView(adapter.itemCount == 0)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // Re-build the query each time when STARTED (e.g. in multi-window this might be
            // visible, but not RESUMED) and again every minute as query conditions change based
            // on time. On the downside this runs even if the tab is not visible (tied to RESUMED).
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    updateCalendarQuery()
                    delay(DateUtils.MINUTE_IN_MILLIS + Random.nextLong(DateUtils.SECOND_IN_MILLIS))
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        recyclerView.isGone = isEmpty
        textViewEmpty.isGone = !isEmpty
    }

    private suspend fun updateCalendarQuery() {
        viewModel.updateCalendarQuery(type == CalendarType.UPCOMING)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PreferenceManager.getDefaultSharedPreferences(activity)
            .unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.calendar_menu, menu)

        // set menu items to current values
        val context = requireContext()
        menu.findItem(R.id.menu_action_calendar_onlyfavorites).isChecked =
            CalendarSettings.isOnlyFavorites(context)
        menu.findItem(R.id.menu_action_calendar_onlypremieres).isChecked =
            CalendarSettings.isOnlyPremieres(context)
        menu.findItem(R.id.menu_action_calendar_onlycollected).isChecked =
            CalendarSettings.isOnlyCollected(context)
        menu.findItem(R.id.menu_action_calendar_nospecials).isChecked =
            DisplaySettings.isHidingSpecials(context)
        menu.findItem(R.id.menu_action_calendar_nowatched).isChecked =
            CalendarSettings.isHidingWatchedEpisodes(context)
        menu.findItem(R.id.menu_action_calendar_infinite).isChecked =
            CalendarSettings.isInfiniteScrolling(context)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_calendar_onlyfavorites -> {
                toggleFilterSetting(item, CalendarSettings.KEY_ONLY_FAVORITE_SHOWS)
                true
            }
            R.id.menu_action_calendar_onlypremieres -> {
                toggleFilterSetting(item, CalendarSettings.KEY_ONLY_PREMIERES)
                true
            }
            R.id.menu_action_calendar_onlycollected -> {
                toggleFilterSetting(item, CalendarSettings.KEY_ONLY_COLLECTED)
                true
            }
            R.id.menu_action_calendar_nospecials -> {
                toggleFilterSetting(item, DisplaySettings.KEY_HIDE_SPECIALS)
                true
            }
            R.id.menu_action_calendar_nowatched -> {
                toggleFilterSetting(item, CalendarSettings.KEY_HIDE_WATCHED_EPISODES)
                true
            }
            R.id.menu_action_calendar_infinite -> {
                toggleFilterSetting(item, CalendarSettings.KEY_INFINITE_SCROLLING_2)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFilterSetting(item: MenuItem, key: String) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit {
            putBoolean(key, !item.isChecked)
        }
        // refresh filter icon state
        requireActivity().invalidateOptionsMenu()
    }

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            CalendarSettings.KEY_ONLY_FAVORITE_SHOWS,
            CalendarSettings.KEY_ONLY_PREMIERES,
            CalendarSettings.KEY_ONLY_COLLECTED,
            DisplaySettings.KEY_HIDE_SPECIALS,
            CalendarSettings.KEY_HIDE_WATCHED_EPISODES,
            CalendarSettings.KEY_INFINITE_SCROLLING_2 -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    updateCalendarQuery()
                }
            }
        }
    }

    private val calendarItemClickListener = object :
        CalendarAdapter2.ItemClickListener {
        override fun onItemClick(episodeId: Long) {
            val intent = EpisodesActivity.intentEpisode(episodeId, requireContext())
            Utils.startActivityWithAnimation(activity, intent, view)
        }

        override fun onItemLongClick(anchor: View, episode: SgEpisode2WithShow) {
            val context = anchor.context

            val popupMenu = PopupMenu(context, anchor)
            val menu = popupMenu.menu

            // only display the action appropriate for the items current state
            if (EpisodeTools.isWatched(episode.watched)) {
                menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 0, R.string.action_unwatched)
            } else {
                menu.add(0, CONTEXT_FLAG_WATCHED_ID, 0, R.string.action_watched)
            }
            if (episode.episode_collected) {
                menu.add(0, CONTEXT_COLLECTION_REMOVE_ID, 1, R.string.action_collection_remove)
            } else {
                menu.add(0, CONTEXT_COLLECTION_ADD_ID, 1, R.string.action_collection_add)
            }
            // display check-in if only trakt is connected
            if (TraktCredentials.get(context).hasCredentials()
                && !HexagonSettings.isEnabled(context)) {
                menu.add(0, CONTEXT_CHECKIN_ID, 2, R.string.checkin)
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_CHECKIN_ID -> {
                        CheckInDialogFragment.show(
                            requireContext(),
                            parentFragmentManager,
                            episode.id
                        )
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_FLAG_WATCHED_ID -> {
                        updateEpisodeWatchedState(episode.id, true)
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_FLAG_UNWATCHED_ID -> {
                        updateEpisodeWatchedState(episode.id, false)
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_COLLECTION_ADD_ID -> {
                        updateEpisodeCollectionState(episode.id, true)
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_COLLECTION_REMOVE_ID -> {
                        updateEpisodeCollectionState(episode.id, false)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }

            popupMenu.show()
        }

        override fun onItemWatchBoxClick(episode: SgEpisode2WithShow, isWatched: Boolean) {
            updateEpisodeWatchedState(episode.id, !isWatched)
        }
    }

    private fun updateEpisodeCollectionState(episodeId: Long, addToCollection: Boolean) {
        EpisodeTools.episodeCollected(context, episodeId, addToCollection)
    }

    private fun updateEpisodeWatchedState(episodeId: Long, isWatched: Boolean) {
        EpisodeTools.episodeWatched(
            context, episodeId, if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED
        )
    }

    companion object {
        const val ARG_CALENDAR_TYPE = "calendarType"

        private const val CONTEXT_FLAG_WATCHED_ID = 0
        private const val CONTEXT_FLAG_UNWATCHED_ID = 1
        private const val CONTEXT_CHECKIN_ID = 2
        private const val CONTEXT_COLLECTION_ADD_ID = 3
        private const val CONTEXT_COLLECTION_REMOVE_ID = 4
    }

}
