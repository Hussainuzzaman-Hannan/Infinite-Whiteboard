package com.zayaanify.infinitewhiteboard.data.local

import androidx.room.TypeConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.zayaanify.infinitewhiteboard.domain.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
sealed interface SerializableCanvasElement

@Serializable
data class SerializablePathElement(
    val id: String,
    val pageId: String,
    val createdAt: Long,
    val zIndex: Int,
    val points: List<Pair<Float, Float>>,
    val color: Long,
    val strokeWidth: Float,
    val opacity: Float,
    val isEraser: Boolean
) : SerializableCanvasElement

@Serializable
data class SerializableShapeElement(
    val id: String,
    val pageId: String,
    val createdAt: Long,
    val zIndex: Int,
    val shapeType: String,
    val startOffset: Pair<Float, Float>,
    val endOffset: Pair<Float, Float>,
    val color: Long,
    val strokeWidth: Float,
    val opacity: Float
) : SerializableCanvasElement

@Serializable
data class SerializableTextElement(
    val id: String,
    val pageId: String,
    val createdAt: Long,
    val zIndex: Int,
    val text: String,
    val position: Pair<Float, Float>,
    val color: Long,
    val textSize: Float,
    val isBold: Boolean,
    val isItalic: Boolean,
    val isEditing: Boolean
) : SerializableCanvasElement

@Serializable
data class SerializableStickyNoteElement(
    val id: String,
    val pageId: String,
    val createdAt: Long,
    val zIndex: Int,
    val text: String,
    val position: Pair<Float, Float>,
    val color: Long,
    val width: Float,
    val height: Float,
    val fontSize: Float
) : SerializableCanvasElement

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromOffset(offset: Offset): String {
        return "${offset.x},${offset.y}"
    }

    @TypeConverter
    fun toOffset(data: String): Offset {
        val parts = data.split(",")
        return Offset(parts[0].toFloat(), parts[1].toFloat())
    }

    @TypeConverter
    fun fromColor(color: Color): Long {
        return color.value.toLong()
    }

    @TypeConverter
    fun toColor(value: Long): Color {
        return Color(value.toULong())
    }

    @TypeConverter
    fun fromCanvasElements(elements: List<CanvasElement>): String {
        val serializableList = elements.map { element ->
            when (element) {
                is PathElement -> {
                    SerializablePathElement(
                        id = element.id,
                        pageId = element.pageId,
                        createdAt = element.createdAt,
                        zIndex = element.zIndex,
                        points = element.points.map { it.x to it.y },
                        color = element.color.value.toLong(),
                        strokeWidth = element.strokeWidth,
                        opacity = element.opacity,
                        isEraser = element.isEraser
                    )
                }
                is ShapeElement -> {
                    SerializableShapeElement(
                        id = element.id,
                        pageId = element.pageId,
                        createdAt = element.createdAt,
                        zIndex = element.zIndex,
                        shapeType = when (element.shapeType) {
                            is DrawingTool.Shape.Rectangle -> "Rectangle"
                            is DrawingTool.Shape.Circle -> "Circle"
                            is DrawingTool.Shape.Line -> "Line"
                            is DrawingTool.Shape.Arrow -> "Arrow"
                            else -> "Rectangle"
                        },
                        startOffset = element.startOffset.x to element.startOffset.y,
                        endOffset = element.endOffset.x to element.endOffset.y,
                        color = element.color.value.toLong(),
                        strokeWidth = element.strokeWidth,
                        opacity = element.opacity
                    )
                }
                is TextElement -> {
                    SerializableTextElement(
                        id = element.id,
                        pageId = element.pageId,
                        createdAt = element.createdAt,
                        zIndex = element.zIndex,
                        text = element.text,
                        position = element.position.x to element.position.y,
                        color = element.color.value.toLong(),
                        textSize = element.textSize,
                        isBold = element.isBold,
                        isItalic = element.isItalic,
                        isEditing = element.isEditing
                    )
                }
                is StickyNoteElement -> {
                    SerializableStickyNoteElement(
                        id = element.id,
                        pageId = element.pageId,
                        createdAt = element.createdAt,
                        zIndex = element.zIndex,
                        text = element.text,
                        position = element.position.x to element.position.y,
                        color = element.color.value.toLong(),
                        width = element.width,
                        height = element.height,
                        fontSize = element.fontSize
                    )
                }
                else -> throw IllegalArgumentException("Unknown element type")
            }
        }
        return json.encodeToString(serializableList)
    }

    @TypeConverter
    fun toCanvasElements(data: String): List<CanvasElement> {
        if (data.isEmpty()) return emptyList()

        val serializableList = json.decodeFromString<List<SerializableCanvasElement>>(data)
        return serializableList.mapNotNull { obj ->
            when (obj) {
                is SerializablePathElement -> {
                    PathElement(
                        id = obj.id,
                        pageId = obj.pageId,
                        createdAt = obj.createdAt,
                        zIndex = obj.zIndex,
                        points = obj.points.map { Offset(it.first, it.second) },
                        color = Color(obj.color.toULong()),
                        strokeWidth = obj.strokeWidth,
                        opacity = obj.opacity,
                        isEraser = obj.isEraser
                    )
                }
                is SerializableShapeElement -> {
                    val shapeType = when (obj.shapeType) {
                        "Rectangle" -> DrawingTool.Shape.Rectangle()
                        "Circle" -> DrawingTool.Shape.Circle()
                        "Line" -> DrawingTool.Shape.Line()
                        "Arrow" -> DrawingTool.Shape.Arrow()
                        else -> DrawingTool.Shape.Rectangle()
                    }
                    ShapeElement(
                        id = obj.id,
                        pageId = obj.pageId,
                        createdAt = obj.createdAt,
                        zIndex = obj.zIndex,
                        shapeType = shapeType,
                        startOffset = Offset(obj.startOffset.first, obj.startOffset.second),
                        endOffset = Offset(obj.endOffset.first, obj.endOffset.second),
                        color = Color(obj.color.toULong()),
                        strokeWidth = obj.strokeWidth,
                        opacity = obj.opacity
                    )
                }
                is SerializableTextElement -> {
                    TextElement(
                        id = obj.id,
                        pageId = obj.pageId,
                        createdAt = obj.createdAt,
                        zIndex = obj.zIndex,
                        text = obj.text,
                        position = Offset(obj.position.first, obj.position.second),
                        color = Color(obj.color.toULong()),
                        textSize = obj.textSize,
                        isBold = obj.isBold,
                        isItalic = obj.isItalic,
                        isEditing = obj.isEditing
                    )
                }
                is SerializableStickyNoteElement -> {
                    StickyNoteElement(
                        id = obj.id,
                        pageId = obj.pageId,
                        createdAt = obj.createdAt,
                        zIndex = obj.zIndex,
                        text = obj.text,
                        position = Offset(obj.position.first, obj.position.second),
                        color = Color(obj.color.toULong()),
                        width = obj.width,
                        height = obj.height,
                        fontSize = obj.fontSize
                    )
                }
                else -> null
            }
        }
    }
}