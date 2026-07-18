package com.sudeng.zhangben.ui.screen.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import com.sudeng.zhangben.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _transaction = MutableStateFlow<TransactionWithCategory?>(null)
    val transaction: StateFlow<TransactionWithCategory?> = _transaction

    private var _transactionId: Long = 0

    fun loadTransaction(id: Long) {
        _transactionId = id
        viewModelScope.launch {
            _transaction.value = transactionRepository.getTransactionWithCategoryById(id)
        }
    }

    fun deleteTransaction() {
        _transaction.value?.let { item ->
            viewModelScope.launch {
                transactionRepository.deleteTransaction(item.transaction)
            }
        }
    }
}
