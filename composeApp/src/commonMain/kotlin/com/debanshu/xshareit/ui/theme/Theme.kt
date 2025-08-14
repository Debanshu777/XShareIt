package com.debanshu.xshareit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme

/**
 * Main theme composable for the XShareIt app
 *
 * @param darkTheme Whether to use dark theme, defaults to system setting
 * @param content The content to be themed
 */
@Composable
fun XShareItTheme(
    shapes: Shapes = XShareItTheme.shapes,
    typography: Typography = XShareItTheme.typography,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberDynamicColorScheme(
        Color(0xFF4285F4),
        useDarkTheme,
        isAmoled = true
    )
    CompositionLocalProvider() {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

object XShareItTheme {
    val dimensions: Dimensions
        @Composable @ReadOnlyComposable get() = Dimensions

    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable @ReadOnlyComposable get() = Typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = AppShapes
}