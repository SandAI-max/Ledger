package com.sudeng.zhangben.ui.screen.ocr

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.TransactionType
import com.sudeng.zhangben.ui.theme.Blue60
import com.sudeng.zhangben.ui.theme.ExpenseRed
import com.sudeng.zhangben.ui.theme.IncomeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val amountRegex = Regex("^\\d*\\.?\\d{0,2}$")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OcrUploadScreen(
    onBack: () -> Unit,
    viewModel: OcrUploadViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // 多图片选择器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.size > 10) return@rememberLauncherForActivityResult
        val remainingSlots = 10 - imageUris.size
        if (remainingSlots <= 0) return@rememberLauncherForActivityResult
        val validUris = uris.take(remainingSlots)
        coroutineScope.launch {
            val bitmaps = mutableListOf<Bitmap>()
            val timestamps = mutableListOf<Long>()
            withContext(Dispatchers.IO) {
                validUris.forEach { uri ->
                    try {
                        val bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        bitmaps.add(bmp)
                        // 读取照片拍摄时间
                        val cursor = context.contentResolver.query(
                            uri, arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN),
                            null, null, null
                        )
                        val ts = cursor?.use {
                            if (it.moveToFirst()) it.getLong(0) else 0L
                        } ?: 0L
                        if (ts > 0) timestamps.add(ts) else timestamps.add(System.currentTimeMillis())
                    } catch (e: Exception) {
                        // skip corrupted images
                    }
                }
            }
            imageUris = imageUris + validUris.take(bitmaps.size)
            viewModel.addImages(bitmaps, timestamps)
        }
    }

    LaunchedEffect(state.allSaved) {
        if (state.allSaved) {
            viewModel.reset()
            onBack()
        }
    }

    val anyNotProcessed = state.items.any { !it.processed && !it.isProcessing }
    val allProcessed = state.items.isNotEmpty() && state.items.all { it.processed }
    val hasDataToSave = state.items.any { it.processed && (it.amount ?: 0.0) > 0 }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("截图识别") },
            navigationIcon = {
                IconButton(onClick = { viewModel.reset(); onBack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 未选择图片时
            if (state.items.isEmpty() && !state.isProcessing) {
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "批量截图识别",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "一次性选择最多10张支付截图\n自动识别金额、商户并智能分类排序",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { launcher.launch("image/*") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择截图（最多10张）", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // 进度条
            if (state.isProcessing) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("正在识别 ${state.processedCount}/${state.totalCount} ...",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = if (state.totalCount > 0) state.processedCount.toFloat() / state.totalCount else 0f,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            // 图片缩略图网格
            if (state.items.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "已选 ${state.totalCount} 张",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (anyNotProcessed) {
                            Button(
                                onClick = { viewModel.startBatchOcr() },
                                enabled = !state.isProcessing,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("开始识别")
                            }
                        }
                        if (state.items.size < 10) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { launcher.launch("image/*") },
                                enabled = !state.isProcessing,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("+ 添加")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 缩略图行
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.items.forEachIndexed { index, item ->
                            if (index < imageUris.size) {
                                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUris[index]),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            val removeIndex = index
                                            viewModel.removeItem(item.id)
                                            imageUris = imageUris.filterIndexed { i, _ -> i != removeIndex }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "删除",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 识别结果列表
                if (allProcessed) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "识别结果",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "共 ${state.items.count { (it.amount ?: 0.0) > 0 }} 条",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // 识别结果卡片
            itemsIndexed(
                state.items.filter { it.processed },
                key = { _, item -> item.id }
            ) { index, item ->
                OcrResultCard(
                    index = index,
                    item = item,
                    categories = state.categories,
                    onCategoryChanged = { viewModel.updateItemCategory(item.id, it) },
                    onAmountChanged = { viewModel.updateItemAmount(item.id, it) },
                    onNoteChanged = { viewModel.updateItemNote(item.id, it) },
                    onTypeChanged = { viewModel.updateItemType(item.id, it) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 失败项
            itemsIndexed(
                state.items.filter { !it.processed && it.error != null },
                key = { _, item -> item.id }
            ) { _, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseRed.copy(alpha = 0.06f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        item.error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 全部保存按钮
            if (hasDataToSave && !state.isProcessing) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.saveAll() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue60
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "全部保存（${state.items.count { (it.amount ?: 0.0) > 0 }} 条）",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrResultCard(
    index: Int,
    item: OcrItem,
    categories: List<CategoryEntity>,
    onCategoryChanged: (Long?) -> Unit,
    onAmountChanged: (Double?) -> Unit,
    onNoteChanged: (String) -> Unit,
    onTypeChanged: (TransactionType) -> Unit
) {
    val sdf = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
    } }
    val timeStr = item.timestamp?.let { sdf.format(Date(it)) } ?: item.dateStr ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (timeStr.isNotEmpty()) {
                    Text(
                        timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val isExpense = item.transactionType == TransactionType.EXPENSE
                FilterChip(
                    selected = isExpense,
                    onClick = { onTypeChanged(TransactionType.EXPENSE) },
                    label = { Text("支出", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        selectedLabelColor = ExpenseRed
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = !isExpense,
                    onClick = { onTypeChanged(TransactionType.INCOME) },
                    label = { Text("收入", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        selectedLabelColor = IncomeGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (item.amount != null) "%.2f".format(item.amount) else "",
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || amountRegex.matches(newValue)) {
                            onAmountChanged(newValue.toDoubleOrNull())
                        }
                    },
                    label = { Text("金额") },
                    prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(160.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.merchant ?: item.note.ifEmpty { "" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.dateStr?.let { d ->
                        Text(
                            d,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 分类选择
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val expenseCategories = categories.filter { it.type.name == "EXPENSE" }.take(6)
                expenseCategories.forEach { cat ->
                    FilterChip(
                        selected = item.categoryId == cat.id,
                        onClick = { onCategoryChanged(cat.id) },
                        label = { Text("${cat.icon} ${cat.name}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = item.note,
                onValueChange = { onNoteChanged(it) },
                label = { Text("备注") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
    }
}
