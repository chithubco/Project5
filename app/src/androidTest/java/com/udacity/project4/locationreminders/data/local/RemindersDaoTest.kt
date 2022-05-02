package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.source.FakeReminderData.REMINDER_1

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb(){
        database.close()
    }

    @Test
    fun `insert_reminder`() = runBlockingTest {
        // GIVEN a single reminder is added to the database
        val reminder = REMINDER_1
        database.reminderDao().saveReminder(reminder)

        // WHEN the result is queried
        val response = database.reminderDao().getReminders()

        // THEN we should get one record in the list
        assertThat(response).isNotNull()
        assertThat(response).isNotEmpty()
        assertThat(response.size).isEqualTo(1)
    }

    @Test
    fun `insert_a_reminder_and_retrieve_details`() = runBlockingTest {
        // GIVEN a single reminder is added to the database
        val reminder = REMINDER_1
        database.reminderDao().saveReminder(reminder)
        // WHEN the result is queried
        val response = REMINDER_1.title?.let { database.reminderDao().getReminderById(it) }

        // THEN we should get one record in the list
        assertThat(response).isNotNull()
        assertThat(response?.id).isEqualTo(reminder.id)
        assertThat(response?.title).isEqualTo(reminder.title)
        assertThat(response?.location).isEqualTo(reminder.location)
        assertThat(response?.description).isEqualTo(reminder.description)
        assertThat(response?.latitude).isEqualTo(reminder.latitude)
        assertThat(response?.longitude).isEqualTo(reminder.longitude)
    }

    @Test
    fun `delete_reminder_empty_list`() = runBlockingTest {
        // GIVEN a single reminder is added to the database
        val reminder = REMINDER_1
        database.reminderDao().saveReminder(reminder)

        // WHEN the result is queried
        val response = database.reminderDao().getReminders()

        // THEN we should get one record in the list
        assertThat(response).isNotNull()
        assertThat(response).isNotEmpty()
        assertThat(response.size).isEqualTo(1)

        database.reminderDao().deleteAllReminders()
        val loaded = database.reminderDao().getReminders()
        assertThat(loaded).isEmpty()
    }

}