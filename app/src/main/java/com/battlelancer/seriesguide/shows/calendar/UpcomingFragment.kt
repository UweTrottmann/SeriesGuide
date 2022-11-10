package com.battlelancer.seriesguide.shows.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.battlelancer.seriesguide.databinding.FragmentCalendarUpcomingBinding
import com.battlelancer.seriesguide.shows.ShowsActivityImpl

/**
 * A [CalendarFragment2] that displays to be released episodes.
 */
class UpcomingFragment : CalendarFragment2() {

    override val tabPosition: Int = ShowsActivityImpl.Tab.UPCOMING.index

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCalendarUpcomingBinding.inflate(layoutInflater, container, false)
        recyclerView = binding.recyclerViewCalendarUpcoming
        textViewEmpty = binding.textViewCalendarUpcomingEmpty
        return binding.root
    }

    override suspend fun updateCalendarQuery() {
        viewModel.updateCalendarQuery(true)
    }

}