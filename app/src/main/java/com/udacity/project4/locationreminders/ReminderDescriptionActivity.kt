package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }

        fun stringBuilderForReminderDetail(reminder: ReminderDataItem): String {
            val sb = StringBuilder()
            sb.append("Title :" + reminder.title + "\n")
            sb.append("Dec :" + reminder.description + "\n")
            sb.append("Loc :" +reminder.location + "\n")
            return sb.toString()
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )
        val bundle = intent.extras
        if (bundle != null) {
            val data  = bundle.get(EXTRA_ReminderDataItem) as ReminderDataItem
            Log.i("GeofenceReceiver", stringBuilderForReminderDetail(data))
            binding.reminderDataItem = data
        }else{
        }

    }
}
