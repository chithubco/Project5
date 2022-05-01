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
import com.udacity.project4.utils.Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private val TAG = "GeofenceReceiver"
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
        Log.i("GeofenceReceiver","JobIntent onCreate")
    }

    override fun onHandleWork(intent: Intent) {
        //TODO: handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area
        //TODO call @sendNotification
        val requestId = intent.getStringExtra("requestId").toString()
        val latitude = intent.getStringExtra("latitude").toString()
        val longitude = intent.getStringExtra("longitude").toString()


        if (latitude != null && longitude != null && requestId != null){
            Log.i("GeofenceReceiver","Lat : ${latitude}")
            Log.i("GeofenceReceiver","Long : ${longitude}")
            Log.i("GeofenceReceiver","RequestId : ${requestId}")
            createGeofence(latitude,longitude,requestId)
        }
        if (isStopped) return
    }

    @SuppressLint("MissingPermission")
    private fun createGeofence(latitude: String,longitude: String, requestId: String) {
        val position = LatLng(latitude.toDouble(), longitude.toDouble())
        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(position.latitude, position.longitude,
                Constants.GEOFENCE_RADIUS_IN_METERS
            )
            .setLoiteringDelay(Constants.GEOFENCE_LOITERING_DELAY_IN_MILLISECONDS)
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
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

    override fun onDestroy() {
        super.onDestroy()
        Log.i("GeofenceReceiver","JobIntent Destroyed")
    }
}