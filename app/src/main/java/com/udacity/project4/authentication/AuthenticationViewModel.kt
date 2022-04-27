package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.udacity.project4.utils.FireBaseUserLiverData

class AuthenticationViewModel: ViewModel() {
    enum class AuthenticationState{
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState = FireBaseUserLiverData().map { user ->
        if (user != null){
            AuthenticationState.AUTHENTICATED
        }else{
            AuthenticationState.UNAUTHENTICATED
        }
    }
}