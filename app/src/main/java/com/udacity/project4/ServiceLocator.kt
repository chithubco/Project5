package com.udacity.project4

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.runBlocking

object ServiceLocator {
    private val lock = Any()
    private var database: RemindersDatabase? = null

    @Volatile
    var reminderRepo: ReminderDataSource? = null

        @VisibleForTesting set

    fun provideTasksRepository(context: Context): ReminderDataSource {
        synchronized(this) {
            return reminderRepo ?: createReminderRepository(context)
        }
    }

    private fun createReminderRepository(context: Context): ReminderDataSource {
        val database = database ?: createDataBase(context)
        val newRepo = RemindersLocalRepository(database.reminderDao())
        reminderRepo = newRepo
        return newRepo
    }

    private fun createDataBase(context: Context): RemindersDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java, "locationReminders.db"
        ).build()
        database = result
        return result
    }

    @VisibleForTesting
    fun resetRepository() {
        synchronized(lock) {
            runBlocking {
                reminderRepo?.deleteAllReminders()
            }
            // Clear all data to avoid test pollution.
            database?.apply {
                clearAllTables()
                close()
            }
            database = null
            reminderRepo = null
        }
    }
}