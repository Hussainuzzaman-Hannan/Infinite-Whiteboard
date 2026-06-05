package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.graphics.Color

data class ToolSettings(
    val selectedTool: DrawingTool = DrawingTool.Pen(),
    val strokeColor: Color = Color.White,  // সাদা পেন (SoftBoard স্টাইল)
    val strokeWidth: Float = 5f,
    val textColor: Color = Color.White,    // সাদা টেক্সট
    val textSize: Float = 24f,
    val stickyNoteTextSize: Float = 14f,
    val opacity: Float = 1f
)