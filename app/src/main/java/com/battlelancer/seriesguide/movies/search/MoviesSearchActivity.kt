// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.search

import android.content.Context
import android.content.Intent
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
import androidx.fragment.app.add
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoviesSearchBinding
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment
import com.battlelancer.seriesguide.movies.MoviesDiscoverLink
import com.battlelancer.seriesguide.movies.TmdbMoviesDataSource
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity.Companion.intentLink
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity.Companion.intentSearch
import com.battlelancer.seriesguide.streaming.WatchProviderFilterDialogFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.dialogs.LanguagePickerDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.SearchHistory
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.ViewTools.hideSoftKeyboard
import com.battlelancer.seriesguide.util.commitReorderingAllowed
import com.battlelancer.seriesguide.util.findDialog
import com.battlelancer.seriesguide.util.safeShow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Hosts [MoviesSearchFragment], provides a special toolbar with search bar that expands.
 *
 * If launched with [intentSearch] the search bar is always shown and initially focused.
 * If launched with [intentLink] the search bar can be shown with a menu item and hidden by
 * going up or back.
 */
class MoviesSearchActivity : BaseMessageActivity() {

    lateinit var binding: ActivityMoviesSearchBinding

    private lateinit var searchHistory: SearchHistory
    private lateinit var searchHistoryAdapter: ArrayAdapter<String>
    private var showSearchView = false

    private var link: MoviesDiscoverLink? = null

    private var yearPicker: YearPickerDialogFragment? = null
    private var languagePicker: LanguagePickerDialogFragment? = null
    private val model: MoviesSearchViewModel by viewModels {
        MoviesSearchViewModelFactory(application, link)
    }

    private val isSearchOnly: Boolean
        get() = link == null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoviesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)
        binding.sgAppBarLayout.liftOnScrollTargetViewId =
            MoviesSearchFragment.liftOnScrollTargetViewId

        link = MoviesDiscoverLink.fromId(
            intent.getIntExtra(EXTRA_ID_LINK, MoviesDiscoverLink.NO_LINK_ID)
        )

        // show search view?
        showSearchView = if (isSearchOnly) {
            true
        } else savedInstanceState?.getBoolean(STATE_SEARCH_VISIBLE, false) ?: false

        setupViews()

        if (savedInstanceState == null) {
            if (showSearchView) {
                ViewTools.showSoftKeyboardOnSearchView(window, binding.autoCompleteViewToolbar)
            }
            supportFragmentManager.commitReorderingAllowed {
                add<MoviesSearchFragment>(R.id.containerMoviesSearchFragment)
            }
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

    private fun setupViews() {
        setupActionBar()

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setSearchViewVisible(showSearchView)
        if (!isSearchOnly) {
            // When search view is optional,
            // make back button restore original list and hide search view
            onBackPressedDispatcher.addCallback(onBackPressedWithSearchVisibleCallback)
        }

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
        searchHistory = SearchHistory(this, "movies")
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
            YearPickerDialogFragment.create(model.releaseYearRaw.value)
                .also { yearPicker = it }
                .apply { onPickedListener = onYearPickedListener }
                .safeShow(supportFragmentManager, TAG_YEAR_PICKER)
        }
        binding.chipMoviesSearchOriginalLanguage.setOnClickListener {
            LanguagePickerDialogFragment.createForMovies(model.originalLanguage.value)
                .also { languagePicker = it }
                .apply { onPickedListener = onLanguagePickedListener }
                .safeShow(supportFragmentManager, TAG_LANGUAGE_PICKER)
        }
        binding.chipMoviesSearchWatchProviders.setOnClickListener {
            WatchProviderFilterDialogFragment.showForMovies(supportFragmentManager)
        }
        lifecycleScope.launch {
            model.filters.collectLatest {
                // Hide unsupported filters
                val isSearching = it.queryString.isNotEmpty() || isSearchOnly
                binding.chipMoviesSearchReleaseYear.isGone =
                    !isSearching && !TmdbMoviesDataSource.supportsYearFilter(link)
                binding.chipMoviesSearchOriginalLanguage.isGone = isSearching
                binding.chipMoviesSearchWatchProviders.isGone =
                    isSearching || !TmdbMoviesDataSource.supportsWatchProviderFilter(link)

                binding.chipMoviesSearchReleaseYear.apply {
                    val hasYear = it.releaseYear != null
                    isChipIconVisible = hasYear
                    text = if (hasYear) {
                        it.releaseYear.toString()
                    } else {
                        getString(R.string.filter_year)
                    }
                }

                binding.chipMoviesSearchOriginalLanguage.apply {
                    val hasLanguage = it.originalLanguage != null
                    isChipIconVisible = hasLanguage
                    text = if (hasLanguage) {
                        LanguageTools.buildLanguageDisplayName(it.originalLanguage!!)
                    } else {
                        getString(R.string.filter_language)
                    }
                }

                binding.chipMoviesSearchWatchProviders.apply {
                    isChipIconVisible = !it.watchProviderIds.isNullOrEmpty()
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
                ViewTools.showSoftKeyboardOnSearchView(window, binding.autoCompleteViewToolbar)
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

            // When search view is optional, make up button restore original list and hide search view
            android.R.id.home -> {
                if (!isSearchOnly && showSearchView) {
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

    private fun search() {
        binding.autoCompleteViewToolbar.text.toString()
            .trim()
            .let { query ->
                if (query.isNotEmpty()) {
                    // Must hide keyboard before clearing focus
                    hideSoftKeyboard()
                    binding.autoCompleteViewToolbar.clearFocus() // also dismisses drop-down

                    // perform search
                    model.queryString.value = query
                    // update history
                    if (searchHistory.saveRecentSearch(query)) {
                        searchHistoryAdapter.clear()
                        searchHistoryAdapter.addAll(searchHistory.searchHistory)
                    }
                }
            }
    }

    private fun setSearchViewVisible(visible: Boolean) {
        showSearchView = visible
        onBackPressedWithSearchVisibleCallback.isEnabled = visible
        binding.textInputLayoutToolbar.isGone = !visible
        // set title for screen readers
        setTitle(if (visible) R.string.search else link!!.titleRes)
        // hide title if search bar is shown
        supportActionBar?.setDisplayShowTitleEnabled(!visible)
    }

    private fun hideSearchViewAndRemoveQuery() {
        setSearchViewVisible(false)
        invalidateOptionsMenu()
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
            model.releaseYearRaw.value = year
        }
    }

    private val onLanguagePickedListener = object : LanguagePickerDialogFragment.OnPickedListener {
        override fun onPicked(languageCode: String?) {
            model.originalLanguage.value = languageCode
        }
    }

    companion object {
        private const val EXTRA_ID_LINK = "idLink"
        private const val STATE_SEARCH_VISIBLE = "searchVisible"
        private const val TAG_YEAR_PICKER = "yearPicker"
        private const val TAG_LANGUAGE_PICKER = "languagePicker"

        fun intentSearch(context: Context): Intent =
            Intent(context, MoviesSearchActivity::class.java)

        fun intentLink(context: Context, link: MoviesDiscoverLink): Intent =
            Intent(context, MoviesSearchActivity::class.java)
                .putExtra(EXTRA_ID_LINK, link.id)
    }
}