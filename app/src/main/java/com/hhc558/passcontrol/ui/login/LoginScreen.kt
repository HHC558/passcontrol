package com.hhc558.passcontrol.ui.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.ui.PasswordTextField

@Composable
fun LoginScreen(navController: NavHostController) {
    val vm: LoginViewModel = viewModel()
    val error by vm.error.collectAsState()
    val success by vm.success.collectAsState()

    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(success) {
        if (success) {
            navController.navigate("main") { popUpTo("login") { inclusive = true } }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))
            Text("密码管家", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "请输入登录密码进入账号密码管理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(32.dp))
            PasswordTextField(
                value = password,
                onValueChange = { password = it },
                label = "登录密码",
                showPassword = showPassword,
                onToggleShow = { showPassword = !showPassword }
            )
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.login(password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("forgot") }) {
                Text("忘记密码？")
            }
        }
    }
}