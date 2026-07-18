package com.sudeng.zhangben.ui.screen.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.CategoryType
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.util.ExportFormat

@Composable
fun ProfileScreen(
    onNavigateToTransactions: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val transactionCount by viewModel.transactionCount.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val shareIntent by viewModel.shareIntent.collectAsStateWithLifecycle()

    LaunchedEffect(shareIntent) {
        shareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "导出账单"))
            viewModel.clearShareIntent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\uD83D\uDCB0", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("苏苏记账", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("v1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "共 $transactionCount 条记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("功能", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                ProfileItem(
                    icon = "\uD83D\uDCCB",
                    title = "全部账单",
                    subtitle = "查看所有交易记录",
                    onClick = onNavigateToTransactions
                )
                ProfileItem(
                    icon = "\uD83C\uDFF7\uFE0F",
                    title = "管理分类",
                    subtitle = "编辑、添加或删除分类",
                    onClick = { viewModel.showCategoryManager = true }
                )
                ProfileItem(
                    icon = "\u267F\uFE0F",
                    title = "无障碍服务",
                    subtitle = if (isAccessibilityEnabled) "已开启" else "未开启，点击去设置",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                ProfileItem(
                    icon = "\uD83D\uDD14",
                    title = "通知监听",
                    subtitle = "需在系统设置中授权",
                    onClick = {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("数据管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                ProfileItem(
                    icon = "\uD83D\uDCBE",
                    title = "导出数据",
                    subtitle = "导出账单为 CSV 文件",
                    onClick = { viewModel.showExportDialog = true }
                )
            }
        }
    }

    if (viewModel.showCategoryManager) {
        val cats by viewModel.categories.collectAsStateWithLifecycle()
        CategoryManageDialog(
            categories = cats,
            onDismiss = { viewModel.showCategoryManager = false },
            onAdd = { name, icon, type, parentId -> viewModel.addCategory(name, icon, type, parentId) },
            onUpdate = { cat -> viewModel.updateCategory(cat) },
            onDelete = { cat -> viewModel.deleteCategory(cat) }
        )
    }

    if (viewModel.showExportDialog) {
        ExportFormatDialog(
            onDismiss = { viewModel.showExportDialog = false },
            onSelect = { format ->
                viewModel.exportData(format)
                viewModel.showExportDialog = false
            }
        )
    }
}

@Composable
private fun CategoryManageDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, String, CategoryType, Long?) -> Unit,
    onUpdate: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<CategoryEntity?>(null) }
    var newName by remember { mutableStateOf("") }
    var newIcon by remember { mutableStateOf("\uD83D\uDCB0") }
    var newType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var editName by remember { mutableStateOf("") }
    var editIcon by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分类") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
                val incomeCategories = categories.filter { it.type == CategoryType.INCOME }

                Text("支出分类", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                expenseCategories.forEach { cat ->
                    CategoryRow(
                        cat = cat,
                        onEdit = {
                            editName = cat.name; editIcon = cat.icon
                            showEditDialog = cat
                        },
                        onDelete = { onDelete(cat) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("收入分类", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                incomeCategories.forEach { cat ->
                    CategoryRow(
                        cat = cat,
                        onEdit = {
                            editName = cat.name; editIcon = cat.icon
                            showEditDialog = cat
                        },
                        onDelete = { onDelete(cat) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = {
                    newName = ""; newIcon = "\uD83D\uDCB0"; newType = CategoryType.EXPENSE
                    showAddDialog = true
                }) { Text("+ 添加分类") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    if (showEditDialog != null) {
        val cat = showEditDialog
        if (cat != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = null },
                title = { Text("编辑分类") },
                text = {
                    Column {
                        OutlinedTextField(value = editName, onValueChange = { editName = it },
                            label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = editIcon, onValueChange = { editIcon = it },
                            label = { Text("图标") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editName.isNotBlank()) {
                            onUpdate(cat.copy(name = editName, icon = editIcon))
                            showEditDialog = null
                        }
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { showEditDialog = null }) { Text("取消") } }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加分类") },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it },
                        label = { Text("分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newIcon, onValueChange = { newIcon = it },
                        label = { Text("图标 (emoji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        FilterChip(selected = newType == CategoryType.EXPENSE,
                            onClick = { newType = CategoryType.EXPENSE }, label = { Text("支出") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = newType == CategoryType.INCOME,
                            onClick = { newType = CategoryType.INCOME }, label = { Text("收入") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onAdd(newName, newIcon, newType, null)
                        showAddDialog = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun CategoryRow(
    cat: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${cat.icon} ${cat.name}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f))
        TextButton(onClick = onEdit) {
            Text("编辑", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onDelete) {
            Text("删除", color = ExpenseRed, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProfileItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
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
