package com.sudeng.zhangben.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.sudeng.zhangben.data.local.entity.TransactionSource
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.repository.CategoryRepository
import com.sudeng.zhangben.data.repository.TransactionRepository
import com.sudeng.zhangben.util.MerchantClassifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PaymentNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var merchantClassifier: MerchantClassifier

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastNotificationText = ""
    private var lastNotificationTime = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val source = when (packageName) {
            "com.tencent.mm" -> TransactionSource.WECHAT
            "com.eg.android.AlipayGphone" -> TransactionSource.ALIPAY
            else -> return
        }

        val notification = sbn.notification
        val extras = notification.extras

        // 收集所有文本信息
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val subText = extras.getString("android.subText") ?: ""
        val summaryText = extras.getString("android.summaryText") ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val infoText = extras.getString("android.infoText") ?: ""
        val fullText = listOf(title, text, subText, summaryText, bigText, infoText)
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        if (fullText.isBlank()) return

        // 检查是否包含支付相关信息
        val paymentKeywords = listOf(
            "支付", "付款", "消费", "扣款", "支出", "账单",
            "成功", "交易", "收款", "订单"
        )
        val isPaymentNotification = paymentKeywords.any { fullText.contains(it) }
        if (!isPaymentNotification) return

        val amount = extractAmount(fullText) ?: return
        if (amount <= 0.01 || amount > 100000) return

        // 防重复
        val now = System.currentTimeMillis()
        if (fullText == lastNotificationText && (now - lastNotificationTime) < 1500) return
        lastNotificationText = fullText
        lastNotificationTime = now

        val merchant = extractMerchant(fullText, source)
        val sourceLabel = if (source == TransactionSource.WECHAT) "微信" else "支付宝"

        ioScope.launch {
            try {
                val categories = categoryRepository.getAllCategoriesList()
                val categoryId = if (merchant != null) {
                    merchantClassifier.classify(merchant, categories)
                } else null

                transactionRepository.addTransaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = categoryId,
                    note = merchant ?: "${sourceLabel}自动记账",
                    timestamp = System.currentTimeMillis(),
                    source = source
                )

                val catName = categories.find { it.id == categoryId }?.name
                showCaptureNotification(sourceLabel, amount, merchant ?: "未知商户", catName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel()
    }

    private fun extractAmount(text: String): Double? {
        val patterns = listOf(
            Regex("(?:￥|¥|¥|\\uffe5)\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("([\\d,]+\\.\\d{2})\\s*元"),
            Regex("金额[:：]\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("消费[:：]\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("付款[:：]\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("支出[:：]\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("([\\d,]+\\.\\d{2})")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val raw = match.groupValues[1].replace(",", "")
                val amount = raw.toDoubleOrNull()
                if (amount != null && amount > 0.01 && amount < 100000.0) {
                    return amount
                }
            }
        }
        return null
    }

    private fun extractMerchant(text: String, source: TransactionSource): String? {
        val patterns = listOf(
            Regex("收款方[:：]\\s*(\\S+)"),
            Regex("商户[:：]\\s*(\\S+)"),
            Regex("商户名称[:：]\\s*(\\S+)"),
            Regex("商家[:：]\\s*(\\S+)"),
            Regex("对方[:：]\\s*(\\S+)"),
            Regex("([\\u4e00-\\u9fa5a-zA-Z0-9]+(?:店|超市|餐厅|美食|科技|公司|银行|医院|药房|物业|酒店|快餐|烧烤))")
        )
        for (p in patterns) {
            val m = p.find(text)
            if (m != null) {
                val name = m.groupValues[1].trim()
                if (name.length in 2..30) return name
            }
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "capture_channel",
                "记账捕获通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "自动记账成功时显示通知" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showCaptureNotification(source: String, amount: Double, merchant: String, category: String?) {
        val title = "已自动记录 $source${category?.let { "($it)" } ?: ""}"
        val content = "$merchant - ¥%.2f".format(amount)
        val notification = NotificationCompat.Builder(this, "capture_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
