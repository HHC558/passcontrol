package com.hhc558.passcontrol.xlsx

import com.hhc558.passcontrol.data.AccountView

/** 从 xlsx 文件解析出的一条记录（明文密码，仅内存）。 */
data class ImportRow(
    val platform: String,
    val username: String,
    val password: String,
    val url: String?,
    val email: String?,
    val createdAt: Long?,
    /** 源文件中的行号（用于展示） */
    val rowNumber: Int
)

/** 一条“修改”记录：旧值 + 新值 + 变更字段（字段名 -> 旧值 to 新值）。 */
data class ModifiedItem(
    val old: AccountView,
    val new: ImportRow,
    val changes: List<Pair<String, Pair<String, String>>>
)

/** 导入差异结果：新增/修改/删除/无变化/跳过。 */
data class ImportDiff(
    val added: List<ImportRow>,
    val modified: List<ModifiedItem>,
    val deleted: List<AccountView>,
    val unchanged: Int,
    val skipped: Int
) {
    val hasChanges: Boolean get() = added.isNotEmpty() || modified.isNotEmpty() || deleted.isNotEmpty()
}