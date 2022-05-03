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
//        _viewModel = get() as RemindersListViewModel
    }

    @Test
    fun `test showLoading to change from state while loading reminders`() = mainCoroutineRule.runBlockingTest {
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
    fun `reminderList returns a value when a record is added`() = mainCoroutineRule.runBlockingTest {
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
}