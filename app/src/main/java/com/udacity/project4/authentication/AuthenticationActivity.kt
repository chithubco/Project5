package com.udacity.project4.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.udacity.project4.R
import com.udacity.project4.utils.Constants.SIGN_IN_REQUEST_CODE
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var mViewModel: AuthenticationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        val loginButton: Button = findViewById(R.id.btn_login)
        loginButton.setOnClickListener {
            launchSigInFlow()
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)
        observeAuthenticationState()
    }

    private fun observeAuthenticationState() {
        mViewModel = ViewModelProvider(this).get(AuthenticationViewModel::class.java)
        mViewModel.authenticationState.observe(this, Observer { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    Log.i("MainFragment","Successfully singened in ${FirebaseAuth.getInstance().currentUser?.displayName}")
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                }
                else -> {
                    Log.i("MainFragment","Successfully singened in ${FirebaseAuth.getInstance().currentUser?.displayName}")
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.i(
                    "MainFragment",
                    "Successfully singned in ${FirebaseAuth.getInstance().currentUser?.displayName}"
                )
                val intent = Intent(this, RemindersActivity::class.java).apply {

                }
                startActivity(intent)

            } else {
                Log.i("MainFragment", "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun launchSigInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(), SIGN_IN_REQUEST_CODE
        )
    }
}
