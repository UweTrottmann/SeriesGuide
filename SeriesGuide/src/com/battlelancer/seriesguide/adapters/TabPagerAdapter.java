
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

import net.simonvt.menudrawer.MenuDrawer;

import java.util.ArrayList;

public class TabPagerAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener, ActionBar.TabListener {

    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    private Context mContext;
    private ActionBar mActionBar;
    private MenuDrawer mMenu;
    private ViewPager mViewPager;

    static final class TabInfo {
        private final Class<?> mClass;
        private final Bundle mArgs;

        TabInfo(Class<?> fragmentClass, Bundle args) {
            mClass = fragmentClass;
            mArgs = args;
        }
    }

    public TabPagerAdapter(FragmentManager fm, Context context, ActionBar actionBar,
            ViewPager pager, MenuDrawer menu) {
        super(fm);
        mContext = context;
        mActionBar = actionBar;
        mMenu = menu;
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
    }

    public void addTab(int titleRes, Class<?> fragmentClass, Bundle args) {
        TabInfo tab = new TabInfo(fragmentClass, args);
        mTabs.add(tab);
        ActionBar.Tab actionBarTab = mActionBar.newTab().setText(titleRes);
        actionBarTab.setTabListener(this);
        mActionBar.addTab(actionBarTab);
        notifyDataSetChanged();
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
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
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
        mActionBar.setSelectedNavigationItem(position);

        mMenu.setTouchMode(position == 0
                ? MenuDrawer.TOUCH_MODE_FULLSCREEN
                : MenuDrawer.TOUCH_MODE_BEZEL);
    }

}
