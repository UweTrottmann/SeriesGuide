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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.uwetrottmann.seriesguide.R;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.MenuDrawer.OnDrawerStateChangeListener;

/**
 * Adds onto {@link BaseActivity} by attaching a navigation drawer.
 */
public abstract class BaseNavDrawerActivity extends BaseActivity {

    private MenuDrawer mMenuDrawer;

    @Override
    protected void onCreate(Bundle arg0) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(arg0);

        setupNavDrawer();
    }

    /**
     * Attaches the {@link MenuDrawer}.
     */
    private void setupNavDrawer() {
        mMenuDrawer = getAttachedMenuDrawer();

        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);
        // setting size in pixels, oh come on...
        int menuSize = (int) getResources().getDimension(R.dimen.slidingmenu_width);
        mMenuDrawer.setMenuSize(menuSize);
        mMenuDrawer.setOnDrawerStateChangeListener(new OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                // helps hiding actions when the drawer is open
                if (newState == MenuDrawer.STATE_CLOSED || newState == MenuDrawer.STATE_OPEN) {
                    supportInvalidateOptionsMenu();
                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
            }
        });

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment f = new SlidingMenuFragment();
        ft.add(R.id.menu_frame, f);
        ft.commit();
    }

    /*
     * Creates an {@link MenuDrawer} attached to this activity as an overlay.
     * Subclasses may override this to set their own layout and drawer type.
     */
    protected MenuDrawer getAttachedMenuDrawer() {
        MenuDrawer menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
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

    protected void toggleMenu() {
        mMenuDrawer.toggleMenu();
    }

}
