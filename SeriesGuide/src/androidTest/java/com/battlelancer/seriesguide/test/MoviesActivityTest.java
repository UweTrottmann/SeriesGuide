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

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.pressImeActionButton;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.swipeLeft;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.swipeRight;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

@LargeTest
public class MoviesActivityTest extends ActivityInstrumentationTestCase2<MoviesActivity> {

    public MoviesActivityTest() {
        super(MoviesActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testTabSwitching() {
        // go through tabs by tapping tab
        onView(withText("SEARCH")).perform(click());
        onView(withText("COLLECTION")).perform(click());

        // go back via swiping view
        onView(withId(R.id.pagerMovies)).perform(swipeRight());
        onView(withId(R.id.pagerMovies)).perform(swipeLeft());
        onView(withId(R.id.pagerMovies)).perform(swipeRight());
        onView(withId(R.id.pagerMovies)).perform(swipeRight());
    }

    public void testSearch() {
        onView(withId(R.id.pagerMovies)).perform(swipeLeft());
        onView(withId(R.id.editTextMoviesSearch)).perform(typeText("James Bond"));
        onView(withId(R.id.editTextMoviesSearch)).perform(pressImeActionButton());

        SystemClock.sleep(3000);

        onView(withId(R.id.pagerMovies)).perform(swipeRight());
    }
}
