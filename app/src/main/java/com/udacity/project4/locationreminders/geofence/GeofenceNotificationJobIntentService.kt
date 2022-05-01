package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceNotificationJobIntentService : JobIntentService(), CoroutineScope{

    private val TAG = "GeofenceReceiver"
    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

//    private val dataSource: RemindersLocalRepository by inject()

    companion object {
        private const val JOB_ID = 574

        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"

        //        TODO: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceNotificationJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
    override fun onHandleWork(intent: Intent) {

        val fenceId = intent.getStringExtra("fenceId")
        Log.i(TAG,"Job Service ${fenceId}")
        fenceId?.let { sendNotification(it) }
    }

    //TODO: get the request id of the current geofence
//    private fun sendNotification(triggeringGeofences: List<Geofence>) {
    private fun sendNotification(requestId: String) {
//        val requestId = ""

        //Get the local repository instance
//        val remindersLocalRepository: RemindersLocalRepository by inject()
//        Interaction to the repository has to be through a coroutine scope
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
//            val result = remindersLocalRepository.getReminder(requestId)
            val dataSource : RemindersLocalRepository = get()
            val result = dataSource.getReminder(requestId)
            Log.i(TAG,result.toString())
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data
                //send a notification to the user with the reminder details
                com.udacity.project4.utils.sendNotification(
                    this@GeofenceNotificationJobIntentService, ReminderDataItem(
                        reminderDTO.title,
                        reminderDTO.description,
                        reminderDTO.location,
                        reminderDTO.latitude,
                        reminderDTO.longitude,
                        reminderDTO.id
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}