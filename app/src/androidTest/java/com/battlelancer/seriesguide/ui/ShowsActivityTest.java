// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * To avoid flakiness, turn off system animations on the virtual or physical devices used for
 * testing. On your device, under Settings > Developer options, disable the following 3 settings:
 * <p>
 * - Window animation scale
 * - Transition animation scale
 * - Animator duration scale
 */
@RunWith(AndroidJUnit4.class)
public class ShowsActivityTest {

    // The new ActivityScenarioRule crashes when launching the activity with
    // java.io.IOException: Failed to load asset path /data/app/~~1yCLpUnAoaSjRHXrCd_Q5Q==/com.battlelancer.seriesguide-aHv2_nXKaE_sVSlEuX_yig==/base.apk
    @SuppressWarnings("deprecation")
    @Rule
    public ActivityTestRule<ShowsActivity> mActivityTestRule = new ActivityTestRule<>(
            ShowsActivity.class);

    @Before
    public void setUp() {
        // switch to in-memory database to ensure a clean state for the add show test
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase.switchToInMemory(context);
    }

    @Test
    public void testAddShowAndSetWatchedThenReturn() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Open discover screen
        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.buttonShowsAdd), isDisplayed()));
        floatingActionButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Open add show dialog
        // Note: first up to 4 items might be links.
        int showPosition = 5;
        ViewInteraction recyclerView = onView(withId(R.id.recyclerViewShowsDiscover));
        recyclerView.perform(actionOnItemAtPosition(showPosition, click()));

        // Click to add the show
        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.buttonPositive), withText(R.string.action_shows_add),
                        isDisplayed()));
        appCompatButton.perform(click());

        // Click on the show again to open it (this will wait until adding is finished due to
        // the progress indicator being shown)
        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.recyclerViewShowsDiscover)));
        recyclerView2.perform(actionOnItemAtPosition(showPosition, click()));

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.buttonEpisodeWatched), withText(R.string.action_watched),
                        isDisplayed()));
        appCompatButton2.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pressBack();

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pressBack();
    }
}
