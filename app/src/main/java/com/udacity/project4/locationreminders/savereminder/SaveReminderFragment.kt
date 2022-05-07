package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.dto.GeocodeDTO
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants.BACKGROUND_LOCATION_PERMISSION_INDEX
import com.udacity.project4.utils.Constants.LOCATION_PERMISSION_INDEX
import com.udacity.project4.utils.Constants.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
import com.udacity.project4.utils.Constants.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.utils.Constants.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    var geocode: GeocodeDTO? = null
    var hasLocationDetails = false

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private val TAG = "SaveReminder"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            geocode = SaveReminderFragmentArgs.fromBundle(it).geocode
        }
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            navigateToSelectMap()
        }
        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            //Validate Entry
            val dataItem = ReminderDataItem(title, description, location, latitude, longitude)
            if (_viewModel.validateEnteredData(dataItem)) {
                if (checkPermissions()) {
                    _viewModel.isAllPermissionsGranted.value = true
                } else {
                    makeLocationPermissionRequest()
                }
            }
        }
        setUpObservers()
        setupArgs()
    }


    private fun setupArgs() {
        if (geocode?.latitude.isNullOrEmpty() || geocode?.longitude.isNullOrEmpty() || geocode?.location.isNullOrEmpty()) {
            hasLocationDetails = false
            Log.i("Geocode12", geocode.toString())
//            binding.saveReminder.isEnabled = false
        } else {
            Log.i("Geocode not empty", geocode.toString())
            geocode?.let {
                _viewModel.reminderSelectedLocationStr.value = geocode?.location
                geocode!!.latitude.let {
                    _viewModel.latitude.postValue(geocode?.latitude?.toDouble())
                }
                geocode!!.latitude.let {
                    _viewModel.longitude.postValue(geocode?.longitude?.toDouble())
                }
                binding.saveReminder.isEnabled = true

            }
        }
    }

    private fun setUpObservers() {
        Log.i(TAG, "Register Observers")
        _viewModel.isAllPermissionsGranted.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (it) {
                    //Save
                    val title = _viewModel.reminderTitle.value
                    val description = _viewModel.reminderDescription.value
                    val location = _viewModel.reminderSelectedLocationStr.value
                    val latitude = _viewModel.latitude.value
                    val longitude = _viewModel.longitude.value

                    val serviceIntent = Intent(requireContext(), GeofenceTransitionsJobIntentService::class.java)
                    serviceIntent.putExtra("requestId", title)
                    serviceIntent.putExtra("latitude", latitude.toString())
                    serviceIntent.putExtra("longitude", longitude.toString())
                    GeofenceTransitionsJobIntentService.enqueueWork(requireContext(), serviceIntent)

                    //             2) save the reminder to the local db
                    val reminder =
                        ReminderDataItem(title, description, location, latitude, longitude)
                    _viewModel.validateAndSaveReminder(reminder)
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }

    @TargetApi(29)
    private fun hasLocationPermission(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun makeLocationPermissionRequest() {
        if (hasLocationPermission())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d("Save Reminder", "Request foreground only location permission")
        Log.d("Save Reminder", "Build Version $runningQOrLater")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    private fun isGPSServiceAvailable(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            checkDeviceLocationSettings()
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_geofence_explanation,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()

            Log.d("SaveReminderFrag", "onRequestPermissionResult Denied")
        } else {
            Log.d("SaveReminderFrag", "onRequestPermissionResult Granted")
            checkPermissions()
        }
    }

    private fun navigateToSelectMap() {
        val action =
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        findNavController().navigate(action)
    }

    private fun checkPermissions(): Boolean {
        Log.i(TAG, "checkPermission()")
        if (hasLocationPermission()) {
            if (isGPSServiceAvailable()) {
                Log.i(TAG, "isGPSServiceAvailable() True")
                return true
            }
        }
        Log.i(TAG, "hasLocationPermission() False")
        return false
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
//                    exception.startResolutionForResult(
//                        requireActivity(),
//                        REQUEST_TURN_DEVICE_LOCATION_ON
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.i(TAG, "GPS Enabled")
                _viewModel.isAllPermissionsGranted.value = true
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
