package com.hhc558.passcontrol.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hhc558.passcontrol.PassControlApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as PassControlApp).container.vaultRepository

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun login(password: String) {
        if (repo.login(password)) {
            _error.value = null
            _success.value = true
        } else {
            _error.value = "密码错误，请重试"
        }
    }
}