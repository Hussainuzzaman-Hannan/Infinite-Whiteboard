package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.graphics.Color

sealed class DrawingTool {
    data class Pen(
        val color: Color = Color.Black,
        val strokeWidth: Float = 5f,
        val opacity: Float = 1f
    ) : DrawingTool()

    data class Highlighter(
        val color: Color = Color.Yellow,
        val strokeWidth: Float = 20f,
        val opacity: Float = 0.4f
    ) : DrawingTool()

    data class Eraser(
        val strokeWidth: Float = 20f
    ) : DrawingTool()

    // Text tool - object instead of data class
    object Text : DrawingTool()

    // StickyNote tool - object instead of data class
    object StickyNote : DrawingTool()

    sealed class Shape : DrawingTool() {
        data class Rectangle(
            val color: Color = Color.Black,
            val strokeWidth: Float = 5f
        ) : Shape()

        data class Circle(
            val color: Color = Color.Black,
            val strokeWidth: Float = 5f
        ) : Shape()

        data class Line(
            val color: Color = Color.Black,
            val strokeWidth: Float = 5f
        ) : Shape()

        data class Arrow(
            val color: Color = Color.Black,
            val strokeWidth: Float = 5f
        ) : Shape()
    }
}