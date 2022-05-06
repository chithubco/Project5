package com.udacity.project4.utils

import java.util.concurrent.TimeUnit

object Constants {
    const val SIGN_IN_REQUEST_CODE = 1234
    const val PERMISSION_LOCATION_REQUEST_CODE = 1
    const val PERMISSION_BACKGROND_LOCATION_REQUEST_CODE = 5
    const val PERMISSION_REQUEST_ENABLE_GPS = 2004
    const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 9004
    const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 9001

    // Geofence constants
    val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)
    const val GEOFENCE_LOITERING_DELAY_IN_MILLISECONDS = 1000
    const val GEOFENCE_RADIUS_IN_METERS = 1000f
    const val GEOFENCE_REQUEST_ID = "1234"
    const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"
    const val LOCATION_PERMISSION_INDEX = 0
    const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

    // Notification Constants
    const val CHANNEL_ID = "333"
    const val NOTIFICATION_ID = 12
}