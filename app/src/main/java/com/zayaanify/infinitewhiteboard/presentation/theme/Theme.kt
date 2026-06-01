// presentation/theme/Theme.kt
package com.zayaanify.whiteboard.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    background = Color(0xFFF8F9FA),
    outline = Color(0xFFE5E7EB)
)

@Composable
fun WhiteboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}