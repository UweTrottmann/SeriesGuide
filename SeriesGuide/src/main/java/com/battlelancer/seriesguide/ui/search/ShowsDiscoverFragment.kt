package com.battlelancer.seriesguide.ui.search

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.search.AddFragment.OnAddingShowEvent
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.tasks.RemoveShowTask
import com.battlelancer.seriesguide.widgets.EmptyView
import com.battlelancer.seriesguide.widgets.EmptyViewSwipeRefreshLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays a list of shows with new episodes with links to popular shows and if connected to trakt
 * also with links to recommendations, watched and collected shows. If a search event is received,
 * displays search results. If a link is clicked launches a new activity to display them.
 */
class ShowsDiscoverFragment : Fragment() {

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

    /** Two letter ISO 639-1 language code or 'xx' meaning any language. */
    private lateinit var languageCode: String
    private val languageCodeAny: String by lazy { getString(R.string.language_code_any) }
    private var shouldTryAnyLanguage = false
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(KEY_QUERY)
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
        ViewTools.setSwipeRefreshLayoutColors(activity!!.theme, swipeRefreshLayout)

        emptyView.visibility = View.GONE
        emptyView.setButtonClickListener {
            if (shouldTryAnyLanguage && languageCode != languageCodeAny) {
                // try again with any language
                shouldTryAnyLanguage = false
                changeLanguage(languageCodeAny)
                loadResults()
            } else {
                // already set to any language or retrying, force loading results again
                loadResults(true)
            }
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

        adapter = ShowsDiscoverAdapter(context!!, itemClickListener,
                TraktCredentials.get(context).hasCredentials(), true)
        recyclerView.adapter = adapter
    }

    private val itemClickListener = object : ShowsDiscoverAdapter.OnItemClickListener {
        override fun onLinkClick(anchor: View, link: TraktShowsLink) {
            Utils.startActivityWithAnimation(activity,
                    TraktShowsActivity.intent(context!!, link),
                    anchor)
        }

        override fun onItemClick(item: SearchResult) {
            if (item.state != SearchResult.STATE_ADDING) {
                if (item.state == SearchResult.STATE_ADDED) {
                    // already in library, open it
                    startActivity(OverviewActivity.intentShow(context, item.tvdbid))
                } else {
                    // guard against onClick called after fragment is paged away (multi-touch)
                    // onSaveInstanceState might already be called
                    if (isResumed) {
                        // display more details in a dialog
                        AddShowDialogFragment.showAddDialog(item, fragmentManager)
                    }
                }
            }
        }

        override fun onAddClick(item: SearchResult) {
            // post to let other fragments know show is getting added
            EventBus.getDefault().post(OnAddingShowEvent(item.tvdbid))
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

        languageCode = DisplaySettings.getSearchLanguage(context)

        // observe and load results
        model = ViewModelProviders.of(this).get(ShowsDiscoverViewModel::class.java)
        model.data.observe(this, Observer { handleResultsUpdate(it) })
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

            if (it.successful && !hasResults && languageCode != languageCodeAny) {
                shouldTryAnyLanguage = true
                emptyView.setButtonText(R.string.action_try_any_language)
            } else {
                emptyView.setButtonText(R.string.action_try_again)
            }
            emptyView.setMessage(result.emptyText)
            emptyView.visibility = if (hasResults) View.GONE else View.VISIBLE

            recyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE
            adapter.updateSearchResults(result.searchResults, result.isResultsForQuery)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.tvdb_add_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val itemId = item?.itemId
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
        // guard against onClick called after fragment is up navigated (multi-touch)
        // onSaveInstanceState might already be called
        if (isResumed) {
            fragmentManager?.let {
                val dialogFragment = LanguageChoiceDialogFragment.newInstance(
                        R.array.languageCodesShowsWithAny, languageCode)
                dialogFragment.show(fragmentManager, "dialog-language")
            }
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_QUERY, query)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // when switching tabs while still showing refresh animation, old content remains stuck
        // so force clear the drawing cache and animation: http://stackoverflow.com/a/27073879
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.apply {
                isRefreshing = false
                destroyDrawingCache()
                clearAnimation()
            }
        }

        unbinder.unbind()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: LanguageChoiceDialogFragment.LanguageChangedEvent) {
        changeLanguage(event.selectedLanguageCode)
        loadResults()
    }

    private fun changeLanguage(languageCode: String) {
        this.languageCode = languageCode

        // save selected search language
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(DisplaySettings.KEY_LANGUAGE_SEARCH, languageCode)
                .apply()
        Timber.d("Set search language to %s", languageCode)
    }

    /**
     * Called if the user triggers adding a single new show through the add dialog. The show is not
     * actually added, yet.
     *
     * @see [onShowAddedEvent]
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAddingShowEvent(event: OnAddingShowEvent) {
        if (event.showTvdbId > 0) {
            adapter.setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADDING)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowAddedEvent(event: AddShowTask.OnShowAddedEvent) {
        when {
            event.successful -> setShowAdded(event.showTvdbId)
            event.showTvdbId > 0 -> setShowNotAdded(event.showTvdbId)
            else -> adapter.setAllPendingNotAdded()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowRemovedEvent(event: RemoveShowTask.OnShowRemovedEvent) {
        if (event.resultCode == NetworkResult.SUCCESS) {
            setShowNotAdded(event.showTvdbId)
        }
    }

    private fun setShowAdded(showTvdbId: Int) {
        adapter.setStateForTvdbId(showTvdbId, SearchResult.STATE_ADDED)
    }

    private fun setShowNotAdded(showTvdbId: Int) {
        adapter.setStateForTvdbId(showTvdbId, SearchResult.STATE_ADD)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTabClickEvent(event: TabClickEvent) {
        if (event.position == SearchActivity.TAB_POSITION_SEARCH) {
            recyclerView.smoothScrollToPosition(0)
        }
    }

}