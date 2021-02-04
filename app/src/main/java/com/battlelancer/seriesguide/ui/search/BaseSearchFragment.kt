package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.view.View
import android.widget.GridView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.SearchActivity
import org.greenrobot.eventbus.EventBus

abstract class BaseSearchFragment : Fragment() {

    @BindView(R.id.textViewSearchEmpty)
    lateinit var emptyView: View

    @BindView(R.id.gridViewSearch)
    lateinit var gridView: GridView

    var initialSearchArgs: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // use initial query (if any)
            val queryEvent = EventBus.getDefault()
                .getStickyEvent(SearchActivity.SearchQueryEvent::class.java)
            if (queryEvent != null) {
                initialSearchArgs = queryEvent.args
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ButterKnife.bind(this, view)

        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true)

        emptyView.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    protected fun updateEmptyState(hasNoResults: Boolean, hasQuery: Boolean) {
        if (hasNoResults && hasQuery) {
            emptyView.visibility = View.VISIBLE
            gridView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            gridView.visibility = View.VISIBLE
        }
    }

}
