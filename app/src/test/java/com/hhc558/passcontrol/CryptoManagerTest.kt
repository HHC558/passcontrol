package com.hhc558.passcontrol

import com.hhc558.passcontrol.crypto.CryptoManager
import com.hhc558.passcontrol.util.B64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CryptoManagerTest {
    private val crypto = CryptoManager()

    @Test
    fun base64KnownVector() {
        assertEquals("SGVsbG8=", B64.encode("Hello".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun base64RoundTripWithUnicodeAndZero() {
        val data = "hello 世界 \u0000 test".toByteArray(Charsets.UTF_8)
        assertArrayEquals(data, B64.decode(B64.encode(data)))
    }

    @Test
    fun pbkdf2Deterministic() {
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        assertArrayEquals(crypto.pbkdf2("password", salt), crypto.pbkdf2("password", salt))
        assertEquals(32, crypto.pbkdf2("password", salt).size)
    }

    @Test
    fun pbkdf2SensitiveToPasswordAndSalt() {
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        assertFalse(crypto.pbkdf2("password", salt).contentEquals(crypto.pbkdf2("password1", salt)))
        assertFalse(
            crypto.pbkdf2("password", salt).contentEquals(
                crypto.pbkdf2("password", byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1))
            )
        )
    }

    @Test
    fun encryptDecryptRoundTrip() {
        val key = crypto.generateKey()
        val plain = "P@ssw0rd!中文<&>".toByteArray(Charsets.UTF_8)
        val enc = crypto.encrypt(key, plain)
        assertArrayEquals(plain, crypto.decrypt(key, enc))
    }

    @Test
    fun encryptedPayloadsAreUnique() {
        val key = crypto.generateKey()
        val p = "same".toByteArray(Charsets.UTF_8)
        assertNotEquals(crypto.encrypt(key, p), crypto.encrypt(key, p))
    }

    @Test
    fun decryptWithWrongKeyFails() {
        val key = crypto.generateKey()
        val other = crypto.generateKey()
        val enc = crypto.encrypt(key, "secret".toByteArray(Charsets.UTF_8))
        assertThrows(Exception::class.java) { crypto.decrypt(other, enc) }
    }

    @Test
    fun generateKeyIs32Bytes() {
        assertEquals(32, crypto.generateKey().size)
    }
}