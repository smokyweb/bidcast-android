package io.bidswipe.app.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.AuthRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.SignUpResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(val repo: AuthRepository) : ViewModel() {

    private var _signUpResponse = MutableLiveData<Resource<SignUpResponse>>()
    val signUpRepo: MutableLiveData<Resource<SignUpResponse>>
        get() = _signUpResponse

    fun signUp(
        firstName: RequestBody,
        lastName: RequestBody,
        email: RequestBody,
        password: RequestBody,
        confirmPassword: RequestBody,
    ) = viewModelScope.launch {
        _signUpResponse.value = repo.signUp(firstName, lastName, email, password, confirmPassword)
    }

    private var _loginResponse = MutableLiveData<Resource<LoginResponse>>()
    val loginRepo: MutableLiveData<Resource<LoginResponse>>
        get() = _loginResponse

    fun login(
        email: RequestBody,
        password: RequestBody,
    ) = viewModelScope.launch {
        _loginResponse.value = repo.login(email, password)
    }

    private var _forgotPasswordResponse = MutableLiveData<Resource<CommonResponse>>()
    val forgotPasswordRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _forgotPasswordResponse

    fun forgotPassword(
        email: RequestBody,
    ) = viewModelScope.launch {
        _forgotPasswordResponse.value = repo.forgotPassword(email)
    }

    private var _verifyOtpResponse = MutableLiveData<Resource<CommonResponse>>()
    val verifyOtpRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _verifyOtpResponse

    fun verifyOtp(
        email: RequestBody,
        code: RequestBody,
    ) = viewModelScope.launch {
        _verifyOtpResponse.value = repo.verifyOtp(email, code)
    }

    private var _resetPasswordResponse = MutableLiveData<Resource<CommonResponse>>()
    val resetPasswordRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _resetPasswordResponse

    fun resetPassword(
        email: RequestBody,
        password: RequestBody,
        confirmPassword: RequestBody,
    ) = viewModelScope.launch {
        _resetPasswordResponse.value = repo.resetPassword(email, password, confirmPassword)
    }

}