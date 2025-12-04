// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann
// Copyright 2013 The Android Open Source Project

package com.uwetrottmann.seriesguide.widgets

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 *
 * To use the component, simply add it to your view hierarchy. Then in your activity or fragment call
 * [setViewPager2] providing it the ViewPager this layout is being used for.
 *
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via [setSelectedIndicatorColors]. The alternative is via the [TabColorizer] interface which provides you
 * complete control over which color is used for any individual position.
 *
 * The views used as tabs can be customized by calling [setCustomTabView],
 * providing the layout ID of your custom layout.
 */
class SlidingTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {

    fun interface OnTabClickListener {
        fun onTabClick(position: Int)
    }

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * [setCustomTabColorizer].
     */
    interface TabColorizer {
        /**
         * @return The color of the indicator used when [position] is selected.
         */
        fun getIndicatorColor(position: Int): Int
    }

    fun interface TabTitleSupplier {
        fun getTabTitle(position: Int): String
    }

    private var titleOffset: Int

    private var tabViewLayoutId = 0
    private var tabViewTextViewId = 0

    private var viewPager: ViewPager? = null
    private var viewPagerPageChangeListener: ViewPager2.OnPageChangeCallback? = null
    private var viewPager2: ViewPager2? = null

    private var onTabClickListener: OnTabClickListener? = null

    private val tabStrip: SlidingTabStrip

