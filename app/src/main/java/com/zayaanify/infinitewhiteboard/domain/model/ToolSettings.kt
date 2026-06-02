package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.graphics.Color

data class ToolSettings(
    val selectedTool: DrawingTool = DrawingTool.Pen(),  // 'selectedTool' not 'tool'
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val textColor: Color = Color.Black,
    val textSize: Float = 24f,
    val opacity: Float = 1f
)