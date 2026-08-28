package com.hhc558.passcontrol

import com.hhc558.passcontrol.data.AccountView
import com.hhc558.passcontrol.xlsx.XlsxService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxServiceTest {
    private val service = XlsxService()

    private fun sampleRecords() = listOf(
        AccountView(1, "GitHub", "user1", "p@ss&<word>", "https://github.com", "user1@example.com", 1_700_000_000_000L),
        AccountView(2, "淘宝", "taobao_user", "abc123", null, null, 1_700_000_001_000L)
    )

    @Test
    fun roundTrip() {
        val bytes = service.writeRecords(sampleRecords())
        val rows = service.readRows(bytes)
        assertEquals(2, rows.size)

        val first = rows[0]
        assertEquals("GitHub", first.platform)
        assertEquals("user1", first.username)
        assertEquals("p@ss&<word>", first.password)
        assertEquals("https://github.com", first.url)
        assertEquals("user1@example.com", first.email)
        assertNotNull(first.createdAt)

        val second = rows[1]
        assertEquals("淘宝", second.platform)
        assertEquals("taobao_user", second.username)
        assertEquals("abc123", second.password)
        assertEquals(null, second.url)
        assertEquals(null, second.email)
        assertNotNull(second.createdAt)
    }

    @Test
    fun emptyRecordsRoundTrip() {
        val bytes = service.writeRecords(emptyList())
        assertTrue(service.readRows(bytes).isEmpty())
    }

    @Test
    fun specialCharactersRoundTrip() {
        val bytes = service.writeRecords(
            listOf(AccountView(1, "A&B<C>", "u\"'", "p&<>&\"", "https://x.y/a?b=1&c=2", null, 0L))
        )
        val rows = service.readRows(bytes)
        assertEquals("A&B<C>", rows[0].platform)
        assertEquals("u\"'", rows[0].username)
        assertEquals("p&<>&\"", rows[0].password)
        assertEquals("https://x.y/a?b=1&c=2", rows[0].url)
    }

    @Test
    fun rejectsInvalidFile() {
        var thrown = false
        try {
            service.readRows("this is not a zip".toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }
}