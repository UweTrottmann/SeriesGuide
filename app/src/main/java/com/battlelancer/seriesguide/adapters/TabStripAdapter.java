package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Helper class for easy setup of a {@link com.battlelancer.seriesguide.widgets.SlidingTabLayout}.
 */
public class TabStripAdapter extends FragmentPagerAdapter {

    static final class TabInfo {

        private final Class<?> fragmentClass;
        private final Bundle args;
        private final int titleRes;

        TabInfo(Class<?> fragmentClass, Bundle args, int titleRes) {
            this.fragmentClass = fragmentClass;
            this.args = args;
            this.titleRes = titleRes;
        }
    }

    private final ArrayList<TabInfo> tabs = new ArrayList<>();
    private final Context context;
    private final FragmentManager fragmentManager;

    private final ViewPager viewPager;
    private final SlidingTabLayout tabLayout;

    public TabStripAdapter(FragmentManager fragmentManager, Context context, ViewPager pager,
            SlidingTabLayout tabLayout) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.context = context;

        // setup view pager
        viewPager = pager;
        viewPager.setAdapter(this);

        // setup tabs
        this.tabLayout = tabLayout;
        this.tabLayout.setCustomTabView(R.layout.tabstrip_item_allcaps, R.id.textViewTabStripItem);
        this.tabLayout.setSelectedIndicatorColors(ContextCompat.getColor(context, R.color.white));
        this.tabLayout.setViewPager(viewPager);
    }

    /**
     * Adds a new tab. Make sure to call {@link #notifyTabsChanged} after you have added them all.
     */
    public void addTab(@StringRes int titleRes, Class<?> fragmentClass, Bundle args) {
        tabs.add(new TabInfo(fragmentClass, args, titleRes));
    }

    /**
     * Update an existing tab. Make sure to call {@link #notifyTabsChanged} afterwards.
     */
    public void updateTab(int titleRes, Class<?> fragmentClass, Bundle args, int position) {
        if (position >= 0 && position < tabs.size()) {
            // update tab info
            tabs.set(position, new TabInfo(fragmentClass, args, titleRes));

            // find current fragment of tab
            Fragment oldFragment = fragmentManager
                    .findFragmentByTag(makeFragmentName(viewPager.getId(), getItemId(position)));
            // remove it
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(oldFragment);
            transaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
    }

    /**
     * Notifies the adapter and tab strip that the tabs have changed.
     */
    public void notifyTabsChanged() {
        notifyDataSetChanged();
        tabLayout.setViewPager(viewPager);
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo tab = tabs.get(position);
        return Fragment.instantiate(context, tab.fragmentClass.getName(), tab.args);
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        TabInfo tabInfo = tabs.get(position);
        if (tabInfo != null) {
            return context.getString(tabInfo.titleRes).toUpperCase(Locale.getDefault());
        }
        return "";
    }

    /**
     * Copied from FragmentPagerAdapter.
     */
    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
}
