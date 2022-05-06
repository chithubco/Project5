package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.fakes.FakeRemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.source.FakeReminderData.REMINDER_1
import com.udacity.project4.locationreminders.data.local.source.FakeReminderData.REMINDER_2
import com.udacity.project4.locationreminders.data.local.source.FakeReminderData.REMINDER_3
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    private lateinit var dataStore: FakeDataSource
    private lateinit var repo : ReminderDataSource
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

//        dataStore = FakeDataSource()
        repo = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun closeDb(){
        database.close()
    }


    @Test
    fun `request_all_reminders_from_datasource`() = runBlocking{
        val dataList = ArrayList<ReminderDataItem>()
        when(val result : Result<List<ReminderDTO>> = repo.getReminders()){
            is Result.Success<*> ->{
                dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                    //map the reminder data from the DB to the be ready to be displayed on the UI
                    ReminderDataItem(
                        reminder.title,
                        reminder.description,
                        reminder.location,
                        reminder.latitude,
                        reminder.longitude,
                        reminder.id
                    )
                })
            }
        }
        assertThat(dataList).isEmpty()
    }

    @Test
    fun `save_a_reminder_to_repo_test`()= runBlocking {
        repo.saveReminder(REMINDER_1)

        val dataList = ArrayList<ReminderDTO>()
        when(val result : Result<List<ReminderDTO>> = repo.getReminders()){
            is Result.Success<*> ->{
                dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                    //map the reminder data from the DB to the be ready to be displayed on the UI
                    ReminderDTO(
                        reminder.title,
                        reminder.description,
                        reminder.location,
                        reminder.latitude,
                        reminder.longitude,
                        reminder.id
                    )
                })
            }
        }
        assertThat(dataList).contains(REMINDER_1)
        assertThat(dataList.size).isEqualTo(1)
    }

    @Test
    fun `get_single_reminder_from_list`() = runBlocking {
        repo.saveReminder(REMINDER_1)
        repo.saveReminder(REMINDER_2)
        repo.saveReminder(REMINDER_3)

        var response:ReminderDTO? = null

        when(val result : Result<ReminderDTO>? = REMINDER_1.title?.let { repo.getReminder(it) }){
            is Result.Success<*> ->{
                response = result.data as ReminderDTO
            }
        }

        assertThat(response).isNotNull()
        assertThat(response?.title).isEqualTo(REMINDER_1.title)
        assertThat(response?.description).isEqualTo(REMINDER_1.description)
        assertThat(response?.location).isEqualTo(REMINDER_1.location)
        assertThat(response?.latitude).isEqualTo(REMINDER_1.latitude)
        assertThat(response?.longitude).isEqualTo(REMINDER_1.longitude)
    }

    @Test
    fun `when_a_reminder_with_the_given_id_cannot_be_found_there_is_an_error_message`() = runBlocking {
        repo.saveReminder(REMINDER_1)
        repo.saveReminder(REMINDER_2)


        when(val result : Result<ReminderDTO>? = REMINDER_3.title?.let { repo.getReminder(it) }){
            is Result.Error -> {
                assertThat(result.message).isNotNull()
                assertThat(result.message).isNotEmpty()
                assertThat(result.message).isEqualTo("Reminder not found!")
            }
        }

    }
}