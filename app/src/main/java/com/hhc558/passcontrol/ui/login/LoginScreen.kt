package com.hhc558.passcontrol.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.ui.BlackButton
import com.hhc558.passcontrol.ui.GlassBackground
import com.hhc558.passcontrol.ui.GlassCard
import com.hhc558.passcontrol.ui.PasswordTextField
import com.hhc558.passcontrol.ui.theme.ErrorRed
import com.hhc558.passcontrol.ui.theme.GradientBlue
import com.hhc558.passcontrol.ui.theme.GradientPurple
import com.hhc558.passcontrol.ui.theme.Slate500
import com.hhc558.passcontrol.ui.theme.Slate900

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

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            ) {
                // J 字徽章：蓝紫渐变圆形 + 白色粗体 J
                Box(
                    Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GradientBlue, GradientPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "J",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "密码管家",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "请输入登录密码进入账号密码管理",
                    fontSize = 14.sp,
                    color = Slate500,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(36.dp))
                PasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "登录密码",
                    showPassword = showPassword,
                    onToggleShow = { showPassword = !showPassword }
                )
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, fontSize = 13.sp, color = ErrorRed)
                }
                Spacer(Modifier.height(36.dp))
                BlackButton(
                    onClick = { vm.login(password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { navController.navigate("forgot") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("忘记密码？", fontSize = 14.sp, color = Slate500)
                }
            }
        }
    }
}