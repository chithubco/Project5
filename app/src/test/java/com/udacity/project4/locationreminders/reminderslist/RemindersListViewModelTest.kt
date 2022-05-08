package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

//@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var datasource: FakeDataSource
    private lateinit var _viewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewMode() {
        datasource = FakeDataSource()
        _viewModel = RemindersListViewModel(Application(), datasource)
    }


    @Test
    fun `test showLoading to change from state while loading reminders`() =
        mainCoroutineRule.runBlockingTest {
            mainCoroutineRule.pauseDispatcher()
            _viewModel.loadReminders()
            assertThat(_viewModel.showLoading.getOrAwaitValue()).isTrue()
            mainCoroutineRule.resumeDispatcher()
            assertThat(_viewModel.showLoading.getOrAwaitValue()).isFalse()
        }


    @Test
    fun `reminderList is empty when no record is added`() = mainCoroutineRule.runBlockingTest {
        _viewModel.loadReminders()
        val value = _viewModel.remindersList.getOrAwaitValue()
        assertThat(value).isEmpty()
    }

    @Test
    fun `reminderList returns a value when a record is added`() =
        mainCoroutineRule.runBlockingTest {
            val reminder = ReminderDTO(
                "Cool Tile",
                "Cool Description",
                "Cool Location",
                9.052596841535514,
                7.452365927641011
            )
            datasource.saveReminder(reminder = reminder)

            _viewModel.loadReminders()
            val value = _viewModel.remindersList.getOrAwaitValue()
            assertThat(value).isNotEmpty()
        }

    @Test
    fun `show no data when reminder list is empty`() = mainCoroutineRule.runBlockingTest {
        _viewModel.loadReminders()
        val value = _viewModel.remindersList.getOrAwaitValue()
        assertThat(value).isEmpty()
        assertThat(_viewModel.showNoData.getOrAwaitValue()).isTrue()
    }

    @Test
    fun `load reminders when unavailable call error to display show no data`() =
        mainCoroutineRule.runBlockingTest {
            //Make Datasource return an error
            datasource.setReturnError(true)
            val reminder = ReminderDTO(
                "Cool Tile",
                "Cool Description",
                "Cool Location",
                9.052596841535514,
                7.452365927641011
            )
            datasource.saveReminder(reminder = reminder)
            //Then an error message is shown
            _viewModel.loadReminders()
            assertThat(_viewModel.showNoData.getOrAwaitValue()).isTrue()
        }

    @Test
    fun `load reminders with error return error`() = mainCoroutineRule.runBlockingTest {
        //Given 3 reminders are added
        val reminder = ReminderDTO(
            "Cool Tile",
            "Cool Description",
            "Cool Location",
            9.052596841535514,
            7.452365927641011
        )
        val reminder2 = ReminderDTO(
            "Cool Tile",
            "Cool Description",
            "Cool Location",
            9.052596841535514,
            7.452365927641011
        )
        datasource.saveReminder(reminder = reminder)
        datasource.saveReminder(reminder = reminder2)

        //Then the setErrorFlag is called
        datasource.setReturnError(true)
        _viewModel.loadReminders()

        // An Error is raised
        val error = _viewModel.showSnackBar.getOrAwaitValue()
        assertThat(error).contains("Exception")
    }

    @Test
    fun `test showLoading message with no data`() =
        mainCoroutineRule.runBlockingTest {
            mainCoroutineRule.pauseDispatcher()
            datasource.setReturnError(true)
            _viewModel.loadReminders()
            assertThat(_viewModel.showLoading.getOrAwaitValue()).isTrue()

            mainCoroutineRule.resumeDispatcher()

            assertThat(_viewModel.showSnackBar.getOrAwaitValue()).contains("Exception")
        }


}