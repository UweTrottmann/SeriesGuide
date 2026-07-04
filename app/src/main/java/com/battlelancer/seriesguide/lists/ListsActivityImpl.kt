// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2012 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityListsBinding
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.menus.ManualSyncMenu
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ThemeUtils.setDefaultStyle
import com.battlelancer.seriesguide.util.safeShow

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
        addMenuProvider(optionsMenuProvider, this)

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

    private val optionsMenuProvider by lazy {
        object : ManualSyncMenu(this@ListsActivityImpl, R.menu.lists_menu) {

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_action_lists_add -> {
                        AddListDialogFragment.show(supportFragmentManager)
                        return true
                    }
                    R.id.menu_action_lists_search -> {
                        startActivity(Intent(this@ListsActivityImpl, SearchActivity::class.java))
                        return true
                    }
                    R.id.menu_action_lists_edit -> {
                        val selectedListIndex = binding.viewPagerLists.currentItem
                        showListManageDialog(selectedListIndex)
                        return true
                    }
                    R.id.menu_action_lists_sort -> {
                        ListsSortDialogFragment.show(supportFragmentManager)
                        return true
                    }
                    R.id.menu_action_lists_reorder -> {
                        ListsReorderDialogFragment().safeShow(
                            supportFragmentManager,
                            "listsReorderDialog"
                        )
                        return true
                    }
                    else -> return super.onMenuItemSelected(menuItem)
                }
            }
        }
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


    override val snackbarParentView: View
        get() = binding.coordinatorLayoutLists

    companion object {
        const val LISTS_REORDER_LOADER_ID = 2
    }

}