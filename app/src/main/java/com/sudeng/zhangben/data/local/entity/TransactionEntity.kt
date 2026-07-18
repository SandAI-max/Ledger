package com.sudeng.zhangben.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "type") val type: TransactionType,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "source") val source: TransactionSource = TransactionSource.MANUAL
)

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionSource {
    MANUAL, WECHAT, ALIPAY
}
