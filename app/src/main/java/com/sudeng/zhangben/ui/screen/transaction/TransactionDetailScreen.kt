package com.sudeng.zhangben.ui.screen.transaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudeng.zhangben.data.local.entity.TransactionEntity
import com.sudeng.zhangben.data.local.entity.TransactionSource
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("账单详情") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = {
                    viewModel.deleteTransaction()
                    onBack()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }
        )

        transaction?.let { item ->
            val t = item.transaction
            val cat = item.category
            val isExpense = t.type == TransactionType.EXPENSE
            val amountColor = if (isExpense) ExpenseRed else IncomeGreen
            val typeLabel = if (isExpense) "支出" else "收入"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    cat?.icon ?: "\uD83D\uDCB0",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    cat?.name ?: (t.note.ifEmpty { "其他" }),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    (if (isExpense) "-" else "+") + "¥%.2f".format(t.amount),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(32.dp))

                DetailSection {
                    DetailRow(
                        label = "日期时间",
                        value = formatBeijingTime(t.timestamp)
                    )
                    DetailRow(
                        label = "交易类型",
                        value = typeLabel
                    )
                    DetailRow(
                        label = "分类",
                        value = cat?.name ?: "未分类"
                    )
                    DetailRow(
                        label = "金额",
                        value = "¥%.2f".format(t.amount)
                    )
                    DetailRow(
                        label = "来源",
                        value = when (t.source) {
                            TransactionSource.MANUAL -> "手动记录"
                            TransactionSource.WECHAT -> "微信支付"
                            TransactionSource.ALIPAY -> "支付宝"
                        }
                    )
                    if (t.note.isNotEmpty()) {
                        DetailRow(
                            label = "备注",
                            value = t.note
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到账单", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailSection(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatBeijingTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return sdf.format(Date(timestamp))
}
