package com.hhc558.passcontrol.xlsx

import com.hhc558.passcontrol.data.AccountView

/**
 * 导入差异引擎：按「平台名称 + 账号」（trim、忽略大小写）与 APP 内数据比对。
 * - 新增：文件有而 APP 无
 * - 修改：都存在但密码、网址或邮箱不同（保留原创建时间）
 * - 删除：APP 有而文件无
 * - 无变化 / 跳过（平台或账号为空、文件内重复行）
 */
object DiffEngine {

    fun compute(current: List<AccountView>, rows: List<ImportRow>): ImportDiff {
        fun key(p: String, u: String): String = (p.trim() + "\u0000" + u.trim()).lowercase()

        val currentMap = HashMap<String, AccountView>()
        for (a in current) currentMap[key(a.platform, a.username)] = a

        val seenKeys = HashSet<String>()
        val added = ArrayList<ImportRow>()
        val modified = ArrayList<ModifiedItem>()
        var unchanged = 0
        var skipped = 0

        for (row in rows) {
            if (row.platform.isBlank() || row.username.isBlank()) {
                skipped++
                continue
            }
            val k = key(row.platform, row.username)
            if (!seenKeys.add(k)) {
                skipped++ // 文件内重复行
                continue
            }
            val old = currentMap[k]
            if (old == null) {
                added.add(row)
            } else {
                val changes = ArrayList<Pair<String, Pair<String, String>>>()
                if (old.password != row.password) {
                    changes.add("密码" to (old.password to row.password))
                }
                val oldUrl = old.url?.trim() ?: ""
                val newUrl = row.url?.trim() ?: ""
                if (oldUrl != newUrl) {
                    changes.add("网址" to (oldUrl to newUrl))
                }
                val oldEmail = old.email?.trim() ?: ""
                val newEmail = row.email?.trim() ?: ""
                if (oldEmail != newEmail) {
                    changes.add("邮箱" to (oldEmail to newEmail))
                }
                if (changes.isEmpty()) {
                    unchanged++
                } else {
                    modified.add(ModifiedItem(old, row, changes))
                }
            }
        }

        val deleted = current.filter { key(it.platform, it.username) !in seenKeys }
        return ImportDiff(added, modified, deleted, unchanged, skipped)
    }
}