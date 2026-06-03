package com.zayaanify.infinitewhiteboard.data.local

import androidx.room.TypeConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.zayaanify.infinitewhiteboard.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class Converters {

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
    fun fromDrawingTool(tool: DrawingTool): String {
        return Json.encodeToString(tool)
    }

    @TypeConverter
    fun toDrawingTool(data: String): DrawingTool {
        return Json.decodeFromString(data)
    }

    @TypeConverter
    fun fromCanvasElements(elements: List<CanvasElement>): String {
        return Json.encodeToString(elements)
    }

    @TypeConverter
    fun toCanvasElements(data: String): List<CanvasElement> {
        return Json.decodeFromString(data)
    }

    @TypeConverter
    fun fromShapeType(shape: DrawingTool.Shape): String {
        return Json.encodeToString(shape)
    }

    @TypeConverter
    fun toShapeType(data: String): DrawingTool.Shape {
        return Json.decodeFromString(data)
    }
}