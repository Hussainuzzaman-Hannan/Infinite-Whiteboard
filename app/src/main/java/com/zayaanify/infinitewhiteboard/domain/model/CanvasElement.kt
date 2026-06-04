package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

sealed interface CanvasElement {
    val id: String
    val pageId: String
    val createdAt: Long
    val zIndex: Int
}

data class PathElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val zIndex: Int = 0,
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float = 1f,
    val isEraser: Boolean = false,
    val tool: DrawingTool? = null
) : CanvasElement

data class ShapeElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val zIndex: Int = 0,
    val shapeType: DrawingTool.Shape,
    val startOffset: Offset,
    val endOffset: Offset,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float = 1f
) : CanvasElement

data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val zIndex: Int = 0,
    val text: String,
    val position: Offset,
    val color: Color = Color.Black,
    val textSize: Float = 24f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isEditing: Boolean = false
) : CanvasElement

data class StickyNoteElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val zIndex: Int = 0,
    val text: String,
    val position: Offset,
    val color: Color = Color.Yellow,
    val width: Float = 200f,
    val height: Float = 200f,
    val fontSize: Float = 14f
) : CanvasElement

data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val zIndex: Int = 0,
    val imagePath: String,
    val position: Offset,
    val width: Float,
    val height: Float
) : CanvasElement