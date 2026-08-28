package com.hhc558.passcontrol.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hhc558.passcontrol.ui.theme.PureBlack
import com.hhc558.passcontrol.ui.theme.Slate100
import com.hhc558.passcontrol.ui.theme.Slate200
import com.hhc558.passcontrol.ui.theme.Slate300
import com.hhc558.passcontrol.ui.theme.Slate400
import com.hhc558.passcontrol.ui.theme.Slate500
import com.hhc558.passcontrol.ui.theme.Slate600
import com.hhc558.passcontrol.ui.theme.Slate800
import com.hhc558.passcontrol.ui.theme.Slate900
import com.hhc558.passcontrol.ui.theme.GlassGradientEnd
import com.hhc558.passcontrol.ui.theme.GlassGradientStart

/** 毛玻璃背景：slate-100 底 + 若干柔和实心色块（经模糊），无渐变、无阴影。 */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Slate100)) {
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-50).dp)
                .size(260.dp)
                .blur(60.dp)
                .background(Slate200, CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-70).dp)
                .size(300.dp)
                .blur(70.dp)
                .background(Slate300, CircleShape)
        )
        Box(
            Modifier.align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 200.dp)
                .size(160.dp)
                .blur(50.dp)
                .background(Slate200, CircleShape)
        )
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset(x = (-90).dp, y = 140.dp)
                .size(150.dp)
                .blur(40.dp)
                .background(Slate300, CircleShape)
        )
        content()
    }
}

/** 居中毛玻璃卡片：半透明白、圆角、无边框、无阴影。 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        GlassGradientStart.copy(alpha = 0.75f),
                        GlassGradientEnd.copy(alpha = 0.75f)
                    )
                )
            )
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** 纯黑按钮：按下以 0.2s 缓动过渡到 slate-800，无阴影。 */
@Composable
fun BlackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) Slate800 else PureBlack,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "blackButtonBg"
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp)
    ) { content() }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Slate900,
    unfocusedTextColor = Slate900,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = Slate900,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedLabelColor = Slate600,
    unfocusedLabelColor = Slate500
)

/** 无边框输入框：仅靠标签与留白呈现，无分割线。 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = fieldColors()
    )
}

/** 带显隐切换的无边框密码输入框。 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleShow: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    GlassTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                    tint = Slate400
                )
            }
        }
    )
}

/** 复制文本到系统剪贴板并提示。 */
fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "${label}已复制", Toast.LENGTH_SHORT).show()
}

/** 一键打开网址（自动补全 https://），使用系统浏览器打开。 */
fun openUrl(context: Context, rawUrl: String) {
    val url = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开网址", Toast.LENGTH_SHORT).show()
    }
}