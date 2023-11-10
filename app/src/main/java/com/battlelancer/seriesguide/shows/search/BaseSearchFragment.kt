// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search

import android.os.Bundle
import android.view.View
import android.widget.GridView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.util.ThemeUtils
import org.greenrobot.eventbus.EventBus

abstract class BaseSearchFragment : Fragment() {

    abstract val emptyView: View

    abstract val gridView: GridView

    var initialSearchArgs: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // use initial query (if any)
            val queryEvent = EventBus.getDefault()
                .getStickyEvent(SearchActivityImpl.SearchQueryEvent::class.java)
            if (queryEvent != null) {
                initialSearchArgs = queryEvent.args
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true)
        ThemeUtils.applyBottomPaddingForNavigationBar(gridView)

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
