package com.battlelancer.seriesguide.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * To avoid flakiness, turn off system animations on the virtual or physical devices used for
 * testing. On your device, under Settings > Developer options, disable the following 3 settings:
 *
 * - Window animation scale
 * - Transition animation scale
 * - Animator duration scale
 */
@RunWith(AndroidJUnit4.class)
public class ShowsActivityTest {

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

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.buttonShowsAdd), withContentDescription(R.string.action_shows_add),
                        childAtPosition(
                                withId(R.id.rootLayoutShows),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.recyclerViewShowsDiscover),
                        childAtPosition(
                                withId(R.id.constraintLayoutShowsDiscover),
                                1)));
        recyclerView.perform(actionOnItemAtPosition(2, click()));

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.buttonPositive), withText(R.string.action_shows_add),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.recyclerViewShowsDiscover),
                        childAtPosition(
                                withId(R.id.constraintLayoutShowsDiscover),
                                1)));
        recyclerView2.perform(actionOnItemAtPosition(2, click()));

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
                        withContentDescription(R.string.action_watched),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.include_buttons),
                                        0),
                                0),
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

    private static Matcher<View> childAtPosition(final Matcher<View> parentMatcher,
            final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
