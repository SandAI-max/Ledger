package com.sudeng.zhangben.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sudeng.zhangben.ui.screen.budget.BudgetScreen
import com.sudeng.zhangben.ui.screen.home.HomeScreen
import com.sudeng.zhangben.ui.screen.ocr.OcrUploadScreen
import com.sudeng.zhangben.ui.screen.profile.ProfileScreen
import com.sudeng.zhangben.ui.screen.statistics.StatisticsScreen
import com.sudeng.zhangben.ui.screen.transaction.AddTransactionScreen
import com.sudeng.zhangben.ui.screen.transaction.TransactionDetailScreen
import com.sudeng.zhangben.ui.screen.transaction.TransactionListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showTopBar = currentRoute in Screen.bottomNavItems.map { it.route }
    val isHome = currentRoute == Screen.Home.route

    Scaffold(
        topBar = {
            if (showTopBar) {
                val displayTitle = when (currentRoute) {
                    Screen.Home.route -> "苏苏记账"
                    Screen.Transactions.route -> "消费账单"
                    Screen.Statistics.route -> "数据分析"
                    Screen.Budget.route -> "预算管理"
                    Screen.Profile.route -> "个人中心"
                    else -> ""
                }
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            displayTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (isHome) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.92f,
                        animationSpec = spring(dampingRatio = 0.35f, stiffness = 600f)
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.scale(scale)
                            )
                        },
                        label = {
                            Text(
                                screen.title,
                                modifier = Modifier.scale(scale)
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                ) + fadeIn(tween(200))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { -it / 12 },
                    animationSpec = tween(150)
                ) + fadeOut(tween(150))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddClick = { navController.navigate(Screen.AddTransaction.route) },
                    onOcrClick = { navController.navigate(Screen.OcrUpload.route) },
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    },
                    onBudgetClick = {
                        navController.navigate(Screen.Budget.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.Budget.route) {
                BudgetScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.OcrUpload.route) {
                OcrUploadScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                TransactionDetailScreen(
                    transactionId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
