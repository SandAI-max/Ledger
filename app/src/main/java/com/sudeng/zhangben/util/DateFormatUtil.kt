package com.sudeng.zhangben.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun beijingDateFormat(pattern: String): SimpleDateFormat {
    return SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
}
