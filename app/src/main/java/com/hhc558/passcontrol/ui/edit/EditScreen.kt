package com.hhc558.passcontrol.ui.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.ui.PasswordTextField
import com.hhc558.passcontrol.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(navController: NavHostController, accountId: Long) {
    val vm: EditViewModel = viewModel()
    val account by vm.account.collectAsState()
    val error by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    var platform by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load(accountId) }

    LaunchedEffect(account) {
        account?.let {
            platform = it.platform
            username = it.username
            password = it.password
            email = it.email ?: ""
        }
    }

    LaunchedEffect(done) {
        if (done) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (accountId <= 0) "新增记录" else "编辑记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("平台名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("账号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                value = password,
                onValueChange = { password = it },
                label = "密码",
                showPassword = showPassword,
                onToggleShow = { showPassword = !showPassword }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱（选填）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            account?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    "创建时间：${Formatters.formatCreatedAt(it.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.save(accountId, platform, username, password, email) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}