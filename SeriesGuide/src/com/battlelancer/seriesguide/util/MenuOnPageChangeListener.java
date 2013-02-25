
package com.battlelancer.seriesguide.util;

import android.support.v4.view.ViewPager.OnPageChangeListener;

import net.simonvt.menudrawer.MenuDrawer;

/**
 * {@link OnPageChangeListener} which allows to open a {@link MenuDrawer} by
 * swiping once more to the left if displaying the first page.
 */
public class MenuOnPageChangeListener implements OnPageChangeListener {

    private MenuDrawer mMenuDrawer;

    public MenuOnPageChangeListener(MenuDrawer menuDrawer) {
        mMenuDrawer = menuDrawer;
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        mMenuDrawer.setTouchMode(position == 0
                ? MenuDrawer.TOUCH_MODE_FULLSCREEN
                : MenuDrawer.TOUCH_MODE_BEZEL);
    }

}
