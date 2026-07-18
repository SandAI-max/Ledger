package com.sudeng.zhangben.ui.screen.profile

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.CategoryType
import com.sudeng.zhangben.data.repository.CategoryRepository
import com.sudeng.zhangben.data.repository.TransactionRepository
import com.sudeng.zhangben.util.ExportFormat
import com.sudeng.zhangben.util.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _transactionCount = MutableStateFlow(0)
    val transactionCount: StateFlow<Int> = _transactionCount

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    var showCategoryManager by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)

    private val _shareIntent = MutableStateFlow<Intent?>(null)
    val shareIntent: StateFlow<Intent?> = _shareIntent

    init {
        loadData()
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { _categories.value = it }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { list ->
                _transactionCount.value = list.size
            }
        }
    }

    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            val transactions = transactionRepository.getAllTransactionsWithCategoryList()
            if (transactions.isNotEmpty()) {
                _shareIntent.value = exportManager.export(transactions, format)
            }
        }
    }

    fun clearShareIntent() {
        _shareIntent.value = null
    }

    fun addCategory(name: String, icon: String, type: CategoryType, parentId: Long? = null) {
        viewModelScope.launch {
            val maxOrder = (_categories.value.filter { it.parentId == parentId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
            categoryRepository.insertCategory(CategoryEntity(
                name = name, icon = icon, type = type,
                parentId = parentId, sortOrder = maxOrder
            ))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }
}
