package com.sudeng.zhangben.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Filled.Home)
    data object Transactions : Screen("transactions", "消费账单", Icons.Filled.List)
    data object Statistics : Screen("statistics", "数据分析", Icons.Filled.PieChart)
    data object Budget : Screen("budget", "预算管理", Icons.Filled.AccountBalanceWallet)
    data object Profile : Screen("profile", "个人中心", Icons.Filled.Person)
    data object AddTransaction : Screen("add_transaction", "记一笔", Icons.Filled.List)
    data object TransactionDetail : Screen("transaction_detail/{id}", "账单详情", Icons.Filled.List) {
        fun createRoute(id: Long) = "transaction_detail/$id"
    }
    data object OcrUpload : Screen("ocr_upload", "截图识别", Icons.Filled.List) {
        fun createRoute() = "ocr_upload"
    }

    companion object {
        val bottomNavItems = listOf(Home, Transactions, Statistics, Budget, Profile)
    }
}
