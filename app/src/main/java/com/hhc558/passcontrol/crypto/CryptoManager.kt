package com.hhc558.passcontrol.crypto

import com.hhc558.passcontrol.util.B64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密工具：PBKDF2-HMAC-SHA256 密钥派生 + AES-256-GCM 加密。
 * 纯 javax.crypto 实现，不依赖 Android Keystore，便于 JVM 单元测试。
 */
class CryptoManager {

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }

    fun generateKey(): ByteArray = randomBytes(32)

    /** PBKDF2-HMAC-SHA256（默认 10 万次迭代，输出 32 字节） */
    fun pbkdf2(password: String, salt: ByteArray, iterations: Int = 100_000): ByteArray {
        require(iterations > 0) { "iterations 必须大于 0" }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val input = salt + byteArrayOf(0, 0, 0, 1) // 单块（dkLen <= 32），块索引 1
        var u = mac.doFinal(input)
        val t = u.copyOf()
        repeat(iterations - 1) {
            u = mac.doFinal(u)
            for (i in t.indices) {
                t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
            }
        }
        return t
    }

    /** AES-256-GCM 加密，返回 "v1:" + base64(iv + ciphertext) */
    fun encrypt(key: ByteArray, plaintext: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = randomBytes(12)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plaintext)
        return "v1:" + B64.encode(iv + ct)
    }

    /** 解密 [payload]，格式由 [encrypt] 产生 */
    fun decrypt(key: ByteArray, payload: String): ByteArray {
        val raw = B64.decode(payload.removePrefix("v1:"))
        require(raw.size > 12) { "密文长度非法" }
        val iv = raw.copyOfRange(0, 12)
        val ct = raw.copyOfRange(12, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)
}