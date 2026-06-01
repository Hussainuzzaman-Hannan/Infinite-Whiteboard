package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

sealed class DrawingElement {
    abstract val id: String
    abstract val pageId: String
    abstract val zIndex: Int
}

data class PathElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val zIndex: Int = 0,
    val points: List<Offset> = emptyList(),
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f,
    val opacity: Float = 1f,
    val tool: DrawingTool = DrawingTool.Pen,
    val isEraser: Boolean = false
) : DrawingElement()

data class ShapeElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val zIndex: Int = 0,
    val shapeType: DrawingTool.Shape = DrawingTool.Shape.Rectangle,
    val startOffset: Offset = Offset.Zero,
    val endOffset: Offset = Offset.Zero,
    val color: Color = Color.Black,
    val fillColor: Color = Color.Transparent,
    val strokeWidth: Float = 2f
) : DrawingElement()

data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val zIndex: Int = 0,
    val text: String = "",
    val position: Offset = Offset.Zero,
    val fontSize: Float = 16f,
    val color: Color = Color.Black,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isEditing: Boolean = false,
    val width: Float = 200f
) : DrawingElement()

data class StickyNoteElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val zIndex: Int = 0,
    val text: String = "",
    val position: Offset = Offset.Zero,
    val size: Float = 200f,
    val backgroundColor: Color = Color(0xFFFFF176),
    val textColor: Color = Color(0xFF212121),
    val isEditing: Boolean = false
) : DrawingElement()

data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    override val pageId: String,
    override val zIndex: Int = 0,
    val imagePath: String = "",
    val position: Offset = Offset.Zero,
    val width: Float = 300f,
    val height: Float = 200f,
    val rotation: Float = 0f,
    val isSelected: Boolean = false
) : DrawingElement()