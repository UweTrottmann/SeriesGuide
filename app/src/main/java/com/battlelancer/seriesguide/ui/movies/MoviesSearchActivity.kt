package com.battlelancer.seriesguide.ui.movies

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
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoviesSearchBinding
import com.battlelancer.seriesguide.settings.SearchSettings
import com.battlelancer.seriesguide.streaming.DiscoverFilterFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.movies.MovieLocalizationDialogFragment.Companion.show
import com.battlelancer.seriesguide.ui.movies.MovieLocalizationDialogFragment.LocalizationChangedEvent
import com.battlelancer.seriesguide.ui.movies.MoviesSearchFragment.Companion.newInstance
import com.battlelancer.seriesguide.ui.movies.MoviesSearchFragment.OnSearchClickListener
import com.battlelancer.seriesguide.util.HighlightTools
import com.battlelancer.seriesguide.util.SearchHistory
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Hosts [MoviesSearchFragment], provides a special toolbar with search bar that expands.
 */
class MoviesSearchActivity : BaseMessageActivity(), OnSearchClickListener {

    private lateinit var binding: ActivityMoviesSearchBinding

    private lateinit var searchHistory: SearchHistory
    private lateinit var searchHistoryAdapter: ArrayAdapter<String>
    private var showSearchView = false

    private lateinit var link: MoviesDiscoverLink

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoviesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    binding.sgToolbar.autoCompleteViewToolbar
                )
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.containerMoviesSearchFragment, newInstance(link))
                .commit()
        } else {
            postponeEnterTransition()
            // allow the adapter to repopulate during the next layout pass
            // before starting the transition animation
            binding.containerMoviesSearchFragment.post { startPostponedEnterTransition() }
        }

        // Highlight new movies filter.
        HighlightTools.highlightSgToolbarItem(
            HighlightTools.Feature.MOVIE_FILTER,
            this,
            lifecycleScope,
            R.id.menu_action_movies_search_filter,
            R.string.action_movies_filter
        ) {
            link == MoviesDiscoverLink.POPULAR || link == MoviesDiscoverLink.DIGITAL
        }
    }

    private fun setupActionBar(link: MoviesDiscoverLink) {
        super.setupActionBar()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // set title for screen readers
        if (showSearchView) {
            setTitle(R.string.search)
        } else {
            setTitle(link.titleRes)
        }

        setSearchViewVisible(showSearchView)

        // setup search box
        val searchView = binding.sgToolbar.autoCompleteViewToolbar
        searchView.threshold = 1
        searchView.setOnClickListener(searchViewClickListener)
        searchView.onItemClickListener = searchViewItemClickListener
        searchView.setOnEditorActionListener(searchViewActionListener)
        binding.sgToolbar.textInputLayoutToolbar.hint = getString(R.string.movies_search_hint)
        // set in code as XML is overridden
        searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchView.inputType = EditorInfo.TYPE_CLASS_TEXT

        // setup search history
        searchHistory = SearchHistory(this, SearchSettings.KEY_SUFFIX_MOVIES)
        searchHistoryAdapter = ArrayAdapter(
            this,
            R.layout.item_dropdown,
            searchHistory.searchHistory
        )
        searchView.setAdapter(searchHistoryAdapter)
        // drop-down is auto-shown on config change, ensure it is hidden when recreating views
        searchView.dismissDropDown()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.movies_search_menu, menu)

        val itemSearch = menu.findItem(R.id.menu_action_movies_search_display_search)
        itemSearch.isVisible = !showSearchView
        itemSearch.isEnabled = !showSearchView

        val showFilter = TmdbMoviesDataSource.isLinkFilterable(link)
        menu.findItem(R.id.menu_action_movies_search_filter).apply {
            isVisible = showFilter
            isEnabled = showFilter
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_action_movies_search_change_language -> {
                show(supportFragmentManager)
                return true
            }
            R.id.menu_action_movies_search_display_search -> {
                setSearchViewVisible(true)
                ViewTools.showSoftKeyboardOnSearchView(
                    this,
                    binding.sgToolbar.autoCompleteViewToolbar
                )
                showSearchView = true
                invalidateOptionsMenu()
                return true
            }
            R.id.menu_action_movies_search_filter -> {
                DiscoverFilterFragment.showForMovies(supportFragmentManager)
                return true
            }
            R.id.menu_action_movies_search_clear_history -> {
                searchHistory.clearHistory()
                searchHistoryAdapter.clear()
                // setting text to null seems to fix the dropdown from not clearing
                binding.sgToolbar.autoCompleteViewToolbar.setText(null)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SEARCH_VISIBLE, showSearchView)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventLanguageChanged(@Suppress("UNUSED_PARAMETER") event: LocalizationChangedEvent?) {
        // just run the current search again
        search()
    }

    override fun onSearchClick() {
        search()
    }

    private fun search() {
        val fragment = supportFragmentManager.findFragmentById(
            R.id.containerMoviesSearchFragment
        ) ?: return

        val query = binding.sgToolbar.autoCompleteViewToolbar.text.toString().trim()
        // perform search
        val searchFragment = fragment as MoviesSearchFragment
        searchFragment.search(query)
        // update history
        if (searchHistory.saveRecentSearch(query)) {
            searchHistoryAdapter.clear()
            searchHistoryAdapter.addAll(searchHistory.searchHistory)
        }
    }

    private fun setSearchViewVisible(visible: Boolean) {
        binding.sgToolbar.containerSearchBar.visibility = if (visible) View.VISIBLE else View.GONE
        supportActionBar?.setDisplayShowTitleEnabled(!visible)
    }

    private val searchViewClickListener =
        View.OnClickListener { binding.sgToolbar.autoCompleteViewToolbar.showDropDown() }

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

    companion object {
        const val EXTRA_ID_LINK = "idLink"
        private const val STATE_SEARCH_VISIBLE = "searchVisible"
    }
}