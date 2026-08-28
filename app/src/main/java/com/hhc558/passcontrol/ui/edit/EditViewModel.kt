package com.hhc558.passcontrol.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hhc558.passcontrol.PassControlApp
import com.hhc558.passcontrol.data.AccountView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as PassControlApp).container.vaultRepository

    private val _account = MutableStateFlow<AccountView?>(null)
    val account: StateFlow<AccountView?> = _account.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun load(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            _account.value = repo.getById(id)
        }
    }

    /** 保存前校验；不合法时设置错误信息并返回 false。 */
    fun validate(platform: String, username: String, password: String): Boolean {
        val err = when {
            platform.isBlank() -> "请填写平台名称"
            username.isBlank() -> "请填写账号"
            password.isBlank() -> "请填写密码"
            else -> null
        }
        _error.value = err
        return err == null
    }

    fun save(id: Long, platform: String, username: String, password: String, url: String, email: String) {
        when {
            platform.isBlank() -> { _error.value = "请填写平台名称"; return }
            username.isBlank() -> { _error.value = "请填写账号"; return }
            password.isBlank() -> { _error.value = "请填写密码"; return }
        }
        _error.value = null
        viewModelScope.launch {
            val urlTrim = url.trim().takeIf { it.isNotEmpty() }
            val emailTrim = email.trim().takeIf { it.isNotEmpty() }
            if (id <= 0) {
                repo.add(platform, username, password, urlTrim, emailTrim)
            } else {
                repo.update(id, platform, username, password, urlTrim, emailTrim)
            }
            _done.value = true
        }
    }
}