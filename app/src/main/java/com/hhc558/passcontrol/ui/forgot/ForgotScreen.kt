package com.hhc558.passcontrol.ui.forgot

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.ui.PasswordTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotScreen(navController: NavHostController) {
    val vm: ForgotViewModel = viewModel()
    val question = remember { vm.question }
    val error by vm.error.collectAsState()
    val verified by vm.verified.collectAsState()
    val done by vm.done.collectAsState()

    var answer by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }

    LaunchedEffect(done) {
        if (done) {
            navController.navigate("main") { popUpTo("login") { inclusive = true } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("忘记密码") },
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
            if (!verified) {
                Text("请回答密保问题以验证身份", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                Text("密保问题", style = MaterialTheme.typography.labelLarge)
                Text(question ?: "未设置", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("密保答案") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { vm.verify(answer) }, modifier = Modifier.fillMaxWidth()) {
                    Text("验证")
                }
            } else {
                Text("验证成功，请设置新密码", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                PasswordTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "新密码（至少 6 位）",
                    showPassword = showNew,
                    onToggleShow = { showNew = !showNew }
                )
                Spacer(Modifier.height(12.dp))
                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "确认新密码",
                    showPassword = showNew,
                    onToggleShow = { showNew = !showNew }
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { vm.reset(newPassword, confirmPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重置并登录")
                }
            }
        }
    }
}