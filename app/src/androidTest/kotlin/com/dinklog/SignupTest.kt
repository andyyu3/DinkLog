package com.dinklog

import androidx.test.espresso.Espresso.onView
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
import com.dinklog.utils.PreferencesManager
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignupTest {

    private val clearSessionRule = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            PreferencesManager(context).clearSession()
        }
    }

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(clearSessionRule)
        .around(ActivityScenarioRule(MainActivity::class.java))

    @Test
    fun signup_withMismatchedPasswords_showsError() {
        onView(withId(R.id.signup_link)).perform(click())

        onView(withId(R.id.email_input))
            .perform(typeText("user@test.com"), closeSoftKeyboard())
        onView(withId(R.id.name_input))
            .perform(typeText("Test User"), closeSoftKeyboard())
        onView(withId(R.id.password_input))
            .perform(typeText("password1"), closeSoftKeyboard())
        onView(withId(R.id.confirm_password_input))
            .perform(typeText("password2"), closeSoftKeyboard())
        onView(withId(R.id.signup_button)).perform(click())

        onView(withText("Passwords do not match"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun signup_withEmptyFields_showsError() {
        onView(withId(R.id.signup_link)).perform(click())

        onView(withId(R.id.signup_button)).perform(click())

        onView(withText("Please fill in all fields"))
            .check(matches(isDisplayed()))
    }
}