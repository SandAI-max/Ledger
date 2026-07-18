package com.sudeng.zhangben.ui.screen.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.data.local.entity.TransactionWithCategory
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.ui.theme.GrayLight
import com.sudeng.zhangben.ui.theme.IncomeGreen
import com.sudeng.zhangben.util.ExportFormat
import com.sudeng.zhangben.util.beijingDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DateGroup(val label: String, val items: List<TransactionWithCategory>)

@Composable
fun TransactionListScreen(
    onTransactionClick: (Long) -> Unit = {},
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val shareIntent by viewModel.shareIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(shareIntent) {
        shareIntent?.let { intent ->
            context.startActivity(android.content.Intent.createChooser(intent, "导出账单"))
            viewModel.clearShareIntent()
        }
    }

    val groups = remember(transactions) { groupByDate(transactions) }
    var searchText by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }
    var deleteConfirmItem by remember { mutableStateOf<TransactionWithCategory?>(null) }
    val filteredTransactions = remember(transactions, searchText, filterType) {
        transactions.filter { item ->
            val matchType = filterType == "ALL" || item.transaction.type.name == filterType
            val matchSearch = searchText.isEmpty() ||
                    (item.category?.name?.contains(searchText, ignoreCase = true) == true) ||
                    item.transaction.note.contains(searchText, ignoreCase = true)
            matchType && matchSearch
        }
    }
    val filteredGroups = remember(filteredTransactions) { groupByDate(filteredTransactions) }
    var showExportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filteredTransactions.size} 条记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(Icons.Filled.Share, contentDescription = "导出")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("搜索分类或备注...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                listOf("ALL" to "全部", "EXPENSE" to "支出", "INCOME" to "收入").forEach { (type, label) ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { filterType = type },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Text(
                    if (searchText.isNotEmpty() || filterType != "ALL")
                        "未找到匹配的记录" else "暂无账单记录，点击首页 + 记一笔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        } else {
            filteredGroups.forEach { group ->
                item {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(group.items, key = { it.transaction.id }) { item ->
                    TransactionRow(
                        item = item,
                        onClick = { onTransactionClick(item.transaction.id) },
                        onDelete = { deleteConfirmItem = item }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        ExportFormatDialog(
            onDismiss = { showExportDialog = false },
            onSelect = { format ->
                viewModel.exportCsv(format)
                showExportDialog = false
            }
                )
    }

    deleteConfirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(item.transaction)
                    deleteConfirmItem = null
                }) { Text("删除", color = ExpenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) { Text("取消") }
            }
        )
    }
}

private fun groupByDate(transactions: List<TransactionWithCategory>): List<DateGroup> {
    if (transactions.isEmpty()) return emptyList()

    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
    val thisWeekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val thisMonthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val today = mutableListOf<TransactionWithCategory>()
    val yesterday = mutableListOf<TransactionWithCategory>()
    val thisWeek = mutableListOf<TransactionWithCategory>()
    val thisMonth = mutableListOf<TransactionWithCategory>()
    val earlier = mutableListOf<TransactionWithCategory>()

    transactions.forEach { item ->
        when {
            item.transaction.timestamp >= todayStart -> today.add(item)
            item.transaction.timestamp >= yesterdayStart -> yesterday.add(item)
            item.transaction.timestamp >= thisWeekStart -> thisWeek.add(item)
            item.transaction.timestamp >= thisMonthStart -> thisMonth.add(item)
            else -> earlier.add(item)
        }
    }

    return listOfNotNull(
        if (today.isNotEmpty()) DateGroup("今天", today) else null,
        if (yesterday.isNotEmpty()) DateGroup("昨天", yesterday) else null,
        if (thisWeek.isNotEmpty()) DateGroup("本周", thisWeek) else null,
        if (thisMonth.isNotEmpty()) DateGroup("本月", thisMonth) else null,
        if (earlier.isNotEmpty()) DateGroup("更早", earlier) else null
    )
}

@Composable
private fun TransactionRow(
    item: TransactionWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val transaction = item.transaction
    val category = item.category
    val timeFormat = remember { beijingDateFormat("HH:mm") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category?.icon ?: "\uD83D\uDCB0",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category?.name ?: (transaction.note.ifEmpty { "其他" }),
                style = MaterialTheme.typography.bodyLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.note.isNotEmpty()) {
                    Text(
                        " · ${transaction.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val prefix = if (transaction.type == TransactionType.INCOME) "+" else "-"
        val amountColor = if (transaction.type == TransactionType.INCOME)
            IncomeGreen else ExpenseRed
        Text(
            "$prefix¥%.2f".format(transaction.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = GrayLight, modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onSelect: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出格式") },
        text = {
            Column {
                ExportFormat.entries.forEach { format ->
                    TextButton(
                        onClick = { onSelect(format) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(format.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "导出为 ${format.label} 格式",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
