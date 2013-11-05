/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;

import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.MenuDrawer.OnDrawerStateChangeListener;
import net.simonvt.menudrawer.Position;

import java.util.Locale;

/**
 * Adds onto {@link BaseActivity} by attaching a navigation drawer.
 */
public abstract class BaseNavDrawerActivity extends BaseActivity {

    private MenuDrawer mMenuDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);

        mMenuDrawer = getAttachedMenuDrawer();

        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);
        // setting size in pixels, oh come on...
        int menuSize = (int) getResources().getDimension(R.dimen.slidingmenu_width);
        mMenuDrawer.setMenuSize(menuSize);
        mMenuDrawer.setOnDrawerStateChangeListener(new OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                // helps hiding actions when the drawer is opening
                if (newState == MenuDrawer.STATE_CLOSED
                        || (oldState == MenuDrawer.STATE_CLOSED &&
                        (newState == MenuDrawer.STATE_OPENING || newState == MenuDrawer.STATE_DRAGGING))) {
                    supportInvalidateOptionsMenu();
                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
                // Nothing to do
            }
        });

        // Don't recreate the MenuDrawer content on orientation changes
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment f = new SlidingMenuFragment();
            ft.add(R.id.menu_frame, f);
            ft.commit();
        }
    }

    /*
     * Creates an {@link MenuDrawer} attached to this activity as an overlay.
     * Subclasses may override this to set their own layout and drawer type.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected MenuDrawer getAttachedMenuDrawer() {
        Position drawerPosition = Position.LEFT;

        if (AndroidUtils.isJellyBeanMR1OrHigher()) {
            // attach drawer to right side if using RTL layout
            int direction = TextUtils
                    .getLayoutDirectionFromLocale(Locale.getDefault());
            if (direction == View.LAYOUT_DIRECTION_RTL) {
                drawerPosition = Position.RIGHT;
            }
        }

        MenuDrawer menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY, drawerPosition);
        menuDrawer.setupUpIndicator(this);
        menuDrawer.setMenuView(R.layout.menu_frame);
        return menuDrawer;
    }

    @Override
    public void onBackPressed() {
        // close an open menu first
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return;
        }

        super.onBackPressed();
    }

    protected MenuDrawer getMenu() {
        return mMenuDrawer;
    }

    /**
     * Returns true if the navigation drawer is visible in any way (opening,
     * closing, peeking, open).
     */
    protected boolean isMenuDrawerOpen() {
        return getMenu().getDrawerState() != MenuDrawer.STATE_CLOSED;
    }

    protected void toggleMenu() {
        mMenuDrawer.toggleMenu();
    }

}
