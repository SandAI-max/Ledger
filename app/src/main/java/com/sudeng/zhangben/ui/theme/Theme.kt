package com.sudeng.zhangben.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GlassSurfaceLight = Color(0xFFF0F2F8)
private val GlassSurfaceDark = Color(0xFF0D111A)

private val LightColorScheme = lightColorScheme(
    primary = Blue60,
    onPrimary = Color.White,
    primaryContainer = Blue20,
    onPrimaryContainer = Blue90,
    secondary = Blue40,
    onSecondary = Color.White,
    secondaryContainer = Blue10,
    onSecondaryContainer = Blue80,
    tertiary = Blue30,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8EFF9),
    background = GlassSurfaceLight,
    surface = GlassSurfaceLight,
    surfaceVariant = Color(0xFFE2E7F0),
    onBackground = Color(0xFF1A1C20),
    onSurface = Color(0xFF1A1C20),
    onSurfaceVariant = Color(0xFF5C6370)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue30,
    onPrimary = Blue90,
    primaryContainer = Blue60,
    onPrimaryContainer = Blue10,
    secondary = Blue40,
    onSecondary = Color.White,
    secondaryContainer = Blue80,
    onSecondaryContainer = Blue10,
    tertiary = Blue20,
    onTertiary = Blue90,
    tertiaryContainer = Color(0xFF1B2A45),
    background = GlassSurfaceDark,
    surface = GlassSurfaceDark,
    surfaceVariant = Color(0xFF2A3040),
    onBackground = Color(0xFFE2E4E9),
    onSurface = Color(0xFFE2E4E9),
    onSurfaceVariant = Color(0xFFB8BCC6)
)

@Composable
fun 自动化记账本Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
