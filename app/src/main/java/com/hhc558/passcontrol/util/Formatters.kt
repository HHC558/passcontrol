package com.hhc558.passcontrol.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun formatCreatedAt(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    fun timestampForFile(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}