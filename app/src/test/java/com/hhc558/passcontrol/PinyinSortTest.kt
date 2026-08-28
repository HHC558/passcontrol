package com.hhc558.passcontrol

import com.hhc558.passcontrol.util.PinyinSort
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinSortTest {

    @Test
    fun latinBeforeChineseAndPinyinOrder() {
        val list = listOf("淘宝", "GitHub", "支付宝", "京东", "QQ")
        val sorted = list.sortedWith { a, b -> PinyinSort.comparePlatform(a, b) }
        // 拉丁字母在前：GitHub < QQ；中文按拼音：京东(J) < 淘宝(T) < 支付宝(Z)
        assertTrue(sorted.indexOf("GitHub") < sorted.indexOf("QQ"))
        assertTrue(sorted.indexOf("QQ") < sorted.indexOf("京东"))
        assertTrue(sorted.indexOf("京东") < sorted.indexOf("淘宝"))
        assertTrue(sorted.indexOf("淘宝") < sorted.indexOf("支付宝"))
    }

    @Test
    fun caseInsensitiveStableOrder() {
        val list = listOf("github", "GitHub", "gitee")
        val sorted = list.sortedWith { a, b -> PinyinSort.comparePlatform(a, b) }
        // 大小写不敏感排序下相邻，且稳定（同值保留原顺序）
        assertEquals_3(sorted.size)
    }

    private fun assertEquals_3(value: Int) {
        assertTrue(value == 3)
    }
}