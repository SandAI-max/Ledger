package com.sudeng.zhangben.ui.screen.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudeng.zhangben.data.local.dao.CategorySum
import com.sudeng.zhangben.data.local.dao.DailySum
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.ui.theme.GrayText
import com.sudeng.zhangben.ui.theme.IncomeGreen
import com.sudeng.zhangben.ui.theme.MedalBronze
import com.sudeng.zhangben.ui.theme.MedalGold
import com.sudeng.zhangben.ui.theme.MedalSilver
import com.sudeng.zhangben.util.beijingDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val chartColors = listOf(
    ExpenseRed, Color(0xFF1E88E5), IncomeGreen,
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
    Color(0xFFF4511E), Color(0xFF3949AB), Color(0xFF7CB342),
    Color(0xFF5E35B1)
)
private val lineColor = Color(0xFF1A56D0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val expenseCategorySums by viewModel.expenseCategorySums.collectAsStateWithLifecycle()
    val incomeCategorySums by viewModel.incomeCategorySums.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val dailySums by viewModel.dailySums.collectAsStateWithLifecycle()
    val chartType by viewModel.chartType.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val totalInRange by viewModel.totalInRange.collectAsStateWithLifecycle()
    val merchantRanking by viewModel.merchantRanking.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { beijingDateFormat("yyyy-MM-dd") }

    val chartColor = if (chartType == "EXPENSE") ExpenseRed else IncomeGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryCard(
                title = "支出",
                icon = "\uD83D\uDED2",
                amount = totalExpense,
                color = ExpenseRed,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            SummaryCard(
                title = "收入",
                icon = "\uD83D\uDCB0",
                amount = totalIncome,
                color = IncomeGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateButton(
                label = "起始",
                date = dateFormat.format(Date(startDate)),
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            DateButton(
                label = "截止",
                date = dateFormat.format(Date(endDate)),
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = chartType == "EXPENSE",
                onClick = { viewModel.setChartType("EXPENSE") },
                label = { Text("支出") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                    selectedLabelColor = ExpenseRed
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = chartType == "INCOME",
                onClick = { viewModel.setChartType("INCOME") },
                label = { Text("收入") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                    selectedLabelColor = IncomeGreen
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (chartType == "EXPENSE") "支出趋势" else "收入趋势",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "合计 ¥%.2f".format(totalInRange),
                        style = MaterialTheme.typography.bodySmall,
                        color = chartColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LineChart(
                    dailySums = dailySums,
                    color = chartColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        if (expenseCategorySums.isNotEmpty() && totalExpense > 0) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("支出分类占比")
            PieChartWithLegend(expenseCategorySums, totalExpense)
        }

        if (incomeCategorySums.isNotEmpty() && totalIncome > 0) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("收入分类占比")
            PieChartWithLegend(incomeCategorySums, totalIncome)
        }

        if (merchantRanking.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("商户消费排行")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    merchantRanking.forEachIndexed { index, rank ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (index) {
                                    0 -> MedalGold
                                    1 -> MedalSilver
                                    2 -> MedalBronze
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                rank.merchantName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "¥%.2f".format(rank.total),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${rank.count}笔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showStartPicker) {
        var selectedMillis by remember { mutableStateOf(startDate) }
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartPicker = false
                    if (selectedMillis != startDate) {
                        viewModel.setDateRange(selectedMillis, endDate)
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = startDate,
                    initialDisplayMode = DisplayMode.Picker
                ).also { state ->
                    selectedMillis = state.selectedDateMillis ?: startDate
                }
            )
        }
    }

    if (showEndPicker) {
        var selectedMillis by remember { mutableStateOf(endDate) }
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEndPicker = false
                    if (selectedMillis != endDate) {
                        viewModel.setDateRange(startDate, endOfDay(selectedMillis))
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = endDate,
                    initialDisplayMode = DisplayMode.Picker
                ).also { state ->
                    selectedMillis = state.selectedDateMillis ?: endDate
                }
            )
        }
    }
}

private fun endOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

@Composable
private fun DateButton(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LineChart(
    dailySums: List<DailySum>,
    color: Color,
    modifier: Modifier
) {
    if (dailySums.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }

    val dateFormat = remember { beijingDateFormat("MM/dd") }

    Canvas(modifier = modifier) {
        val paddingLeft = 44f
        val paddingRight = 16f
        val paddingTop = 12f
        val paddingBottom = 36f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val maxValue = dailySums.maxOf { it.total }.coerceAtLeast(1.0)
        val minValue = 0.0
        val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

        val points = dailySums.mapIndexed { index, sum ->
            val x = paddingLeft + (index.toFloat() / (dailySums.size - 1).coerceAtLeast(1)) * chartWidth
            val y = paddingTop + chartHeight - ((sum.total - minValue) / valueRange * chartHeight).toFloat()
            Offset(x, y)
        }

        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx1 = (prev.x + curr.x) / 2
                    val cy1 = prev.y
                    val cx2 = (prev.x + curr.x) / 2
                    val cy2 = curr.y
                    cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, paddingTop + chartHeight)
                lineTo(points.first().x, paddingTop + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                color = color.copy(alpha = 0.08f)
            )
        }

        val maxLabels = when {
            dailySums.size <= 8 -> dailySums.size
            dailySums.size <= 14 -> (dailySums.size + 1) / 2
            else -> 7
        }
        val lastIdx = dailySums.size - 1
        val distinctIndices = mutableSetOf<Int>()
        for (i in 0 until maxLabels) {
            val index = if (maxLabels == 1) 0
            else (i * lastIdx.toFloat() / (maxLabels - 1)).roundToInt().coerceIn(0, lastIdx)
            if (distinctIndices.add(index)) {
                val x = paddingLeft + (index.toFloat() / lastIdx.coerceAtLeast(1)) * chartWidth
                val label = dateFormat.format(Date(dailySums[index].dayTimestamp))
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    size.height - 4f,
                    android.graphics.Paint().apply {
                        setColor(0xFF9E9E9E.toInt())
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }

        points.forEach { point ->
            drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = point)
            drawCircle(color = color, radius = 2.5.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
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
private fun PieChartWithLegend(categorySums: List<CategorySum>, total: Double) {
    val topSlices = categorySums.take(chartColors.size)
    val otherTotal = categorySums.drop(chartColors.size).sumOf { it.totalAmount }

    val slices = topSlices.mapIndexed { index, cat ->
        SliceData(
            label = cat.categoryName ?: "其他",
            value = cat.totalAmount.toFloat(),
            color = chartColors[index]
        )
    } + if (otherTotal > 0) listOf(
        SliceData("其他", otherTotal.toFloat(), GrayText)
    ) else emptyList()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 32.dp.toPx()
                val halfStroke = strokeWidth / 2
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - strokeWidth) / 2 - radius + halfStroke,
                    (size.height - strokeWidth) / 2 - radius + halfStroke
                )
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                slices.forEach { slice ->
                    val sweepAngle = (slice.value / total.toFloat()) * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            slices.forEach { slice ->
                val percent = ((slice.value / total.toFloat()) * 100).toInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = slice.color)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "¥%.2f".format(slice.value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class SliceData(
    val label: String,
    val value: Float,
    val color: Color
)
