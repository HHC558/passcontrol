package com.hhc558.passcontrol.ui.main

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hhc558.passcontrol.PassControlApp
import com.hhc558.passcontrol.data.AccountView
import com.hhc558.passcontrol.util.Formatters
import com.hhc558.passcontrol.xlsx.XlsxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as PassControlApp).container
    private val repo = container.vaultRepository

    val accounts: StateFlow<List<AccountView>> = repo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _navigateToPreview = MutableStateFlow(false)
    val navigateToPreview: StateFlow<Boolean> = _navigateToPreview.asStateFlow()

    fun consumeNavigateToPreview() {
        _navigateToPreview.value = false
    }

    fun delete(account: AccountView) {
        viewModelScope.launch {
            repo.delete(account)
        }
    }

    fun logout() {
        repo.logout()
    }

    /** 导出全部记录为 xlsx，返回分享用 Uri（失败返回 null）。 */
    suspend fun exportXlsx(): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val records = repo.getAllOnce()
                val bytes = container.xlsxService.writeRecords(records)
                val file = File(container.appContext.cacheDir, "账号密码备份_${Formatters.timestampForFile()}.xlsx")
                file.writeBytes(bytes)
                FileProvider.getUriForFile(
                    container.appContext,
                    container.appContext.packageName + ".fileprovider",
                    file
                )
            } catch (e: Exception) {
                container.toastMessage.value = "导出失败：${e.message}"
                null
            }
        }
    }

    /** 选择 xlsx 文件后解析并计算差异，成功则跳转预览页。 */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    container.appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (e: Exception) {
                    null
                }
            }
            if (bytes == null || bytes.isEmpty()) {
                container.toastMessage.value = "读取文件失败，请重新选择"
                return@launch
            }
            val rows = try {
                container.xlsxService.readRows(bytes)
            } catch (e: XlsxException) {
                container.toastMessage.value = e.message ?: "解析 xlsx 失败"
                return@launch
            } catch (e: Exception) {
                container.toastMessage.value = "解析 xlsx 失败：${e.message}"
                return@launch
            }
            val current = repo.getAllOnce()
            val diff = container.diffEngine.compute(current, rows)
            container.importFlow.value = diff
            _navigateToPreview.value = true
        }
    }
}