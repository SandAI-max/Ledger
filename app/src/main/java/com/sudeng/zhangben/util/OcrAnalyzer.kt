package com.sudeng.zhangben.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class OcrResult(
    val rawText: String,
    val amount: Double?,
    val merchant: String?,
    val dateStr: String?,
    val isPayment: Boolean,
    val isIncome: Boolean
)

@Singleton
class OcrAnalyzer @Inject constructor() {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    suspend fun analyze(bitmap: Bitmap): OcrResult = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        val rawText = result.text

        val amount = extractAmountFromOcr(rawText)
        val merchant = extractMerchantFromOcr(rawText)
        val dateStr = extractDateFromOcr(rawText)
        val isPayment = rawText.contains("支付") || rawText.contains("付款") ||
                rawText.contains("消费") || rawText.contains("交易") ||
                rawText.contains("成功") || rawText.contains("订单")
        val isIncome = if (rawText.contains("已存入零钱")) {
            true
        } else if (rawText.contains("支付成功")) {
            false
        } else {
            rawText.contains("收款") || rawText.contains("转账") ||
                    rawText.contains("红包") || rawText.contains("退款") ||
                    rawText.contains("收入") || rawText.contains("工资") ||
                    rawText.contains("入账") || rawText.contains("报销") ||
                    rawText.contains("转入") || rawText.contains("奖金") ||
                    rawText.contains("提成") || rawText.contains("退税")
        }

        OcrResult(
            rawText = rawText,
            amount = amount,
            merchant = merchant,
            dateStr = dateStr,
            isPayment = isPayment,
            isIncome = isIncome
        )
    }

    fun close() {
        recognizer.close()
    }

    private fun extractAmountFromOcr(text: String): Double? {
        // 优先级从高到低匹配
        val patterns = listOf(
            // ¥123.45  或 ￥123.45
            Regex("[¥￥]\\s*([\\d,]+\\.\\d{2})"),
            // 123.45元
            Regex("([\\d,]+\\.\\d{2})\\s*元"),
            // 金额：123.45
            Regex("金额[:：]\\s*[¥￥]?\\s*([\\d,]+\\.\\d{2})"),
            // 付款：123.45
            Regex("(?:付款|实付|消费|支付)[:：]\\s*[¥￥]?\\s*([\\d,]+\\.\\d{2})"),
            // 合计：123.45
            Regex("(?:合计|总计|应收)[:：]\\s*[¥￥]?\\s*([\\d,]+\\.\\d{2})"),
            // 纯数字（最后匹配）
            Regex("([\\d,]+\\.[\\d]{2})")
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

    private fun extractMerchantFromOcr(text: String): String? {
        val patterns = listOf(
            Regex("收款方[:：]\\s*(\\S+)"),
            Regex("商户[:：]\\s*(\\S+)"),
            Regex("商家[:：]\\s*(\\S+)"),
            Regex("商户名称[:：]\\s*(\\S+)"),
            Regex("收款单位[:：]\\s*(\\S+)"),
            Regex("对方[:：]\\s*(\\S+)"),
            Regex("(?<=收款方[：:]\\s{0,2})[^\\s\\d]+"),
            Regex("([^\\s\\d]{2,}(?:店|超市|餐厅|餐饮|美食|科技|公司|银行|医院|药房|物业|酒店|快餐|烧烤|便利店|食堂|菜市场|加油站|停车场))")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.length in 2..30 && !name.all { it.isDigit() }) {
                    return name
                }
            }
        }
        return null
    }

    private fun extractDateFromOcr(text: String): String? {
        // 优先级从高到低：有年份的最准确，其次是无年份+时间的，最后是无年份纯日期
        val patterns = listOf(
            // 带年份的完整日期
            Regex("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2})[日号]?\\s*\\d{1,2}:\\d{2}"),
            Regex("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2})[日号]?"),
            Regex("(\\d{4}-\\d{2}-\\d{2})"),
            // 无年份但有时间的日期（最需要 imageTimestamp 补年份）
            Regex("(\\d{1,2}月\\d{1,2}日)\\s*\\d{1,2}:\\d{2}"),
            Regex("(\\d{1,2}/\\d{1,2})\\s*\\d{1,2}:\\d{2}"),
            Regex("(\\d{1,2}月\\d{1,2}日)"),
            Regex("(\\d{1,2}/\\d{1,2})"),
            // 通用前缀匹配
            Regex("日期[:：]\\s*(\\S+)"),
            Regex("时间[:：]\\s*(\\S+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
}
