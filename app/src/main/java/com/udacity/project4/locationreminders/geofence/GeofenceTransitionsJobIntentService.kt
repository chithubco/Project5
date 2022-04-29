package com.udacity.project4.locationreminders.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private val TAG = "JobIntentService"
    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var geofencingClient: GeofencingClient

    companion object {
        private const val JOB_ID = 573

        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"

        //        TODO: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Create a Geofence instance
        geofencingClient = LocationServices.getGeofencingClient(this@GeofenceTransitionsJobIntentService)
    }

    override fun onHandleWork(intent: Intent) {
        //TODO: handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area
        //TODO call @sendNotification
        val input = intent.getStringExtra("requestId").toString()
        val latitude = intent.getStringExtra("latitude").toString()
        val longitude = intent.getStringExtra("longitude").toString()
//        for (i in 2..10){
//            Log.i(TAG,"Run ${input} - ${i}")
//            Thread.sleep(1000)
//        }
        if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()){
                createGeofence(latitude,longitude)
        }

        if (isStopped) return
    }

    //TODO: get the request id of the current geofence
    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        val requestId = ""

        //Get the local repository instance
        val remindersLocalRepository: RemindersLocalRepository by inject()
//        Interaction to the repository has to be through a coroutine scope
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
            val result = remindersLocalRepository.getReminder(requestId)
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data
                //send a notification to the user with the reminder details
                sendNotification(
                    this@GeofenceTransitionsJobIntentService, ReminderDataItem(
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


    @SuppressLint("MissingPermission")
    private fun createGeofence(latitude: String,longitude: String) {
        val position = LatLng(latitude.toDouble(), longitude.toDouble())
        val geofence = Geofence.Builder()
            .setRequestId(Constants.GEOFENCE_REQUEST_ID)
            .setCircularRegion(position.latitude, position.longitude,
                Constants.GEOFENCE_RADIUS_IN_METERS
            )
            .setLoiteringDelay(Constants.GEOFENCE_LOITERING_DELAY_IN_MILLISECONDS)
            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
            .build()

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            if (geofence != null) {
                addGeofence(geofence)
            }
        }.build()

        geofencingClient.removeGeofences(pendingIntentGet()).run {
            addOnCompleteListener {
                geofencingClient.addGeofences(request, pendingIntentGet()).run {
                    addOnSuccessListener {
                        // Geofences added.
                        Log.i(TAG,"Geofence added")
                    }
                    addOnFailureListener {
                        if ((it.message != null)) {
                            Log.w(TAG, it.toString())
                        }
                    }
                }
            }
        }

    }
    private fun pendingIntentGet(): PendingIntent {
        val intent = Intent(this@GeofenceTransitionsJobIntentService, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        return PendingIntent.getBroadcast(
            this@GeofenceTransitionsJobIntentService,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

}