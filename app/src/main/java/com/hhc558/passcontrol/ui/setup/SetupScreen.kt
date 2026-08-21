package com.hhc558.passcontrol.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable
fun SetupScreen(navController: NavHostController) {
    val vm: SetupViewModel = viewModel()
    val error by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(done) {
        if (done) {
            navController.navigate("main") { popUpTo("setup") { inclusive = true } }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text("设置登录密码", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "首次使用，请设置登录密码（至少 6 位）与密保问题。\n忘记密码时回答密保问题即可重置，数据不会丢失。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(24.dp))
            PasswordTextField(
                value = password,
                onValueChange = { password = it },
                label = "登录密码",
                showPassword = showPassword,
                onToggleShow = { showPassword = !showPassword }
            )
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "确认密码",
                showPassword = showPassword,
                onToggleShow = { showPassword = !showPassword }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("密保问题（例如：我的小学名称？）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("密保答案") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.submit(password, confirm, question, answer) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成设置")
            }
        }
    }
}