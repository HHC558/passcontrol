package com.hhc558.passcontrol.ui.main

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hhc558.passcontrol.PassControlApp
import com.hhc558.passcontrol.data.AccountView
import com.hhc558.passcontrol.ui.BlackButton
import com.hhc558.passcontrol.ui.GlassBackground
import com.hhc558.passcontrol.ui.GlassCard
import com.hhc558.passcontrol.ui.copyToClipboard
import com.hhc558.passcontrol.ui.openUrl
import com.hhc558.passcontrol.ui.theme.ErrorRed
import com.hhc558.passcontrol.ui.theme.GradientBlue
import com.hhc558.passcontrol.ui.theme.GradientPurple
import com.hhc558.passcontrol.ui.theme.LightBlueContentEnd
import com.hhc558.passcontrol.ui.theme.LightBlueContentStart
import com.hhc558.passcontrol.ui.theme.Slate400
import com.hhc558.passcontrol.ui.theme.Slate500
import com.hhc558.passcontrol.ui.theme.Slate600
import com.hhc558.passcontrol.ui.theme.Slate700
import com.hhc558.passcontrol.ui.theme.Slate900
import com.hhc558.passcontrol.util.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    var lastExpandedId by remember { mutableStateOf<Long?>(null) }
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

    // 智能滚动：展开时逐帧跟随动画同步平滑滚动（零等待、连贯），收起时恢复列表位置
    LaunchedEffect(expandedId) {
        val current = expandedId
        if (current != null) {
            lastExpandedId = current
            var lastSize = -1
            var stableFrames = 0
            var frames = 0
            while (true) {
                if (++frames > 600) break // 安全上限
                val layout = listState.layoutInfo
                val item = layout.visibleItemsInfo.firstOrNull { it.key == current } ?: break
                val viewportStart = layout.viewportStartOffset
                val viewportEnd = layout.viewportEndOffset
                val top = item.offset
                val bottom = top + item.size
                val overflow = bottom - viewportEnd
                val sizeStable = item.size == lastSize
                lastSize = item.size
                if (overflow > 0) {
                    // 底部超出：同步上滑，让卡片底部始终贴齐屏幕底部（与展开动画同一节奏）
                    stableFrames = 0
                    listState.scroll { scrollBy(overflow.toFloat()) }
                } else if (sizeStable) {
                    stableFrames++
                    if (stableFrames >= 3) {
                        // 尺寸已稳定且完整可见；若顶部被裁则下拉回顶
                        val overTop = viewportStart - top
                        if (overTop > 1) {
                            listState.scroll { scrollBy(overTop.toFloat()) }
                        }
                        break
                    }
                } else {
                    stableFrames = 0
                }
                withFrameNanos { }
            }
        } else {
            val prev = lastExpandedId ?: return@LaunchedEffect
            waitForStableSize(listState, prev)
            val layout = listState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.key == prev }
            if (item == null || item.offset > layout.viewportStartOffset + 120) {
                val index = accounts.indexOfFirst { it.id == prev }
                if (index >= 0) listState.animateScrollToItem(index)
            }
        }
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            container.toastMessage.value = null
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            GlassCard(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                contentPadding = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "密码管家",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { navController.navigate("edit?accountId=-1") }) {
                        Icon(Icons.Outlined.Add, contentDescription = "新增记录", tint = Slate700)
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
                        Icon(Icons.Outlined.FileUpload, contentDescription = "导入 xlsx", tint = Slate700)
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
                        Icon(Icons.Outlined.Share, contentDescription = "导出并分享", tint = Slate700)
                    }
                    IconButton(onClick = {
                        vm.logout()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "退出登录", tint = Slate700)
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (accounts.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "暂无账号密码记录\n点击右上角 ➕ 添加",
                            fontSize = 14.sp,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(accounts, key = { it.id }) { account ->
                            PlatformCard(
                                account = account,
                                expanded = expandedId == account.id,
                                revealed = account.id in revealed,
                                onToggle = { expandedId = if (expandedId == account.id) null else account.id },
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
                    Text("删除", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

/** 平台名称卡片：蓝紫渐变标题栏 + 展开后淡蓝渐变内容区（分层明显）。 */
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
    val corner by animateDpAsState(
        targetValue = if (expanded) 24.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "platformCorner"
    )
    val elevation by animateDpAsState(
        targetValue = if (expanded) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "platformElevation"
    )
    val shape = RoundedCornerShape(corner)

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = GradientPurple.copy(alpha = 0.35f),
                spotColor = GradientBlue.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(GradientBlue, GradientPurple)))
            .clickable(onClick = onToggle)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
            .padding(16.dp)
    ) {
        Column {
            // 标题栏（蓝紫渐变）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    account.platform,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                // 内容区（淡蓝渐变，分层明显）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(LightBlueContentStart, LightBlueContentEnd)
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column {
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
                                        tint = Slate600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        account.url?.let { url ->
                            InfoRow(
                                label = "网址",
                                value = url,
                                trailing = {
                                    IconButton(onClick = { openUrl(context, url) }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.OpenInNew,
                                            contentDescription = "打开网址",
                                            tint = Slate600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(onClick = { copyToClipboard(context, "网址", url) }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = "复制网址",
                                            tint = Slate600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                        account.email?.let { email ->
                            InfoRow(label = "邮箱", value = email)
                        }
                        InfoRow(label = "创建时间", value = Formatters.formatCreatedAt(account.createdAt))
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BlackButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                                Text("编辑", fontSize = 15.sp)
                            }
                            TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                                Text(
                                    "删除",
                                    fontSize = 15.sp,
                                    color = ErrorRed,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Text(label, fontSize = 13.sp, color = Slate500, modifier = Modifier.width(60.dp))
        Text(value, fontSize = 14.sp, color = Slate900, modifier = Modifier.weight(1f))
        if (copyable) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = "复制$label",
                    tint = Slate600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        trailing?.invoke()
    }
}

/** 等待 LazyColumn 中某条目尺寸动画稳定（连续 3 次采样无变化），用于展开/收起后的智能滚动。 */
private suspend fun waitForStableSize(listState: LazyListState, key: Any) {
    var lastBottom = -1
    var stableCount = 0
    var tries = 0
    while (tries < 40) {
        delay(50)
        tries++
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        val bottom = item.offset + item.size
        if (bottom == lastBottom) {
            stableCount++
            if (stableCount >= 3) return
        } else {
            stableCount = 0
        }
        lastBottom = bottom
    }
    delay(150)
}

