package com.hhc558.passcontrol.data

import android.content.Context
import android.content.SharedPreferences
import com.hhc558.passcontrol.crypto.CryptoManager
import com.hhc558.passcontrol.util.B64
import com.hhc558.passcontrol.xlsx.ImportDiff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 保险库仓库：负责登录/密保/重置的密钥包裹与账号 CRUD 的加解密。
 *
 * 设计：随机生成 256 位保险库密钥 K；
 * 用 PBKDF2(登录密码) 与 PBKDF2(密保答案) 分别派生 KEK 并把 K 双重包裹保存。
 * 登录验证 PBKDF2 哈希；忘记密码时答对密保即可解包 K，再设新密码重新包裹，数据不丢。
 */
class VaultRepository(
    private val prefs: SharedPreferences,
    private val crypto: CryptoManager,
    private val accountDao: AccountDao
) {
    private var vaultKey: ByteArray? = null

    val isAuthenticated: Boolean get() = vaultKey != null

    fun isInitialized(): Boolean = prefs.contains(KEY_LOGIN_HASH)

    fun getSecurityQuestion(): String? = prefs.getString(KEY_QUESTION, null)

    /** 首次设置：登录密码 + 密保问题/答案，成功后自动登录。 */
    fun setup(masterPassword: String, question: String, answer: String) {
        require(!isInitialized()) { "保险库已初始化" }
        val key = crypto.generateKey()
        val saltP = crypto.randomBytes(16)
        val saltA = crypto.randomBytes(16)
        val saltLogin = crypto.randomBytes(16)
        val kekP = crypto.pbkdf2(masterPassword, saltP)
        val kekA = crypto.pbkdf2(answer, saltA)
        prefs.edit()
            .putString(KEY_QUESTION, question.trim())
            .putString(KEY_SALT_P, B64.encode(saltP))
            .putString(KEY_SALT_A, B64.encode(saltA))
            .putString(KEY_SALT_LOGIN, B64.encode(saltLogin))
            .putString(KEY_WRAPPED_P, crypto.encrypt(kekP, key))
            .putString(KEY_WRAPPED_A, crypto.encrypt(kekA, key))
            .putString(KEY_LOGIN_HASH, B64.encode(crypto.pbkdf2(masterPassword, saltLogin)))
            .apply()
        vaultKey = key
    }

    /** 登录：校验密码哈希，成功后解包 K 并进入已登录状态。 */
    fun login(masterPassword: String): Boolean {
        val saltLogin = prefs.getString(KEY_SALT_LOGIN, null)?.let { B64.decode(it) } ?: return false
        val expected = prefs.getString(KEY_LOGIN_HASH, null)?.let { B64.decode(it) } ?: return false
        val actual = crypto.pbkdf2(masterPassword, saltLogin)
        if (!crypto.constantTimeEquals(expected, actual)) return false
        val saltP = prefs.getString(KEY_SALT_P, null)?.let { B64.decode(it) } ?: return false
        val wrapped = prefs.getString(KEY_WRAPPED_P, null) ?: return false
        val key = try {
            crypto.decrypt(crypto.pbkdf2(masterPassword, saltP), wrapped)
        } catch (e: Exception) {
            return false
        }
        vaultKey = key
        return true
    }

    /** 校验密保答案；正确时解包 K 并进入已登录状态（供重置密码使用）。 */
    fun verifyAnswer(answer: String): Boolean {
        val saltA = prefs.getString(KEY_SALT_A, null)?.let { B64.decode(it) } ?: return false
        val wrapped = prefs.getString(KEY_WRAPPED_A, null) ?: return false
        val key = try {
            crypto.decrypt(crypto.pbkdf2(answer, saltA), wrapped)
        } catch (e: Exception) {
            return false
        }
        vaultKey = key
        return true
    }

    /** 重置登录密码（需先通过 [verifyAnswer] 或已登录），数据不丢失。 */
    fun resetPassword(newPassword: String) {
        val key = vaultKey ?: throw IllegalStateException("未授权：请先验证密保答案")
        val saltP = crypto.randomBytes(16)
        val saltLogin = crypto.randomBytes(16)
        prefs.edit()
            .putString(KEY_SALT_P, B64.encode(saltP))
            .putString(KEY_SALT_LOGIN, B64.encode(saltLogin))
            .putString(KEY_WRAPPED_P, crypto.encrypt(crypto.pbkdf2(newPassword, saltP), key))
            .putString(KEY_LOGIN_HASH, B64.encode(crypto.pbkdf2(newPassword, saltLogin)))
            .apply()
    }

    fun logout() {
        vaultKey = null
    }

    fun observeAccounts(): Flow<List<AccountView>> =
        accountDao.observeAll().map { list -> list.map { it.toView() } }

    suspend fun getAllOnce(): List<AccountView> = accountDao.getAll().map { it.toView() }

    suspend fun getById(id: Long): AccountView? = accountDao.getById(id)?.toView()

    suspend fun add(platform: String, username: String, password: String, email: String?) {
        val key = requireKey()
        accountDao.insert(
            Account(
                platform = platform.trim(),
                username = username.trim(),
                passwordEncrypted = crypto.encrypt(key, password.toByteArray(Charsets.UTF_8)),
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun update(id: Long, platform: String, username: String, password: String, email: String?) {
        val existing = accountDao.getById(id) ?: return
        val key = requireKey()
        accountDao.update(
            existing.copy(
                platform = platform.trim(),
                username = username.trim(),
                passwordEncrypted = crypto.encrypt(key, password.toByteArray(Charsets.UTF_8)),
                email = email?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun delete(account: AccountView) {
        accountDao.deleteById(account.id)
    }

    /** 应用导入差异：新增插入、修改覆盖（保留原 createdAt）、删除移除。 */
    suspend fun applyImport(diff: ImportDiff) {
        val key = requireKey()
        val now = System.currentTimeMillis()
        for (row in diff.added) {
            accountDao.insert(
                Account(
                    platform = row.platform,
                    username = row.username,
                    passwordEncrypted = crypto.encrypt(key, row.password.toByteArray(Charsets.UTF_8)),
                    email = row.email,
                    createdAt = row.createdAt ?: now
                )
            )
        }
        for (item in diff.modified) {
            accountDao.update(
                item.old.toEntity(
                    passwordEncrypted = crypto.encrypt(key, item.new.password.toByteArray(Charsets.UTF_8)),
                    email = item.new.email
                )
            )
        }
        for (deleted in diff.deleted) {
            accountDao.deleteById(deleted.id)
        }
    }

    private fun requireKey(): ByteArray = vaultKey ?: throw IllegalStateException("未登录")

    private fun Account.toView(): AccountView {
        val key = requireKey()
        val plain = String(crypto.decrypt(key, passwordEncrypted), Charsets.UTF_8)
        return AccountView(id, platform, username, plain, email, createdAt)
    }

    private fun AccountView.toEntity(passwordEncrypted: String, email: String?): Account =
        Account(id, platform, username, passwordEncrypted, email, createdAt)

    private companion object {
        const val KEY_QUESTION = "security_question"
        const val KEY_SALT_P = "salt_password"
        const val KEY_SALT_A = "salt_answer"
        const val KEY_SALT_LOGIN = "salt_login"
        const val KEY_WRAPPED_P = "wrapped_password"
        const val KEY_WRAPPED_A = "wrapped_answer"
        const val KEY_LOGIN_HASH = "login_hash"
    }
}