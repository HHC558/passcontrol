package com.hhc558.passcontrol.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hhc558.passcontrol.PassControlApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as PassControlApp).container.vaultRepository

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun submit(password: String, confirm: String, question: String, answer: String) {
        val q = question.trim()
        val a = answer.trim()
        when {
            password.length < 6 -> { _error.value = "登录密码至少 6 位"; return }
            password != confirm -> { _error.value = "两次输入的密码不一致"; return }
            q.isEmpty() -> { _error.value = "请填写密保问题"; return }
            a.isEmpty() -> { _error.value = "请填写密保答案"; return }
        }
        _error.value = null
        repo.setup(password, q, a)
        _done.value = true
    }
}