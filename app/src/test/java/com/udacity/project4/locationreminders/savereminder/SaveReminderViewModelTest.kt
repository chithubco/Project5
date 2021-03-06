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

        assertThat(value).isFalse()
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

    @Test
    fun `show loading false`() {
        _viewModel.showLoading.value = false
        assertThat(_viewModel.showLoading.getOrAwaitValue()).isFalse()
    }

    @Test
    fun `show loading true`() {
        _viewModel.showLoading.value = true
        assertThat(_viewModel.showLoading.getOrAwaitValue()).isTrue()
    }

    @Test
    fun `show error message true`() {
        _viewModel.showErrorMessage.value = "Error"
        assertThat(_viewModel.showErrorMessage.getOrAwaitValue()).isEqualTo("Error")
    }

    @Test
    fun `show error showSnackBar message`() {
        _viewModel.showSnackBar.value = "Message"
        assertThat(_viewModel.showSnackBar.getOrAwaitValue()).isEqualTo("Message")
    }

    @Test
    fun `show error showToast message`() {
        _viewModel.showToast.value = "Message"
        assertThat(_viewModel.showToast.getOrAwaitValue()).isEqualTo("Message")
    }

    @Test
    fun `show showNoData false`() {
        _viewModel.showNoData.value = false
        assertThat(_viewModel.showNoData.getOrAwaitValue()).isFalse()
    }

    @Test
    fun `show showNoData true`() {
        _viewModel.showNoData.value = true
        assertThat(_viewModel.showNoData.getOrAwaitValue()).isTrue()
    }


}