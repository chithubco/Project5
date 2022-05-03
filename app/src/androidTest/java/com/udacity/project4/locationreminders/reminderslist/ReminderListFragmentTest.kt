package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.GeocodeDTO
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinComponent {
//    private val repo: ReminderDataSource by inject()
    private lateinit var repo: ReminderDataSource

    val dispatcher = TestCoroutineDispatcher()
    val testScope = TestCoroutineScope(dispatcher)

    @get: Rule
    val activityRule = ActivityScenarioRule(RemindersActivity::class.java)

//    @get:Rule
//    var instantExecutorRule = InstantTaskExecutorRule()

//    @get:Rule
//    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup(){
        repo = getApplicationContext<MyApp>().reminderRepo
    }
    //    TODO: test the navigation of the fragments.
    @Test
    fun click_fab_onListView_NavigateToMapFragment() = runBlockingTest {
        // Given we are on the Task Screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN
        onView(withId(R.id.addReminderFAB))
            .perform(click())
        val geocode = GeocodeDTO("","","")
        // THEN
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder(geocode))


    }
//    TODO: test the displayed data on the UI.

    @Test
    fun `launch_list_fragment`() = runBlockingTest {

        testScope.launch {
           repo.deleteAllReminders()
            val reminder = ReminderDTO(
                "Cool Tile",
                "Cool Description",
                "Cool Location",
                9.052596841535514,
                7.452365927641011
            )
            repo.saveReminder(reminder = reminder)
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText("Cool Tile"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        onView(withText("Cool Description"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        onView(withText("Cool Description"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun `launch_list_fragment_with_no_reminder_in_list`() = runBlockingTest {

        testScope.launch {
            repo.deleteAllReminders()
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText("No Data"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    }
//    TODO: add testing for the error messages.
}