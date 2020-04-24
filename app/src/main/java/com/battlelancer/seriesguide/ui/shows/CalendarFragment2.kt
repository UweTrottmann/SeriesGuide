package com.battlelancer.seriesguide.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.widgets.FastScrollerDecoration
import kotlinx.coroutines.launch

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
        val thumbDrawable = requireContext().getDrawable(R.drawable.fast_scroll_thumb) as StateListDrawable
        val trackDrawable = requireContext().getDrawable(R.drawable.fast_scroll_track)
        FastScrollerDecoration(
            recyclerView, thumbDrawable, trackDrawable, thumbDrawable, trackDrawable,
            resources.getDimensionPixelSize(R.dimen.sg_fastscroll_default_thickness),
            resources.getDimensionPixelSize(R.dimen.sg_fastscroll_minimum_height),
            resources.getDimensionPixelSize(R.dimen.sg_fastscroll_minimum_range),
            resources.getDimensionPixelOffset(R.dimen.sg_fastscroll_margin)
        )

        textViewEmpty.setText(
            if (type == CalendarType.UPCOMING) {
                R.string.noupcoming
            } else {
                R.string.norecent
            }
        )

        ViewModelProvider(requireActivity()).get(ShowsActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(
                viewLifecycleOwner,
                Observer { tabPosition: Int? ->
                    if (tabPosition != null) {
                        if (CalendarType.UPCOMING == type
                            && tabPosition == ShowsActivity.InitBundle.INDEX_TAB_UPCOMING
                            || CalendarType.RECENT == type
                            && tabPosition == ShowsActivity.InitBundle.INDEX_TAB_RECENT) {
                            recyclerView.smoothScrollToPosition(0)
                        }
                    }
                }
            )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.upcomingEpisodesLiveData.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
            updateEmptyView(it.isEmpty())
        })
        updateCalendarQuery()

        setHasOptionsMenu(true)
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        recyclerView.isGone = isEmpty
        textViewEmpty.isGone = !isEmpty
    }

    private fun updateCalendarQuery() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateCalendarQuery(type)
        }
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
                updateCalendarQuery()
            }
        }
    }

    private val calendarItemClickListener = object :
        CalendarAdapter2.ItemClickListener {
        override fun onItemClick(episodeTvdbId: Int) {
            val intent = Intent(activity, EpisodesActivity::class.java)
                .putExtra(EpisodesActivity.EXTRA_EPISODE_TVDBID, episodeTvdbId)
            Utils.startActivityWithAnimation(activity, intent, view)
        }

        override fun onItemLongClick(anchor: View, episode: EpisodeWithShow) {
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

            val showTvdbId = episode.showTvdbId
            val episodeTvdbId = episode.episodeTvdbId
            val seasonNumber = episode.season
            val episodeNumber = episode.episodenumber
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_CHECKIN_ID -> {
                        CheckInDialogFragment.show(
                            requireContext(),
                            parentFragmentManager,
                            episode.episodeTvdbId
                        )
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_FLAG_WATCHED_ID -> {
                        updateEpisodeWatchedState(
                            showTvdbId, episodeTvdbId, seasonNumber,
                            episodeNumber, true
                        )
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_FLAG_UNWATCHED_ID -> {
                        updateEpisodeWatchedState(
                            showTvdbId, episodeTvdbId, seasonNumber,
                            episodeNumber, false
                        )
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_COLLECTION_ADD_ID -> {
                        updateEpisodeCollectionState(
                            showTvdbId, episodeTvdbId, seasonNumber,
                            episodeNumber, true
                        )
                        return@setOnMenuItemClickListener true
                    }
                    CONTEXT_COLLECTION_REMOVE_ID -> {
                        updateEpisodeCollectionState(
                            showTvdbId, episodeTvdbId, seasonNumber,
                            episodeNumber, false
                        )
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }

            popupMenu.show()
        }

        override fun onItemWatchBoxClick(episode: EpisodeWithShow, isWatched: Boolean) {
            updateEpisodeWatchedState(
                episode.showTvdbId, episode.episodeTvdbId, episode.season, episode.episodenumber,
                !isWatched
            )
        }
    }

    private fun updateEpisodeCollectionState(
        showTvdbId: Int, episodeTvdbId: Int, seasonNumber: Int,
        episodeNumber: Int, addToCollection: Boolean
    ) {
        EpisodeTools.episodeCollected(
            context, showTvdbId, episodeTvdbId,
            seasonNumber, episodeNumber, addToCollection
        )
    }

    private fun updateEpisodeWatchedState(
        showTvdbId: Int, episodeTvdbId: Int, seasonNumber: Int,
        episodeNumber: Int, isWatched: Boolean
    ) {
        EpisodeTools.episodeWatched(
            context, showTvdbId, episodeTvdbId,
            seasonNumber, episodeNumber,
            if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED
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
