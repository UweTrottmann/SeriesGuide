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

package com.battlelancer.seriesguide.test;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.google.android.apps.common.testing.ui.espresso.action.ViewActions;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerActions.closeDrawer;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerActions.openDrawer;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerMatchers.isClosed;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerMatchers.isOpen;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@LargeTest
public class DrawerNavTest extends ActivityInstrumentationTestCase2<ShowsActivity> {

    public DrawerNavTest() {
        super(ShowsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Espresso will not launch our activity for us, we must launch it via getActivity().
        getActivity();
    }

    public void testNavigateToListsAndBack() throws InterruptedException {
        navigateToAndBack(R.string.lists);
    }

    public void testNavigateToMoviesAndBack() throws InterruptedException {
        navigateToAndBack(R.string.movies);
    }

    public void testNavigateToStatsAndBack() throws InterruptedException {
        navigateToAndBack(R.string.statistics);
    }

    public void testNavigateToSearchAndBack() throws InterruptedException {
        navigateToAndBack(R.string.search_hint);
    }

    private void navigateToAndBack(int drawerItemTitleResId) throws InterruptedException {
        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));

        openDrawer(R.id.drawer_layout);

        onView(withText(drawerItemTitleResId)).perform(click());

        // let the UI do some loading before going back
        Thread.sleep(1500);

        pressBack();

        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));
    }

    public void testNavigateToSameScreen() throws InterruptedException {
        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));

        openDrawer(R.id.drawer_layout);

        onView(withText(R.string.shows)).perform(click());

        // should only close drawer and still display the same screen
        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));
    }

    public void testOpenAndCloseDrawer() {
        // drawer should be closed
        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));

        openDrawer(R.id.drawer_layout);

        onView(withId(R.id.drawer_layout)).check(matches(isOpen()));

        closeDrawer(R.id.drawer_layout);

        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));
    }

}
