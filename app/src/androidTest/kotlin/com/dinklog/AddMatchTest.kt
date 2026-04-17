package com.dinklog

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.dinklog.utils.PreferencesManager
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddMatchTest {

    private val clearSessionRule = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            PreferencesManager(context).clearSession()
        }
    }

    @get:Rule(order = 1)
    val ruleChain: RuleChain = RuleChain
        .outerRule(clearSessionRule)
        .around(ActivityScenarioRule(MainActivity::class.java))

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val testEmail = "test${System.currentTimeMillis()}@example.com"

    // Waits until the location field has text, meaning GPS autofill has completed
    // or our 4-second in-app timeout fired. This prevents a race condition where
    // GPS setText() overwrites our typed value after we've already typed it.
    private fun waitForLocationAutofill(timeoutMs: Long = 5000L) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(R.id.location_input)).check(matches(not(withText(""))))
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
        // Timed out — GPS failed and field is still empty, proceed anyway
    }

    private fun waitForView(viewId: Int, timeoutMs: Long = 5000L) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (e: Exception) {
                Thread.sleep(200)
            }
        }
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    @Test
    fun addMatch_withBlankOpponent_showsValidationError() {
        // Sign up to get a logged-in session
        onView(withId(R.id.signup_link)).perform(click())
        onView(withId(R.id.email_input))
            .perform(typeText(testEmail), closeSoftKeyboard())
        onView(withId(R.id.name_input))
            .perform(typeText("Test User"), closeSoftKeyboard())
        onView(withId(R.id.password_input))
            .perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.confirm_password_input))
            .perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.signup_button)).perform(click())

        // Wait for signup coroutine to finish and navigate to History
        waitForView(R.id.fab_add_match)
        onView(withId(R.id.fab_add_match)).perform(click())

        // Wait for GPS autofill to finish before touching the location field
        waitForLocationAutofill()

        // Fill in scores and location but leave Opponent 1 blank
        onView(withId(R.id.team_score_input))
            .perform(typeText("11"), closeSoftKeyboard())
        onView(withId(R.id.opponent_score_input))
            .perform(typeText("8"), closeSoftKeyboard())
        onView(withId(R.id.location_input))
            .perform(clearText(), typeText("Test Courts"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())

        onView(withText("Opponent 1 name is required"))
            .check(matches(isDisplayed()))
    }
}