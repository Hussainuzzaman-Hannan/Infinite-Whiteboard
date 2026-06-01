// presentation/whiteboard/components/InfiniteCanvas.kt
// কেন: এই Composable টি infinite scrollable canvas বানায়।
// transformGestures দিয়ে zoom + pan handle করা হয়।
// drawIntoCanvas দিয়ে performance optimize করা হয়।

package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import com.zayaanify.infinitewhiteboard.domain.model.*

@Composable
fun InfiniteCanvas(
    canvasState: CanvasState,
    toolSettings: ToolSettings,
    modifier: Modifier = Modifier,
    onDrawStart: (Offset) -> Unit,
    onDrawMove: (Offset) -> Unit,
    onDrawEnd: () -> Unit,
    onZoom: (centroid: Offset, zoom: Float) -> Unit,
    onPan: (Offset) -> Unit,
    onTap: (Offset) -> Unit
) {
    val transform = canvasState.transform
    val isPanMode = toolSettings.tool == DrawingTool.Pan
    val isDrawMode = toolSettings.tool !is DrawingTool.Select &&
            toolSettings.tool !is DrawingTool.Pan

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFFF8F8F8)) // Whiteboard background
            .canvasGestureDetector(
                isPanMode = isPanMode,
                onDrawStart = onDrawStart,
                onDrawMove = onDrawMove,
                onDrawEnd = onDrawEnd,
                onZoom = onZoom,
                onPan = onPan,
                onTap = onTap
            )
    ) {
        // Grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid(transform)
        }

        // Main drawing canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Transform apply করো (zoom + pan)
            withTransform({
                translate(transform.offset.x, transform.offset.y)
                scale(transform.scale, transform.scale, Offset.Zero)
            }) {
                // Committed elements draw করো
                canvasState.elements.sortedBy { it.zIndex }.forEach { element ->
                    drawElement(element)
                }

                // Live drawing (চলমান stroke) draw করো
                canvasState.currentPath?.let { path ->
                    drawLivePath(path)
                }

                // Live shape draw করো
                canvasState.currentShape?.let { shape ->
                    drawLiveShape(shape)
                }
            }
        }
    }
}

// ─── Grid Drawing ──────────────────────────────────────────────────────────

private fun DrawScope.drawGrid(transform: CanvasTransform) {
    val gridSize = 40f * transform.scale  // Zoom করলে grid scale হয়
    val dotRadius = 1.5f
    val dotColor = Color(0xFFCCCCCC)

    // Visible area এ কতগুলো dot দরকার calculate করো
    val startX = (-transform.offset.x / gridSize).toInt() - 1
    val startY = (-transform.offset.y / gridSize).toInt() - 1
    val endX = ((size.width - transform.offset.x) / gridSize).toInt() + 1
    val endY = ((size.height - transform.offset.y) / gridSize).toInt() + 1

    // Performance: শুধু visible dots draw করো
    if (gridSize < 10) return // Too zoomed out, no grid

    for (x in startX..endX) {
        for (y in startY..endY) {
            val screenX = x * gridSize + transform.offset.x
            val screenY = y * gridSize + transform.offset.y
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(screenX, screenY)
            )
        }
    }
}

// ─── Element Drawing ───────────────────────────────────────────────────────

private fun DrawScope.drawElement(element: DrawingElement) {
    when (element) {
        is PathElement -> drawPath(element)
        is ShapeElement -> drawShape(element)
        is TextElement -> { /* TextElement Compose layer এ render হবে */ }
        is StickyNoteElement -> { /* StickyNote Compose layer এ render হবে */ }
        is ImageElement -> { /* Image Compose layer এ render হবে */ }
    }
}

