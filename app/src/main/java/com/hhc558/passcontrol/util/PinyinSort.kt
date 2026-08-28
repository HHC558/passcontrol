package com.hhc558.passcontrol.util

import java.text.Collator
import java.util.Locale

/**
 * 平台名称排序：使用中文区域排序规则（拉丁字母 A-Z 在前，中文按拼音首字母），
 * 实现「按首字母顺序排列」。
 */
object PinyinSort {
    private val collator: Collator = Collator.getInstance(Locale.CHINA)

    fun comparePlatform(a: String, b: String): Int {
        val c = collator.compare(a, b)
        return if (c != 0) c else a.compareTo(b)
    }
}