package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.provider.Settings.Global.getString
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.get
import org.koin.dsl.koinApplication

@ExperimentalCoroutinesApi
//@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var datasource: FakeDataSource
    private lateinit var _viewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewMode() {
        datasource = FakeDataSource()
        _viewModel = SaveReminderViewModel(Application(), datasource)
    }

    @Test
    fun `insert reminder with empty fields`() {
        val reminder = ReminderDataItem("", "", "", 0.00, 0.00)
        val value = _viewModel.validateEnteredData(reminder)

        assertThat(value).isFalse()
    }

    @Test
    fun `insert reminder with empty title fields`() {
        val reminder = ReminderDataItem(
            "",
            "Cool Description",
            "Cool Location",
            9.052596841535514,
            7.452365927641011
        )
        val value = _viewModel.validateEnteredData(reminder)

        assertThat(value).isFalse()
    }

    @Test
    fun `insert reminder with empty description fields`() {
        val reminder = ReminderDataItem(
            "Cool Title",
            "",
            "Cool Location",
            9.052596841535514,
            7.452365927641011
        )
        val value = _viewModel.validateEnteredData(reminder)

        assertThat(value).isTrue()
    }

    @Test
    fun `insert reminder with empty location fields`() {
        val reminder = ReminderDataItem(
            "Cool Title",
            "Cool Description",
            "",
            9.052596841535514,
            7.452365927641011
        )
        val value = _viewModel.validateEnteredData(reminder)

        assertThat(value).isFalse()
    }

    @Test
    fun `insert reminder with empty title fields snack value`() {
        // Given a reminder is created with an empty title field
        val reminder = ReminderDataItem(
            "",
            "Cool Description",
            "Cool Description",
            9.052596841535514,
            7.452365927641011
        )

        // and the validate function is called
        _viewModel.validateEnteredData(reminder)
        val snackBarValue = _viewModel.showSnackBarInt.getOrAwaitValue()

        // Then the snackBarInt value need to be set to the error text
        assertThat(snackBarValue).isNotNull()
        assertThat(snackBarValue).isEqualTo(R.string.err_enter_title)
    }

    @Test
    fun `insert reminder with empty location fields snack value`() {
        val reminder = ReminderDataItem(
            "Cool Title",
            "Cool Description",
            "",
            9.052596841535514,
            7.452365927641011
        )
        _viewModel.validateEnteredData(reminder)
        val snackBarValue = _viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(snackBarValue).isNotNull()
        assertThat(snackBarValue).isEqualTo(R.string.err_select_location)
    }

    // Test save reminder to ensure that toast value is generated
//    @Test
//    fun `insert reminder and save to ensure toast value is shown`() = runBlockingTest {
//        val reminder = ReminderDataItem(
//            "Cool Title",
//            "Cool Description",
//            "Cool Location",
//            9.052596841535514,
//            7.452365927641011
//        )
//        _viewModel.saveReminder(reminderData = reminder)
//        val toastMessage = _viewModel.showToast.getOrAwaitValue()
//
//        assertThat(toastMessage).isNotNull()
////        assertThat(toastMessage).isEqualTo(getString(R.string.reminder_saved))
//    }
}