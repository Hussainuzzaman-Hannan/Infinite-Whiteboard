package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.geometry.Offset

data class Transform(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
) {
    fun screenToCanvas(screenPoint: Offset): Offset {
        return Offset(
            x = (screenPoint.x - offset.x) / scale,
            y = (screenPoint.y - offset.y) / scale
        )
    }

    fun canvasToScreen(canvasPoint: Offset): Offset {
        return Offset(
            x = canvasPoint.x * scale + offset.x,
            y = canvasPoint.y * scale + offset.y
        )
    }

    companion object {
        val INITIAL = Transform(scale = 1f, offset = Offset.Zero)
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 5f
    }
}