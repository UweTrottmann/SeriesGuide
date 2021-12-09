package com.battlelancer.seriesguide.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.util.ThemeUtils.setDefaultStyle
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout

/**
 * Helper class for easy setup of a {@link SlidingTabLayout} with a mutable set of tabs.
 * Requires that tabs each have a unique title string resource
 * as it is used to uniquely identify a tab.
 */
open class TabStripAdapter(
    private val fragmentActivity: FragmentActivity,
    private val viewPager: ViewPager2,
    private val tabLayout: SlidingTabLayout
) : FragmentStateAdapter(fragmentActivity) {

    private val tabs = ArrayList<TabInfo>()

    private val tabTitleSupplier = SlidingTabLayout.TabTitleSupplier { position ->
        val tabInfo = tabs.getOrNull(position)
        return@TabTitleSupplier if (tabInfo != null) {
            tabLayout.context.getString(tabInfo.titleRes)
        } else ""
    }

    init {
        // Preload next/previous page for smoother swiping.
        viewPager.offscreenPageLimit = 1
        viewPager.adapter = this

        tabLayout.setDefaultStyle()
        tabLayout.setViewPager2(viewPager, tabTitleSupplier)
    }

    /**
     * Adds a new tab at the end.
     *
     * Make sure to call [notifyTabsChanged] after you have added them all.
     */
    fun addTab(@StringRes titleRes: Int, fragmentClass: Class<*>, args: Bundle?) {
        tabs.add(tabs.size, TabInfo(fragmentClass, args, titleRes))
    }

    /**
     * Adds a new tab at the given position.
     *
     * Make sure to call [notifyTabsChanged] after you have added them all.
     */
    fun addTab(@StringRes titleRes: Int, fragmentClass: Class<*>, args: Bundle?, position: Int) {
        tabs.add(position, TabInfo(fragmentClass, args, titleRes))
    }

    /**
     * Notifies the adapter and tab strip that the tabs have changed.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun notifyTabsChanged() {
        notifyDataSetChanged()
        // update tabs
        tabLayout.setViewPager2(viewPager, tabTitleSupplier)
    }

    // Using titleRes instead of introducing an ID property as it is unique for tabs of this app.
    override fun getItemId(position: Int): Long {
        return if (position < tabs.size) {
            tabs[position].titleRes.toLong()
        } else {
            RecyclerView.NO_ID
        }
    }

    override fun containsItem(itemId: Long): Boolean =
        tabs.find { it.titleRes.toLong() == itemId } != null

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val tab = tabs[position]
        return fragmentActivity.supportFragmentManager.fragmentFactory.instantiate(
            fragmentActivity.classLoader,
            tab.fragmentClass.name
        ).apply { tab.args?.let { arguments = it } }
    }

    data class TabInfo(
        val fragmentClass: Class<*>,
        val args: Bundle?,
        val titleRes: Int
    )
}