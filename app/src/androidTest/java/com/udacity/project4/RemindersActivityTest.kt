package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.EspressoIdlingResource.countingIdlingResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application



    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource(){

        IdlingRegistry.getInstance().register(countingIdlingResource)
    }
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(countingIdlingResource)
    }


//    TODO: add End to End testing to the app

    @Test
    fun `add_reminder_tet`() = runBlocking {
        //Startup reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB))
            .perform(click())

        onView(withId(R.id.reminderTitle)).perform(replaceText("New Title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("New Description"))
        onView(withId(R.id.selectLocation))
            .perform(click())
        Thread.sleep(2000)
        onView(withId(R.id.map))
            .perform(longClick())
        onView(withText("Add Reminder")).check(matches(isDisplayed()))
        onView(withText("Dismiss")).check(matches(isDisplayed()))
        onView(withText("Add Location Details")).check(matches(isDisplayed()))
        onView(withText("Add Reminder")).perform(click())
        Thread.sleep(2000)

        // Save new instance
        onView(withId(R.id.saveReminder))
            .perform(click())
        Thread.sleep(500)
        //Check List screen

        // Using Toast Matcher
        onView(withText(appContext.getString(R.string.reminder_saved))).inRoot(ToastMatcher()).check(matches(isDisplayed()))

//        onView(withId(com.google.android.material.R.id.snackbar_text))
//            .check(matches(withText(R.string.reminder_saved)))
        //Close reminder screen
        activityScenario.close()

    }

    @Test
    fun `create_new_reminder_with_empty_input`() = runBlockingTest {
        //Startup reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB))
            .perform(click())

        onView(withId(R.id.saveReminder))
            .perform(click())

        //Snack bar with location error
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        Thread.sleep(2000)
    }

    @Test
    fun `create_new_reminder_with_invalid_title_input`() = runBlockingTest {
        //Startup reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB))
            .perform(click())

        onView(withId(R.id.reminderDescription)).perform(replaceText("New Description"))
        onView(withId(R.id.saveReminder))
            .perform(click())

        //Snack bar with location error
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        Thread.sleep(2000)
    }


    @Test
    fun `create_new_reminder_with_invalid_location_input`() = runBlockingTest {
        //Startup reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB))
            .perform(click())

        onView(withId(R.id.reminderTitle)).perform(replaceText("New Title"))
        onView(withId(R.id.saveReminder))
            .perform(click())

        //Snack bar with location error
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

        Thread.sleep(2000)
    }

}
