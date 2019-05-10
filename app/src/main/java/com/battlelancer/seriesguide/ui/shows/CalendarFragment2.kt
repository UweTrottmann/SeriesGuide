package com.battlelancer.seriesguide.ui.shows

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ViewTools

class CalendarFragment2 : Fragment() {

    companion object {
        fun newInstance() = CalendarFragment2()
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: CalendarFragment2ViewModel

    private lateinit var adapter: CalendarAdapter2

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

        adapter = CalendarAdapter2(context!!)

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
            CalendarType.UPCOMING,
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

}
