package com.hhc558.passcontrol.ui.preview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hhc558.passcontrol.PassControlApp
import com.hhc558.passcontrol.xlsx.ImportDiff
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as PassControlApp).container
    private val repo = container.vaultRepository

    val diff: StateFlow<ImportDiff?> = container.importFlow.asStateFlow()

    fun cancel() {
        container.importFlow.value = null
    }

    fun confirm() {
        val d = container.importFlow.value ?: return
        viewModelScope.launch {
            repo.applyImport(d)
            container.importFlow.value = null
            container.toastMessage.value =
                "导入成功：新增 ${d.added.size} / 修改 ${d.modified.size} / 删除 ${d.deleted.size}"
        }
    }
}