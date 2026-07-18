package com.sudeng.zhangben.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.CategoryType
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import com.sudeng.zhangben.data.local.BudgetManager
import com.sudeng.zhangben.data.repository.CategoryRepository
import com.sudeng.zhangben.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetManager: BudgetManager
) : ViewModel() {

    private val _recentTransactions = MutableStateFlow<List<TransactionWithCategory>>(emptyList())
    val recentTransactions: StateFlow<List<TransactionWithCategory>> = _recentTransactions

    private val _monthlyExpense = MutableStateFlow(0.0)
    val monthlyExpense: StateFlow<Double> = _monthlyExpense

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome

    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    init {
        loadData()
        loadBudget()
        initDefaultCategories()
    }

    private fun loadBudget() {
        viewModelScope.launch {
            budgetManager.monthlyBudget.collect { _monthlyBudget.value = it }
        }
    }

    fun setMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            budgetManager.setMonthlyBudget(amount)
        }
    }

    private fun loadData() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val monthEnd = calendar.timeInMillis

        viewModelScope.launch {
            transactionRepository.getTransactionsWithCategoryByDateRange(monthStart, monthEnd)
                .collect { transactions ->
                    _recentTransactions.value = transactions.take(5)
                }
        }
        viewModelScope.launch {
            transactionRepository.getTotalExpenseBetween(monthStart, monthEnd)
                .collect { _monthlyExpense.value = it ?: 0.0 }
        }
        viewModelScope.launch {
            transactionRepository.getTotalIncomeBetween(monthStart, monthEnd)
                .collect { _monthlyIncome.value = it ?: 0.0 }
        }
    }

    private fun initDefaultCategories() {
        viewModelScope.launch {
            categoryRepository.getCount().collect { count ->
                if (count == 0L) {
                    val categories = listOf(
                        CategoryEntity(name = "餐饮", icon = "\uD83C\uDF54", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 0),
                        CategoryEntity(name = "交通", icon = "\uD83D\uDE97", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 1),
                        CategoryEntity(name = "购物", icon = "\uD83D\uDECD\uFE0F", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 2),
                        CategoryEntity(name = "娱乐", icon = "\uD83C\uDFAC", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 3),
                        CategoryEntity(name = "居家", icon = "\uD83C\uDFE0", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 4),
                        CategoryEntity(name = "医疗", icon = "\uD83C\uDFE5", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 5),
                        CategoryEntity(name = "教育", icon = "\uD83D\uDCDA", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 6),
                        CategoryEntity(name = "通讯", icon = "\uD83D\uDCF1", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 7),
                        CategoryEntity(name = "工资", icon = "\uD83D\uDCB0", type = CategoryType.INCOME, isDefault = true, sortOrder = 0),
                        CategoryEntity(name = "奖金", icon = "\uD83C\uDFC6", type = CategoryType.INCOME, isDefault = true, sortOrder = 1),
                        CategoryEntity(name = "投资", icon = "\uD83D\uDCC8", type = CategoryType.INCOME, isDefault = true, sortOrder = 2),
                        CategoryEntity(name = "其他收入", icon = "\uD83D\uDCB5", type = CategoryType.INCOME, isDefault = true, sortOrder = 3)
                    )
                    categoryRepository.insertCategories(categories)
                }
            }
        }
    }
}
