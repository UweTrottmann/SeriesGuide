// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoviesSearchBinding
import com.battlelancer.seriesguide.databinding.FragmentShowsPopularBinding
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.search.TmdbIdExtractor
import com.battlelancer.seriesguide.streaming.WatchProviderFilterDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.LanguagePickerDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.SearchHistory
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.ViewTools.hideSoftKeyboard
import com.battlelancer.seriesguide.util.findDialog
import com.battlelancer.seriesguide.util.safeShow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays shows provided by a [ShowsDiscoverPagingViewModel], expects to be hosted
 * in [ShowsDiscoverPagingActivity] which provides the filter UI.
 *
 * If a query is given, tries to extract the TMDB id and displays an [AddShowDialogFragment],
 * otherwise triggers a search.
 */
class ShowsDiscoverPagingFragment : BaseAddShowsFragment() {

    private var link: DiscoverShowsLink? = null

    val model: ShowsDiscoverPagingViewModel by viewModels(
        extrasProducer = {
            ShowsDiscoverPagingViewModel.creationExtras(
                defaultViewModelCreationExtras,
                link
            )
        },
        factoryProducer = { ShowsDiscoverPagingViewModel.Factory }
    )

    private lateinit var bindingActivity: ActivityMoviesSearchBinding
    private val searchEditText
        get() = bindingActivity.autoCompleteViewToolbar
    private var binding: FragmentShowsPopularBinding? = null

    private var yearPicker: YearPickerDialogFragment? = null
    private var languagePicker: LanguagePickerDialogFragment? = null

