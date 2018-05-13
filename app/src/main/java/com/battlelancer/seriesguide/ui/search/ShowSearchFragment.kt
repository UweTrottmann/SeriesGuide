package com.battlelancer.seriesguide.ui.search

import android.app.SearchManager
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.PopupMenu
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.ui.ListsActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.shows.BaseShowsAdapter
import com.battlelancer.seriesguide.ui.shows.ShowMenuItemClickListener
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.widgets.EmptyView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays show search results.
 */
class ShowSearchFragment : BaseSearchFragment() {

    private lateinit var adapter: ShowResultsAdapter
    private lateinit var searchTriggerListener: SearchTriggerListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (emptyView as EmptyView).setButtonClickListener {
            searchTriggerListener.switchToDiscoverAndSearch()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is SearchTriggerListener) {
            searchTriggerListener = activity as SearchTriggerListener
        } else {
            throw IllegalArgumentException("Activity does not implement SearchTriggerListener")
        }

        adapter = ShowResultsAdapter(activity, onItemClickListener).also {
            gridView.adapter = it
        }

        // load for given query or restore last loader (ignoring args)
        loaderManager.initLoader(SearchActivity.SHOWS_LOADER_ID, loaderArgs,
                searchLoaderCallbacks)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        OverviewActivity.intentShow(context, id.toInt()).let {
            ActivityCompat.startActivity(requireActivity(), it,
                    ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0,
                            view.width, view.height).toBundle())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchActivity.SearchQueryEvent) {
        loaderManager.restartLoader(SearchActivity.SHOWS_LOADER_ID, event.args,
                searchLoaderCallbacks)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventTabClick(event: TabClickEvent) {
        if (event.position == SearchActivity.TAB_POSITION_SHOWS) {
            gridView.smoothScrollToPosition(0)
        }
    }

    private val searchLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            loaderArgs = args
            val query = args?.getString(SearchManager.QUERY)
            return if (query.isNullOrEmpty()) {
                // empty query selects shows with next episodes before this point in time
                CursorLoader(requireContext(), Shows.CONTENT_URI,
                        ShowResultsAdapter.Query.PROJECTION,
                        Shows.SELECTION_WITH_NEXT_NOT_HIDDEN,
                        arrayOf((TimeTools.getCurrentTime(
                                requireContext()) + DateUtils.HOUR_IN_MILLIS).toString()),
                        Shows.SORT_LATEST_EPISODE)
            } else {
                Shows.CONTENT_URI_FILTER.buildUpon().appendPath(query).build().let {
                    CursorLoader(requireActivity(), it, ShowResultsAdapter.Query.PROJECTION,
                            null, null, null)
                }
            }
        }

        override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
            adapter.swapCursor(data)
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {
            adapter.swapCursor(null)
        }
    }

    private val onItemClickListener = object : BaseShowsAdapter.OnItemClickListener {
        override fun onClick(view: View, viewHolder: BaseShowsAdapter.ShowViewHolder) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.shows_popup_menu)
                menu.apply {
                    // show/hide some menu items depending on show properties
                    findItem(
                            R.id.menu_action_shows_favorites_add).isVisible = !viewHolder.isFavorited
                    findItem(
                            R.id.menu_action_shows_favorites_remove).isVisible = viewHolder.isFavorited
                    findItem(R.id.menu_action_shows_hide).isVisible = !viewHolder.isHidden
                    findItem(R.id.menu_action_shows_unhide).isVisible = viewHolder.isHidden

                    // hide unused actions
                    findItem(R.id.menu_action_shows_watched_next).isVisible = false
                }
                setOnMenuItemClickListener(
                        ShowMenuItemClickListener(context,
                                fragmentManager, viewHolder.showTvdbId, viewHolder.episodeTvdbId,
                                ListsActivity.TAG))
            }.show()
        }

        override fun onFavoriteClick(showTvdbId: Int, isFavorite: Boolean) {
            SgApp.getServicesComponent(requireContext()).showTools()
                    .storeIsFavorite(showTvdbId, isFavorite)
        }
    }
}
