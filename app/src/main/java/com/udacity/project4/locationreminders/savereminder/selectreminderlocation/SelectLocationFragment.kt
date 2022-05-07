package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.data.dto.GeocodeDTO
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.Constants.PERMISSION_BACKGROND_LOCATION_REQUEST_CODE
import com.udacity.project4.utils.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.udacity.project4.utils.Permissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.wrapEspressoIdlingResource
import org.koin.android.ext.android.inject
import java.io.IOException
import java.lang.StringBuilder

class SelectLocationFragment : BaseFragment(), GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnPoiClickListener {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocode: GeocodeDTO
    private val TAG = "SelectLocation"

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        setUpObservers()

        val barumak = LatLng(9.052596841535514, 7.452365927641011)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(barumak, 16f))
        binding.btnSaveLocation.visibility = View.GONE

        mMap.uiSettings.apply {
//            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = false
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true

        }
        mMap.setOnMarkerClickListener(this)
        mMap.setOnPoiClickListener(this)
        mMap.setOnMyLocationButtonClickListener(this)
        onMapClicked()
        onMapLongClick()
        setMapStyle(mMap)

        if (!checkPermissions()) {
            makeLocationPermissionRequest()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.btnSaveLocation.setOnClickListener {
            val action =
                SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment(
                    geocode
                )
            findNavController().navigate(action)
        }

        return binding.root
    }

