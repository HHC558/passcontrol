package com.hhc558.passcontrol.util

/** 轻量 Base64 编解码，纯 Kotlin 实现，兼容 JVM 单元测试与 Android。 */
object B64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(data: ByteArray): String {
        val sb = StringBuilder((data.size + 2) / 3 * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else -1
            sb.append(ALPHABET[b0 ushr 2])
            sb.append(ALPHABET[((b0 shl 4) or (if (b1 >= 0) b1 ushr 4 else 0)) and 0x3F])
            sb.append(if (b1 >= 0) ALPHABET[((b1 shl 2) or (if (b2 >= 0) b2 ushr 6 else 0)) and 0x3F] else '=')
            sb.append(if (b2 >= 0) ALPHABET[b2 and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    fun decode(s: String): ByteArray {
        val input = s.trim()
        require(input.length % 4 == 0) { "非法 Base64 长度" }
        val out = java.io.ByteArrayOutputStream(input.length / 4 * 3)
        var i = 0
        while (i < input.length) {
            val c0 = charIndex(input[i])
            val c1 = charIndex(input[i + 1])
            val c2 = if (input[i + 2] == '=') -1 else charIndex(input[i + 2])
            val c3 = if (input[i + 3] == '=') -1 else charIndex(input[i + 3])
            val triple = (c0 shl 18) or (c1 shl 12) or (if (c2 >= 0) c2 shl 6 else 0) or (if (c3 >= 0) c3 else 0)
            out.write((triple ushr 16) and 0xFF)
            if (c2 >= 0) out.write((triple ushr 8) and 0xFF)
            if (c3 >= 0) out.write(triple and 0xFF)
            i += 4
        }
        return out.toByteArray()
    }

    private fun charIndex(c: Char): Int {
        val idx = ALPHABET.indexOf(c)
        require(idx >= 0) { "非法 Base64 字符: $c" }
        return idx
    }
}