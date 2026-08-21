package com.hhc558.passcontrol.ui.forgot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hhc558.passcontrol.PassControlApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForgotViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as PassControlApp).container.vaultRepository

    val question: String? = repo.getSecurityQuestion()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _verified = MutableStateFlow(false)
    val verified: StateFlow<Boolean> = _verified.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun verify(answer: String) {
        if (repo.verifyAnswer(answer.trim())) {
            _error.value = null
            _verified.value = true
        } else {
            _error.value = "密保答案不正确"
        }
    }

    fun reset(newPassword: String, confirm: String) {
        when {
            newPassword.length < 6 -> { _error.value = "新密码至少 6 位"; return }
            newPassword != confirm -> { _error.value = "两次输入的密码不一致"; return }
        }
        _error.value = null
        repo.resetPassword(newPassword)
        _done.value = true
    }
}