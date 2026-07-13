package com.imageflow.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    outline = Outline,
    error = ErrorRed,
    secondary = TagText,
    onSecondary = TagBg
)

/**
 * ImageFlow 永远走暗色主题（Flow 风格）。
 * 即使系统是浅色也强制暗色。
 */
@Composable
fun ImageFlowTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏图标色：暗色主题用浅色图标
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ImageFlowTypography,
        content = content
    )
}
