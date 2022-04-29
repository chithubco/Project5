package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.data.dto.GeocodeDTO
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.Constants.PERMISSION_BACKGROND_LOCATION_REQUEST_CODE
import com.udacity.project4.utils.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.udacity.project4.utils.Permissions.hasLocationPermission
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.io.IOException
import java.lang.StringBuilder

class SelectLocationFragment : BaseFragment(), GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnPoiClickListener {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var mMap: GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var intendedLatLong: LatLng
    private lateinit var selectedReminder: ReminderDTO

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        setUpObservers()
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
        checkLocationPermission()
        mMap.setOnMarkerClickListener(this)
        mMap.setOnPoiClickListener(this)
        mMap.setOnMyLocationButtonClickListener(this)
        onMapClicked()
        onMapLongClick()
        setMapStyle(mMap)
//        addListeners()

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

//        TODO: add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
//        TODO: zoom to the user location after taking his permission
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location

        binding.btnRequestPermission.setOnClickListener {
            checkLocationPermission()
        }

        return binding.root
    }


    @SuppressLint("MissingPermission")
    private fun setUpObservers() {
        _viewModel.hasPermission.observe(viewLifecycleOwner, Observer { hasPermission ->
            Log.i("Permission", hasPermission.toString())
            if (hasPermission) {
                mMap.isMyLocationEnabled = true
                binding.btnRequestPermission.visibility = View.GONE
                zoomIntoLastKnowPostion()
            } else {
                binding.btnRequestPermission.visibility = View.VISIBLE
            }

        })
    }


    private fun checkLocationPermission() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val builder = AlertDialog.Builder(requireContext())
                builder
                    .setMessage(getString(R.string.rational_mesasge))
                    .setTitle("Permission Required")
                    .setPositiveButton("OK") { dialog, id ->
                        makePermissionRequest()
                    }.setNegativeButton("Cancel") { dialog, id ->
                        Log.i("Permission", "Location Permission Cancel")
                        findNavController().popBackStack()
                    }

                val dialog = builder.create().show()

            } else {
                makePermissionRequest()
            }
        } else {
            Log.i("Permission", "Location Permission Granted")
            _viewModel.hasPermission.postValue(true)

        }
    }

    private fun checkBackgroundPermission() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            ) {
                val builder = AlertDialog.Builder(requireContext())
                builder
                    .setMessage(getString(R.string.rational_mesasge))
                    .setTitle("Permission Required")
                    .setPositiveButton("OK") { dialog, id ->
                        makeBackgroundPermissionRequest()
                    }.setNegativeButton("Cancel") { dialog, id ->
                        Toast.makeText(requireContext(), "Permission Needed", Toast.LENGTH_LONG)
                    }

                val dialog = builder.create().show()

            } else {
                makeBackgroundPermissionRequest()
            }
        } else {
            // Performs some action with background location
            Log.i("Permission", "Backgroud Permission Granted")
        }
    }


    private fun makePermissionRequest() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_LOCATION_REQUEST_CODE
        )
    }

    private fun makeBackgroundPermissionRequest() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            PERMISSION_BACKGROND_LOCATION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    findNavController().popBackStack()
                } else {
                    Log.i("Permission", "Permission Granted")
                    _viewModel.hasPermission.postValue(true)
                }
            }
            PERMISSION_BACKGROND_LOCATION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("Permission", "Permission Not Granted")
                } else {
                    Log.i("Permission", "Permission Granted")
                }
            }
        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    private fun zoomIntoLastKnowPostion() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                val position = location?.let { LatLng(it.latitude, it.longitude) }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))
            }
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
        zoomIntoLastKnowPostion()
        return false
    }

    override fun onPoiClick(poi: PointOfInterest?) {
//        Log.i("POI", poi?.latLng?.latitude.toString())
//        Log.i("POI", poi?.latLng?.longitude.toString())
//        Log.i("POI", poi?.name.toString())
//        Log.i("POI", poi?.placeId.toString())

        checkBackgroundPermission()

        var geocode = GeocodeDTO(poi?.name.toString(),poi?.latLng?.latitude.toString(),poi?.latLng?.longitude.toString())

        val stringBuilder = StringBuilder()
        stringBuilder.append("Location details : ${poi?.name.toString()}")
        stringBuilder.append(" would be added to you reminder")

        val builder = AlertDialog.Builder(requireContext())
        builder
            .setMessage(stringBuilder.toString())
            .setTitle("Add Location Details")
            .setPositiveButton("Add Reminder") { dialog, id ->
                // Go pack to add reminder screen
                val action = SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment(geocode)
                findNavController().navigate(action)
            }.setNegativeButton("Dismiss") { dialog, id ->
            }.create().show()
    }

    private fun onMapClicked() {
        mMap.setOnMapClickListener {
            makePermissionRequest()
        }
    }

    /*
    on Map Long Click
     */
    private fun onMapLongClick() {
        mMap.setOnMapLongClickListener {
            checkBackgroundPermission()
            val address = reverseGeocodeLocation(it.latitude,it.longitude)

            var geocode = GeocodeDTO(address?.get(0)?.getAddressLine(0),it.latitude.toString(),it.longitude.toString())

            val stringBuilder = StringBuilder()
            stringBuilder.append("Location details : ${address?.get(0)?.getAddressLine(0)}")
            stringBuilder.append(" ${address?.get(0)?.adminArea}")
            stringBuilder.append(" would be added to you reminder")

            val builder = AlertDialog.Builder(requireContext())
            builder
                .setMessage(stringBuilder.toString())
                .setTitle("Add Location Details")
                .setPositiveButton("Add Reminder") { dialog, id ->
                    // Go pack to add reminder screen
                    val action = SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment(geocode)
                    findNavController().navigate(action)

                }.setNegativeButton("Dismiss") { dialog, id ->
                    //
                }.create().show()
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

    @SuppressLint("MissingPermission")
    private fun reverseGeocodeLocation(latitude: Double,longitude: Double) : List<Address>? {
        var geocodeMatches: List<Address>? = null
        val address1: String?
        val address2: String?
        val state: String?
        val zipCode: String?
        val Country: String?

        try {
            geocodeMatches =
                Geocoder(requireContext()).getFromLocation(latitude, longitude, 1)
        }catch (e:IOException){
            e.printStackTrace()
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


}
