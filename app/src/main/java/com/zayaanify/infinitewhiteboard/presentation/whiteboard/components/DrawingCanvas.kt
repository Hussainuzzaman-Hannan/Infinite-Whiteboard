package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zayaanify.infinitewhiteboard.domain.model.*
import kotlinx.coroutines.delay

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
    onTextPositionUpdate: (String, Offset) -> Unit = { _, _ -> },
    onStickyNotePositionUpdate: (String, Offset) -> Unit = { _, _ -> },
    onCanvasTap: (Offset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var editingTextValue by remember { mutableStateOf("") }
    var editingStickyNoteId by remember { mutableStateOf<String?>(null) }
    var editingStickyNoteValue by remember { mutableStateOf("") }
    var showTextEditor by remember { mutableStateOf(false) }
    var showStickyNoteEditor by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    fun saveText() {
        editingTextId?.let { id ->
            if (editingTextValue.isNotEmpty()) {
                onTextUpdate(id, editingTextValue)
            }
        }
        editingTextId = null
        editingTextValue = ""
        showTextEditor = false
        keyboardController?.hide()
    }

    fun cancelText() {
        editingTextId = null
        editingTextValue = ""
        showTextEditor = false
        keyboardController?.hide()
    }

    fun saveStickyNote() {
        editingStickyNoteId?.let { id ->
            if (editingStickyNoteValue.isNotEmpty()) {
                onStickyNoteUpdate(id, editingStickyNoteValue)
            }
        }
        editingStickyNoteId = null
        editingStickyNoteValue = ""
        showStickyNoteEditor = false
        keyboardController?.hide()
    }

    fun cancelStickyNote() {
        editingStickyNoteId = null
        editingStickyNoteValue = ""
        showStickyNoteEditor = false
        keyboardController?.hide()
    }

    LaunchedEffect(canvasState.elements) {
        val newTextElement = canvasState.elements
            .filterIsInstance<TextElement>()
            .find { it.isEditing && editingTextId == null }

        if (newTextElement != null) {
            editingTextId = newTextElement.id
            editingTextValue = newTextElement.text
            showTextEditor = true
            delay(100)
            keyboardController?.show()
        }
    }

    LaunchedEffect(canvasState.elements) {
        val newStickyNote = canvasState.elements
            .filterIsInstance<StickyNoteElement>()
            .find { it.text == "New Note" && editingStickyNoteId == null }

        if (newStickyNote != null) {
            editingStickyNoteId = newStickyNote.id
            editingStickyNoteValue = newStickyNote.text
            showStickyNoteEditor = true
            delay(100)
            keyboardController?.show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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

                canvasState.currentPath?.let { drawPathElement(it) }
                canvasState.currentShape?.let { drawShapeElement(it) }
            }
        }

        // Text editing overlay
        if (showTextEditor && editingTextId != null) {
            val textElement = canvasState.elements
                .filterIsInstance<TextElement>()
                .find { it.id == editingTextId }

            textElement?.let { element ->
                var localOffset by remember { mutableStateOf(element.position) }

                Card(
                    modifier = Modifier
                        .offset(
                            x = with(density) { localOffset.x.toDp() },
                            y = with(density) { localOffset.y.toDp() }
                        )
                        .width(250.dp)
                        .shadow(4.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    localOffset = Offset(
                                        localOffset.x + dragAmount.x / density.density,
                                        localOffset.y + dragAmount.y / density.density
                                    )
                                },
                                onDragEnd = {
                                    onTextPositionUpdate(element.id, localOffset)
                                }
                            )
                        }
                ) {
                    Column(modifier = Modifier.background(Color.White)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⋯", fontSize = 16.sp, color = Color.Gray)
                        }

                        BasicTextField(
                            value = editingTextValue,
                            onValueChange = { editingTextValue = it },
                            textStyle = TextStyle(
                                color = element.color,
                                fontSize = element.textSize.sp,
                                background = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(8.dp)
                                .background(Color.White),
                            singleLine = false
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cancelText() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF5350),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { saveText() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sticky note editing overlay
        if (showStickyNoteEditor && editingStickyNoteId != null) {
            val stickyNote = canvasState.elements
                .filterIsInstance<StickyNoteElement>()
                .find { it.id == editingStickyNoteId }

            stickyNote?.let { note ->
                var localOffset by remember { mutableStateOf(note.position) }

                Card(
                    modifier = Modifier
                        .offset(
                            x = with(density) { localOffset.x.toDp() },
                            y = with(density) { localOffset.y.toDp() }
                        )
                        .width(220.dp)
                        .shadow(4.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    localOffset = Offset(
                                        localOffset.x + dragAmount.x / density.density,
                                        localOffset.y + dragAmount.y / density.density
                                    )
                                },
                                onDragEnd = {
                                    onStickyNotePositionUpdate(note.id, localOffset)
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.background(note.color)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(note.color.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⋯", fontSize = 16.sp, color = Color.Black.copy(alpha = 0.4f))
                        }

                        BasicTextField(
                            value = editingStickyNoteValue,
                            onValueChange = { editingStickyNoteValue = it },
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(8.dp),
                            singleLine = false
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cancelStickyNote() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF5350),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { saveStickyNote() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", fontSize = 12.sp)
                            }
                        }
                    }
                }
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
    drawRect(
        color = stickyNote.color,
        topLeft = stickyNote.position,
        size = Size(stickyNote.width, stickyNote.height),
        style = Fill
    )

    drawRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = stickyNote.position,
        size = Size(stickyNote.width, stickyNote.height),
        style = Stroke(width = 1f)
    )

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