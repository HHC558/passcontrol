package com.hhc558.passcontrol.data

/** 界面层账号模型：密码已解密为明文（仅内存中存在）。 */
data class AccountView(
    val id: Long,
    val platform: String,
    val username: String,
    val password: String,
    val email: String?,
    val createdAt: Long
)