package com.sudeng.zhangben.ui.screen.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.TransactionEntity
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.repository.CategoryRepository
import com.sudeng.zhangben.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    private val _transactionType = MutableStateFlow(TransactionType.EXPENSE)
    val transactionType: StateFlow<TransactionType> = _transactionType

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        loadCategories()
    }

    fun setType(type: TransactionType) {
        _transactionType.value = type
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesByType(_transactionType.value.name)
                .collect { _categories.value = it }
        }
    }

    fun saveTransaction(
        amount: Double,
        categoryId: Long?,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            transactionRepository.addTransaction(
                amount = amount,
                type = _transactionType.value,
                categoryId = categoryId,
                note = note,
                timestamp = timestamp
            )
            _saved.value = true
        }
    }
}
