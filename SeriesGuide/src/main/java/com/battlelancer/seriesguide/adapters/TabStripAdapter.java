/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.adapters;

import com.astuetz.PagerSlidingTabStrip;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Helper class for easy setup of a {@link PagerSlidingTabStrip}.
 */
public class TabStripAdapter extends FragmentPagerAdapter {

    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    private final Context mContext;

    private final FragmentManager mFragmentManager;

    private final ViewPager mViewPager;

    private final PagerSlidingTabStrip mTabStrip;

    static final class TabInfo {

        private final Class<?> mClass;

        private final Bundle mArgs;

        private final int mTitleRes;

        TabInfo(Class<?> fragmentClass, Bundle args, int titleRes) {
            mClass = fragmentClass;
            mArgs = args;
            mTitleRes = titleRes;
        }
    }

    public TabStripAdapter(FragmentManager fm, Context context, ViewPager pager,
            PagerSlidingTabStrip tabs) {
        super(fm);
        mFragmentManager = fm;
        mContext = context;
        mTabStrip = tabs;
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mTabStrip.setViewPager(mViewPager);
    }

    /**
     * Adds a new tab. Make sure to call {@link #notifyTabsChanged} after you have added them all.
     */
    public void addTab(int titleRes, Class<?> fragmentClass, Bundle args) {
        mTabs.add(new TabInfo(fragmentClass, args, titleRes));
    }

    /**
     * Update an existing tab. Make sure to call {@link #notifyTabsChanged} afterwards.
     */
    public void updateTab(int titleRes, Class<?> fragmentClass, Bundle args, int position) {
        if (position >= 0 && position < mTabs.size()) {
            // update tab info
            mTabs.set(position, new TabInfo(fragmentClass, args, titleRes));

            // find current fragment of tab
            Fragment oldFragment = mFragmentManager
                    .findFragmentByTag(makeFragmentName(mViewPager.getId(), getItemId(position)));
            // remove it
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.remove(oldFragment);
            transaction.commitAllowingStateLoss();
            mFragmentManager.executePendingTransactions();
        }
    }

    /**
     * Notifies the adapter and tab strip that the tabs have changed.
     */
    public void notifyTabsChanged() {
        notifyDataSetChanged();
        mTabStrip.notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo tab = mTabs.get(position);
        return Fragment.instantiate(mContext, tab.mClass.getName(), tab.mArgs);
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        TabInfo tabInfo = mTabs.get(position);
        if (tabInfo != null) {
            return mContext.getString(tabInfo.mTitleRes).toUpperCase(Locale.getDefault());
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
