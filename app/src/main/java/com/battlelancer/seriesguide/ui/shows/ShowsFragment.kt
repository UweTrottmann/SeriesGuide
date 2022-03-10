package com.battlelancer.seriesguide.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStateAtLeast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider.Companion.notifyDataChanged
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.ui.OverviewActivity.Companion.intentShow
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.preferences.MoreOptionsActivity
import com.battlelancer.seriesguide.ui.shows.FilterShowsView.ShowFilter
import com.battlelancer.seriesguide.ui.shows.FilterShowsView.ShowFilter.Companion.allDisabled
import com.battlelancer.seriesguide.ui.shows.FirstRunView.ButtonEvent
import com.battlelancer.seriesguide.ui.shows.FirstRunView.ButtonType
import com.battlelancer.seriesguide.ui.shows.ShowsAdapter.ShowItem
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationFragment.Companion.show
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings.getSortQuery2
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings.saveFilter
import com.battlelancer.seriesguide.ui.shows.SortShowsView.ShowSortOrder
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.widgets.SgFastScroller
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import kotlin.random.Random

/**
 * Displays the list of shows in a users local library with sorting and filtering abilities. The
 * main view of the app.
 */
class ShowsFragment : Fragment() {

    private lateinit var showSortOrder: ShowSortOrder
    private lateinit var showFilter: ShowFilter

    private lateinit var adapter: ShowsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: Button
    private lateinit var emptyViewFilter: Button