    private lateinit var adapter: SearchResultPagingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = DiscoverShowsLink.fromId(requireArguments().getInt(ARG_LINK))
    }

    private lateinit var searchHistory: SearchHistory
    private lateinit var searchHistoryAdapter: ArrayAdapter<String>
    private var showSearchView = false
    private val isSearchOnly: Boolean
        get() = link == null

    private fun requireDiscoverActivity(): ShowsDiscoverPagingActivity =
        requireActivity() as ShowsDiscoverPagingActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingActivity = requireDiscoverActivity().binding
        return FragmentShowsPopularBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        requireDiscoverActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        // When search view is optional, make up button restore original list and hide search view
        bindingActivity.sgToolbar.setNavigationOnClickListener {
            if (!isSearchOnly && showSearchView) {
                hideSearchViewAndRemoveQuery()
            } else {
                requireActivity().finish()
            }
        }

        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewShowsPopular)
        ThemeUtils.applyBottomMarginForNavigationBar(binding.textViewPoweredByShowsPopular)

        // show search view?
        showSearchView = if (isSearchOnly) {
            true
        } else savedInstanceState?.getBoolean(STATE_SEARCH_VISIBLE, false) ?: false

        setSearchViewVisible(showSearchView)
        if (!isSearchOnly) {
            // When search view is optional,
            // make back button restore original list and hide search view
            requireActivity().onBackPressedDispatcher.addCallback(
                onBackPressedWithSearchVisibleCallback
            )
        }

        // search history
        searchHistory = SearchHistory(requireContext(), "shows")
        searchHistoryAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            searchHistory.searchHistory
        )

        // search box
        searchEditText.apply {
            threshold = 1
            setOnClickListener(searchViewClickListener)
            onItemClickListener = searchViewItemClickListener
            setOnEditorActionListener(searchViewActionListener)
            bindingActivity.textInputLayoutToolbar.hint = getString(R.string.checkin_searchhint)
            // set in code as XML is overridden
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = EditorInfo.TYPE_CLASS_TEXT
            setAdapter(searchHistoryAdapter)
            // drop-down is auto-shown on config change, ensure it is hidden when recreating views
            dismissDropDown()
        }

        if (savedInstanceState == null) {
            if (showSearchView) {
                ViewTools.showSoftKeyboardOnSearchView(
                    requireActivity().window,
                    searchEditText
                )
            }
        }

        binding.swipeRefreshLayoutShowsPopular.apply {
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { adapter.refresh() }
        }

        binding.emptyViewShowsPopular.apply {
            setButtonText(R.string.action_try_again)
            setButtonClickListener { adapter.refresh() }
            // do not show error message when initially loading
            isGone = true
        }

        binding.recyclerViewShowsPopular.apply {
            setHasFixedSize(true)
            layoutManager =
                AutoGridLayoutManager(
                    context,
                    R.dimen.showgrid_columnWidth,
                    1,
                    1
                )
        }

        adapter = SearchResultPagingAdapter(
            requireContext(),
            itemClickListener,
            showWatchlistActions = TraktCredentials.get(requireContext()).hasCredentials()
        )
        binding.recyclerViewShowsPopular.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            model.items.collectLatest {
                adapter.submitData(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .collectLatest { loadStates ->
                    Timber.d("loadStates=$loadStates")
                    val refresh = loadStates.refresh
                    binding.swipeRefreshLayoutShowsPopular.isRefreshing =
                        refresh is LoadState.Loading
                    if (refresh is LoadState.Error) {
                        binding.emptyViewShowsPopular.apply {
                            setMessage(refresh.error.message)
                            setButtonGone(false)
                            isVisible = true
                        }
                        binding.recyclerViewShowsPopular.isGone = true
                    } else if (refresh is LoadState.NotLoading
                        && adapter.itemCount == 0
                        // Only no results if not searching, or if searching with a query
                        && (!isSearchOnly || model.queryString.value.isNotEmpty())) {
                        binding.emptyViewShowsPopular.apply {
                            setMessage(R.string.empty_no_results)
                            // No point in refreshing if there are no results
                            setButtonGone(true)
                            isVisible = true
                        }
                        binding.recyclerViewShowsPopular.isGone = true
                    } else {
                        binding.emptyViewShowsPopular.isGone = true
                        binding.recyclerViewShowsPopular.isVisible = true
                    }
                }
        }

        // Re-attach listeners to any showing dialogs
        yearPicker = findDialog<YearPickerDialogFragment>(parentFragmentManager, TAG_YEAR_PICKER)
            ?.also { it.onPickedListener = onYearPickedListener }
        languagePicker =
            findDialog<LanguagePickerDialogFragment>(parentFragmentManager, TAG_LANGUAGE_PICKER)
                ?.also { it.onPickedListener = onLanguagePickedListener }

        bindingActivity.chipMoviesSearchReleaseYear.setOnClickListener {
            YearPickerDialogFragment.create(model.firstReleaseYearRaw.value)
                .also { yearPicker = it }
                .apply { onPickedListener = onYearPickedListener }
                .safeShow(parentFragmentManager, TAG_YEAR_PICKER)
        }
        bindingActivity.chipMoviesSearchOriginalLanguage.setOnClickListener {
            LanguagePickerDialogFragment.createForShows(model.originalLanguage.value)
                .also { languagePicker = it }
                .apply { onPickedListener = onLanguagePickedListener }
                .safeShow(parentFragmentManager, TAG_LANGUAGE_PICKER)
        }
        bindingActivity.chipMoviesSearchWatchProviders.setOnClickListener {
            WatchProviderFilterDialogFragment.showForShows(parentFragmentManager)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            model.filters.collectLatest {
                // Hide unsupported filters
                val isSearching = it.queryString.isNotEmpty() || isSearchOnly
                bindingActivity.chipMoviesSearchOriginalLanguage.isGone = isSearching
                bindingActivity.chipMoviesSearchWatchProviders.isGone = isSearching

                bindingActivity.chipMoviesSearchReleaseYear.apply {
                    val hasYear = it.firstReleaseYear != null
                    isChipIconVisible = hasYear
                    text = if (hasYear) {
                        it.firstReleaseYear.toString()
                    } else {
                        getString(R.string.filter_year)
                    }
                }
                bindingActivity.chipMoviesSearchOriginalLanguage.apply {
                    val hasLanguage = it.originalLanguage != null
                    isChipIconVisible = hasLanguage
                    text = if (hasLanguage) {
                        LanguageTools.buildLanguageDisplayName(it.originalLanguage!!)
                    } else {
                        getString(R.string.filter_language)
                    }
                }
                bindingActivity.chipMoviesSearchWatchProviders.apply {
                    isChipIconVisible = it.watchProviderIds?.isNotEmpty() ?: false
                }
            }
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        if (savedInstanceState == null) {
            if (isSearchOnly) {
                handleSharedQuery()
            }
        }
    }

    private fun handleSharedQuery() {
        val query = requireArguments().getString(ARG_QUERY)
        if (query.isNullOrEmpty()) return
        // try to match TMDB URLs
        lifecycleScope.launch {
            val showTmdbId = TmdbIdExtractor(requireContext(), query).tryToExtract()
            if (showTmdbId > 0) {
                // found an id, display the add dialog
                AddShowDialogFragment.show(requireContext(), parentFragmentManager, showTmdbId)
            } else {
                // no id, do a search instead
                searchEditText.setText(query)
                search()
            }
        }
    }

    // Re-using movies discover menu as it is currently identical
    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.movies_search_menu, menu)

            val itemSearch = menu.findItem(R.id.menu_action_movies_search_display_search)
            itemSearch.isVisible = !showSearchView
            itemSearch.isEnabled = !showSearchView
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_movies_search_change_language -> {
                    L10nDialogFragment.show(
                        parentFragmentManager,
                        ShowsSettings.getShowsSearchLanguage(requireContext()),
                        L10nDialogFragment.TAG_DISCOVER
                    )
                    return true
                }

                R.id.menu_action_movies_search_display_search -> {
                    setSearchViewVisible(true)
                    ViewTools.showSoftKeyboardOnSearchView(
                        requireActivity().window,
                        searchEditText
                    )
                    requireActivity().invalidateOptionsMenu()
                    return true
                }

                R.id.menu_action_movies_search_clear_history -> {
                    searchHistory.clearHistory()
                    searchHistoryAdapter.clear()
                    // setting text to null seems to fix the dropdown from not clearing
                    searchEditText.setText(null)
                    return true
                }

                else -> false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SEARCH_VISIBLE, showSearchView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        yearPicker?.onPickedListener = null
        languagePicker?.onPickedListener = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: L10nDialogFragment.LanguageChangedEvent) {
        if (L10nDialogFragment.TAG_DISCOVER == event.tag) {
            ShowsSettings.saveShowsSearchLanguage(requireContext(), event.selectedLanguageCode)
            adapter.refresh()
        }
    }

    override fun setAllPendingNotAdded() {
        adapter.setAllPendingNotAdded()
    }

    override fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        adapter.setStateForTmdbId(showTmdbId, newState)
    }

    private fun setSearchViewVisible(visible: Boolean) {
        showSearchView = visible
        onBackPressedWithSearchVisibleCallback.isEnabled = visible
        bindingActivity.textInputLayoutToolbar.isGone = !visible
        requireDiscoverActivity().apply {
            // set title for screen readers
            setTitle(if (visible) R.string.search else link!!.titleRes)
            // hide title if search bar is shown
            supportActionBar?.setDisplayShowTitleEnabled(!visible)
        }
    }

    private fun hideSearchViewAndRemoveQuery() {
        setSearchViewVisible(false)
        requireActivity().invalidateOptionsMenu()
        model.removeQuery()
    }

    private val onBackPressedWithSearchVisibleCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hideSearchViewAndRemoveQuery()
        }
    }

    private fun search() {
        searchEditText.text.toString()
            .trim()
            .let { query ->
                if (query.isNotEmpty()) {
                    // Must hide keyboard before clearing focus
                    requireActivity().hideSoftKeyboard()
                    searchEditText.clearFocus() // also dismisses drop-down

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

    private val searchViewClickListener =
        View.OnClickListener { searchEditText.showDropDown() }

    private val searchViewItemClickListener =
        AdapterView.OnItemClickListener { _, _, _, _ -> search() }

    private val searchViewActionListener =
        TextView.OnEditorActionListener { _: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                search()
                return@OnEditorActionListener true
            }
            false
        }

    private val onYearPickedListener = object : YearPickerDialogFragment.OnPickedListener {
        override fun onPicked(year: Int?) {
            model.firstReleaseYearRaw.value = year
        }
    }

    private val onLanguagePickedListener = object : LanguagePickerDialogFragment.OnPickedListener {
        override fun onPicked(languageCode: String?) {
            model.originalLanguage.value = languageCode
        }
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShowsPopular

        private const val ARG_LINK = "link"
        private const val ARG_QUERY = "query"
        private const val STATE_SEARCH_VISIBLE = "searchVisible"
        private const val TAG_YEAR_PICKER = "yearPicker"
        private const val TAG_LANGUAGE_PICKER = "languagePicker"

        /**
         * If in search only mode ([link] is `null`) will use given [query] to extract a TMDB ID
         * and display an [AddShowDialogFragment] directly or otherwise trigger a search with it.
         */
        fun newInstance(link: DiscoverShowsLink?, query: String?) =
            ShowsDiscoverPagingFragment().apply {
                val linkId = link?.id ?: DiscoverShowsLink.NO_LINK_ID
                arguments = bundleOf(
                    ARG_LINK to linkId,
                    ARG_QUERY to query
                )
            }
    }

}