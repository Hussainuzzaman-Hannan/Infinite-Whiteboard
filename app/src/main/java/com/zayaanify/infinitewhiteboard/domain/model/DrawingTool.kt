package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.graphics.Color

sealed class DrawingTool {
    data object Pen : DrawingTool()
    data object Highlighter : DrawingTool()
    data object Eraser : DrawingTool()
    data object Select : DrawingTool()
    data object Pan : DrawingTool()
    data object Text : DrawingTool()
    data object StickyNote : DrawingTool()
    data object Image : DrawingTool()

    sealed class Shape : DrawingTool() {
        data object Rectangle : Shape()
        data object Circle : Shape()
        data object Line : Shape()
        data object Arrow : Shape()
    }
}

data class ToolSettings(
    val tool: DrawingTool = DrawingTool.Pen,
    val strokeColor: Color = Color.Black,
    val fillColor: Color = Color.Transparent,
    val strokeWidth: Float = 4f,
    val opacity: Float = 1f,
    val fontSize: Float = 16f,
    val textColor: Color = Color.Black,
    val isFillEnabled: Boolean = false
)