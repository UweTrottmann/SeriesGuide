
package com.battlelancer.seriesguide.util;

import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.slidingmenu.lib.SlidingMenu;

/**
 * {@link OnPageChangeListener} which allows to open a {@link SlidingMenu} by
 * swiping once more to the left if displaying the first page.
 */
public class MenuOnPageChangeListener implements OnPageChangeListener {

    private SlidingMenu mSm;

    public MenuOnPageChangeListener(SlidingMenu sm) {
        mSm = sm;
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                mSm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
                break;
            default:
                mSm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
                break;
        }
    }

}
