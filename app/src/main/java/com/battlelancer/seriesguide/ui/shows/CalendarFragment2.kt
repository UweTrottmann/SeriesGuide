package com.battlelancer.seriesguide.ui.shows

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager

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
    }

}
