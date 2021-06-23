package com.battlelancer.seriesguide.ui

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.getLastListsTabPosition
import com.battlelancer.seriesguide.settings.DisplaySettings.isSortOrderIgnoringArticles
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.lists.AddListDialogFragment
import com.battlelancer.seriesguide.ui.lists.ListManageDialogFragment
import com.battlelancer.seriesguide.ui.lists.ListsActivityViewModel
import com.battlelancer.seriesguide.ui.lists.ListsDistillationSettings
import com.battlelancer.seriesguide.ui.lists.ListsDistillationSettings.ListsSortOrder
import com.battlelancer.seriesguide.ui.lists.ListsDistillationSettings.ListsSortOrderChangedEvent
import com.battlelancer.seriesguide.ui.lists.ListsPagerAdapter
import com.battlelancer.seriesguide.ui.lists.ListsReorderDialogFragment
import com.battlelancer.seriesguide.ui.lists.ListsTools
import com.battlelancer.seriesguide.util.ThemeUtils.setDefaultStyle
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.safeShow
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Hosts a view pager to display and manage user created lists.
 */
class ListsActivity : BaseTopActivity() {

    @BindView(R.id.viewPagerTabs)
    var viewPager: ViewPager? = null

    @BindView(R.id.tabLayoutTabs)
    var tabs: SlidingTabLayout? = null

