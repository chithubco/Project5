package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.udacity.project4.utils.Permissions.hasBackgroundLocationPermission
import com.udacity.project4.utils.Permissions.hasLocationPermission
import com.udacity.project4.utils.Permissions.requestBackgroundLocationPermission
import com.udacity.project4.utils.Permissions.requestLocationPermission
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import org.koin.android.ext.android.bind
import org.koin.android.ext.android.inject
import java.security.Permissions

class SelectLocationFragment : BaseFragment(), GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnPoiClickListener,
    EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var mMap: GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var intendedLatLong: LatLng
    private lateinit var selectedReminder: ReminderDTO

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        setUpObservers()
        enableMyLocation()
        // Create a Geofence instance
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        val barumak = LatLng(9.052596841535514, 7.452365927641011)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(barumak, 16f))

        mMap.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = false
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
        }
        setMapStyle(mMap)
//        addObservers()
        mMap.setOnMarkerClickListener(this)
        mMap.setOnPoiClickListener(this)
        mMap.setOnMyLocationButtonClickListener(this)
        onMapClicked()
        onMapLongClick()
//        addListeners()
//        displayInfoMessage()

    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!hasLocationPermission(requireContext())) {
            _viewModel.hasPermission.postValue(false)
            requestLocationPermission(this)
        } else {
            _viewModel.hasPermission.postValue(true)
            mMap.isMyLocationEnabled = true
        }
    }

    private fun setUpObservers(){
        _viewModel.hasPermission.observe(viewLifecycleOwner, Observer { hasPermission ->
            Log.i("Permission",hasPermission.toString())
            if (hasPermission){
                binding.btnRequestPermission.visibility = View.GONE
            }else{
                binding.btnRequestPermission.visibility = View.VISIBLE
            }

        })
    }

    private fun navigateBackIfLocationIsNotGranted() {
        if (!hasLocationPermission(requireContext())) {
            findNavController().popBackStack()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//        TODO: add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()
        binding.btnRequestPermission.setOnClickListener {
            if (!hasLocationPermission(requireContext())) {
                requestLocationPermission(this)
            }else{
                _viewModel.hasPermission.postValue(true)
                mMap.isMyLocationEnabled = true
            }
        }

        return binding.root
    }


    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onMyLocationButtonClick(): Boolean {
        if (!hasLocationPermission(requireContext())) {
            Log.i("Permission", "onMyLocationButtonClick No Permission ")
            requestLocationPermission(this)
            return false
        }
        _viewModel.hasPermission.postValue(true)
        return false
    }

    override fun onPoiClick(p0: PointOfInterest?) {
        TODO("Not yet implemented")
    }

    private fun onMapClicked() {
        mMap.setOnMapClickListener {

        }
    }

    private fun onMapLongClick() {
        mMap.setOnMapLongClickListener {
            intendedLatLong = it
            if (hasBackgroundLocationPermission(requireContext())) {
                Log.i("Permission", "onMapClicked has Permission ")
            } else {
                Log.i("Permission", "onMapClicked No Permission ")
                requestBackgroundLocationPermission(this)
            }

        }
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

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        Log.i("Permission",requestCode.toString())
//        if (requestCode == Constants.REQUEST_TURN_DEVICE_LOCATION_ON) {
//
//        }
//
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_CANCELED){
            Log.i("Permission", "onActivityResult Cancel Button")
        }
        if (requestCode == PERMISSION_LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(requireContext(), "From App Settings", Toast.LENGTH_SHORT)
                .show();
            _viewModel.hasPermission.postValue(true)
        }
        if (requestCode == PERMISSION_LOCATION_REQUEST_CODE && resultCode == RESULT_CANCELED) {
            // Do something after user returned from app settings screen, like showing a Toast.
            requestLocationPermission(this)
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            Log.i("Permission", "onPermissionDenied If")
            SettingsDialog.Builder(requireActivity()).build().show()
        }
//        else if (EasyPermissions.somePermissionDenied(this,Manifest.permission.ACCESS_FINE_LOCATION)){
//            Log.i("Permission", "onPermissionDenied Specific")
//            SettingsDialog.Builder(requireActivity()).build().show()
//        }
        else {
            if (requestCode == PERMISSION_LOCATION_REQUEST_CODE) {
                Log.i("Permission", "onPermissionDenied Else")
//                findNavController().popBackStack()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.i("Permission", "onPermissionGranted ")
        _viewModel.hasPermission.postValue(true)
        mMap.isMyLocationEnabled = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_LOCATION_REQUEST_CODE) {
            if (hasLocationPermission(requireContext())){
                _viewModel.hasPermission.postValue(true)
            }else{
                requestLocationPermission(this)
            }
        }
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun setMapStyle(resource: Int) {
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

    override fun onRationaleAccepted(requestCode: Int) {
        Log.i("Permission", "onRationaleAccepted ")
        _viewModel.hasPermission.postValue(true)
    }

    override fun onRationaleDenied(requestCode: Int) {
        Log.i("Permission", "onRationaleDenied ")
    }


}