    init {
        // Disable the Scroll Bar
        isHorizontalScrollBarEnabled = false
        // Make sure that the Tab Strips fills this View
        isFillViewport = true

        titleOffset = (TITLE_OFFSET_DIPS * resources.displayMetrics.density).toInt()

        tabStrip = SlidingTabStrip(context)
        addView(tabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Set the custom [TabColorizer] to be used.
     *
     * If you only require simple customisation then you can use
     * [setSelectedIndicatorColors] to achieve similar effects.
     */
    fun setCustomTabColorizer(tabColorizer: TabColorizer?) {
        tabStrip.setCustomTabColorizer(tabColorizer)
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same
     * color.
     */
    fun setSelectedIndicatorColors(vararg colors: Int) {
        tabStrip.setSelectedIndicatorColors(*colors)
    }

    /**
     * Whether to draw an underline below all tabs.
     */
    fun setDisplayUnderline(displayUnderline: Boolean) {
        tabStrip.setDisplayUnderline(displayUnderline)
    }

    /**
     * Sets the color to be used as an underline below all tabs.
     */
    fun setUnderlineColor(color: Int) {
        tabStrip.setUnderlineColor(color)
    }

    /**
     * Set a page change listener to observe page changes.
     *
     * @see ViewPager2.registerOnPageChangeCallback
     */
    fun setOnPageChangeListener(listener: ViewPager2.OnPageChangeCallback?) {
        viewPagerPageChangeListener = listener
    }

    /**
     * Set the [OnTabClickListener].
     */
    fun setOnTabClickListener(listener: OnTabClickListener?) {
        onTabClickListener = listener
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId id of the [android.widget.TextView] in the inflated view
     */
    fun setCustomTabView(layoutResId: Int, textViewId: Int) {
        tabViewLayoutId = layoutResId
        tabViewTextViewId = textViewId
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    fun setViewPager(viewPager: ViewPager?) {
        tabStrip.removeAllViews()

        this.viewPager = viewPager
        if (viewPager != null) {
            @Suppress("DEPRECATION")
            viewPager.setOnPageChangeListener(InternalViewPagerListener())
            populateTabStrip()
        }
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    fun setViewPager2(viewPager: ViewPager2?, tabTitleSupplier: TabTitleSupplier) {
        tabStrip.removeAllViews()

        this.viewPager2 = viewPager
        if (viewPager != null) {
            viewPager.registerOnPageChangeCallback(InternalViewPagerListener())
            populateTabStrip2(tabTitleSupplier)
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set
     * via [setCustomTabView].
     */
    private fun createDefaultTabView(context: Context): TextView {
        val textView = TextView(context)
        textView.gravity = Gravity.CENTER
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP.toFloat())
        textView.typeface = Typeface.DEFAULT_BOLD

        // If we're running on Honeycomb or newer, then we can use the Theme's
        // selectableItemBackground to ensure that the View has a pressed state
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        textView.setBackgroundResource(outValue.resourceId)

        // If we're running on ICS or newer, enable all-caps to match the Action Bar tab style
        textView.isAllCaps = true

        val padding = (TAB_VIEW_PADDING_DIPS * resources.displayMetrics.density).toInt()
        textView.setPadding(padding, padding, padding, padding)

        return textView
    }

    private fun populateTabStrip() {
        val adapter = viewPager?.adapter ?: return
        populateTabStrip(adapter.count) { position ->
            val titleOrNull = adapter.getPageTitle(position)
            titleOrNull?.toString() ?: ""
        }
    }

    private fun populateTabStrip2(tabTitleSupplier: TabTitleSupplier) {
        val adapter = viewPager2?.adapter ?: return
        populateTabStrip(adapter.itemCount, tabTitleSupplier)
    }

    private fun populateTabStrip(itemCount: Int, tabTitleSupplier: TabTitleSupplier) {
        val tabClickListener = TabClickListener()

        for (i in 0 until itemCount) {
            var tabView: View? = null
            var tabTitleView: TextView? = null

            if (tabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(context).inflate(tabViewLayoutId, tabStrip, false)
                tabTitleView = tabView.findViewById(tabViewTextViewId)
            }

            if (tabView == null) {
                tabView = createDefaultTabView(context)
            }

            if (tabTitleView == null && tabView is TextView) {
                tabTitleView = tabView
            }

            if (tabTitleView == null) {
                throw IllegalArgumentException("tabTitleView == null")
            }
            tabTitleView.text = tabTitleSupplier.getTabTitle(i)
            tabView.setOnClickListener(tabClickListener)

            tabStrip.addView(tabView)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewPager?.let {
            scrollToTab(it.currentItem, 0)
        }
        viewPager2?.let {
            scrollToTab(it.currentItem, 0)
        }
    }

    private fun scrollToTab(tabIndex: Int, positionOffset: Int) {
        val tabStripChildCount = tabStrip.childCount
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return
        }

        val selectedChild = tabStrip.getChildAt(tabIndex)
        if (selectedChild != null) {
            var targetScrollX = selectedChild.left + positionOffset

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= titleOffset
            }

            scrollTo(targetScrollX, 0)
        }

        // Update selected tab view once scrolling has stopped.
        if (positionOffset == 0) {
            for (i in 0 until tabStripChildCount) {
                val child = tabStrip.getChildAt(i)
                child.isSelected = i == tabIndex
                child.isActivated = i == tabIndex
            }
        }
    }

    private inner class InternalViewPagerListener : ViewPager2.OnPageChangeCallback(),
        ViewPager.OnPageChangeListener {
        private var scrollState = 0

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val tabStripChildCount = tabStrip.childCount
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return
            }

            tabStrip.onViewPagerPageChanged(position, positionOffset)

            val selectedTitle = tabStrip.getChildAt(position)
            val extraOffset = if (selectedTitle != null) {
                (positionOffset * selectedTitle.width).toInt()
            } else {
                0
            }
            scrollToTab(position, extraOffset)

            viewPagerPageChangeListener?.onPageScrolled(
                position,
                positionOffset,
                positionOffsetPixels
            )
        }

        override fun onPageScrollStateChanged(state: Int) {
            scrollState = state

            viewPagerPageChangeListener?.onPageScrollStateChanged(state)
        }

        override fun onPageSelected(position: Int) {
            if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
                tabStrip.onViewPagerPageChanged(position, 0f)
                scrollToTab(position, 0)
            }

            viewPagerPageChangeListener?.onPageSelected(position)
        }
    }

    private inner class TabClickListener : OnClickListener {
        override fun onClick(v: View) {
            for (i in 0 until tabStrip.childCount) {
                if (v == tabStrip.getChildAt(i)) {
                    onTabClickListener?.onTabClick(i)
                    viewPager?.currentItem = i
                    viewPager2?.currentItem = i
                    return
                }
            }
        }
    }

    companion object {
        private const val TITLE_OFFSET_DIPS = 24
        private const val TAB_VIEW_PADDING_DIPS = 16
        private const val TAB_VIEW_TEXT_SIZE_SP = 12
    }
}
