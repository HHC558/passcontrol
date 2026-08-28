package com.hhc558.passcontrol.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账号密码记录。密码以 AES-GCM 加密字符串落库，永不存明文。
 * 结构：平台名称、账号、密码、网址（可空）、邮箱（可空）、创建时间（自动生成）。
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val username: String,
    val passwordEncrypted: String,
    val url: String?,
    val email: String?,
    val createdAt: Long
)