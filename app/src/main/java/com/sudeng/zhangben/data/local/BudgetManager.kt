package com.sudeng.zhangben.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget")

@Singleton
class BudgetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val monthlyBudgetKey = doublePreferencesKey("monthly_budget")
    private val categoryBudgetsKey = stringPreferencesKey("category_budgets")

    val monthlyBudget: Flow<Double> = context.budgetDataStore.data.map { prefs ->
        prefs[monthlyBudgetKey] ?: 0.0
    }

    val categoryBudgets: Flow<Map<Long, Double>> = context.budgetDataStore.data.map { prefs ->
        val raw = prefs[categoryBudgetsKey] ?: ""
        parseCategoryBudgets(raw)
    }

    suspend fun setMonthlyBudget(amount: Double) {
        context.budgetDataStore.edit { prefs ->
            prefs[monthlyBudgetKey] = amount
        }
    }

    suspend fun setCategoryBudget(categoryId: Long, amount: Double) {
        context.budgetDataStore.edit { prefs ->
            val raw = prefs[categoryBudgetsKey] ?: ""
            val map = parseCategoryBudgets(raw).toMutableMap()
            if (amount > 0) {
                map[categoryId] = amount
            } else {
                map.remove(categoryId)
            }
            prefs[categoryBudgetsKey] = encodeCategoryBudgets(map)
        }
    }

    suspend fun removeCategoryBudget(categoryId: Long) {
        setCategoryBudget(categoryId, 0.0)
    }

    private fun parseCategoryBudgets(raw: String): Map<Long, Double> {
        if (raw.isEmpty()) return emptyMap()
        return raw.split(";").mapNotNull { part ->
            val parts = part.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val amount = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                id to amount
            } else null
        }.toMap()
    }

    private fun encodeCategoryBudgets(map: Map<Long, Double>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }
}
