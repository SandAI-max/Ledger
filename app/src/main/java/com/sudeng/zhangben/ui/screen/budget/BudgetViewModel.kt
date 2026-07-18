package com.sudeng.zhangben.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.BudgetManager
import com.sudeng.zhangben.data.local.dao.CategorySum
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.repository.CategoryRepository
import com.sudeng.zhangben.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoryBudgetItem(
    val category: CategoryEntity,
    val spent: Double,
    val budget: Double
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetManager: BudgetManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    private val _monthlyExpense = MutableStateFlow(0.0)
    val monthlyExpense: StateFlow<Double> = _monthlyExpense

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome

    private val _categoryItems = MutableStateFlow<List<CategoryBudgetItem>>(emptyList())
    val categoryItems: StateFlow<List<CategoryBudgetItem>> = _categoryItems

    init {
        loadData()
    }

    fun setMonthlyBudget(amount: Double) {
        viewModelScope.launch { budgetManager.setMonthlyBudget(amount) }
    }

    fun setCategoryBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch { budgetManager.setCategoryBudget(categoryId, amount) }
    }

    private fun loadData() {
        viewModelScope.launch {
            budgetManager.monthlyBudget.collect { _monthlyBudget.value = it }
        }
        val (start, end) = getMonthRange()
        viewModelScope.launch {
            transactionRepository.getTotalExpenseBetween(start, end)
                .collect { _monthlyExpense.value = it ?: 0.0 }
        }
        viewModelScope.launch {
            transactionRepository.getTotalIncomeBetween(start, end)
                .collect { _monthlyIncome.value = it ?: 0.0 }
        }

        viewModelScope.launch {
            combine(
                transactionRepository.getCategorySums("EXPENSE", start, end),
                categoryRepository.getCategoriesByType("EXPENSE"),
                budgetManager.categoryBudgets
            ) { sums, categories, budgets ->
                val sumMap = sums.associateBy { it.categoryName }
                categories.map { cat ->
                    val spent = sumMap[cat.name]?.totalAmount ?: 0.0
                    CategoryBudgetItem(
                        category = cat,
                        spent = spent,
                        budget = budgets[cat.id] ?: 0.0
                    )
                }.sortedByDescending { it.spent }
            }.collect { _categoryItems.value = it }
        }
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return Pair(start, cal.timeInMillis)
    }
}
