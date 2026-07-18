package com.sudeng.zhangben.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val transactions = transactionRepository.getAllTransactionsWithCategoryList()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            }
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            val csv = buildString {
                appendLine("日期,类型,分类,金额,备注,来源")
                transactions.forEach { item ->
                    val t = item.transaction
                    val type = if (t.type == TransactionType.EXPENSE) "支出" else "收入"
                    val cat = item.category?.name ?: "未分类"
                    val source = when (t.source) {
                        com.sudeng.zhangben.data.local.entity.TransactionSource.MANUAL -> "手动"
                        com.sudeng.zhangben.data.local.entity.TransactionSource.WECHAT -> "微信"
                        com.sudeng.zhangben.data.local.entity.TransactionSource.ALIPAY -> "支付宝"
                    }
                    val note = t.note.ifEmpty { "无备注" }
                    appendLine("${dateFormat.format(Date(t.timestamp))},$type,$cat,${t.amount},$note,$source")
                }
            }

            val backupDir = File(applicationContext.filesDir, "backups")
            backupDir.mkdirs()

            val file = File(backupDir, "自动备份_${fileDateFormat.format(Date())}.csv")
            file.writeText(csv, Charsets.UTF_8)

            val files = backupDir.listFiles()
            if (files != null && files.size > 5) {
                val sorted = files.sortedByDescending { f -> f.lastModified() }
                for (i in 5 until sorted.size) {
                    sorted[i].delete()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
