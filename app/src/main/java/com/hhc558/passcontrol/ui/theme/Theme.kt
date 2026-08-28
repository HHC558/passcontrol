package com.hhc558.passcontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 极简 Slate 主题：主色中性灰、按钮纯黑、无边框分割线，
 * 背景 slate-100、玻璃卡片为半透明白。
 */
private val LightColors = lightColorScheme(
    primary = PureBlack,
    onPrimary = Color.White,
    primaryContainer = Slate200,
    onPrimaryContainer = Slate900,
    secondary = Slate500,
    onSecondary = Color.White,
    background = Slate100,
    onBackground = Slate900,
    surface = GlassWhite,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun PassControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}