    private var scheduledUpdateJob: Job? = null
    private val activityModel by viewModels<ShowsActivityViewModel>({ requireActivity() })
    private val model by viewModels<ShowsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_shows, container, false)
        recyclerView = v.findViewById(R.id.recyclerViewShows)
        SgFastScroller(requireContext(), recyclerView)
        emptyView = v.findViewById(R.id.emptyViewShows)
        ViewTools.setVectorDrawableTop(emptyView, R.drawable.ic_add_white_24dp)
        emptyView.setOnClickListener { startActivityAddShows() }
        emptyViewFilter = v.findViewById(R.id.emptyViewShowsFilter)
        ViewTools.setVectorDrawableTop(emptyViewFilter, R.drawable.ic_filter_white_24dp)
        emptyViewFilter.setOnClickListener {
            ShowsDistillationSettings.filterLiveData.setValue(allDisabled())
            saveFilter(
                requireContext(),
                null,
                null,
                null,
                null,
                null
            )
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.setHasFixedSize(true)
        val layoutManager = AutoGridLayoutManager(
            context,
            R.dimen.showgrid_columnWidth, 1, 1
        )
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == ShowsAdapter.VIEW_TYPE_FIRST_RUN) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
        recyclerView.layoutManager = layoutManager

        activityModel.scrollTabToTopLiveData.observe(viewLifecycleOwner) { tabPosition: Int? ->
            if (tabPosition != null
                && tabPosition == ShowsActivity.INDEX_TAB_SHOWS) {
                recyclerView.smoothScrollToPosition(0)
            }
        }

        // get settings
        showFilter = ShowFilter.fromSettings(requireContext())
        showSortOrder = ShowSortOrder.fromSettings(requireContext())

        // prepare view adapter
        adapter = ShowsAdapter(requireContext(), onItemClickListener)
        if (!FirstRunView.hasSeenFirstRunFragment(requireContext())) {
            adapter.displayFirstRunHeader = true
        }
        recyclerView.adapter = adapter

        model.showItemsLiveData.observe(viewLifecycleOwner) { showItems: MutableList<ShowItem>? ->
            adapter.submitList(showItems)
            // note: header is added later, but if it is shown should not treat as empty
            val isEmpty = (!adapter.displayFirstRunHeader
                    && (showItems == null || showItems.isEmpty()))
            updateEmptyView(isEmpty)

            // Once latest data loaded, schedule next query refresh.
            // Some changes to the displayed data are not based on actual (detectable) changes to
            // the underlying data, but because time has passed (e.g. relative time displays,
            // release time has passed).
            scheduledUpdateJob?.cancel()
            scheduledUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                // Use STARTED as in multi-window this might be visible, but not RESUMED.
                // On the downside this runs even if the tab is not visible (tied to RESUMED).
                viewLifecycleOwner.lifecycle.whenStateAtLeast(Lifecycle.State.STARTED) {
                    Timber.d("Scheduled query update")
                    delay(DateUtils.MINUTE_IN_MILLIS + Random.nextLong(DateUtils.SECOND_IN_MILLIS))
                    updateShowsQuery()
                }
            }
        }

        // Run initial query and refresh query immediately each time when STARTED.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateShowsQuery()
            }
        }

        // watch for sort order changes
        ShowsDistillationSettings.sortOrderLiveData
            .observe(viewLifecycleOwner) { showSortOrder: ShowSortOrder ->
                this.showSortOrder = showSortOrder
                // re-run query
                updateShowsQuery()
            }

        // watch for filter changes
        ShowsDistillationSettings.filterLiveData
            .observe(viewLifecycleOwner) { showFilter: ShowFilter ->
                this.showFilter = showFilter
                // re-run query
                updateShowsQuery()
                // refresh filter menu icon state
                requireActivity().invalidateOptionsMenu()
            }

        // hide floating action button when scrolling shows
        val buttonAddShow: FloatingActionButton =
            requireActivity().findViewById(R.id.buttonShowsAdd)
        recyclerView.addOnScrollListener(FabRecyclerViewScrollDetector(buttonAddShow))

        // listen for some settings changes
        PreferenceManager
            .getDefaultSharedPreferences(requireActivity())
            .registerOnSharedPreferenceChangeListener(onPreferenceChangeListener)

        setHasOptionsMenu(true)
    }

    private fun updateShowsQuery() {
        Timber.d("Running query update.")
        model.updateQuery(
            showFilter, getSortQuery2(
                showSortOrder.sortOrderId, showSortOrder.isSortFavoritesFirst,
                showSortOrder.isSortIgnoreArticles
            )
        )
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty) {
            if (showFilter.isAnyFilterEnabled()) {
                emptyViewFilter.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            } else {
                emptyView.visibility = View.VISIBLE
                emptyViewFilter.visibility = View.GONE
            }
        } else {
            emptyView.visibility = View.GONE
            emptyViewFilter.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        // Query update already repeated when STARTED, prevent scheduled update from also running.
        scheduledUpdateJob?.cancel()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.shows_menu, menu)

        // set filter icon state
        menu.findItem(R.id.menu_action_shows_filter).setIcon(
            if (showFilter.isAnyFilterEnabled()) {
                R.drawable.ic_filter_selected_white_24dp
            } else {
                R.drawable.ic_filter_white_24dp
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_shows_add -> {
                startActivityAddShows()
                true
            }
            R.id.menu_action_shows_filter -> {
                show(parentFragmentManager)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventFirstRunButton(event: ButtonEvent) {
        when (event.type) {
            ButtonType.ADD_SHOW -> {
                startActivity(
                    Intent(activity, SearchActivity::class.java).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH
                    )
                )
            }
            ButtonType.SIGN_IN -> {
                startActivity(Intent(activity, MoreOptionsActivity::class.java))
                // Launching a top activity, adjust animation to match.
                requireActivity().overridePendingTransition(
                    R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg
                )
            }
            ButtonType.RESTORE_BACKUP -> {
                startActivity(Intent(activity, DataLiberationActivity::class.java))
            }
            ButtonType.DISMISS -> {
                adapter.displayFirstRunHeader = false
                updateShowsQuery()
            }
        }
    }

    private fun startActivityAddShows() {
        startActivity(
            Intent(activity, SearchActivity::class.java).putExtra(
                SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH
            )
        )
    }

    private val onItemClickListener: ShowsAdapter.OnItemClickListener =
        object : ShowsAdapter.OnItemClickListener {
            override fun onItemClick(anchor: View, showRowId: Long) {
                // display overview for this show
                val intent = intentShow(requireContext(), showRowId)
                ActivityCompat.startActivity(
                    requireContext(), intent,
                    ActivityOptionsCompat.makeScaleUpAnimation(
                        anchor, 0, 0, anchor.width,
                        anchor.height
                    ).toBundle()
                )
            }

            override fun onItemMenuClick(anchor: View, show: ShowItem) {
                val popupMenu = PopupMenu(anchor.context, anchor)
                popupMenu.inflate(R.menu.shows_popup_menu)

                // show/hide some menu items depending on show properties
                val menu = popupMenu.menu
                menu.findItem(R.id.menu_action_shows_favorites_add).isVisible = !show.isFavorite
                menu.findItem(R.id.menu_action_shows_favorites_remove).isVisible = show.isFavorite
                menu.findItem(R.id.menu_action_shows_hide).isVisible = !show.isHidden
                menu.findItem(R.id.menu_action_shows_unhide).isVisible = show.isHidden
                popupMenu.setOnMenuItemClickListener(
                    ShowMenuItemClickListener(
                        context, parentFragmentManager,
                        show.rowId, show.nextEpisodeId
                    )
                )
                popupMenu.show()
            }

            override fun onItemSetWatchedClick(show: ShowItem) {
                EpisodeTools.episodeWatchedIfNotZero(context, show.nextEpisodeId)
            }
        }

    private val onPreferenceChangeListener =
        OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String ->
            if (key == AdvancedSettings.KEY_UPCOMING_LIMIT) {
                updateShowsQuery()
                // refresh all list widgets
                notifyDataChanged(requireContext())
            }
        }
}