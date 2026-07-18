package com.sudeng.zhangben.ui.screen.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.entity.TransactionEntity
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import com.sudeng.zhangben.data.repository.TransactionRepository
import com.sudeng.zhangben.util.ExportFormat
import com.sudeng.zhangben.util.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionWithCategory>>(emptyList())
    val transactions: StateFlow<List<TransactionWithCategory>> = _transactions

    private val _shareIntent = MutableStateFlow<android.content.Intent?>(null)
    val shareIntent: StateFlow<android.content.Intent?> = _shareIntent

    init {
        viewModelScope.launch {
            transactionRepository.getAllTransactionsWithCategory().collect {
                _transactions.value = it
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun exportCsv(format: ExportFormat) {
        viewModelScope.launch {
            val list = transactionRepository.getAllTransactionsWithCategoryList()
            if (list.isNotEmpty()) {
                _shareIntent.value = exportManager.export(list, format)
            }
        }
    }

    fun clearShareIntent() {
        _shareIntent.value = null
    }
}
