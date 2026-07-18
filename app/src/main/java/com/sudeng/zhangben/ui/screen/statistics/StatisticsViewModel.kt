package com.sudeng.zhangben.ui.screen.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudeng.zhangben.data.local.dao.CategorySum
import com.sudeng.zhangben.data.local.dao.DailySum
import com.sudeng.zhangben.data.local.dao.MerchantRank
import com.sudeng.zhangben.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _expenseCategorySums = MutableStateFlow<List<CategorySum>>(emptyList())
    val expenseCategorySums: StateFlow<List<CategorySum>> = _expenseCategorySums

    private val _incomeCategorySums = MutableStateFlow<List<CategorySum>>(emptyList())
    val incomeCategorySums: StateFlow<List<CategorySum>> = _incomeCategorySums

    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome

    private val _dailySums = MutableStateFlow<List<DailySum>>(emptyList())
    val dailySums: StateFlow<List<DailySum>> = _dailySums

    private val _chartType = MutableStateFlow("EXPENSE")
    val chartType: StateFlow<String> = _chartType

    private val _startDate = MutableStateFlow(getDefaultStartDate())
    val startDate: StateFlow<Long> = _startDate

    private val _endDate = MutableStateFlow(getDefaultEndDate())
    val endDate: StateFlow<Long> = _endDate

    private val _totalInRange = MutableStateFlow(0.0)
    val totalInRange: StateFlow<Double> = _totalInRange

    private val _merchantRanking = MutableStateFlow<List<MerchantRank>>(emptyList())
    val merchantRanking: StateFlow<List<MerchantRank>> = _merchantRanking

    private var dailySumsJob: Job? = null
    private var merchantRankingJob: Job? = null
    private var categorySumsJob: Job? = null

    init {
        loadData()
    }

    fun setChartType(type: String) {
        _chartType.value = type
        loadDailySums()
    }

    fun setDateRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
        loadCategorySums(start, end)
        loadDailySums()
        loadMerchantRanking()
    }

    private fun loadData() {
        val (monthStart, monthEnd) = getCurrentMonthRange()
        loadCategorySums(monthStart, monthEnd)
        loadDailySums()
        loadMerchantRanking()
    }

    private fun loadMerchantRanking() {
        merchantRankingJob?.cancel()
        merchantRankingJob = viewModelScope.launch {
            transactionRepository.getMerchantRanking(_startDate.value, _endDate.value)
                .collect { _merchantRanking.value = it }
        }
    }

    private fun loadDailySums() {
        dailySumsJob?.cancel()
        dailySumsJob = viewModelScope.launch {
            transactionRepository.getDailySums(_chartType.value, _startDate.value, _endDate.value)
                .collect { sums ->
                    _dailySums.value = fillDailyGaps(sums, _startDate.value, _endDate.value)
                    _totalInRange.value = sums.sumOf { it.total }
                }
        }
    }

    private fun fillDailyGaps(sums: List<DailySum>, start: Long, end: Long): List<DailySum> {
        if (sums.isEmpty()) return emptyList()
        val sumMap = sums.associateBy { (it.dayTimestamp / 86400000) * 86400000 }
        val result = mutableListOf<DailySum>()
        val dayMs = 86400000L
        var currentDay = (start / dayMs) * dayMs
        val endDay = (end / dayMs) * dayMs
        while (currentDay <= endDay) {
            val existing = sumMap[currentDay]
            result.add(if (existing != null) existing else DailySum(currentDay, 0.0))
            currentDay += dayMs
        }
        return result
    }

    private fun loadCategorySums(start: Long, end: Long) {
        categorySumsJob?.cancel()
        categorySumsJob = viewModelScope.launch {
            launch {
                transactionRepository.getCategorySums("EXPENSE", start, end)
                    .collect { _expenseCategorySums.value = it }
            }
            launch {
                transactionRepository.getCategorySums("INCOME", start, end)
                    .collect { _incomeCategorySums.value = it }
            }
            launch {
                transactionRepository.getTotalExpenseBetween(start, end)
                    .collect { _totalExpense.value = it ?: 0.0 }
            }
            launch {
                transactionRepository.getTotalIncomeBetween(start, end)
                    .collect { _totalIncome.value = it ?: 0.0 }
            }
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    private fun getDefaultStartDate(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDefaultEndDate(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
