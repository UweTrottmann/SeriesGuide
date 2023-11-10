// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivitySearchBinding
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.shows.search.discover.AddShowDialogFragment
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverFragment
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.TabStripAdapter
import com.battlelancer.seriesguide.util.HighlightTools
import com.battlelancer.seriesguide.util.SearchHistory
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.google.android.gms.actions.SearchIntents
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Handles search intents and displays a [EpisodeSearchFragment] when needed or redirects
 * directly to an [EpisodesActivity].
 */
open class SearchActivityImpl : BaseMessageActivity(), AddShowDialogFragment.OnAddShowListener,
    SearchTriggerListener {

    private lateinit var binding: ActivitySearchBinding

    private val viewPager
        get() = binding.pagerSearch

    private val searchAutoCompleteView
        get() = binding.sgToolbar.autoCompleteViewToolbar

    private lateinit var searchHistory: SearchHistory
    private lateinit var searchHistoryAdapter: ArrayAdapter<String>
    private var remoteSearchVisible: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)

        setupActionBar()

        bindViews(savedInstanceState == null)

        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(this) {
            startActivity(SimilarShowsActivity.intent(this, it.tmdbId, it.title))
        }

        handleSearchIntent(intent)
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun bindViews(mayShowKeyboard: Boolean) {
        searchAutoCompleteView.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || (event != null && event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                triggerTvdbSearch()
                true
            } else {
                false
            }
        }

        // setup search history (only used by TVDb search)
        searchHistory = SearchHistory(this, "thetvdb")
        searchHistoryAdapter = ArrayAdapter(
            this, R.layout.item_dropdown, searchHistory.searchHistory
        )
        searchAutoCompleteView.apply {
            threshold = 1
            setOnClickListener { v -> (v as AutoCompleteTextView).showDropDown() }
            setOnItemClickListener { _, _, _, _ -> triggerTvdbSearch() }
            // set in code as XML is overridden
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = EditorInfo.TYPE_CLASS_TEXT
            // drop-down is auto-shown on config change, ensure it is hidden when recreating views
            dismissDropDown()
        }

        val tabs = binding.tabsSearch
        val tabsAdapter = TabStripAdapter(this, viewPager, tabs)
        tabs.setOnPageChangeListener(pageChangeListener)
        tabs.setOnTabClickListener { position ->
            if (viewPager.currentItem == position) {
                EventBus.getDefault().post(TabClickEvent(position))
            }
        }

        tabsAdapter.apply {
            addTab(R.string.shows, ShowSearchFragment::class.java, null)
            addTab(R.string.episodes, EpisodeSearchFragment::class.java, null)
            addTab(R.string.title_discover, ShowsDiscoverFragment::class.java, null)
            notifyTabsChanged()
        }

        // set default tab
        if (intent != null && intent.extras != null) {
            val defaultTab = intent.extras!!.getInt(EXTRA_DEFAULT_TAB)
            if (defaultTab < tabsAdapter.itemCount) {
                viewPager.setCurrentItem(defaultTab, false)
            }
            if (mayShowKeyboard &&
                (defaultTab == TAB_POSITION_SHOWS || defaultTab == TAB_POSITION_EPISODES)) {
                ViewTools.showSoftKeyboardOnSearchView(this, searchAutoCompleteView)
            }
        } else if (mayShowKeyboard) {
            // also show keyboard when showing first tab (added tab)
            ViewTools.showSoftKeyboardOnSearchView(this, searchAutoCompleteView)
        }

        // Highlight new shows filter.
        HighlightTools.highlightSgToolbarItem(
            HighlightTools.Feature.SHOW_FILTER,
            this,
            lifecycle,
            R.id.menu_action_shows_search_filter,
            R.string.action_shows_filter
        ) {
            viewPager.currentItem == TAB_POSITION_SEARCH
        }
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            remoteSearchVisible = position == TAB_POSITION_SEARCH
            searchAutoCompleteView.setAdapter<ArrayAdapter<String>>(if (remoteSearchVisible) searchHistoryAdapter else null)
            binding.sgToolbar.textInputLayoutToolbar.hint =
                getString(if (remoteSearchVisible) R.string.checkin_searchhint else R.string.search)

            // Change the scrolling view the AppBarLayout should use to determine if it should lift.
            // This is required so the AppBarLayout does not flicker its background when scrolling.
            binding.sgAppBarLayout.liftOnScrollTargetViewId = when (position) {
                TAB_POSITION_SHOWS -> ShowSearchFragment.liftOnScrollTargetViewId
                TAB_POSITION_EPISODES -> EpisodeSearchFragment.liftOnScrollTargetViewId
                TAB_POSITION_SEARCH -> ShowsDiscoverFragment.liftOnScrollTargetViewId
                else -> throw IllegalArgumentException("Unknown page position")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSearchIntent(intent)
    }

    private fun handleSearchIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val action = intent.action
        // global or Google Now voice search
        if (Intent.ACTION_SEARCH == action || SearchIntents.ACTION_SEARCH == action) {
            val launchIntent = getIntent()

            // searching episodes within a show?
            val appData = launchIntent.getBundleExtra(SearchManager.APP_DATA)
            if (appData != null) {
                val showTitle = appData.getString(EpisodeSearchFragment.ARG_SHOW_TITLE)
                if (!TextUtils.isEmpty(showTitle)) {
                    // change title + switch to episodes tab if show restriction was submitted
                    viewPager.setCurrentItem(TAB_POSITION_EPISODES, false)
                }
            }

            val query = launchIntent.getStringExtra(SearchManager.QUERY)
            searchAutoCompleteView.setText(query)
            triggerLocalSearch(query)
        } else if (Intent.ACTION_SEND == action) {
            // text share intents from other apps
            if ("text/plain" == intent.type) {
                handleSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
        }
    }

    private fun handleSharedText(sharedText: String?) {
        if (sharedText.isNullOrEmpty()) {
            return
        }

        // try to match TMDB URLs
        lifecycleScope.launch {
            val showTmdbId = TmdbIdExtractor(applicationContext, sharedText).tryToExtract()
            if (showTmdbId > 0) {
                // found an id, display the add dialog
                AddShowDialogFragment.show(supportFragmentManager, showTmdbId)
            } else {
                // no id, populate the search field instead
                viewPager.setCurrentItem(TAB_POSITION_SEARCH, false)
                searchAutoCompleteView.setText(sharedText)
                triggerTvdbSearch()
                triggerLocalSearch(sharedText)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // set after view states are restored to avoid triggering
        searchAutoCompleteView.addTextChangedListener(textWatcher)
    }

    override fun onPause() {
        super.onPause()
        searchAutoCompleteView.removeTextChangedListener(textWatcher)
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            triggerLocalSearch(s)
        }

        override fun afterTextChanged(s: Editable) {}
    }

    private fun triggerLocalSearch(queryRaw: CharSequence?) {
        val query = if (TextUtils.isEmpty(queryRaw)) "" else queryRaw.toString()

        val args = Bundle()
        args.putString(SearchManager.QUERY, query)

        val extras = intent.extras
        if (extras != null) {
            val appData = extras.getBundle(SearchManager.APP_DATA)
            if (appData != null) {
                args.putBundle(SearchManager.APP_DATA, appData)
            }
        }

        EventBus.getDefault().postSticky(SearchQueryEvent(args))
    }

    private fun triggerTvdbSearch() {
        if (remoteSearchVisible) {
            searchAutoCompleteView.dismissDropDown()
            // extract and post query
            val query = searchAutoCompleteView.text.toString().trim()
            EventBus.getDefault().postSticky(SearchQuerySubmitEvent(query))
            // update history
            if (query.isNotEmpty()) {
                if (searchHistory.saveRecentSearch(query)) {
                    searchHistoryAdapter.clear()
                    searchHistoryAdapter.addAll(searchHistory.searchHistory)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // remove any stored initial queries so they are not used when re-creating
        EventBus.getDefault().removeStickyEvent(SearchQueryEvent::class.java)
        EventBus.getDefault().removeStickyEvent(SearchQuerySubmitEvent::class.java)
    }

    override fun onAddShow(show: SearchResult) {
        TaskManager.getInstance().performAddTask(this, show)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: ClearSearchHistoryEvent) {
        searchHistory.clearHistory()
        searchHistoryAdapter.clear()
        // setting text to null seems to fix the dropdown from not clearing
        searchAutoCompleteView.text = null
    }


    override val snackbarParentView: View
        get() = findViewById(R.id.coordinatorLayoutSearch)

    override fun switchToDiscoverAndSearch() {
        viewPager.currentItem = TAB_POSITION_SEARCH
        triggerTvdbSearch()
    }

    /** Used by [ShowsDiscoverFragment] to indicate the search history should be cleared.  */
    class ClearSearchHistoryEvent

    /**
     * Used by [ShowSearchFragment] and [EpisodeSearchFragment] to search as the user
     * types.
     */
    class SearchQueryEvent(val args: Bundle)

    /**
     * Used by [ShowsDiscoverFragment] to submit a query. Unlike local search it is not type
     * and search.
     */
    class SearchQuerySubmitEvent(val query: String)

    companion object {

        /**
         * Which tab to select upon launch.
         */
        const val EXTRA_DEFAULT_TAB = "default_tab"

        const val TAB_POSITION_SHOWS = 0
        const val TAB_POSITION_EPISODES = 1
        const val TAB_POSITION_SEARCH = 2

        const val SHOWS_LOADER_ID = 100
        const val EPISODES_LOADER_ID = 101
        const val TRAKT_BASE_LOADER_ID = 200
    }

}