package com.udacity.project4.locationreminders

import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModelTest
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModelTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    SaveReminderViewModelTest::class,
    RemindersListViewModelTest::class
)
class ImplementationReminderTestSuite {
}