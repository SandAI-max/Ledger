package com.sudeng.zhangben.domain.model

import com.sudeng.zhangben.data.local.entity.CategoryType
import com.sudeng.zhangben.data.local.entity.TransactionSource
import com.sudeng.zhangben.data.local.entity.TransactionType

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long? = null,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val note: String = "",
    val timestamp: Long,
    val source: TransactionSource
)

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val type: CategoryType
)
