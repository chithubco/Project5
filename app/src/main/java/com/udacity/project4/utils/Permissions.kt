package com.udacity.project4.utils

import android.Manifest
import android.content.Context
import androidx.fragment.app.Fragment
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {
    fun hasLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )


    fun requestLocationPermission(fragment: Fragment) {
        EasyPermissions.requestPermissions(
            fragment,
            "This app requires fine location permission to function",
            Constants.PERMISSION_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun hasBackgroundLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

    fun requestBackgroundLocationPermission(fragment: Fragment) {
        EasyPermissions.requestPermissions(
            fragment,
            "This app requires permission to add reminders",
            Constants.PERMISSION_BACKGROND_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }
}