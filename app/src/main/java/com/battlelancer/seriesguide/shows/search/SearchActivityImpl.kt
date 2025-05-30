// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivitySearchBinding
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.TabStripAdapter
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.google.android.gms.actions.SearchIntents
import org.greenrobot.eventbus.EventBus

/**
 * Provides tabs to search shows ([ShowSearchFragment]) or episodes ([EpisodeSearchFragment])
 * added to the database, provides the search UI used by those fragments.
 *
 * Handles the [Intent.ACTION_SEARCH] and [SearchIntents.ACTION_SEARCH] intents.
 * When [SearchManager.APP_DATA] contains a [EpisodeSearchFragment.ARG_SHOW_TITLE] switches to the
 * episodes tab.
 */
open class SearchActivityImpl : BaseMessageActivity() {

    private lateinit var binding: ActivitySearchBinding

    private val viewPager
        get() = binding.pagerSearch

    private val searchAutoCompleteView
        get() = binding.sgToolbar.autoCompleteViewToolbar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)

        setupActionBar()

        bindViews(savedInstanceState == null)

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
        binding.sgToolbar.textInputLayoutToolbar.hint = getString(R.string.search)

        searchAutoCompleteView.apply {
            // set in code as XML is overridden
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = EditorInfo.TYPE_CLASS_TEXT
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
                ViewTools.showSoftKeyboardOnSearchView(window, searchAutoCompleteView)
            }
        } else if (mayShowKeyboard) {
            // also show keyboard when showing first tab (added tab)
            ViewTools.showSoftKeyboardOnSearchView(window, searchAutoCompleteView)
        }
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            // Change the scrolling view the AppBarLayout should use to determine if it should lift.
            // This is required so the AppBarLayout does not flicker its background when scrolling.
            binding.sgAppBarLayout.liftOnScrollTargetViewId = when (position) {
                TAB_POSITION_SHOWS -> ShowSearchFragment.liftOnScrollTargetViewId
                TAB_POSITION_EPISODES -> EpisodeSearchFragment.liftOnScrollTargetViewId
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

    override fun onDestroy() {
        super.onDestroy()
        // remove any stored initial queries so they are not used when re-creating
        EventBus.getDefault().removeStickyEvent(SearchQueryEvent::class.java)
    }

    override val snackbarParentView: View
        get() = findViewById(R.id.coordinatorLayoutSearch)

    /**
     * Used by [ShowSearchFragment] and [EpisodeSearchFragment] to search as the user
     * types.
     */
    class SearchQueryEvent(val args: Bundle)

    companion object {

        /**
         * Which tab to select upon launch.
         */
        const val EXTRA_DEFAULT_TAB = "default_tab"

        const val TAB_POSITION_SHOWS = 0
        const val TAB_POSITION_EPISODES = 1
    }

}