package com.sudeng.zhangben.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import com.sudeng.zhangben.ui.theme.Blue60
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.ui.theme.IncomeGreen
import com.sudeng.zhangben.ui.theme.WarningOrange
import com.sudeng.zhangben.util.beijingDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onOcrClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val monthlyExpense by viewModel.monthlyExpense.collectAsStateWithLifecycle()
    val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }

    val monthLabel = remember {
        val cal = Calendar.getInstance()
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("本月概览", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "支出",
                        icon = "\uD83D\uDED2",
                        amount = monthlyExpense,
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    SummaryCard(
                        title = "收入",
                        icon = "\uD83D\uDCB0",
                        amount = monthlyIncome,
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                CompactBudgetBar(
                    expense = monthlyExpense,
                    budget = monthlyBudget,
                    onClick = onBudgetClick
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Text("最近交易", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (recentTransactions.isEmpty()) {
                item {
                    Text(
                        "暂无记录，点击右下角记一笔",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentTransactions, key = { it.transaction.id }) { item ->
                    TransactionItem(
                        item = item,
                        onClick = { onTransactionClick(item.transaction.id) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        val rotation by animateFloatAsState(
            targetValue = if (fabExpanded) 45f else 0f,
            animationSpec = tween(200)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
                exit = fadeOut(tween(100)) + slideOutVertically(tween(100)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                "截图识别",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        SmallFloatingActionButton(
                            onClick = { fabExpanded = false; onOcrClick() },
                            containerColor = Blue60
                        ) { Icon(Icons.Filled.Image, contentDescription = "截图识别") }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                "记一笔",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        SmallFloatingActionButton(
                            onClick = { fabExpanded = false; onAddClick() },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) { Icon(Icons.Filled.Add, contentDescription = "记一笔") }
                    }
                }
            }

            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "菜单",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun CompactBudgetBar(
    expense: Double,
    budget: Double,
    onClick: () -> Unit
) {
    val progress = if (budget > 0) (expense / budget).coerceIn(0.0, 1.0).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83D\uDCB0", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("月度预算", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                if (budget > 0) {
                    Text(
                        "¥%.0f / ¥%.0f".format(expense, budget),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("点击设置", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (budget > 0) {
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        progress >= 1f -> ExpenseRed
                        progress >= 0.8f -> WarningOrange
                        else -> IncomeGreen
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    icon: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "¥%.2f".format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TransactionItem(item: TransactionWithCategory, onClick: () -> Unit) {
    val dateFormat = beijingDateFormat("MM/dd HH:mm")
    val transaction = item.transaction
    val category = item.category
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category?.icon ?: "\uD83D\uDCB0",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category?.name ?: (transaction.note.ifEmpty { "其他" }),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                dateFormat.format(Date(transaction.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val prefix = if (transaction.type == TransactionType.INCOME) "+" else "-"
        val amountColor = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
        Text(
            "$prefix¥%.2f".format(transaction.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
