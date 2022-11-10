package com.battlelancer.seriesguide.shows.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.battlelancer.seriesguide.databinding.FragmentCalendarRecentBinding
import com.battlelancer.seriesguide.shows.ShowsActivityImpl

/**
 * A [CalendarFragment2] that displays recently released episodes.
 */
class RecentFragment : CalendarFragment2() {

    override val tabPosition: Int = ShowsActivityImpl.Tab.RECENT.index

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCalendarRecentBinding.inflate(layoutInflater, container, false)
        recyclerView = binding.recyclerViewCalendarRecent
        textViewEmpty = binding.textViewCalendarRecentEmpty
        return binding.root
    }

    override suspend fun updateCalendarQuery() {
        viewModel.updateCalendarQuery(false)
    }

}