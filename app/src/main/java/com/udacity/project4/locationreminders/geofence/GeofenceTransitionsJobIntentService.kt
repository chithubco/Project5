package com.udacity.project4.locationreminders.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
//import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.udacity.project4.utils.Constants.NOTIFICATION_RESPONSIVENESS
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private val TAG = "GeofenceReceiver"
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var geofencingClient: GeofencingClient

    companion object {
        private const val JOB_ID = 573

        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"

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
        val requestId = intent.getStringExtra("requestId").toString()
        val latitude = intent.getStringExtra("latitude").toString()
        val longitude = intent.getStringExtra("longitude").toString()


        if (latitude != null && longitude != null && requestId != null){
            if (checkPermissions()){
                createGeofence(latitude,longitude,requestId)
            }else{
                Log.i(TAG,"Requesting Permission")
            }
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
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
            .build()

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            if (geofence != null) {
                addGeofence(geofence)
            }
        }.build()


        geofencingClient.addGeofences(request, pendingIntentGet()).run {
            addOnSuccessListener {
                Log.i(TAG,"Geofence added")
                Toast.makeText(this@GeofenceTransitionsJobIntentService,"Geofence Added", Toast.LENGTH_LONG).show()
            }
            addOnFailureListener {
                if ((it.message != null)) {
                    Log.w(TAG, it.toString())
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

    @TargetApi(29)
    private fun hasLocationPermission(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(this@GeofenceTransitionsJobIntentService,
                            Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this@GeofenceTransitionsJobIntentService, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun isGPSServiceAvailable(): Boolean{
        val locationManager = this@GeofenceTransitionsJobIntentService.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            return false
        }
        return true
    }
    private fun checkPermissions(): Boolean {
        Log.i("checkPermission", "checkPermission()")
        if (hasLocationPermission()) {
            if (isGPSServiceAvailable()) {
                Log.i("checkPermission", "True")
                return true
            }
        }
        Log.i("checkPermission", "False")
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("GeofenceReceiver","JobIntent Destroyed")
    }
}