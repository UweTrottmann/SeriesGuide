// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.GridView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.movies.MoviesDistillationSettings.MoviesSortOrderChangedEvent
import com.battlelancer.seriesguide.ui.MoviesActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A shell for a fragment displaying a number of movies.
 */
abstract class MoviesBaseFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var adapter: MoviesCursorAdapter

    /**
     * Return a loader id different from any other used within [com.battlelancer.seriesguide.ui.MoviesActivity].
     */
    abstract val loaderId: Int

    @get:StringRes
    abstract val emptyViewTextResId: Int

    abstract val gridView: GridView

    abstract val emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true)

        emptyView.setText(emptyViewTextResId)
        gridView.emptyView = emptyView

        ViewModelProvider(requireActivity()).get(MoviesActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) {
                if (it != null) {
                    if (it.tabPosition == getTabPosition(it.isShowingNowTab)) {
                        gridView.smoothScrollToPosition(0)
                    }
                }
            }

        adapter = MoviesCursorAdapter(
            requireContext(), MovieClickListenerImpl(requireContext()),
            loaderId
        )
        gridView.adapter = adapter

        requireActivity().addMenuProvider(
            MoviesOptionsMenu(requireActivity()),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        LoaderManager.getInstance(this).initLoader(loaderId, null, this)
    }

    override fun onDestroy() {
        super.onDestroy()

        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: MoviesSortOrderChangedEvent) {
        LoaderManager.getInstance(this).restartLoader(loaderId, null, this)
    }

    /**
     * @return The current position in the tab strip.
     * @see MoviesActivity
     */
    internal abstract fun getTabPosition(showingNowTab: Boolean): Int

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

}
