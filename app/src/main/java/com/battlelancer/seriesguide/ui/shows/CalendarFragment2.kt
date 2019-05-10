package com.battlelancer.seriesguide.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools

class CalendarFragment2 : Fragment() {

    enum class CalendarType(val id: Int) {
        UPCOMING(1),
        RECENT(2)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: CalendarFragment2ViewModel

    private lateinit var adapter: CalendarAdapter2
    private lateinit var type: CalendarType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val argType = arguments!!.getInt(ARG_CALENDAR_TYPE)
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CalendarAdapter2(context!!, calendarItemClickListener)

        val layoutManager = AutoGridLayoutManager(
            context,
            R.dimen.showgrid_columnWidth, 1, 1
        )
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == CalendarAdapter2.VIEW_TYPE_HEADER) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }

        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = layoutManager
            it.adapter = adapter
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(CalendarFragment2ViewModel::class.java)
        viewModel.upcomingEpisodesLiveData.observe(this, Observer {
            adapter.submitList(it)
        })
        updateCalendarQuery()

        PreferenceManager.getDefaultSharedPreferences(activity)
            .registerOnSharedPreferenceChangeListener(prefChangeListener)

        setHasOptionsMenu(true)
    }

    private fun updateCalendarQuery() {
        viewModel.updateCalendarQuery(
            type,
            CalendarSettings.isOnlyCollected(context),
            CalendarSettings.isOnlyFavorites(context),
            CalendarSettings.isHidingWatchedEpisodes(context)
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        PreferenceManager.getDefaultSharedPreferences(activity)
            .unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.calendar_menu, menu)

        val visibilitySettingsIcon = ViewTools.vectorIconWhite(
            activity, activity!!.theme, R.drawable.ic_visibility_black_24dp
        )
        menu.findItem(R.id.menu_calendar_visibility).icon = visibilitySettingsIcon

        // set menu items to current values
        val context = context
        menu.findItem(R.id.menu_action_calendar_onlyfavorites).isChecked =
            CalendarSettings.isOnlyFavorites(context)
        menu.findItem(R.id.menu_action_calendar_onlycollected).isChecked =
            CalendarSettings.isOnlyCollected(context)
        menu.findItem(R.id.menu_action_calendar_nospecials).isChecked =
            DisplaySettings.isHidingSpecials(context)
        menu.findItem(R.id.menu_action_calendar_nowatched).isChecked =
            CalendarSettings.isHidingWatchedEpisodes(context)

        menu.findItem(R.id.menu_action_calendar_infinite).isVisible = false
        menu.findItem(R.id.menu_action_calendar_infinite).isEnabled = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_calendar_onlyfavorites -> {
                toggleFilterSetting(item, CalendarSettings.KEY_ONLY_FAVORITE_SHOWS)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFilterSetting(item: MenuItem, key: String) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit {
            putBoolean(key, !item.isChecked)
        }
        // refresh filter icon state
        activity!!.invalidateOptionsMenu()
    }

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (CalendarSettings.KEY_ONLY_FAVORITE_SHOWS == key
            || CalendarSettings.KEY_ONLY_COLLECTED == key
            || DisplaySettings.KEY_HIDE_SPECIALS == key
            || CalendarSettings.KEY_HIDE_WATCHED_EPISODES == key
            || CalendarSettings.KEY_INFINITE_SCROLLING == key) {
            updateCalendarQuery()
        }
    }

    private val calendarItemClickListener = object :
        CalendarAdapter2.ItemClickListener {
        override fun onItemClick(episodeTvdbId: Int) {
            val intent = Intent(activity, EpisodesActivity::class.java)
                .putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeTvdbId)
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
                            getContext()!!,
                            fragmentManager,
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
