package com.hhc558.passcontrol.ui.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.ui.BlackButton
import com.hhc558.passcontrol.ui.GlassBackground
import com.hhc558.passcontrol.ui.GlassCard
import com.hhc558.passcontrol.ui.GlassTextField
import com.hhc558.passcontrol.ui.PasswordTextField
import com.hhc558.passcontrol.ui.theme.ErrorRed
import com.hhc558.passcontrol.ui.theme.Slate500
import com.hhc558.passcontrol.ui.theme.Slate900
import com.hhc558.passcontrol.util.Formatters

@Composable
fun EditScreen(navController: NavHostController, accountId: Long) {
    val vm: EditViewModel = viewModel()
    val account by vm.account.collectAsState()
    val error by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    var platform by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load(accountId) }

    LaunchedEffect(account) {
        account?.let {
            platform = it.platform
            username = it.username
            password = it.password
            url = it.url ?: ""
            email = it.email ?: ""
        }
    }

    LaunchedEffect(done) {
        if (done) navController.popBackStack()
    }

    SaveConfirmDialog(
        show = showSaveDialog,
        onConfirm = {
            showSaveDialog = false
            vm.save(accountId, platform, username, password, url, email)
        },
        onDismiss = { showSaveDialog = false }
    )

    GlassBackground {
        GlassCard(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentPadding = 20.dp
        ) {
            Row_Back(navController, if (accountId <= 0) "新增记录" else "编辑记录")
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                GlassTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = "平台名称"
                )
                Spacer(Modifier.height(20.dp))
                GlassTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "账号"
                )
                Spacer(Modifier.height(20.dp))
                PasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "密码",
                    showPassword = showPassword,
                    onToggleShow = { showPassword = !showPassword }
                )
                Spacer(Modifier.height(20.dp))
                GlassTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "网址（选填）",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Spacer(Modifier.height(20.dp))
                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "邮箱（选填）",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                account?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "创建时间：${Formatters.formatCreatedAt(it.createdAt)}",
                        fontSize = 13.sp,
                        color = Slate500
                    )
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, fontSize = 13.sp, color = ErrorRed)
                }
                Spacer(Modifier.height(28.dp))
                BlackButton(
                    onClick = {
                        if (vm.validate(platform, username, password)) showSaveDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun Row_Back(navController: NavHostController, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = Slate900)
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}

@Composable
private fun SaveConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("确认保存") },
            text = { Text("确认保存本次修改吗？") },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }
}