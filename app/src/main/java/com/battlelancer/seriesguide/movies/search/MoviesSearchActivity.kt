// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoviesSearchBinding
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment
import com.battlelancer.seriesguide.movies.MoviesDiscoverAdapter
import com.battlelancer.seriesguide.movies.MoviesDiscoverLink
import com.battlelancer.seriesguide.movies.TmdbMoviesDataSource
import com.battlelancer.seriesguide.shows.search.popular.LanguagePickerDialogFragment
import com.battlelancer.seriesguide.shows.search.popular.YearPickerDialogFragment
import com.battlelancer.seriesguide.streaming.WatchProviderFilterDialogFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.SearchHistory
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.findDialog
import com.battlelancer.seriesguide.util.safeShow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Hosts [MoviesSearchFragment], provides a special toolbar with search bar that expands.
 */
class MoviesSearchActivity : BaseMessageActivity(), MoviesSearchFragment.OnSearchClickListener {

    lateinit var binding: ActivityMoviesSearchBinding

    private lateinit var searchHistory: SearchHistory
    private lateinit var searchHistoryAdapter: ArrayAdapter<String>
    private var showSearchView = false

    private lateinit var link: MoviesDiscoverLink

    private var yearPicker: YearPickerDialogFragment? = null
    private var languagePicker: LanguagePickerDialogFragment? = null
    private val model: MoviesSearchViewModel by viewModels {
        MoviesSearchViewModelFactory(application, link)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoviesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)
        binding.sgAppBarLayout.liftOnScrollTargetViewId =
            MoviesSearchFragment.liftOnScrollTargetViewId

        val linkId = intent.getIntExtra(
            EXTRA_ID_LINK,
            MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT.id
        )
        link = MoviesDiscoverLink.fromId(linkId)
        showSearchView = link == MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT
        if (savedInstanceState != null) {
            showSearchView = savedInstanceState.getBoolean(STATE_SEARCH_VISIBLE, showSearchView)
        }

        setupActionBar(link)

