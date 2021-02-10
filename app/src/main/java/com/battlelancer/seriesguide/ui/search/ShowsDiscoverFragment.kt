package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.dialogs.ShowL10nDialogFragment
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.search.AddFragment.OnAddingShowEvent
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.widgets.EmptyView
import com.uwetrottmann.seriesguide.widgets.EmptyViewSwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays a list of shows with new episodes with links to popular shows and if connected to trakt
 * also with links to recommendations, watched and collected shows. If a search event is received,
 * displays search results. If a link is clicked launches a new activity to display them.
 */
class ShowsDiscoverFragment : BaseAddShowsFragment() {

    private val KEY_QUERY = "searchQuery"

    @BindView(R.id.swipeRefreshLayoutShowsDiscover)
    lateinit var swipeRefreshLayout: EmptyViewSwipeRefreshLayout

    @BindView(R.id.recyclerViewShowsDiscover)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.emptyViewShowsDiscover)
    lateinit var emptyView: EmptyView

    private lateinit var unbinder: Unbinder
    private lateinit var adapter: ShowsDiscoverAdapter
    private lateinit var model: ShowsDiscoverViewModel

    /** Two letter ISO 639-1 language code. */
    private lateinit var languageCode: String
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        query = if (savedInstanceState != null) {
            // restore last query
            savedInstanceState.getString(KEY_QUERY) ?: ""
        } else {
            // use initial query (if any)
            val queryEvent = EventBus.getDefault().getStickyEvent(
                    SearchActivity.SearchQuerySubmitEvent::class.java)
            queryEvent?.query ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shows_discover, container, false).also {
            unbinder = ButterKnife.bind(this, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.setSwipeableChildren(R.id.scrollViewShowsDiscover,
                R.id.recyclerViewShowsDiscover)
        swipeRefreshLayout.setOnRefreshListener { loadResults(true) }
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, swipeRefreshLayout)

        emptyView.visibility = View.GONE
        emptyView.setButtonClickListener {
            // Retrying, force load results again.
            loadResults(true)
        }

        val layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth,
                2, 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        ShowsDiscoverAdapter.VIEW_TYPE_LINK -> 1
                        ShowsDiscoverAdapter.VIEW_TYPE_HEADER -> spanCount
                        ShowsDiscoverAdapter.VIEW_TYPE_SHOW -> 2
                        else -> 0
                    }
                }
            }
        }

        recyclerView.apply {
            setHasFixedSize(true)
            this.layoutManager = layoutManager
        }

        adapter = ShowsDiscoverAdapter(requireContext(), discoverItemClickListener,
                TraktCredentials.get(context).hasCredentials(), true)
        recyclerView.adapter = adapter
    }

    private val discoverItemClickListener = object : ShowsDiscoverAdapter.OnItemClickListener {
        override fun onLinkClick(anchor: View, link: TraktShowsLink) {
            Utils.startActivityWithAnimation(activity,
                    TraktShowsActivity.intent(requireContext(), link),
                    anchor)
        }

        override fun onItemClick(item: SearchResult) {
            if (item.state != SearchResult.STATE_ADDING) {
                if (item.state == SearchResult.STATE_ADDED) {
                    // already in library, open it
                    lifecycleScope.launchWhenStarted {
                        val showId = withContext(Dispatchers.IO) {
                            SgRoomDatabase.getInstance(requireContext()).sgShow2Helper()
                                .getShowIdByTmdbId(item.tmdbId)
                        }
                        startActivity(OverviewActivity.intentShow(context, showId))
                    }
                } else {
                    // display more details in a dialog
                    AddShowDialogFragment.show(parentFragmentManager, item)
                }
            }
        }

        override fun onAddClick(item: SearchResult) {
            // post to let other fragments know show is getting added
            EventBus.getDefault().post(OnAddingShowEvent(item.tmdbId))
            TaskManager.getInstance().performAddTask(context, item)
        }

        override fun onMenuWatchlistClick(view: View, showTvdbId: Int) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.add_dialog_popup_menu)
                // only support adding shows to watchlist
                menu.findItem(R.id.menu_action_show_watchlist_remove).isVisible = false
                setOnMenuItemClickListener(
                        TraktAddFragment.AddItemMenuItemClickListener(context, showTvdbId))
            }.show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // enable menu
        setHasOptionsMenu(true)

        languageCode = DisplaySettings.getShowsSearchLanguage(context)

        // observe and load results
        model = ViewModelProvider(this).get(ShowsDiscoverViewModel::class.java)
        model.data.observe(viewLifecycleOwner, Observer { handleResultsUpdate(it) })
        loadResults()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchActivity.SearchQuerySubmitEvent) {
        query = event.query
        loadResults()
    }

    private fun loadResults(forceLoad: Boolean = false) {
        val willLoad = model.data.load(query, languageCode, forceLoad)
        if (willLoad) swipeRefreshLayout.isRefreshing = true
    }

    private fun handleResultsUpdate(result: ShowsDiscoverLiveData.Result?) {
        result?.let {
            swipeRefreshLayout.isRefreshing = false

            val hasResults = result.searchResults.isNotEmpty()

            emptyView.setButtonText(R.string.action_try_again)
            emptyView.setMessage(result.emptyText)
            emptyView.visibility = if (hasResults) View.GONE else View.VISIBLE

            recyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE
            adapter.updateSearchResults(result.searchResults, result.isResultsForQuery)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tvdb_add_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_action_shows_search_clear_history) {
            // tell the hosting activity to clear the search view history
            EventBus.getDefault().post(SearchActivity.ClearSearchHistoryEvent())
            return true
        }
        if (itemId == R.id.menu_action_shows_search_change_language) {
            displayLanguageSettings()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayLanguageSettings() {
        ShowL10nDialogFragment.show(
            parentFragmentManager,
            languageCode,
            ShowL10nDialogFragment.TAG_DISCOVER
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_QUERY, query)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unbinder.unbind()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ShowL10nDialogFragment.LanguageChangedEvent) {
        if (ShowL10nDialogFragment.TAG_DISCOVER != event.tag) {
            return
        }
        changeLanguage(event.selectedLanguageCode)
        loadResults()
    }

    private fun changeLanguage(languageCode: String) {
        this.languageCode = languageCode

        // save selected search language
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(DisplaySettings.KEY_LANGUAGE_SEARCH, languageCode)
        }
        Timber.d("Set search language to %s", languageCode)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTabClickEvent(event: TabClickEvent) {
        if (event.position == SearchActivity.TAB_POSITION_SEARCH) {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    override fun setAllPendingNotAdded() {
        adapter.setAllPendingNotAdded()
    }

    override fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        adapter.setStateForTmdbId(showTmdbId, newState)
    }

}