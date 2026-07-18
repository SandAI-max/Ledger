package com.sudeng.zhangben.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.sudeng.zhangben.R
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
class PaymentAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var merchantClassifier: MerchantClassifier

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCapturedText = ""
    private var lastCaptureTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = arrayOf("com.tencent.mm", "com.eg.android.AlipayGphone")
        }
        serviceInfo = info
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val packageName = event.packageName?.toString() ?: return
            val source = when {
                packageName == "com.tencent.mm" -> TransactionSource.WECHAT
                packageName == "com.eg.android.AlipayGphone" -> TransactionSource.ALIPAY
                else -> return
            }

            val rootNode = try {
                rootInActiveWindow
            } catch (e: Exception) {
                null
            } ?: return

            try {
                extractPaymentInfo(rootNode, source)
            } finally {
                rootNode.recycle()
            }
        }
    }

    private fun extractPaymentInfo(rootNode: AccessibilityNodeInfo, source: TransactionSource) {
        val text = getAllText(rootNode)

        val keywords = when (source) {
            TransactionSource.WECHAT -> listOf("支付成功", "付款成功", "微信支付", "支付结果")
            TransactionSource.ALIPAY -> listOf("支付成功", "付款成功", "交易成功", "支付结果")
            TransactionSource.MANUAL -> return
        }

        val containsPayment = keywords.any { text.contains(it) }
        if (!containsPayment) return

        val amount = extractAmount(text) ?: return
        if (amount <= 0 || amount > 100000) return

        // 防重复：同一笔交易1.5秒内不重复记录
        val now = System.currentTimeMillis()
        if (text == lastCapturedText && (now - lastCaptureTime) < 1500) return
        lastCapturedText = text
        lastCaptureTime = now

        val merchant = extractMerchant(text, source)

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
                showCaptureNotification(
                    sourceLabel,
                    amount,
                    merchant ?: "未知商户",
                    catName
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        collectText(node, sb)
        return sb.toString()
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.let { sb.append(it).append(" ") }
        node.contentDescription?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectText(child, sb)
                child.recycle()
            }
        }
    }

    private fun extractAmount(text: String): Double? {
        val patterns = listOf(
            Regex("(?:￥|¥|¥|\\uffe5)\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("([\\d,]+\\.\\d{2})\\s*元"),
            Regex("金额[:：]\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("付款[:：]\\s*([\\d,]+\\.?\\d{0,2})"),
            Regex("([\\d,]+\\.\\d{2})")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val raw = match.groupValues[1].replace(",", "")
                val amount = raw.toDoubleOrNull()
                if (amount != null && amount > 0.01) {
                    return amount
                }
            }
        }
        return null
    }

    private fun extractMerchant(text: String, source: TransactionSource): String? {
        val patterns = when (source) {
            TransactionSource.WECHAT -> listOf(
                Regex("收款方[:：]\\s*(\\S+)"),
                Regex("商户[:：]\\s*(\\S+)"),
                Regex("商家[:：]\\s*(\\S+)"),
                Regex("([\\u4e00-\\u9fa5a-zA-Z0-9]+(?:店|超市|餐厅|美食|科技|公司|银行|医院|药房|物业))")
            )
            TransactionSource.ALIPAY -> listOf(
                Regex("收款方[:：]\\s*(\\S+)"),
                Regex("商户[:：]\\s*(\\S+)"),
                Regex("商家[:：]\\s*(\\S+)"),
                Regex("商户名称[:：]\\s*(\\S+)"),
                Regex("对方[:：]\\s*(\\S+)"),
                Regex("([\\u4e00-\\u9fa5a-zA-Z0-9]+(?:店|超市|餐厅|科技|公司|银行|医院|药房|物业))")
            )
            TransactionSource.MANUAL -> return null
        }
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel()
    }
}
