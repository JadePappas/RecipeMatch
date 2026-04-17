package com.example.recipematch

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.recipematch.ui.MainActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testLogoutFunctionality() {
        // 1. Navigate to Profile
        onView(withId(R.id.navigation_profile)).perform(click())

        // 2. Open Settings
        onView(withId(R.id.settings_button)).perform(click())

        // 3. Click Logout button
        onView(withId(R.id.btn_logout)).perform(click())

        // 4. Confirm Logout in Dialog
        onView(withId(R.id.btn_confirm_logout)).perform(click())

        // 5. Verify navigation to Login screen
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
    }

    @Test
    fun testDiscoverSearchAndFilter() {
        // 1. Navigate to Discover
        onView(withId(R.id.navigation_discover)).perform(click())

        // 2. Verify Discover Title
        onView(withId(R.id.tv_discover_title)).check(matches(withText("Discover Recipes")))

        // 3. Perform a search
        onView(withId(R.id.et_search_recipe))
            .perform(typeText("Pasta"), closeSoftKeyboard())
        onView(withId(R.id.btn_search_discover)).perform(click())

        // 4. Select a filter chip (e.g., Italian)
        onView(withText("Italian")).perform(click())

        // 5. Verify chip selection
        onView(allOf(withText("Italian"), isChecked())).check(matches(isDisplayed()))
    }

    @Test
    fun testHomePantryInteraction() {
        // 1. Start on Home - Verify greeting starts with "Hi,"
        onView(withId(R.id.tv_greeting)).check(matches(withText(startsWith("Hi,"))))

        // 2. Click Update Pantry button on Home card
        onView(withId(R.id.btn_update_pantry)).perform(click())

        // 3. Verify it navigated to Pantry tab
        onView(withText("My Pantry")).check(matches(isDisplayed()))
        onView(withId(R.id.pantry_tabs)).check(matches(isDisplayed()))

        // 4. Navigate back to Home via bottom nav
        onView(withId(R.id.navigation_home)).perform(click())
        onView(withId(R.id.tv_greeting)).check(matches(isDisplayed()))
    }
}
