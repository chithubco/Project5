package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.dto.GeocodeDTO
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragmentDirections
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.Constants.BACKGROUND_LOCATION_PERMISSION_INDEX
import com.udacity.project4.utils.Constants.LOCATION_PERMISSION_INDEX
import com.udacity.project4.utils.Constants.PERMISSION_REQUEST_ENABLE_GPS
import com.udacity.project4.utils.Constants.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
import com.udacity.project4.utils.Constants.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    var geocode: GeocodeDTO? = null
    var hasLocationDetails = false
    private var hasLocationPermission = false

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

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
            if (hasLocationPermission()) {
               navigateToSelectMap()
            }
            makeLocationPermissionRequest()
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

                if (checkPermissions()){
                    val serviceIntent =
                        Intent(requireContext(), GeofenceTransitionsJobIntentService::class.java)
                    serviceIntent.putExtra("requestId", title)
                    serviceIntent.putExtra("latitude", latitude.toString())
                    serviceIntent.putExtra("longitude", longitude.toString())
                    GeofenceTransitionsJobIntentService.enqueueWork(requireContext(), serviceIntent)
                }

//             2) save the reminder to the local db
                val reminder = ReminderDataItem(title, description, location, latitude, longitude)
                _viewModel.validateAndSaveReminder(reminder)
            }
        }
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

    @TargetApi(29)
    private fun hasLocationPermission(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))
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
    private fun isGPSServiceAvailable(): Boolean{
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            buildAlertGPSDisabled()
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d("SaveReminderFrag", "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
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
            navigateToSelectMap()
        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun navigateToSelectMap(resolve:Boolean = true){
        val action = SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        findNavController().navigate(action)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            PERMISSION_REQUEST_ENABLE_GPS -> {
                if (hasLocationPermission){
                    Log.i("GPS","Access Given")
                }else{
                    requestPermissionDialog()
                }
            }
        }
    }

    private fun checkPermissions():Boolean{
        Log.i("checkPermission","checkPermission()")
        if (hasLocationPermission()){
            if (isGPSServiceAvailable()){
                Log.i("checkPermission","True")
                return true
            }
        }
        Log.i("checkPermission","False")
        return false
    }

    private fun buildAlertGPSDisabled(){
        val builder = AlertDialog.Builder(requireContext())
        builder
            .setMessage("GPS Service is Required for this app to work")
            .setTitle("Location Permission")
            .setPositiveButton("Ok") { dialog, id ->
                val gpsIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(gpsIntent,PERMISSION_REQUEST_ENABLE_GPS)
            }.setNegativeButton("Cancel") { dialog, id ->
                //
            }.create().show()
    }

    private fun requestPermissionDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder
            .setMessage("Location Service is Required for this app to work")
            .setTitle("Location Permission")
            .setPositiveButton("Ok") { dialog, id ->
                makeLocationPermissionRequest()
            }.setNegativeButton("Cancel") { dialog, id ->
                //
            }.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
