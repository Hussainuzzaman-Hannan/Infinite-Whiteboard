package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.geometry.Offset

data class CanvasTransform(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
) {
    companion object {
        val INITIAL = CanvasTransform()
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 5f
    }

    fun screenToCanvas(screenPoint: Offset): Offset = Offset(
        x = (screenPoint.x - offset.x) / scale,
        y = (screenPoint.y - offset.y) / scale
    )

    fun canvasToScreen(canvasPoint: Offset): Offset = Offset(
        x = canvasPoint.x * scale + offset.x,
        y = canvasPoint.y * scale + offset.y
    )

    fun zoomAt(centroid: Offset, zoomFactor: Float): CanvasTransform {
        val newScale = (scale * zoomFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        val newOffset = Offset(
            x = centroid.x - (centroid.x - offset.x) * (newScale / scale),
            y = centroid.y - (centroid.y - offset.y) * (newScale / scale)
        )
        return copy(scale = newScale, offset = newOffset)
    }

    fun pan(delta: Offset): CanvasTransform =
        copy(offset = offset + delta)
}

data class CanvasState(
    val transform: CanvasTransform = CanvasTransform.INITIAL,
    val elements: List<DrawingElement> = emptyList(),
    val selectedElementId: String? = null,
    val isDrawing: Boolean = false,
    val currentPath: PathElement? = null,
    val currentShape: ShapeElement? = null
)