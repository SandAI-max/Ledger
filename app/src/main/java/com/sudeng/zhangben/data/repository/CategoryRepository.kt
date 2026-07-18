package com.sudeng.zhangben.data.repository

import com.sudeng.zhangben.data.local.dao.CategoryDao
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    fun getCount(): Flow<Long> = categoryDao.getCount()

    suspend fun getAllCategoriesList(): List<CategoryEntity> =
        categoryDao.getAllCategoriesList()

    suspend fun insertCategories(categories: List<CategoryEntity>) {
        categoryDao.insertCategories(categories)
    }

    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }
}
