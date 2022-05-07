package com.udacity.project4

import com.udacity.project4.locationreminders.data.local.RemindersDaoTest
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepositoryTest
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ReminderListFragmentTest::class,
    RemindersActivityTest::class,
    RemindersLocalRepositoryTest::class,
    RemindersDaoTest::class
)
class ReminderTestSuite {
}