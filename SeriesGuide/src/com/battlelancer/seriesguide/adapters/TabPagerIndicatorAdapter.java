
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.viewpagerindicator.TabPageIndicator;

import java.util.ArrayList;
import java.util.Locale;

public class TabPagerIndicatorAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener, ActionBar.TabListener {

    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    private Context mContext;
    private ViewPager mViewPager;
    private TabPageIndicator mIndicator;

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

    public TabPagerIndicatorAdapter(FragmentManager fm, Context context, ViewPager pager,
            TabPageIndicator indicator) {
        super(fm);
        mContext = context;
        mViewPager = pager;
        mIndicator = indicator;
        mViewPager.setAdapter(this);
        mIndicator.setViewPager(pager);
        mIndicator.setOnPageChangeListener(this);
    }

    public void addTab(int titleRes, Class<?> fragmentClass, Bundle args) {
        TabInfo tab = new TabInfo(fragmentClass, args, titleRes);
        mTabs.add(tab);
        notifyDataSetChanged();
        mIndicator.notifyDataSetChanged();
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

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mIndicator.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
    }

}
