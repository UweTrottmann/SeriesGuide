// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentAddshowTraktBinding
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.widgets.EmptyView
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.tasks.AddShowToWatchlistTask
import com.battlelancer.seriesguide.util.tasks.BaseShowActionTask.ShowChangedEvent
import com.battlelancer.seriesguide.util.tasks.RemoveShowFromWatchlistTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.LinkedList

/**
 * Displays the watched shows, show collection or shows watchlist of the connected Trakt user for
 * the purpose of adding them. Shows on the watchlist can also be removed from the watchlist.
 */
class TraktAddFragment : AddFragment() {

    private var binding: FragmentAddshowTraktBinding? = null
    private lateinit var listType: TraktAddLoader.Type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val typeOrdinal = requireArguments().getInt(ARG_TYPE)
        listType = TraktAddLoader.Type.entries.find { it.ordinal == typeOrdinal }
            ?: throw IllegalArgumentException("Invalid Type ordinal $typeOrdinal")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddshowTraktBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override val contentContainer: View
        get() = binding!!.containerAddContent

    override val progressBar: View
        get() = binding!!.progressBarAdd

    override val resultsGridView: GridView
        get() = binding!!.gridViewAdd

    override val emptyView: EmptyView
        get() = binding!!.emptyViewAdd

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ThemeUtils.applyBottomPaddingForNavigationBar(resultsGridView)
        ThemeUtils.applyBottomMarginForNavigationBar(binding!!.textViewPoweredByAddShowTrakt)

        // set initial view states
        setProgressVisible(visible = true, animate = false)

        // setup adapter, enable context menu only for watchlist
        adapter = AddAdapter(
            requireActivity(), ArrayList(), itemClickListener,
            listType == TraktAddLoader.Type.WATCHLIST
        )

        // load data
        LoaderManager.getInstance(this)
            .initLoader(
                ShowsTraktActivity.TRAKT_BASE_LOADER_ID + listType.ordinal, null,
                traktAddCallbacks
            )

        // add menu options
        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private val itemClickListener: AddAdapter.ItemClickListener =
        object : AddAdapter.ItemClickListener {
            override fun onItemClick(item: SearchResult) {
                if (item.state != SearchResult.STATE_ADDING) {
                    if (item.state == SearchResult.STATE_ADDED) {
                        // already in library, open it
                        startActivity(
                            OverviewActivity.intentShowByTmdbId(requireContext(), item.tmdbId)
                        )
                    } else {
                        // display more details in a dialog
                        AddShowDialogFragment.show(parentFragmentManager, item)
                    }
                }
            }

            override fun onAddClick(item: SearchResult) {
                EventBus.getDefault().post(OnAddingShowEvent(item.tmdbId))
                TaskManager.getInstance().performAddTask(requireContext(), item)
            }

            override fun onMoreOptionsClick(view: View, showTmdbId: Int) {
                val popupMenu = PopupMenu(view.context, view)
                popupMenu.inflate(R.menu.add_show_popup_menu)

                // prevent adding shows to watchlist already on watchlist
                if (listType == TraktAddLoader.Type.WATCHLIST) {
                    popupMenu.menu.findItem(R.id.menu_action_show_watchlist_add).isVisible = false
                }
                popupMenu.setOnMenuItemClickListener(
                    AddItemMenuItemClickListener(requireContext(), showTmdbId)
                )
                popupMenu.show()
            }
        }

    class AddItemMenuItemClickListener(
        private val context: Context,
        private val showTmdbId: Int
    ) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            val itemId = item.itemId
            if (itemId == R.id.menu_action_show_watchlist_add) {
                @Suppress("DEPRECATION") // AsyncTask
                AddShowToWatchlistTask(context, showTmdbId)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                return true
            }
            if (itemId == R.id.menu_action_show_watchlist_remove) {
                @Suppress("DEPRECATION") // AsyncTask
                RemoveShowFromWatchlistTask(context, showTmdbId)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                return true
            }
            return false
        }
    }

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.trakt_library_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_add_all) {
                val searchResults = searchResults
                if (searchResults != null) {
                    val showsToAdd = LinkedList<SearchResult>()
                    // only include shows not already added
                    for (result in searchResults) {
                        if (result.state == SearchResult.STATE_ADD) {
                            showsToAdd.add(result)
                            result.state = SearchResult.STATE_ADDING
                        }
                    }
                    EventBus.getDefault().post(OnAddingShowEvent())
                    TaskManager.getInstance()
                        .performAddTask(context, showsToAdd, false, false)
                }
                // disable the item so the user has to come back
                menuItem.isEnabled = false
                return true
            }
            return false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: ShowChangedEvent?) {
        if (listType == TraktAddLoader.Type.WATCHLIST) {
            // reload watchlist if a show was removed
            LoaderManager.getInstance(this)
                .restartLoader(
                    ShowsTraktActivity.TRAKT_BASE_LOADER_ID + listType.ordinal, null,
                    traktAddCallbacks
                )
        }
    }

    override fun setupEmptyView(buttonEmptyView: EmptyView) {
        buttonEmptyView.setButtonClickListener {
            setProgressVisible(visible = true, animate = false)
            LoaderManager.getInstance(this@TraktAddFragment)
                .restartLoader(
                    ShowsTraktActivity.TRAKT_BASE_LOADER_ID + listType.ordinal, null,
                    traktAddCallbacks
                )
        }
    }

    private val traktAddCallbacks: LoaderManager.LoaderCallbacks<TraktAddLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktAddLoader.Result> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<TraktAddLoader.Result> {
                return TraktAddLoader(requireContext(), listType)
            }

            override fun onLoadFinished(
                loader: Loader<TraktAddLoader.Result>,
                data: TraktAddLoader.Result
            ) {
                updateSearchResults(data.results)
                setEmptyMessage(data.emptyText)
                setProgressVisible(false, true)
            }

            override fun onLoaderReset(loader: Loader<TraktAddLoader.Result>) {
                // keep currently displayed data
            }
        }

    companion object {
        /**
         * Which Trakt list should be shown. Ordinal of one of [TraktAddLoader.Type].
         */
        private const val ARG_TYPE = "traktListType"

        val liftOnScrollTargetViewId = R.id.gridViewAdd

        fun newInstance(type: TraktAddLoader.Type): TraktAddFragment {
            val f = TraktAddFragment()
            val args = Bundle()
            args.putInt(ARG_TYPE, type.ordinal)
            f.arguments = args
            return f
        }
    }
}