package com.battlelancer.seriesguide.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.*;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.widgets.WatchedBox;
import org.hamcrest.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.CursorMatchers.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SetAllOlderWatchedTest {

    @Rule
    public ActivityTestRule<ShowsActivity> mActivityTestRule = new ActivityTestRule<>(
            ShowsActivity.class);

    @Test
    public void tvSeriesTest () throws InterruptedException {
        seriesTest("Game of Thrones");
    }

    @Test
    public void netflixSeriesTest() throws InterruptedException {
        seriesTest("Sense8");
    }

    private void seriesTest(String title) throws InterruptedException {

        onView(allOf(withId(R.id.buttonShowsAdd), withContentDescription(R.string.action_shows_add)))
                .perform(click());

        onView(withId(R.id.editTextSearchBar))
                .perform(click());

        onView(withId(R.id.editTextSearchBar))
                .perform(replaceText(title), closeSoftKeyboard(), pressImeActionButton());

        onView(childAtPosition(
                allOf(withId(android.R.id.list),
                        withParent(withId(R.id.containerAddContent))), 0))
                .perform(click());

        onView(allOf(withId(R.id.buttonPositive), withText(R.string.action_shows_add)))
                .perform(click());

        goBack();

        Thread.sleep(1000);

        onData(withRowString("seriestitle", startsWith(title)))
                .perform(click());

        Thread.sleep(1000);

        onView(allOf(withId(R.id.pagerOverview),
                        withParent(allOf(withId(R.id.coordinatorLayoutOverview),
                                withParent(withId(R.id.drawer_layout))))))
                .perform(swipeLeft());

        onData(withRowInt("combinednr", 1)) // season 1
                .perform(click());

        Thread.sleep(1000);

        onData(withRowInt("episodenumber", 3))
                .onChildView(withContentDescription(R.string.description_menu_overflow))
                .perform(click());

        onView(allOf(withId(android.R.id.title), withText(R.string.mark_untilhere)))
                .perform(click());

        onData(withRowInt("episodenumber", 1))
                .onChildView(Matchers.<View>instanceOf(WatchedBox.class))
                .check(matches(new CustomTypeSafeMatcher<View>("WatchedBox flagged as Watched") {
                    @Override
                    protected boolean matchesSafely(View view) {
                        return ((WatchedBox) view).getEpisodeFlag() == EpisodeFlags.WATCHED;
                    }
                }));

        goBack();

        goBack();

        onData(withRowString("seriestitle", startsWith(title)))
                .onChildView(withContentDescription(R.string.description_menu_overflow))
                .perform(click());

        onView(allOf(withId(android.R.id.title), withText(R.string.delete_show)))
                .perform(click());

        onView(allOf(withId(R.id.buttonPositive), withText(R.string.delete_show)))
                .perform(click());

    }

    private static void goBack() {
        onView(allOf(withContentDescription("Navigate up"),
                withParent(withId(R.id.sgToolbar))))
                .perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

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
