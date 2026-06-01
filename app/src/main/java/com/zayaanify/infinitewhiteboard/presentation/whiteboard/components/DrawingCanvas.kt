package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.zayaanify.infinitewhiteboard.domain.model.CanvasState
import com.zayaanify.infinitewhiteboard.domain.model.DrawingTool
import com.zayaanify.infinitewhiteboard.domain.model.PathElement
import com.zayaanify.infinitewhiteboard.domain.model.ShapeElement

@Composable
fun DrawingCanvas(
    canvasState: CanvasState,
    currentPageId: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // ক্যানভাসের জুম (scale) এবং প্যান (offset) ম্যাট্রিক্স অ্যাপ্লাই করা হচ্ছে
        withTransform({
            translate(left = canvasState.transform.offset.x, top = canvasState.transform.offset.y)
            scale(scaleX = canvasState.transform.scale, scaleY = canvasState.transform.scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
        }) {

            // ১. ইতিমধ্যে সংরক্ষিত সব এলিমেন্ট ড্র করা (শুধুমাত্র কারেন্ট পেজের জন্য)
            canvasState.elements.filter { it.pageId == currentPageId }.forEach { element ->
                when (element) {
                    is PathElement -> drawPathElement(element)
                    is ShapeElement -> drawShapeElement(element)
                    else -> { /* অন্যান্য উপাদান পরবর্তী ফেজে যুক্ত হবে */ }
                }
            }

            // ২. বর্তমানে ইউজার যে লাইনটি লাইভ আঁকছেন (Current Path)
            canvasState.currentPath?.let { drawPathElement(it) }

            // ৩. বর্তমানে ইউজার যে শেপটি লাইভ আঁকছেন (Current Shape)
            canvasState.currentShape?.let { drawShapeElement(it) }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathElement(pathElement: PathElement) {
    if (pathElement.points.size < 2) return

    val path = Path().apply {
        val firstPoint = pathElement.points.first()
        moveTo(firstPoint.x, firstPoint.y)
        for (i in 1 until pathElement.points.size) {
            val point = pathElement.points[i]
            lineTo(point.x, point.y)
        }
    }

    // হাইলাইটার বা ইরেজারের জন্য ব্লেন্ডিং মোড সেট করা
    val blendMode = if (pathElement.isEraser) BlendMode.Clear else BlendMode.SrcOver

    drawPath(
        path = path,
        color = if (pathElement.isEraser) Color.Transparent else pathElement.color,
        alpha = pathElement.opacity,
        style = Stroke(
            width = pathElement.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShapeElement(shapeElement: ShapeElement) {
    val left = minOf(shapeElement.startOffset.x, shapeElement.endOffset.x)
    val top = minOf(shapeElement.startOffset.y, shapeElement.endOffset.y)
    val width = kotlin.math.abs(shapeElement.endOffset.x - shapeElement.startOffset.x)
    val height = kotlin.math.abs(shapeElement.endOffset.y - shapeElement.startOffset.y)

    if (width == 0f && height == 0f) return

    val size = androidx.compose.ui.geometry.Size(width, height)
    val topLeft = androidx.compose.ui.geometry.Offset(left, top)

    when (shapeElement.shapeType) {
        is DrawingTool.Shape.Rectangle -> {
            drawRect(
                color = shapeElement.color,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = shapeElement.strokeWidth)
            )
        }
        is DrawingTool.Shape.Circle -> {
            drawOval(
                color = shapeElement.color,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = shapeElement.strokeWidth)
            )
        }
        is DrawingTool.Shape.Line -> {
            drawLine(
                color = shapeElement.color,
                start = shapeElement.startOffset,
                end = shapeElement.endOffset,
                strokeWidth = shapeElement.strokeWidth,
                cap = StrokeCap.Round
            )
        }
        else -> { /* Arrow বা অন্যান্য শেপ ফিউচারে হ্যান্ডেল হবে */ }
    }
}