package com.hhc558.passcontrol.ui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.data.AccountView
import com.hhc558.passcontrol.util.Formatters
import com.hhc558.passcontrol.xlsx.ImportRow
import com.hhc558.passcontrol.xlsx.ModifiedItem

private val BlueAdded = Color(0xFF2563EB)
private val GreenModified = Color(0xFF16A34A)
private val RedDeleted = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(navController: NavHostController) {
    val vm: ImportPreviewViewModel = viewModel()
    val diff by vm.diff.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var leaveRequested by remember { mutableStateOf(false) }

    LaunchedEffect(diff) {
        if (diff == null && !leaveRequested) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入预览") },
                navigationIcon = {
                    IconButton(onClick = {
                        leaveRequested = true
                        vm.cancel()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val d = diff ?: return@Scaffold
        Column(Modifier.padding(padding).fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "导入预览说明",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    LegendDot(BlueAdded, "新增", "文件中存在、APP 中没有的记录")
                    LegendDot(GreenModified, "修改", "账号/密码/邮箱有变化的记录（保留原创建时间）")
                    LegendDot(RedDeleted, "删除", "APP 中存在、文件中没有的记录")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (d.added.isNotEmpty()) {
                    item { SectionHeader("新增（${d.added.size} 条）", BlueAdded) }
                    items(d.added, key = { "a${it.rowNumber}" }) { row ->
                        AddedCard(row)
                    }
                }
                if (d.modified.isNotEmpty()) {
                    item { SectionHeader("修改（${d.modified.size} 条）", GreenModified) }
                    items(d.modified, key = { "m${it.old.id}" }) { item ->
                        ModifiedCard(item)
                    }
                }
                if (d.deleted.isNotEmpty()) {
                    item { SectionHeader("删除（${d.deleted.size} 条）", RedDeleted) }
                    items(d.deleted, key = { "d${it.id}" }) { account ->
                        DeletedCard(account)
                    }
                }
                item {
                    Text(
                        "无变化：${d.unchanged} 条" +
                            if (d.skipped > 0) "，跳过无效行：${d.skipped} 条" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        leaveRequested = true
                        vm.cancel()
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消导入")
                }
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认导入")
                }
            }
        }
    }

    if (showConfirmDialog && diff != null) {
        val d = diff!!
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认导入") },
            text = {
                Text("确认后将应用以下变更：\n新增 ${d.added.size} 条\n修改 ${d.modified.size} 条\n删除 ${d.deleted.size} 条\n\n是否继续？")
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    leaveRequested = true
                    vm.confirm()
                    navController.popBackStack()
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun LegendDot(color: Color, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text("●", color = color, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            "$title：$desc",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = color)
}

@Composable
private fun PreviewCard(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun AddedCard(row: ImportRow) {
    PreviewCard("${row.platform} - ${row.username}", BlueAdded) {
        LabeledValue("账号", row.username)
        LabeledValue("密码", row.password)
        row.email?.let { LabeledValue("邮箱", it) }
        LabeledValue("创建时间", row.createdAt?.let { Formatters.formatCreatedAt(it) } ?: "导入时自动生成")
    }
}

@Composable
private fun ModifiedCard(item: ModifiedItem) {
    PreviewCard("${item.old.platform} - ${item.old.username}", GreenModified) {
        for ((field, pair) in item.changes) {
            LabeledValue("$field", "${pair.first}  →  ${pair.second}")
        }
    }
}

@Composable
private fun DeletedCard(account: AccountView) {
    PreviewCard("${account.platform} - ${account.username}", RedDeleted) {
        Text(
            account.username,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = TextDecoration.LineThrough
        )
        LabeledValue("创建时间", Formatters.formatCreatedAt(account.createdAt))
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(Modifier.padding(vertical = 1.dp)) {
        Text("$label：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}