    private lateinit var listsAdapter: ListsPagerAdapter
    private lateinit var viewModel: ListsActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs_drawer)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_lists)

        viewModel = ViewModelProvider(this).get(ListsActivityViewModel::class.java)

        setupViews(savedInstanceState)
        setupSyncProgressBar(R.id.progressBarTabs)

        LoaderManager.getInstance(this)
            .initLoader(LISTS_LOADER_ID, null, listsLoaderCallbacks)
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        ButterKnife.bind(this)

        listsAdapter = ListsPagerAdapter(supportFragmentManager)

        viewPager!!.adapter = listsAdapter

        tabs!!.setDefaultStyle()
        tabs!!.setOnTabClickListener { position: Int ->
            if (viewPager!!.currentItem == position) {
                showListManageDialog(position)
            }
        }
        tabs!!.setViewPager(viewPager)

        if (savedInstanceState == null) {
            viewPager!!.setCurrentItem(getLastListsTabPosition(this), false)
        }
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(DisplaySettings.KEY_LAST_ACTIVE_LISTS_TAB, viewPager!!.currentItem)
            .apply()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.lists_menu, menu)

        // set current sort order and check box states
        val sortOrderId = ListsDistillationSettings.getSortOrderId(this)
        val sortTitleItem = menu.findItem(R.id.menu_action_lists_sort_title)
        sortTitleItem.setTitle(R.string.action_shows_sort_title)
        val sortLatestItem = menu.findItem(R.id.menu_action_lists_sort_latest_episode)
        sortLatestItem.setTitle(R.string.action_shows_sort_latest_episode)
        val sortOldestItem = menu.findItem(R.id.menu_action_lists_sort_oldest_episode)
        sortOldestItem.setTitle(R.string.action_shows_sort_oldest_episode)
        val lastWatchedItem = menu.findItem(R.id.menu_action_lists_sort_last_watched)
        lastWatchedItem.setTitle(R.string.action_shows_sort_last_watched)
        val remainingItem = menu.findItem(R.id.menu_action_lists_sort_remaining)
        remainingItem.setTitle(R.string.action_shows_sort_remaining)
        when (sortOrderId) {
            ListsSortOrder.TITLE_ALPHABETICAL_ID -> {
                ViewTools.setMenuItemActiveString(sortTitleItem)
            }
            ListsSortOrder.LATEST_EPISODE_ID -> {
                ViewTools.setMenuItemActiveString(sortLatestItem)
            }
            ListsSortOrder.OLDEST_EPISODE_ID -> {
                ViewTools.setMenuItemActiveString(sortOldestItem)
            }
            ListsSortOrder.LAST_WATCHED_ID -> {
                ViewTools.setMenuItemActiveString(lastWatchedItem)
            }
            ListsSortOrder.LEAST_REMAINING_EPISODES_ID -> {
                ViewTools.setMenuItemActiveString(remainingItem)
            }
        }
        menu.findItem(R.id.menu_action_lists_sort_ignore_articles).isChecked =
            isSortOrderIgnoringArticles(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_action_lists_add) {
            AddListDialogFragment.show(supportFragmentManager)
            return true
        }
        if (itemId == R.id.menu_action_lists_search) {
            startActivity(Intent(this, SearchActivity::class.java))
            return true
        }
        if (itemId == R.id.menu_action_lists_edit) {
            val selectedListIndex = viewPager!!.currentItem
            showListManageDialog(selectedListIndex)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_title) {
            changeSortOrder(ListsSortOrder.TITLE_ALPHABETICAL_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_latest_episode) {
            changeSortOrder(ListsSortOrder.LATEST_EPISODE_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_oldest_episode) {
            changeSortOrder(ListsSortOrder.OLDEST_EPISODE_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_last_watched) {
            changeSortOrder(ListsSortOrder.LAST_WATCHED_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_remaining) {
            changeSortOrder(ListsSortOrder.LEAST_REMAINING_EPISODES_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_ignore_articles) {
            toggleSortIgnoreArticles()
            return true
        }
        if (itemId == R.id.menu_action_lists_reorder) {
            ListsReorderDialogFragment().safeShow(supportFragmentManager, "listsReorderDialog")
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showListManageDialog(selectedListIndex: Int) {
        val listId = listsAdapter.getListId(selectedListIndex)
        if (!TextUtils.isEmpty(listId)) {
            ListManageDialogFragment.show(listId, supportFragmentManager)
        }
    }

    override fun onSelectedCurrentNavItem() {
        viewModel.scrollTabToTop(viewPager!!.currentItem)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: ListsChangedEvent?) {
        LoaderManager.getInstance(this)
            .restartLoader(LISTS_LOADER_ID, null, listsLoaderCallbacks)
    }

    private fun changeSortOrder(sortOrderId: Int) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(ListsDistillationSettings.KEY_SORT_ORDER, sortOrderId)
            .apply()

        // refresh icon state
        supportInvalidateOptionsMenu()

        // post event, so all active list fragments can react
        EventBus.getDefault().post(ListsSortOrderChangedEvent())
    }

    private fun toggleSortIgnoreArticles() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(
                DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                !isSortOrderIgnoringArticles(this)
            )
            .apply()

        // refresh icon state
        supportInvalidateOptionsMenu()

        // refresh all list widgets
        ListWidgetProvider.notifyDataChanged(this)

        // post event, so all active list fragments can react
        EventBus.getDefault().post(ListsSortOrderChangedEvent())
    }

    override fun getSnackbarParentView(): View {
        return findViewById(R.id.rootLayoutTabs)
    }

    private val listsLoaderCallbacks: LoaderManager.LoaderCallbacks<Cursor> =
        object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                // load lists, order by order number, then name
                return CursorLoader(
                    this@ListsActivity,
                    SeriesGuideContract.Lists.CONTENT_URI,
                    ListsPagerAdapter.ListsQuery.PROJECTION, null, null,
                    SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME
                )
            }

            override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
                // precreate first list
                if (data != null && data.count == 0) {
                    val listName = getString(R.string.first_list)
                    ListsTools.addList(this@ListsActivity, listName)
                }
                listsAdapter.swapCursor(data)
                // update tabs
                tabs!!.setViewPager(viewPager)
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {
                listsAdapter.swapCursor(null)
            }
        }

    companion object {
        const val LISTS_LOADER_ID = 1
        const val LISTS_REORDER_LOADER_ID = 2
    }

    class ListsChangedEvent

}