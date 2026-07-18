package com.sudeng.zhangben.ui.screen.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.ui.theme.IncomeGreen
import com.sudeng.zhangben.ui.theme.WarningOrange
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val monthlyExpense by viewModel.monthlyExpense.collectAsStateWithLifecycle()
    val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
    val categoryItems by viewModel.categoryItems.collectAsStateWithLifecycle()
    var showTotalDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryBudgetItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("月度总预算", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("支出", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                            Text(
                                "¥%.2f".format(monthlyExpense),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                        Text(" / ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("预算", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "¥%.0f".format(monthlyBudget),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { showTotalDialog = true }) {
                                    Text(if (monthlyBudget > 0) "修改" else "设置")
                                }
                            }
                        }
                    }

                    if (monthlyBudget > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val progress = (monthlyExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)
                        val progressColor = when {
                            progress >= 1f -> ExpenseRed
                            progress >= 0.8f -> WarningOrange
                            else -> IncomeGreen
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val remaining = monthlyBudget - monthlyExpense
                        Row {
                            if (remaining >= 0) {
                                Text(
                                    "剩余 ¥%.2f (%.0f%%)".format(remaining, (1 - progress) * 100),
                                    style = MaterialTheme.typography.bodySmall, color = IncomeGreen, fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    "超支 ¥%.2f".format(-remaining),
                                    style = MaterialTheme.typography.bodySmall, color = ExpenseRed, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("收入: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥%.2f".format(monthlyIncome), style = MaterialTheme.typography.bodySmall, color = IncomeGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("分类预算", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(categoryItems, key = { it.category.id }) { item ->
            CategoryBudgetRow(
                item = item,
                onClick = { editingCategory = item }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showTotalDialog) {
        BudgetAmountDialog(
            title = "设置月度总预算",
            currentAmount = monthlyBudget,
            onDismiss = { showTotalDialog = false },
            onConfirm = { viewModel.setMonthlyBudget(it); showTotalDialog = false }
        )
    }

    editingCategory?.let { item ->
        BudgetAmountDialog(
            title = "设置 ${item.category.icon} ${item.category.name} 预算",
            currentAmount = item.budget,
            onDismiss = { editingCategory = null },
            onConfirm = {
                viewModel.setCategoryBudget(item.category.id, it)
                editingCategory = null
            }
        )
    }
}

@Composable
private fun CategoryBudgetRow(
    item: CategoryBudgetItem,
    onClick: () -> Unit
) {
    val hasBudget = item.budget > 0
    val progress = if (hasBudget) (item.spent / item.budget).toFloat().coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress >= 1f -> ExpenseRed
        progress >= 0.8f -> WarningOrange
        else -> IncomeGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.category.icon, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    item.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "¥%.2f".format(item.spent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (hasBudget) {
                    Text(
                        " / ¥%.0f".format(item.budget),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasBudget) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (item.spent <= item.budget) "剩余 ¥%.2f".format(item.budget - item.spent)
                    else "超支 ¥%.2f".format(item.spent - item.budget),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress >= 1f) ExpenseRed else progressColor
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "点击设置预算",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BudgetAmountDialog(
    title: String,
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember(currentAmount) {
        mutableStateOf(if (currentAmount > 0) currentAmount.toInt().toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) amount = newValue
                },
                label = { Text("预算金额") },
                prefix = { Text("¥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