private fun DrawScope.drawPath(pathElement: PathElement) {
    if (pathElement.points.size < 2) {
        // Single dot draw করো
        pathElement.points.firstOrNull()?.let { point ->
            drawCircle(
                color = pathElement.color.copy(alpha = pathElement.opacity),
                radius = pathElement.strokeWidth / 2,
                center = point
            )
        }
        return
    }

    val path = Path().apply {
        moveTo(pathElement.points.first().x, pathElement.points.first().y)

        // Smooth curve এর জন্য quadratic bezier use করো
        for (i in 1 until pathElement.points.size) {
            val prev = pathElement.points[i - 1]
            val current = pathElement.points[i]
            val midX = (prev.x + current.x) / 2
            val midY = (prev.y + current.y) / 2
            quadraticTo(prev.x, prev.y, midX, midY)
        }
        lineTo(pathElement.points.last().x, pathElement.points.last().y)
    }

    val blendMode = if (pathElement.isEraser) BlendMode.Clear else BlendMode.SrcOver

    drawPath(
        path = path,
        color = pathElement.color.copy(alpha = pathElement.opacity),
        style = Stroke(
            width = pathElement.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

private fun DrawScope.drawLivePath(pathElement: PathElement) {
    drawPath(pathElement) // Same logic
}

private fun DrawScope.drawShape(shapeElement: ShapeElement) {
    val paint = Paint().apply {
        color = shapeElement.color
        style = PaintingStyle.Stroke
        strokeWidth = shapeElement.strokeWidth
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
    }

    val start = shapeElement.startOffset
    val end = shapeElement.endOffset

    when (shapeElement.shapeType) {
        DrawingTool.Shape.Rectangle -> {
            drawRect(
                color = shapeElement.color,
                topLeft = Offset(
                    minOf(start.x, end.x),
                    minOf(start.y, end.y)
                ),
                size = androidx.compose.ui.geometry.Size(
                    kotlin.math.abs(end.x - start.x),
                    kotlin.math.abs(end.y - start.y)
                ),
                style = Stroke(width = shapeElement.strokeWidth)
            )

            // Fill color আলাদাভাবে draw করো
            if (shapeElement.fillColor != Color.Transparent) {
                drawRect(
                    color = shapeElement.fillColor,
                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                    size = androidx.compose.ui.geometry.Size(
                        kotlin.math.abs(end.x - start.x),
                        kotlin.math.abs(end.y - start.y)
                    )
                )
            }
        }

        DrawingTool.Shape.Circle -> {
            val centerX = (start.x + end.x) / 2
            val centerY = (start.y + end.y) / 2
            val radiusX = kotlin.math.abs(end.x - start.x) / 2
            val radiusY = kotlin.math.abs(end.y - start.y) / 2
            val radius = maxOf(radiusX, radiusY)

            drawCircle(
                color = shapeElement.color,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = shapeElement.strokeWidth)
            )
        }

        DrawingTool.Shape.Line -> {
            drawLine(
                color = shapeElement.color,
                start = start,
                end = end,
                strokeWidth = shapeElement.strokeWidth,
                cap = StrokeCap.Round
            )
        }

        DrawingTool.Shape.Arrow -> {
            drawLine(
                color = shapeElement.color,
                start = start,
                end = end,
                strokeWidth = shapeElement.strokeWidth,
                cap = StrokeCap.Round
            )
            // Arrowhead draw করো
            drawArrowHead(start, end, shapeElement.color, shapeElement.strokeWidth)
        }
    }
}

private fun DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    val angle = kotlin.math.atan2(
        (end.y - start.y).toDouble(),
        (end.x - start.x).toDouble()
    ).toFloat()

    val arrowLength = 20f + strokeWidth * 2
    val arrowAngle = 0.5f

    val arrowPoint1 = Offset(
        x = end.x - arrowLength * kotlin.math.cos(angle - arrowAngle),
        y = end.y - arrowLength * kotlin.math.sin(angle - arrowAngle)
    )
    val arrowPoint2 = Offset(
        x = end.x - arrowLength * kotlin.math.cos(angle + arrowAngle),
        y = end.y - arrowLength * kotlin.math.sin(angle + arrowAngle)
    )

    val arrowPath = Path().apply {
        moveTo(arrowPoint1.x, arrowPoint1.y)
        lineTo(end.x, end.y)
        lineTo(arrowPoint2.x, arrowPoint2.y)
    }

    drawPath(
        path = arrowPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawLiveShape(shapeElement: ShapeElement) {
    drawShape(shapeElement) // Same logic
}

// ─── Gesture Detection ─────────────────────────────────────────────────────

private fun Modifier.canvasGestureDetector(
    isPanMode: Boolean,
    onDrawStart: (Offset) -> Unit,
    onDrawMove: (Offset) -> Unit,
    onDrawEnd: () -> Unit,
    onZoom: (centroid: Offset, zoom: Float) -> Unit,
    onPan: (Offset) -> Unit,
    onTap: (Offset) -> Unit
): Modifier = this.pointerInput(isPanMode) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        firstDown.consume()

        var isSingleTouch = true
        var hasMoved = false

        do {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }

            when {
                // Two finger: zoom + pan
                pointers.size >= 2 -> {
                    isSingleTouch = false
                    if (hasMoved) {
                        onDrawEnd() // Single touch এর drawing cancel করো
                        hasMoved = false
                    }
                    val zoom = event.calculateZoom()
                    val centroid = event.calculateCentroid()
                    val pan = event.calculatePan()

                    if (zoom != 1f) {
                        onZoom(centroid, zoom)
                    }
                    if (pan != Offset.Zero) {
                        onPan(pan)
                    }
                    event.changes.forEach { it.consume() }
                }

                // Single finger: draw or pan
                pointers.size == 1 -> {
                    val change = pointers.first()
                    val position = change.position

                    if (isPanMode) {
                        // Pan mode: drag করলে canvas move হয়
                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            onPan(delta)
                        }
                    } else {
                        // Draw mode
                        if (!hasMoved) {
                            onDrawStart(position)
                            hasMoved = true
                        } else {
                            onDrawMove(position)
                        }
                    }
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        // Gesture শেষ
        if (hasMoved && isSingleTouch && !isPanMode) {
            onDrawEnd()
        } else if (!hasMoved) {
            // এটা tap ছিল
            onTap(firstDown.position)
        }
    }
}