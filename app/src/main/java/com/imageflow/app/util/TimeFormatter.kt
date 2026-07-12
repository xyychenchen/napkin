package com.imageflow.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun relative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        if (diff < TimeUnit.MINUTES.toMillis(1)) return "刚刚"
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            val m = TimeUnit.MILLISECONDS.toMinutes(diff)
            return "$m 分钟前"
        }
        if (diff < TimeUnit.DAYS.toMillis(1)) {
            val h = TimeUnit.MILLISECONDS.toHours(diff)
            return "$h 小时前"
        }
        val that = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val dayDiff = today.get(Calendar.DAY_OF_YEAR) - that.get(Calendar.DAY_OF_YEAR)
        if (dayDiff == 1) return "昨天 ${timeFmt.format(Date(timestamp))}"
        if (dayDiff in 2..6) return "$dayDiff 天前"
        return dayFmt.format(Date(timestamp))
    }

    fun full(timestamp: Long): String = fullFmt.format(Date(timestamp))
}
