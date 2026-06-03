package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.graphics.Color

data class ToolSettings(
    val selectedTool: DrawingTool = DrawingTool.Pen(),
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val textColor: Color = Color.Black,
    val textSize: Float = 24f,      // টেক্সটের জন্য ফন্ট সাইজ
    val stickyNoteTextSize: Float = 14f,  // স্টিকি নোটের জন্য ফন্ট সাইজ
    val opacity: Float = 1f
)