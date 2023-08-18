package com.battlelancer.seriesguide.lists

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.databinding.ActivityListsBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ThemeUtils.setDefaultStyle
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.safeShow
import org.greenrobot.eventbus.EventBus

/**
 * Hosts a view pager to display and manage user created lists.
 */
open class ListsActivityImpl : BaseTopActivity() {

    private lateinit var binding: ActivityListsBinding
    private lateinit var listsAdapter: ListsPagerAdapter
    private val viewModel by viewModels<ListsActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_lists)

        setupViews()
        setupSyncProgressBar(R.id.sgProgressBar)

        viewModel.listsLiveData.observe(this) { items ->
            listsAdapter.updateItems(items)
            // update tabs
            binding.sgAppBarLayout.sgTabLayout.setViewPager2(binding.viewPagerLists) { position ->
                items[position].name
            }
            if (!viewModel.hasRestoredLastListsTabPosition) {
                viewModel.hasRestoredLastListsTabPosition = true
                binding.viewPagerLists.setCurrentItem(
                    ListsSettings.getLastListsTabPosition(this),
                    false
                )
            }
        }
    }

    private fun setupViews() {
        listsAdapter = ListsPagerAdapter(this)

        binding.viewPagerLists.adapter = listsAdapter

        val tabLayout = binding.sgAppBarLayout.sgTabLayout
        tabLayout.setDefaultStyle()
        tabLayout.setOnTabClickListener { position: Int ->
            if (binding.viewPagerLists.currentItem == position) {
                showListManageDialog(position)
            }
        }

        // This is a workaround to avoid the app bar color flickering when scrolling,
        // however, when switching to another tab it will still briefly flicker when starting
        // to scroll up. Getting and setting the RecyclerView directly on page change makes no
        // difference.
        binding.viewPagerLists.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.sgAppBarLayout.sgAppBarLayout.liftOnScrollTargetViewId =
                    SgListFragment.liftOnScrollTargetViewId
            }
        })
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(ListsSettings.KEY_LAST_ACTIVE_LISTS_TAB, binding.viewPagerLists.currentItem)
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
            ListsDistillationSettings.ListsSortOrder.TITLE_ALPHABETICAL_ID -> {
                ViewTools.setMenuItemActiveString(sortTitleItem)
            }
            ListsDistillationSettings.ListsSortOrder.LATEST_EPISODE_ID -> {
                ViewTools.setMenuItemActiveString(sortLatestItem)
            }
            ListsDistillationSettings.ListsSortOrder.OLDEST_EPISODE_ID -> {
                ViewTools.setMenuItemActiveString(sortOldestItem)
            }
            ListsDistillationSettings.ListsSortOrder.LAST_WATCHED_ID -> {
                ViewTools.setMenuItemActiveString(lastWatchedItem)
            }
            ListsDistillationSettings.ListsSortOrder.LEAST_REMAINING_EPISODES_ID -> {
                ViewTools.setMenuItemActiveString(remainingItem)
            }
        }
        menu.findItem(R.id.menu_action_lists_sort_ignore_articles).isChecked =
            DisplaySettings.isSortOrderIgnoringArticles(this)

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
            val selectedListIndex = binding.viewPagerLists.currentItem
            showListManageDialog(selectedListIndex)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_title) {
            changeSortOrder(ListsDistillationSettings.ListsSortOrder.TITLE_ALPHABETICAL_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_latest_episode) {
            changeSortOrder(ListsDistillationSettings.ListsSortOrder.LATEST_EPISODE_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_oldest_episode) {
            changeSortOrder(ListsDistillationSettings.ListsSortOrder.OLDEST_EPISODE_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_last_watched) {
            changeSortOrder(ListsDistillationSettings.ListsSortOrder.LAST_WATCHED_ID)
            return true
        }
        if (itemId == R.id.menu_action_lists_sort_remaining) {
            changeSortOrder(ListsDistillationSettings.ListsSortOrder.LEAST_REMAINING_EPISODES_ID)
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
        val listId = listsAdapter.getItemListId(selectedListIndex)
        if (!listId.isNullOrEmpty()) {
            ListManageDialogFragment.show(listId, supportFragmentManager)
        }
    }

    override fun onSelectedCurrentNavItem() {
        viewModel.scrollTabToTop(binding.viewPagerLists.currentItem)
    }

    private fun changeSortOrder(sortOrderId: Int) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(ListsDistillationSettings.KEY_SORT_ORDER, sortOrderId)
            .apply()

        // refresh icon state
        invalidateOptionsMenu()

        // post event, so all active list fragments can react
        EventBus.getDefault().post(ListsDistillationSettings.ListsSortOrderChangedEvent())
    }

    private fun toggleSortIgnoreArticles() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(
                DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                !DisplaySettings.isSortOrderIgnoringArticles(this)
            )
            .apply()

        // refresh icon state
        invalidateOptionsMenu()

        // refresh all list widgets
        ListWidgetProvider.notifyDataChanged(this)

        // post event, so all active list fragments can react
        EventBus.getDefault().post(ListsDistillationSettings.ListsSortOrderChangedEvent())
    }

    override val snackbarParentView: View
        get() = binding.coordinatorLayoutLists

    companion object {
        const val LISTS_REORDER_LOADER_ID = 2
    }

}