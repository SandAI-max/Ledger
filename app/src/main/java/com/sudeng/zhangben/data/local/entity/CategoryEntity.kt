package com.sudeng.zhangben.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String,
    @ColumnInfo(name = "type") val type: CategoryType,
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)

enum class CategoryType {
    INCOME, EXPENSE
}
