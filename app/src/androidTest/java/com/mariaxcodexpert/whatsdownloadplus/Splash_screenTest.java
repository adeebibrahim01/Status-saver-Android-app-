package com.mariaxcodexpert.whatsdownloadplus;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mariaxcodexpert.whatsdownloadplus.Splash.Splash_screen;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class Splash_screenTest extends BaseTest {

    @Rule
    public ActivityScenarioRule<Splash_screen> mActivityScenarioRule =
            new ActivityScenarioRule<>(Splash_screen.class);

    @Test
    public void splash_screenTest() throws Exception {

        // --- LOGCAT FIX: GIVE TIME TO TRANSITION ---
        // Logs ke mutabiq activity resume hone ke baad logic run hota hai.
        // 6 seconds ka wait Splash timer aur Ads ko skip karne ke liye kafi hai.
        Thread.sleep(7000);

        // --- STEP 1: SELECT WHATSAPP ---
        // Yahan 'allOf' ke saath 'isDisplayed()' zaroori hai
        onView(allOf(withId(R.id.selectWhatsappcheckbox), isDisplayed()))
                .perform(click());

        // --- STEP 2: CLICK ALLOW ---
        onView(allOf(withId(R.id.allowStatusFolderButton), withText("Allow"), isDisplayed()))
                .perform(scrollTo(), click());

        // --- STEP 3: SYSTEM PERMISSION DIALOG ---
        Thread.sleep(2000); // Dialog pop-up hone ka wait
        allowPermissionsIfNeeded(); // BaseTest method

        // Dashboard aane ka wait
        Thread.sleep(3000);

        // --- STEP 4: DASHBOARD NAVIGATION ---
        onView(allOf(withId(R.id.btnImages), isDisplayed()))
                .perform(click());

        // Image Selection and Save
        onView(allOf(withId(R.id.imageThumb), isDisplayed()))
                .perform(actionOnItemAtPosition(0, click())); // Pehli image par click

        onView(allOf(withId(R.id.cardSave), isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.btnDownloadSelected), isDisplayed()))
                .perform(click());

        // --- STEP 5: VIDEOS TAB ---
        onView(allOf(withContentDescription("Videos"), isDisplayed()))
                .perform(click());

        // Close logic
        onView(allOf(withId(R.id.ivClose), isDisplayed()))
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