package com.sudeng.zhangben.data.repository

import com.sudeng.zhangben.data.local.dao.CategorySum
import com.sudeng.zhangben.data.local.dao.DailySum
import com.sudeng.zhangben.data.local.dao.MerchantRank
import com.sudeng.zhangben.data.local.dao.TransactionDao
import com.sudeng.zhangben.data.local.entity.TransactionEntity
import com.sudeng.zhangben.data.local.entity.TransactionSource
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategory()

    fun getTransactionsWithCategoryByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsWithCategoryByDateRange(startTime, endTime)

    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startTime, endTime)

    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalExpenseBetween(startTime, endTime)

    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalIncomeBetween(startTime, endTime)

    fun getCategorySums(type: String, startTime: Long, endTime: Long): Flow<List<CategorySum>> =
        transactionDao.getCategorySums(type, startTime, endTime)

    fun getDailySums(type: String, startTime: Long, endTime: Long): Flow<List<DailySum>> =
        transactionDao.getDailySums(type, startTime, endTime)

    fun getMerchantRanking(startTime: Long, endTime: Long): Flow<List<MerchantRank>> =
        transactionDao.getMerchantRanking(startTime, endTime)

    suspend fun getAllTransactionsWithCategoryList(): List<TransactionWithCategory> =
        transactionDao.getAllTransactionsWithCategoryList()

    suspend fun getTransactionWithCategoryById(id: Long): TransactionWithCategory? =
        transactionDao.getTransactionWithCategoryById(id)

    suspend fun addTransaction(
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        note: String,
        timestamp: Long,
        source: TransactionSource = TransactionSource.MANUAL
    ): Long {
        val entity = TransactionEntity(
            amount = amount,
            type = type,
            categoryId = categoryId,
            note = note,
            timestamp = timestamp,
            source = source
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }
}
