package com.udacity.project4.locationreminders.savereminder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.dto.GeocodeDTO
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    var geocode: GeocodeDTO? = null
    var hasLocationDetails = false

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
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }


        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            Log.i("ViewModel", location.toString())
            val serviceIntent =
                Intent(requireContext(), GeofenceTransitionsJobIntentService::class.java)
            serviceIntent.putExtra("requestId", title)
            serviceIntent.putExtra("latitude", latitude.toString())
            serviceIntent.putExtra("longitude", longitude.toString())
            GeofenceTransitionsJobIntentService.enqueueWork(requireContext(), serviceIntent)
//             2) save the reminder to the local db
            val reminder = ReminderDataItem(title, description, location, latitude, longitude)
            _viewModel.validateAndSaveReminder(reminder)

        }
        setupArgs()
    }

    private fun setupArgs() {

        if (geocode?.latitude.isNullOrEmpty() || geocode?.longitude.isNullOrEmpty() || geocode?.location.isNullOrEmpty()) {
            hasLocationDetails = false
            Log.i("Geocode12", geocode.toString())
            binding.saveReminder.isEnabled = false
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

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
