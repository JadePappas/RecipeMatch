package com.example.recipematch

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.recipematch.ui.MainActivity
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testNavigationAndProfileSettings() {
        // 1. Navigate to Profile
        onView(withId(R.id.navigation_profile)).perform(click())

        // 2. Verify Profile Content
        onView(withId(R.id.profile_name)).check(matches(withText("John Doe")))

        // 3. Open Settings
        onView(withId(R.id.settings_button)).perform(click())

        // 4. Verify Settings UI
        onView(withText("Profile Settings")).check(matches(isDisplayed()))
        onView(withId(R.id.edit_display_name)).check(matches(isDisplayed()))

        // 5. Test text input in Settings
        onView(withId(R.id.edit_display_name))
            .perform(replaceText("Jane Doe"), closeSoftKeyboard())
        onView(withId(R.id.edit_display_name)).check(matches(withText("Jane Doe")))

        // 6. Go back to Profile
        onView(withId(R.id.btn_settings_back)).perform(click())
        onView(withId(R.id.profile_name)).check(matches(isDisplayed()))
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
        // Using isChecked() instead of isSelected() for Material Chips
        onView(allOf(withText("Italian"), isChecked())).check(matches(isDisplayed()))
    }

    @Test
    fun testHomePantryInteraction() {
        // 1. Start on Home
        onView(withId(R.id.tv_greeting)).check(matches(withText("Hi, User!")))

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
