package com.sudeng.zhangben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sudeng.zhangben.ui.navigation.AppNavigation
import com.sudeng.zhangben.ui.theme.自动化记账本Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            自动化记账本Theme {
                AppNavigation()
            }
        }
    }
}
