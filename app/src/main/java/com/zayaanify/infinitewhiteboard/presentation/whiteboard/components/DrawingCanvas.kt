package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zayaanify.infinitewhiteboard.domain.model.*

@Composable
fun DrawingCanvas(
    canvasState: CanvasState,
    currentPageId: String,
    onDrawStart: (Offset) -> Unit,
    onDrawMove: (Offset) -> Unit,
    onDrawEnd: () -> Unit,
    onZoom: (Offset, Float) -> Unit,
    onPan: (Offset) -> Unit,
    onTextUpdate: (String, String) -> Unit = { _, _ -> },
    onStickyNoteUpdate: (String, String) -> Unit = { _, _ -> },
    onCanvasTap: (Offset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var editingTextValue by remember { mutableStateOf("") }
    var editingStickyNoteId by remember { mutableStateOf<String?>(null) }
    var editingStickyNoteValue by remember { mutableStateOf("") }

    val density = LocalDensity.current

    // আমরা toolSettings প্যারামিটার হিসেবে নেব না - এটি WhiteboardScreen থেকে আসবে
    // বর্তমানে আমরা শুধু ড্রইং টুলের জন্য currentTool জানার প্রয়োজন নেই
    // কারণ ড্রইং জেসচার WhiteboardScreen থেকে আসে

    Box(modifier = modifier.fillMaxSize()) {
        // Canvas for drawing with full gesture handling
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        var isDrawing = false
                        var isPanning = false
                        var zooming = false
                        var initialZoom = 1f
                        var initialPan = Offset.Zero
                        var pointerId = down.id
                        var currentPosition = down.position

                        // আমরা এখানে টুল চেক করব না - ড্রইং সবসময় সক্রিয় থাকবে
                        // টেক্সট এবং স্টিকি নোটের জন্য ট্যাপ হ্যান্ডলিং আলাদাভাবে করা হবে
                        isDrawing = true
                        onDrawStart(currentPosition)

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (zoom != 1f && event.changes.size >= 2) {
                                zooming = true
                                val centroid = event.changes
                                    .map { it.position }
                                    .reduce { acc, pos ->
                                        Offset(acc.x + pos.x, acc.y + pos.y)
                                    }
                                    .let { Offset(it.x / event.changes.size, it.y / event.changes.size) }
                                onZoom(centroid, zoom / initialZoom)
                                initialZoom = zoom
                            }

                            if (pan != Offset.Zero && event.changes.size >= 2) {
                                isPanning = true
                                onPan(pan - initialPan)
                                initialPan = pan
                            }

                            val currentEventPosition = event.changes.find { it.id == pointerId }?.position
                            if (!zooming && !isPanning && currentEventPosition != null && isDrawing) {
                                currentPosition = currentEventPosition
                                onDrawMove(currentPosition)
                            }

                            if (event.changes.all { !it.pressed }) {
                                if (isDrawing) {
                                    onDrawEnd()
                                }
                                // ট্যাপ ইভেন্ট সবসময় পাঠানো হবে
                                onCanvasTap(currentPosition)
                                break
                            }
                        }
                    }
                }
        ) {
            withTransform({
                translate(left = canvasState.transform.offset.x, top = canvasState.transform.offset.y)
                scale(scaleX = canvasState.transform.scale, scaleY = canvasState.transform.scale, pivot = Offset.Zero)
            }) {

                // Draw all existing elements for current page
                canvasState.elements.filter { it.pageId == currentPageId }.forEach { element ->
                    when (element) {
                        is PathElement -> drawPathElement(element)
                        is ShapeElement -> drawShapeElement(element)
                        is TextElement -> {
                            if (editingTextId != element.id) {
                                drawTextElement(element)
                            }
                        }
                        is StickyNoteElement -> {
                            if (editingStickyNoteId != element.id) {
                                drawStickyNoteElement(element)
                            }
                        }
                        else -> {}
                    }
                }

                // Draw current path (being drawn)
                canvasState.currentPath?.let { drawPathElement(it) }

                // Draw current shape (being drawn)
                canvasState.currentShape?.let { drawShapeElement(it) }
            }
        }

        // Text editing overlay
        editingTextId?.let { textId ->
            val textElement = canvasState.elements
                .filterIsInstance<TextElement>()
                .find { it.id == textId }

            textElement?.let { element ->
                val screenPosition = canvasState.transform.canvasToScreen(element.position)

                BasicTextField(
                    value = editingTextValue,
                    onValueChange = { editingTextValue = it },
                    textStyle = TextStyle(
                        color = element.color,
                        fontSize = element.textSize.sp,
                        background = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .offset(x = with(density) { screenPosition.x.toDp() }, y = with(density) { screenPosition.y.toDp() })
                        .width(200.dp)
                        .background(Color.White)
                )
            }
        }

        // Sticky note editing overlay
        editingStickyNoteId?.let { noteId ->
            val stickyNote = canvasState.elements
                .filterIsInstance<StickyNoteElement>()
                .find { it.id == noteId }

            stickyNote?.let { note ->
                val screenPosition = canvasState.transform.canvasToScreen(note.position)

                BasicTextField(
                    value = editingStickyNoteValue,
                    onValueChange = { editingStickyNoteValue = it },
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        background = note.color
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .offset(x = with(density) { screenPosition.x.toDp() }, y = with(density) { screenPosition.y.toDp() })
                        .width(with(density) { note.width.toDp() })
                        .background(note.color)
                )
            }
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

    val size = Size(width, height)
    val topLeft = Offset(left, top)

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
        else -> {}
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTextElement(textElement: TextElement) {
    if (textElement.text.isEmpty()) return

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = textElement.color.toArgb()
            textSize = textElement.textSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
            if (textElement.isBold) {
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
        }

        val lines = textElement.text.split("\n")
        var yOffset = textElement.position.y + textElement.textSize
        lines.forEach { line ->
            drawText(
                line,
                textElement.position.x,
                yOffset,
                paint
            )
            yOffset += textElement.textSize + 4
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStickyNoteElement(stickyNote: StickyNoteElement) {
    // Draw sticky note background
    drawRect(
        color = stickyNote.color,
        topLeft = stickyNote.position,
        size = Size(stickyNote.width, stickyNote.height),
        style = Fill
    )

    // Draw border
    drawRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = stickyNote.position,
        size = Size(stickyNote.width, stickyNote.height),
        style = Stroke(width = 1f)
    )

    // Draw fold effect (top-right corner)
    val foldSize = 20f
    val foldPath = Path().apply {
        moveTo(stickyNote.position.x + stickyNote.width - foldSize, stickyNote.position.y)
        lineTo(stickyNote.position.x + stickyNote.width, stickyNote.position.y)
        lineTo(stickyNote.position.x + stickyNote.width, stickyNote.position.y + foldSize)
        close()
    }
    drawPath(
        path = foldPath,
        color = Color.Black.copy(alpha = 0.1f),
        style = Fill
    )

    // Draw text
    if (stickyNote.text.isNotEmpty()) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
                isAntiAlias = true
            }

            val lines = stickyNote.text.split("\n")
            var yOffset = stickyNote.position.y + 20f
            lines.forEach { line ->
                if (yOffset < stickyNote.position.y + stickyNote.height - 20f) {
                    drawText(
                        line,
                        stickyNote.position.x + 10f,
                        yOffset,
                        paint
                    )
                    yOffset += 20f
                }
            }
        }
    }
}