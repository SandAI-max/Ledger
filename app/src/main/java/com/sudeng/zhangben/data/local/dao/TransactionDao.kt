package com.sudeng.zhangben.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sudeng.zhangben.data.local.entity.TransactionEntity
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionWithCategoryById(id: Long): TransactionWithCategory?

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsWithCategoryList(): List<TransactionWithCategory>

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsWithCategoryByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("""
        SELECT c.name AS categoryName, c.icon AS categoryIcon, SUM(t.amount) AS totalAmount
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE t.type = :type AND t.timestamp BETWEEN :startTime AND :endTime
        GROUP BY t.category_id
        ORDER BY totalAmount DESC
    """)
    fun getCategorySums(type: String, startTime: Long, endTime: Long): Flow<List<CategorySum>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = :type AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalAmount(type: String, startTime: Long, endTime: Long): Double?

    @Query("""
        SELECT (t.timestamp / 86400000) * 86400000 AS dayTimestamp,
               SUM(t.amount) AS total
        FROM transactions t
        WHERE t.type = :type AND t.timestamp BETWEEN :startTime AND :endTime
        GROUP BY dayTimestamp
        ORDER BY dayTimestamp
    """)
    fun getDailySums(type: String, startTime: Long, endTime: Long): Flow<List<DailySum>>

    @Query("""
        SELECT COALESCE(NULLIF(t.note, ''), c.name) AS merchantName,
               SUM(t.amount) AS total, COUNT(*) AS count
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE t.type = 'EXPENSE' AND t.timestamp BETWEEN :startTime AND :endTime
        GROUP BY merchantName
        ORDER BY total DESC
        LIMIT 20
    """)
    fun getMerchantRanking(startTime: Long, endTime: Long): Flow<List<MerchantRank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
