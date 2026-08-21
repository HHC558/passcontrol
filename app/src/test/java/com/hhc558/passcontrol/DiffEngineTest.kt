package com.hhc558.passcontrol

import com.hhc558.passcontrol.data.AccountView
import com.hhc558.passcontrol.xlsx.DiffEngine
import com.hhc558.passcontrol.xlsx.ImportRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffEngineTest {

    private fun account(id: Long, platform: String, username: String, password: String, email: String?) =
        AccountView(id, platform, username, password, email, 0L)

    private fun row(platform: String, username: String, password: String, email: String?) =
        ImportRow(platform, username, password, email, null, 1)

    @Test
    fun detectsAddedModifiedDeletedUnchanged() {
        val current = listOf(
            account(1, "GitHub", "user1", "old-pass", "a@b.com"),
            account(2, "淘宝", "taobao", "abc123", null),
            account(3, "微博", "weibo", "x", null)
        )
        val rows = listOf(
            row("GitHub", "user1", "new-pass", "a@b.com"),
            row("淘宝", "taobao", "abc123", "new@mail.com"),
            row("支付宝", "ali", "p1", null),
            row("微博", "weibo", "x", null)
        )
        val diff = DiffEngine.compute(current, rows)

        assertEquals(1, diff.added.size)
        assertEquals("支付宝", diff.added[0].platform)
        assertEquals(2, diff.modified.size)
        assertEquals(1, diff.deleted.size)
        assertEquals("微博", diff.deleted[0].platform)
        assertEquals(1, diff.unchanged)
        assertEquals(0, diff.skipped)
        assertTrue(diff.hasChanges)
    }

    @Test
    fun modifiedChangesListed() {
        val current = listOf(account(1, "A", "a", "old", null))
        val rows = listOf(row("A", "a", "new", "e@x.com"))
        val diff = DiffEngine.compute(current, rows)
        val item = diff.modified[0]
        assertEquals(1L, item.old.id)
        assertEquals(2, item.changes.size)
        assertEquals("密码", item.changes[0].first)
        assertEquals("邮箱", item.changes[1].first)
    }

    @Test
    fun caseInsensitiveTrimMatch() {
        val current = listOf(account(1, " GitHub ", "User1", "p", null))
        val rows = listOf(row("github", "user1", "p", null))
        val diff = DiffEngine.compute(current, rows)
        assertEquals(0, diff.added.size)
        assertEquals(1, diff.unchanged)
        assertEquals(0, diff.deleted.size)
    }

    @Test
    fun blankAndDuplicateRowsSkipped() {
        val current = listOf(account(1, "A", "a", "p", null))
        val rows = listOf(
            row("", "x", "p", null),
            row("B", "", "p", null),
            row("A", "a", "p", null),
            row("A", "a", "p", null)
        )
        val diff = DiffEngine.compute(current, rows)
        assertEquals(0, diff.added.size)
        assertEquals(1, diff.unchanged)
        assertEquals(2, diff.skipped)
    }

    @Test
    fun noChanges() {
        val current = listOf(account(1, "A", "a", "p", "e@x.com"))
        val rows = listOf(row("A", "a", "p", "e@x.com"))
        val diff = DiffEngine.compute(current, rows)
        assertFalse(diff.hasChanges)
        assertEquals(1, diff.unchanged)
        assertEquals(0, diff.deleted.size)
    }

    @Test
    fun allDeletedWhenFileEmpty() {
        val current = listOf(account(1, "A", "a", "p", null), account(2, "B", "b", "p", null))
        val diff = DiffEngine.compute(current, emptyList())
        assertEquals(2, diff.deleted.size)
        assertTrue(diff.hasChanges)
    }
}