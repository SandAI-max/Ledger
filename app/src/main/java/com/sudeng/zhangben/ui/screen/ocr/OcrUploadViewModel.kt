package com.sudeng.zhangben.ui.screen.ocr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.repository.CategoryRepository
import com.sudeng.zhangben.data.repository.TransactionRepository
import com.sudeng.zhangben.util.MerchantClassifier
import com.sudeng.zhangben.util.OcrAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

data class OcrItem(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap? = null,
    var amount: Double? = null,
    var merchant: String? = null,
    var dateStr: String? = null,
    var timestamp: Long? = null,
    var isProcessing: Boolean = false,
    var processed: Boolean = false,
    var error: String? = null,
    var categoryId: Long? = null,
    var note: String = "",
    var imageTimestamp: Long? = null,
    var transactionType: TransactionType = TransactionType.EXPENSE
)

data class OcrBatchUiState(
    val items: List<OcrItem> = emptyList(),
    val isProcessing: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val allSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OcrUploadViewModel @Inject constructor(
    private val ocrAnalyzer: OcrAnalyzer,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantClassifier: MerchantClassifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrBatchUiState())
    val uiState: StateFlow<OcrBatchUiState> = _uiState

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun addImages(bitmaps: List<Bitmap>, imageTimestamps: List<Long>) {
        if (bitmaps.size > 10) return
        val currentItems = _uiState.value.items.toMutableList()
        bitmaps.forEachIndexed { index, bitmap ->
            if (currentItems.size < 10) {
                val imgTs = imageTimestamps.getOrNull(index)
                currentItems.add(OcrItem(bitmap = bitmap, imageTimestamp = imgTs))
            }
        }
        _uiState.value = _uiState.value.copy(
            items = currentItems,
            totalCount = currentItems.size
        )
    }

    fun removeItem(id: String) {
        val items = _uiState.value.items.filter { it.id != id }
        _uiState.value = _uiState.value.copy(
            items = items,
            totalCount = items.size
        )
    }

    fun startBatchOcr() {
        val items = _uiState.value.items
        if (items.isEmpty() || _uiState.value.isProcessing) return

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            processedCount = 0
        )

        viewModelScope.launch {
            val updatedItems = items.toMutableList()
            var processed = 0

            for (i in updatedItems.indices) {
                val item = updatedItems[i]
                val bitmap = item.bitmap ?: continue

                updatedItems[i] = item.copy(isProcessing = true)
                _uiState.value = _uiState.value.copy(
                    items = updatedItems.toList(),
                    processedCount = processed
                )

                try {
                    val result = withContext(Dispatchers.IO) {
                        ocrAnalyzer.analyze(bitmap)
                    }

                    val categories = _uiState.value.categories
                    val categoryId = if (result.merchant != null) {
                        merchantClassifier.classify(result.merchant, categories)
                    } else null

                    val timestamp = parseDateToTimestamp(result.dateStr, item.imageTimestamp)

                    updatedItems[i] = item.copy(
                        isProcessing = false,
                        processed = true,
                        amount = result.amount,
                        merchant = result.merchant,
                        dateStr = result.dateStr,
                        timestamp = timestamp,
                        categoryId = categoryId,
                        note = result.merchant ?: "",
                        transactionType = if (result.isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    )
                } catch (e: Exception) {
                    updatedItems[i] = item.copy(
                        isProcessing = false,
                        processed = false,
                        error = "识别失败: ${e.message}"
                    )
                }

                processed++
                _uiState.value = _uiState.value.copy(
                    items = updatedItems.toList(),
                    processedCount = processed
                )
            }

            // 自动排序：有时间戳的按时间排，没时间的排前面
            val sorted = updatedItems.sortedBy { it.timestamp ?: Long.MAX_VALUE }
            _uiState.value = _uiState.value.copy(
                items = sorted,
                isProcessing = false,
                processedCount = processed
            )
        }
    }

    fun updateItemCategory(id: String, categoryId: Long?) {
        val items = _uiState.value.items.map { item ->
            if (item.id == id) item.copy(categoryId = categoryId) else item
        }
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun updateItemAmount(id: String, amount: Double?) {
        val items = _uiState.value.items.map { item ->
            if (item.id == id) item.copy(amount = amount) else item
        }
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun updateItemNote(id: String, note: String) {
        val items = _uiState.value.items.map { item ->
            if (item.id == id) item.copy(note = note) else item
        }
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun updateItemType(id: String, type: TransactionType) {
        val items = _uiState.value.items.map { item ->
            if (item.id == id) item.copy(transactionType = type) else item
        }
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun saveAll() {
        val items = _uiState.value.items.filter { it.processed && (it.amount ?: 0.0) > 0 }
        if (items.isEmpty()) return

        viewModelScope.launch {
            try {
                items.forEach { item ->
                    val amt = item.amount ?: return@forEach
                    transactionRepository.addTransaction(
                        amount = amt,
                        type = item.transactionType,
                        categoryId = item.categoryId,
                        note = item.note.ifEmpty { item.merchant ?: "截图识别" },
                        timestamp = item.timestamp ?: System.currentTimeMillis()
                    )
                }
                _uiState.value = _uiState.value.copy(allSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "保存失败: ${e.message}")
            }
        }
    }

    fun reset() {
        _uiState.value.items.forEach { it.bitmap?.recycle() }
        _uiState.value = OcrBatchUiState(categories = _uiState.value.categories)
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.items.forEach { it.bitmap?.recycle() }
    }

    private fun parseDateToTimestamp(dateStr: String?, imageTimestamp: Long?): Long? {
        if (dateStr == null) return null
        val beijingZone = TimeZone.getTimeZone("Asia/Shanghai")

        // 从图片文件时间戳提取年份，作为无年份日期的兜底
        val fallbackYear = imageTimestamp?.let {
            Calendar.getInstance(beijingZone).apply { timeInMillis = it }.get(Calendar.YEAR)
        }

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd",
            "yyyy年MM月dd日 HH:mm:ss", "yyyy年MM月dd日 HH:mm", "yyyy年MM月dd日",
            "yyyy年M月d日 HH:mm:ss", "yyyy年M月d日 HH:mm", "yyyy年M月d日",
            "MM月dd日 HH:mm", "MM/dd HH:mm",
            "M月d日 HH:mm", "M/d HH:mm",
            "MM月dd日", "MM/dd", "M月d日", "M/d"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.CHINA).apply {
                    timeZone = beijingZone
                    isLenient = true
                }
                val cal = Calendar.getInstance(beijingZone)
                val parsed = sdf.parse(dateStr) ?: continue
                cal.time = parsed

                // 无年份的日期，用图片文件的拍摄时间推断年份
                val hasYear = pattern.startsWith("yyyy") || pattern.startsWith("y")
                if (!hasYear && fallbackYear != null) {
                    cal.set(Calendar.YEAR, fallbackYear)
                }

                return cal.timeInMillis
            } catch (_: Exception) {}
        }
        return null
    }
}
