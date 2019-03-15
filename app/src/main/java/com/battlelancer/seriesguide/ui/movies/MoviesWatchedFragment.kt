package com.battlelancer.seriesguide.ui.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.MoviesActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MoviesWatchedFragment : Fragment() {

    companion object {
        fun newInstance() = MoviesWatchedFragment()
    }

    private lateinit var viewModel: MoviesWatchedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_movies_watched, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MoviesWatchedViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventTabClick(event: MoviesActivity.MoviesTabClickEvent) {
        val positionOfThisTab = if (event.showingNowTab) {
            MoviesActivity.TAB_POSITION_WATCHED_WITH_NOW
        } else {
            MoviesActivity.TAB_POSITION_WATCHED_DEFAULT
        }
        if (event.position == positionOfThisTab) {
            // TODO recyclerView.smoothScrollToPosition(0)
        }
    }

}
