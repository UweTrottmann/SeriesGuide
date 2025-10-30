// Copyright (C) 2013 The Android Open Source Project
// Copyright 2014, 2016-2019, 2021, 2022 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.uwetrottmann.seriesguide.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 * <p/>
 * To use the component, simply add it to your view hierarchy. Then in your activity or fragment call {@link
 * #setViewPager2(ViewPager2, TabTitleSupplier)} providing it the ViewPager this layout is being
 * used for.
 * <p/>
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via {@link #setSelectedIndicatorColors(int...)}. The alternative is via the {@link
 * SlidingTabLayout.TabColorizer} interface which provides you
 * complete control over which color is used for any individual position.
 * <p/>
 * The views used as tabs can be customized by calling {@link #setCustomTabView(int, int)},
 * providing the layout ID of your custom layout.
 */
public class SlidingTabLayout extends HorizontalScrollView {

    public interface OnTabClickListener {
        void onTabClick(int position);
    }

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with {@link
     * #setCustomTabColorizer(SlidingTabLayout.TabColorizer)}.
     */
    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position} is selected.
         */
        int getIndicatorColor(int position);
    }

    public interface TabTitleSupplier {
        String getTabTitle(int position);
    }

    private static final int TITLE_OFFSET_DIPS = 24;
    private static final int TAB_VIEW_PADDING_DIPS = 16;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;

    private int titleOffset;

    private int tabViewLayoutId;
    private int tabViewTextViewId;

    private ViewPager viewPager;
    private ViewPager2.OnPageChangeCallback viewPagerPageChangeListener;
    private ViewPager2 viewPager2;

    private OnTabClickListener onTabClickListener;

    private final SlidingTabStrip tabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        titleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);

        tabStrip = new SlidingTabStrip(context);
        addView(tabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * Set the custom {@link SlidingTabLayout.TabColorizer} to
     * be used.
     *
     * If you only require simple custmisation then you can use
     * {@link #setSelectedIndicatorColors(int...)} to achieve
     * similar effects.
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        tabStrip.setCustomTabColorizer(tabColorizer);
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same
     * color.
     */
    public void setSelectedIndicatorColors(int... colors) {
        tabStrip.setSelectedIndicatorColors(colors);
    }

    /**
     * Whether to draw an underline below all tabs.
     */
    public void setDisplayUnderline(boolean displayUnderline) {
        tabStrip.setDisplayUnderline(displayUnderline);
    }

    /**
     * Sets the color to be used as an underline below all tabs.
     */
    public void setUnderlineColor(int color) {
        tabStrip.setUnderlineColor(color);
    }

    /**
     * Set a page change listener to observe page changes.
     *
     * @see ViewPager2#registerOnPageChangeCallback(ViewPager2.OnPageChangeCallback)
     */
    public void setOnPageChangeListener(ViewPager2.OnPageChangeCallback listener) {
        viewPagerPageChangeListener = listener;
    }

    /**
     * Set the {@link SlidingTabLayout.OnTabClickListener}.
     */
    public void setOnTabClickListener(OnTabClickListener listener) {
        onTabClickListener = listener;
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId id of the {@link android.widget.TextView} in the inflated view
     */
    public void setCustomTabView(int layoutResId, int textViewId) {
        tabViewLayoutId = layoutResId;
        tabViewTextViewId = textViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    public void setViewPager(ViewPager viewPager) {
        tabStrip.removeAllViews();

        this.viewPager = viewPager;
        if (viewPager != null) {
            viewPager.setOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    public void setViewPager2(ViewPager2 viewPager, TabTitleSupplier tabTitleSupplier) {
        tabStrip.removeAllViews();

        this.viewPager2 = viewPager;
        if (viewPager2 != null) {
            viewPager2.registerOnPageChangeCallback(new InternalViewPagerListener());
            populateTabStrip2(tabTitleSupplier);
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set
     * via
     * {@link #setCustomTabView(int, int)}.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        textView.setTypeface(Typeface.DEFAULT_BOLD);

        // If we're running on Honeycomb or newer, then we can use the Theme's
        // selectableItemBackground to ensure that the View has a pressed state
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true);
        textView.setBackgroundResource(outValue.resourceId);

        // If we're running on ICS or newer, enable all-caps to match the Action Bar tab style
        textView.setAllCaps(true);

        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);

        return textView;
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) return;
        populateTabStrip(adapter.getCount(), position -> {
            CharSequence titleOrNull = adapter.getPageTitle(position);
            return titleOrNull == null ? "" : titleOrNull.toString();
        });
    }

    private void populateTabStrip2(TabTitleSupplier tabTitleSupplier) {
        @SuppressWarnings("rawtypes")
        final RecyclerView.Adapter adapter = viewPager2.getAdapter();
        if (adapter == null) return;
        populateTabStrip(adapter.getItemCount(), tabTitleSupplier);
    }

    private void populateTabStrip(int itemCount, TabTitleSupplier tabTitleSupplier) {
        final OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < itemCount; i++) {
            View tabView = null;
            TextView tabTitleView = null;

            if (tabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(getContext()).inflate(tabViewLayoutId, tabStrip,
                        false);
                tabTitleView = tabView.findViewById(tabViewTextViewId);
            }

            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
            }

            if (tabTitleView == null && tabView instanceof TextView) {
                tabTitleView = (TextView) tabView;
            }

            if (tabTitleView == null) {
                throw new IllegalArgumentException("tabTitleView == null");
            }
            tabTitleView.setText(tabTitleSupplier.getTabTitle(i));
            tabView.setOnClickListener(tabClickListener);

            tabStrip.addView(tabView);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (viewPager != null) {
            scrollToTab(viewPager.getCurrentItem(), 0);
        }
        if (viewPager2 != null) {
            scrollToTab(viewPager2.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = tabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }

        View selectedChild = tabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            int targetScrollX = selectedChild.getLeft() + positionOffset;

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= titleOffset;
            }

            scrollTo(targetScrollX, 0);
        }

        // Update selected tab view once scrolling has stopped.
        if (positionOffset == 0) {
            for (int i = 0; i < tabStripChildCount; i++) {
                View child = tabStrip.getChildAt(i);
                child.setSelected(i == tabIndex);
                child.setActivated(i == tabIndex);
            }
        }
    }

    private class InternalViewPagerListener extends ViewPager2.OnPageChangeCallback
            implements ViewPager.OnPageChangeListener {
        private int scrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int tabStripChildCount = tabStrip.getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }

            tabStrip.onViewPagerPageChanged(position, positionOffset);

            View selectedTitle = tabStrip.getChildAt(position);
            int extraOffset = (selectedTitle != null)
                    ? (int) (positionOffset * selectedTitle.getWidth())
                    : 0;
            scrollToTab(position, extraOffset);

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageScrolled(position, positionOffset,
                        positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            scrollState = state;

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
                tabStrip.onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageSelected(position);
            }
        }
    }

    private class TabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                if (v == tabStrip.getChildAt(i)) {
                    if (onTabClickListener != null) {
                        onTabClickListener.onTabClick(i);
                    }
                    if (viewPager != null) {
                        viewPager.setCurrentItem(i);
                    }
                    if (viewPager2 != null) {
                        viewPager2.setCurrentItem(i);
                    }
                    return;
                }
            }
        }
    }
}
