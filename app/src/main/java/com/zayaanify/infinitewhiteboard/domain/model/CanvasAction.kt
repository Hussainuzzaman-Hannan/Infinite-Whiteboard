package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.geometry.Offset

sealed class CanvasAction {
    data class StartDrawing(val position: Offset) : CanvasAction()
    data class ContinueDrawing(val position: Offset) : CanvasAction()
    data class EndDrawing(val position: Offset) : CanvasAction()

    data class StartShape(val shape: DrawingTool.Shape, val position: Offset) : CanvasAction()
    data class UpdateShape(val position: Offset) : CanvasAction()
    data class EndShape(val position: Offset) : CanvasAction()

    data class Zoom(val delta: Float, val pivot: Offset) : CanvasAction()
    data class Pan(val delta: Offset) : CanvasAction()

    data class ChangeTool(val tool: DrawingTool) : CanvasAction()
    data class ClearCanvas(val pageId: String) : CanvasAction()
    data class DeleteElement(val elementId: String) : CanvasAction()
}