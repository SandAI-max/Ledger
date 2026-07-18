package com.sudeng.zhangben.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.sudeng.zhangben.data.local.entity.TransactionSource
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat(val label: String, val ext: String, val mimeType: String) {
    TXT("TXT 文本", ".txt", "text/plain"),
    HTML("HTML 文档", ".html", "text/html"),
    CSV("CSV 表格", ".csv", "text/*")
}

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun export(transactions: List<TransactionWithCategory>, format: ExportFormat): Intent {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val fileName = "账单导出_${fileDateFormat.format(Date())}${format.ext}"
        val file = File(exportDir, fileName)

        val content = when (format) {
            ExportFormat.TXT -> generateTxt(transactions, dateFormat)
            ExportFormat.HTML -> generateHtml(transactions, dateFormat)
            ExportFormat.CSV -> generateCsv(transactions, dateFormat)
        }

        file.writeText(content, Charsets.UTF_8)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "账单导出")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun generateTxt(
        transactions: List<TransactionWithCategory>,
        df: SimpleDateFormat
    ): String = buildString {
        appendLine("=" .repeat(42))
        appendLine("  苏苏记账 - 账单导出")
        appendLine("  导出时间: ${df.format(Date())}")
        appendLine("=" .repeat(42))
        appendLine()
        appendLine("%-20s %-6s %-10s %s".format("日期", "类型", "金额", "分类/备注"))
        appendLine("-" .repeat(42))
        transactions.forEach { item ->
            val t = item.transaction
            val type = if (t.type == TransactionType.EXPENSE) "支出" else "收入"
            val prefix = if (t.type == TransactionType.EXPENSE) "-" else "+"
            val cat = item.category?.name ?: ""
            val note = if (t.note.isNotEmpty()) "(${t.note})" else ""
            appendLine("%-20s %-6s %-10s %s%s".format(
                df.format(Date(t.timestamp)), type,
                "${prefix}¥%.2f".format(t.amount), cat, note
            ))
        }
        appendLine("-" .repeat(42))
        val totalExpense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
        val totalIncome = transactions.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        appendLine("总支出: ¥%.2f".format(totalExpense))
        appendLine("总收入: ¥%.2f".format(totalIncome))
        appendLine("结余: ¥%.2f".format(totalIncome - totalExpense))
    }

    private fun generateHtml(
        transactions: List<TransactionWithCategory>,
        df: SimpleDateFormat
    ): String = buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
        append("<style>")
        append("body{font-family:'Microsoft YaHei',sans-serif;padding:20px;color:#333}")
        append("h1{color:#1A56D0;text-align:center}")
        append("table{width:100%;border-collapse:collapse;margin-top:16px}")
        append("th{background:#1A56D0;color:white;padding:10px;text-align:left}")
        append("td{padding:8px 10px;border-bottom:1px solid #eee}")
        append("tr:hover{background:#f5f7fa}")
        append(".expense{color:#E53935}.income{color:#43A047}")
        append(".summary{background:#f0f4ff;font-weight:bold}")
        append("</style></head><body>")
        append("<h1>苏苏记账 - 账单导出</h1>")
        append("<p>导出时间: ${df.format(Date())} | 共 ${transactions.size} 条记录</p>")
        append("<table><tr><th>日期</th><th>类型</th><th>分类</th><th>金额</th><th>备注</th><th>来源</th></tr>")
        transactions.forEach { item ->
            val t = item.transaction
            val type = if (t.type == TransactionType.EXPENSE) "支出" else "收入"
            val cls = if (t.type == TransactionType.EXPENSE) "expense" else "income"
            val prefix = if (t.type == TransactionType.EXPENSE) "-" else "+"
            val cat = item.category?.name ?: "未分类"
            val source = when (t.source) {
                TransactionSource.MANUAL -> "手动"; TransactionSource.WECHAT -> "微信"; TransactionSource.ALIPAY -> "支付宝"
            }
            append("<tr><td>${df.format(Date(t.timestamp))}</td>")
            append("<td>$type</td><td>$cat</td>")
            append("<td class=\"$cls\">${prefix}¥%.2f".format(t.amount))
            append("</td><td>${t.note.ifEmpty { "-" }}</td><td>$source</td></tr>")
        }
        val totalExpense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        val totalIncome = transactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
        append("<tr class=\"summary\"><td colspan=\"3\">合计</td>")
        append("<td class=\"expense\">支出: ¥%.2f".format(totalExpense))
        append("<br><span class=\"income\">收入: ¥%.2f".format(totalIncome))
        append("</span></td><td colspan=\"2\">结余: ¥%.2f".format(totalIncome - totalExpense))
        append("</td></tr></table></body></html>")
    }

    private fun generateCsv(
        transactions: List<TransactionWithCategory>,
        df: SimpleDateFormat
    ): String = buildString {
        append("\uFEFF")
        appendLine("日期,类型,分类,金额,备注,来源")
        transactions.forEach { item ->
            val t = item.transaction
            val type = if (t.type == TransactionType.EXPENSE) "支出" else "收入"
            val cat = item.category?.name ?: "未分类"
            val source = when (t.source) {
                TransactionSource.MANUAL -> "手动"; TransactionSource.WECHAT -> "微信"; TransactionSource.ALIPAY -> "支付宝"
            }
            val note = t.note.ifEmpty { "无备注" }
            appendLine("${df.format(Date(t.timestamp))},$type,$cat,${t.amount},$note,$source")
        }
    }
}