//    override fun onResume() {
//        super.onResume()
//        if (checkPermissions()) {
//            _viewModel.hasPermission.postValue(true)
//        }
//    }


    @SuppressLint("MissingPermission")
    private fun setUpObservers() {
        _viewModel.hasPermission.observe(viewLifecycleOwner, Observer { hasPermission ->
            if (hasPermission) {
                mMap.isMyLocationEnabled = true
            }
        })

        _viewModel.hasGPSPermission.observe(viewLifecycleOwner, Observer { perm ->
            if (!perm) {

            }
        })

        _viewModel.isReadyToSave.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.btnSaveLocation.visibility = View.VISIBLE
            } else {
                binding.btnSaveLocation.visibility = View.GONE
            }
        })
    }


    private fun isGPSServiceAvailable(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            checkDeviceLocationSettings()
            _viewModel.hasGPSPermission.postValue(false)
            return false
        }
        _viewModel.hasGPSPermission.postValue(true)
        return true
    }


    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        Constants.REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                    _viewModel.hasGPSPermission.postValue(false)
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
            if ( it.isSuccessful ) {
                _viewModel.hasGPSPermission.postValue(true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == Constants.REQUEST_TURN_DEVICE_LOCATION_ON) {
            Log.i(TAG,"onActivityResult")
            Snackbar.make(
                binding.root,
                R.string.location_required_error,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }


    @SuppressLint("MissingPermission")
    private fun zoomIntoLastKnownPosition() {
        if (isGPSServiceAvailable()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val position = LatLng(it.latitude, it.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))
                    }

                }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return true
    }

    override fun onMyLocationButtonClick(): Boolean {
        if (isGPSServiceAvailable()) {
            zoomIntoLastKnownPosition()
        }
        return false
    }

    override fun onPoiClick(poi: PointOfInterest?) {

        geocode = GeocodeDTO(
            poi?.name.toString(),
            poi?.latLng?.latitude.toString(),
            poi?.latLng?.longitude.toString()
        )

        val stringBuilder = StringBuilder()
        stringBuilder.append("Location details : ${poi?.name.toString()}")
        stringBuilder.append(" would be added to you reminder")

        val builder = AlertDialog.Builder(requireContext())
        builder
            .setMessage(stringBuilder.toString())
            .setTitle("Add Location Details")
            .setPositiveButton("Add Reminder") { dialog, id ->
                // Go pack to add reminder screen
                poi?.latLng?.let { addMarker(it, poi?.name.toString(), poi?.placeId.toString()) }
                _viewModel.isReadyToSave.postValue(true)
            }.setNegativeButton("Dismiss") { dialog, id ->
                _viewModel.isReadyToSave.postValue(false)
            }.create().show()

    }

    private fun onMapClicked() {
        mMap.setOnMapClickListener {

        }
    }

    /*
    on Map Long Click
     */
    private fun onMapLongClick() {
        mMap.setOnMapLongClickListener {
            if (isGPSServiceAvailable()) {
                val address = reverseGeocodeLocation(it.latitude, it.longitude)

                geocode = GeocodeDTO(
                    address?.get(0)?.getAddressLine(0),
                    it.latitude.toString(),
                    it.longitude.toString()
                )

                val stringBuilder = StringBuilder()
                stringBuilder.append("Location details : ${address?.get(0)?.getAddressLine(0)}")
                stringBuilder.append(" ${address?.get(0)?.adminArea}")
                stringBuilder.append(" would be added to you reminder")

                val builder = AlertDialog.Builder(requireContext())
                builder
                    .setMessage(stringBuilder.toString())
                    .setTitle("Add Location Details")
                    .setPositiveButton("Add Reminder") { dialog, id ->
                        addMarker(it, "New Location", "Snippet")
                        _viewModel.isReadyToSave.postValue(true)
                    }.setNegativeButton("Dismiss") { dialog, id ->
                        _viewModel.isReadyToSave.postValue(false)
                    }.create().show()
            }

        }
    }

    private fun addMarker(position: LatLng, title: String, snippet: String) {
        val marker = mMap.addMarker(MarkerOptions().position(position).title(title))
        marker.snippet = snippet
        addCircle(position)
    }

    private fun addCircle(position: LatLng) {
        mMap.addCircle(
            CircleOptions().apply {
                center(position)
                radius(500.0)
                fillColor(R.color.purple_200)
                strokeColor(R.color.purple_200)
            }
        )
    }

    private fun setMapStyle(googleMap: GoogleMap) {
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.style
                )
            )
            if (!success) {
                Log.d("Maps", "Failed to add Styles to map")
            }
        } catch (e: Exception) {
            Log.d("Maps", e.toString())
        }
    }


    private fun updateMapStyle(resource: Int) {
        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    resource
                )
            )
        } catch (e: Exception) {
            Log.d("Map Style", e.toString())
        }
    }

    private fun reverseGeocodeLocation(latitude: Double, longitude: Double): List<Address>? {
        var geocodeMatches: List<Address>? = null
        val address1: String?
        val address2: String?
        val state: String?
        val zipCode: String?
        val Country: String?

        wrapEspressoIdlingResource {
            try {
                geocodeMatches =
                    Geocoder(requireContext()).getFromLocation(latitude, longitude, 1)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        geocodeMatches.let {
            address1 = it?.get(0)?.getAddressLine(0)
            address2 = it?.get(0)?.getAddressLine(1)
            state = it?.get(0)?.adminArea
            zipCode = it?.get(0)?.postalCode
            Country = it?.get(0)?.countryName
        }
        return geocodeMatches

    }

    // Foreground Permission
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

    private fun hasFineLocationPermission(): Boolean {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
    }

    @TargetApi(29)
    private fun makeLocationPermissionRequest() {
        if (hasLocationPermission())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                Constants.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> Constants.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d("Save Reminder", "Request foreground only location permission")
        Log.d("Save Reminder", "Build Version $runningQOrLater")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (
            grantResults.isEmpty() ||
            grantResults[Constants.LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == Constants.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[Constants.BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            if (hasFineLocationPermission()) {
                Log.d("SaveReminderFrag", "Have fine location permission")
                _viewModel.hasPermission.postValue(true)
            } else {
//                Snackbar.make(
//                    binding.root,
//                    R.string.permission_denied_explanation,
//                    Snackbar.LENGTH_INDEFINITE
//                )
//                    .setAction(R.string.settings) {
//                        startActivity(Intent().apply {
//                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
//                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                        })
//                    }.show()
            }
            Log.d("SaveReminderFrag", "onRequestPermissionResult Denied")
        } else {
            Log.d("SaveReminderFrag", "onRequestPermissionResult Granted")
            _viewModel.hasPermission.postValue(true)
        }
    }

    private fun navigateToSelectMap(resolve: Boolean = true) {
        val action =
            SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        findNavController().navigate(action)
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

}
