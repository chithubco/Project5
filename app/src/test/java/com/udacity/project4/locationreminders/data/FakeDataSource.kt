package com.udacity.project4.locationreminders.data

import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.lang.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private val reminders = mutableListOf<ReminderDTO>()
    private val observableReminders = MutableLiveData<List<ReminderDTO>>(reminders)

    private var shouldReturnError = false

    fun setReturnError(value: Boolean){
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError){
            return Result.Error("Test Exception")
        }
        return try {
            Result.Success(reminders)
        }catch (ex: Exception){
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
        refreshData()
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError){
            return Result.Error("Test Exception")
        }
        return try {
            val response = reminders?.find { it.title == id }!!
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
        refreshData()
    }

    private fun refreshData() {
        observableReminders.postValue(reminders)
    }


}