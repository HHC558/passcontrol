package com.hhc558.passcontrol.ui.main

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.PassControlApp
import com.hhc558.passcontrol.data.AccountView
import com.hhc558.passcontrol.ui.copyToClipboard
import com.hhc558.passcontrol.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val vm: MainViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = (context.applicationContext as PassControlApp).container

    val accounts by vm.accounts.collectAsState()
    val navigateToPreview by vm.navigateToPreview.collectAsState()
    val toast by container.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var revealed by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var deleteTarget by remember { mutableStateOf<AccountView?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFile(it) }
    }

    LaunchedEffect(navigateToPreview) {
        if (navigateToPreview) {
            vm.consumeNavigateToPreview()
            navController.navigate("import_preview")
        }
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            container.toastMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("密码管家") },
                actions = {
                    IconButton(onClick = { navController.navigate("edit?accountId=-1") }) {
                        Icon(Icons.Outlined.Add, contentDescription = "新增记录")
                    }
                    IconButton(onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/octet-stream",
                                "application/zip",
                                "*/*"
                            )
                        )
                    }) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = "导入 xlsx")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val uri = vm.exportXlsx()
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "分享账号密码备份"))
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "导出并分享")
                    }
                    IconButton(onClick = {
                        vm.logout()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "退出登录")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "暂无账号密码记录\n点击顶栏 ➕ 添加",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    PlatformCard(
                        account = account,
                        expanded = account.id in expanded,
                        revealed = account.id in revealed,
                        onToggle = {
                            expanded = if (account.id in expanded) expanded - account.id else expanded + account.id
                        },
                        onToggleReveal = {
                            revealed = if (account.id in revealed) revealed - account.id else revealed + account.id
                        },
                        onEdit = { navController.navigate("edit?accountId=${account.id}") },
                        onDelete = { deleteTarget = account },
                        context = context
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录") },
            text = { Text("确定删除「${target.platform} - ${target.username}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target)
                    deleteTarget = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

/** 平台名称卡片：默认只显示平台名称，点击展开详情。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformCard(
    account: AccountView,
    expanded: Boolean,
    revealed: Boolean,
    onToggle: () -> Unit,
    onToggleReveal: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    context: Context
) {
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    account.platform,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "账号",
                    value = account.username,
                    copyable = true,
                    onCopy = { copyToClipboard(context, "账号", account.username) }
                )
                InfoRow(
                    label = "密码",
                    value = if (revealed) account.password else "••••••••",
                    copyable = revealed,
                    onCopy = { copyToClipboard(context, "密码", account.password) },
                    trailing = {
                        IconButton(onClick = onToggleReveal, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (revealed) "隐藏密码" else "显示密码",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
                account.url?.let { url ->
                    InfoRow(label = "网址", value = url)
                }
                account.email?.let { email ->
                    InfoRow(label = "邮箱", value = email)
                }
                InfoRow(label = "创建时间", value = Formatters.formatCreatedAt(account.createdAt))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Text("编辑")
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    copyable: Boolean = false,
    onCopy: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(
            "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (copyable) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = "复制$label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        trailing?.invoke()
    }
}