        if (savedInstanceState == null) {
            if (showSearchView) {
                ViewTools.showSoftKeyboardOnSearchView(
                    this,
                    binding.autoCompleteViewToolbar
                )
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.containerMoviesSearchFragment, MoviesSearchFragment.newInstance(link))
                .commit()
        } else {
            postponeEnterTransition()
            // allow the adapter to repopulate during the next layout pass
            // before starting the transition animation
            binding.containerMoviesSearchFragment.post { startPostponedEnterTransition() }

            // Re-attach listeners to any showing dialogs
            yearPicker =
                findDialog<YearPickerDialogFragment>(supportFragmentManager, TAG_YEAR_PICKER)
                    ?.also { it.onPickedListener = onYearPickedListener }
            languagePicker =
                findDialog<LanguagePickerDialogFragment>(
                    supportFragmentManager,
                    TAG_LANGUAGE_PICKER
                )?.also { it.onPickedListener = onLanguagePickedListener }
        }
    }

    private fun setupActionBar(link: MoviesDiscoverLink) {
        super.setupActionBar()

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setSearchViewVisible(showSearchView)
        // When search view is shown, make back button restore original list and hide search view
        onBackPressedDispatcher.addCallback(onBackPressedWithSearchVisibleCallback)

        // search box
        val searchView = binding.autoCompleteViewToolbar
        searchView.threshold = 1
        searchView.setOnClickListener(searchViewClickListener)
        searchView.onItemClickListener = searchViewItemClickListener
        searchView.setOnEditorActionListener(searchViewActionListener)
        binding.textInputLayoutToolbar.hint = getString(R.string.movies_search_hint)
        // set in code as XML is overridden
        searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchView.inputType = EditorInfo.TYPE_CLASS_TEXT

        // search history
        searchHistory = SearchHistory(this, "tmdb")
        searchHistoryAdapter = ArrayAdapter(
            this,
            R.layout.item_dropdown,
            searchHistory.searchHistory
        )
        searchView.setAdapter(searchHistoryAdapter)
        // drop-down is auto-shown on config change, ensure it is hidden when recreating views
        searchView.dismissDropDown()

        // filters
        binding.chipMoviesSearchReleaseYear.setOnClickListener {
            YearPickerDialogFragment.create(model.releaseYear.value)
                .also { yearPicker = it }
                .apply { onPickedListener = onYearPickedListener }
                .safeShow(supportFragmentManager, TAG_YEAR_PICKER)
        }
        binding.chipMoviesSearchOriginalLanguage.setOnClickListener {
            LanguagePickerDialogFragment.create(model.originalLanguage.value)
                .also { languagePicker = it }
                .apply { onPickedListener = onLanguagePickedListener }
                .safeShow(supportFragmentManager, TAG_LANGUAGE_PICKER)
        }
        binding.chipMoviesSearchWatchProviders.setOnClickListener {
            WatchProviderFilterDialogFragment.showForMovies(supportFragmentManager)
        }
        lifecycleScope.launch {
            model.queryString.collectLatest {
                // Hide unsupported filters
                val hasQuery = it.isNotEmpty()
                binding.chipMoviesSearchReleaseYear.isGone =
                    !hasQuery && !TmdbMoviesDataSource.supportsYearFilter(link)
                binding.chipMoviesSearchOriginalLanguage.isGone = hasQuery
                binding.chipMoviesSearchWatchProviders.isGone =
                    hasQuery || !TmdbMoviesDataSource.supportsWatchProviderFilter(link)
            }
        }
        lifecycleScope.launch {
            model.releaseYear.collectLatest { year ->
                binding.chipMoviesSearchReleaseYear.apply {
                    val hasYear = year != null
                    isChipIconVisible = hasYear
                    text = if (hasYear) {
                        year.toString()
                    } else {
                        getString(R.string.filter_year)
                    }
                }
            }
        }
        lifecycleScope.launch {
            model.originalLanguage.collectLatest { language ->
                binding.chipMoviesSearchOriginalLanguage.apply {
                    val hasLanguage = language != null
                    isChipIconVisible = hasLanguage
                    text = if (hasLanguage) {
                        LanguageTools.buildLanguageDisplayName(language!!)
                    } else {
                        getString(R.string.filter_language)
                    }
                }
            }
        }
        lifecycleScope.launch {
            model.watchProviderIds.collectLatest {
                binding.chipMoviesSearchWatchProviders.apply {
                    isChipIconVisible = it.isNotEmpty()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.movies_search_menu, menu)

        val itemSearch = menu.findItem(R.id.menu_action_movies_search_display_search)
        itemSearch.isVisible = !showSearchView
        itemSearch.isEnabled = !showSearchView

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_action_movies_search_change_language -> {
                MovieLocalizationDialogFragment.show(supportFragmentManager)
                return true
            }

            R.id.menu_action_movies_search_display_search -> {
                setSearchViewVisible(true)
                ViewTools.showSoftKeyboardOnSearchView(
                    this,
                    binding.autoCompleteViewToolbar
                )
                invalidateOptionsMenu()
                return true
            }

            R.id.menu_action_movies_search_clear_history -> {
                searchHistory.clearHistory()
                searchHistoryAdapter.clear()
                // setting text to null seems to fix the dropdown from not clearing
                binding.autoCompleteViewToolbar.setText(null)
                return true
            }

            // When search view is shown, make up button restore original list and hide search view
            android.R.id.home -> {
                if (showSearchView) {
                    hideSearchViewAndRemoveQuery()
                    return true
                } else {
                    return super.onOptionsItemSelected(item)
                }
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SEARCH_VISIBLE, showSearchView)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventLanguageChanged(@Suppress("UNUSED_PARAMETER") event: MovieLocalizationDialogFragment.LocalizationChangedEvent?) {
        // just run the current search again
        search()
    }

    override fun onSearchClick() {
        search()
    }

    private fun search() {
        val query = binding.autoCompleteViewToolbar.text.toString().trim()
        // perform search
        model.queryString.value = query
        // update history
        if (searchHistory.saveRecentSearch(query)) {
            searchHistoryAdapter.clear()
            searchHistoryAdapter.addAll(searchHistory.searchHistory)
        }
        // if search query is empty hide search bar, show search button again
        if (query.isEmpty()) {
            hideSearchView()
        }
    }

    private fun setSearchViewVisible(visible: Boolean) {
        showSearchView = visible
        onBackPressedWithSearchVisibleCallback.isEnabled = visible
        binding.textInputLayoutToolbar.isGone = !visible
        // set title for screen readers
        setTitle(if (visible) R.string.search else link.titleRes)
        // hide title if search bar is shown
        supportActionBar?.setDisplayShowTitleEnabled(!visible)
    }

    private fun hideSearchView() {
        setSearchViewVisible(false)
        invalidateOptionsMenu()
    }

    private fun hideSearchViewAndRemoveQuery() {
        hideSearchView()
        model.removeQuery()
    }

    private val onBackPressedWithSearchVisibleCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hideSearchViewAndRemoveQuery()
        }
    }

    private val searchViewClickListener =
        View.OnClickListener { binding.autoCompleteViewToolbar.showDropDown() }

    private val searchViewItemClickListener =
        AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long -> search() }

    private val searchViewActionListener =
        OnEditorActionListener { _: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                search()
                return@OnEditorActionListener true
            }
            false
        }

    private val onYearPickedListener = object : YearPickerDialogFragment.OnPickedListener {
        override fun onPicked(year: Int?) {
            model.releaseYear.value = year
        }
    }

    private val onLanguagePickedListener = object : LanguagePickerDialogFragment.OnPickedListener {
        override fun onPicked(languageCode: String?) {
            model.originalLanguage.value = languageCode
        }
    }

    companion object {
        const val EXTRA_ID_LINK = "idLink"
        private const val STATE_SEARCH_VISIBLE = "searchVisible"
        private const val TAG_YEAR_PICKER = "yearPicker"
        private const val TAG_LANGUAGE_PICKER = "languagePicker"
    }
}