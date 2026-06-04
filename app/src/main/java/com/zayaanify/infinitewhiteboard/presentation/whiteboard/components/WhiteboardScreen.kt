package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zayaanify.infinitewhiteboard.domain.model.*
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.WhiteboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteboardScreen(viewModel: WhiteboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokePicker by remember { mutableStateOf(false) }
    var showFontSizePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        DrawingCanvas(
            canvasState = uiState.canvasState,
            currentPageId = uiState.currentPageId,
            onDrawStart = { offset -> viewModel.onDrawStart(offset) },
            onDrawMove = { offset -> viewModel.onDrawMove(offset) },
            onDrawEnd = { viewModel.onDrawEnd() },
            onZoom = { centroid, zoom -> viewModel.onZoom(centroid, zoom) },
            onPan = { delta -> viewModel.onPan(delta) },
            onTextUpdate = { id, newText -> viewModel.updateTextElement(id, newText) },
            onCancelText = { id -> viewModel.cancelTextElement(id) },
            onStickyNoteUpdate = { id, newText -> viewModel.updateStickyNote(id, newText) },
            onCancelStickyNote = { id -> viewModel.cancelStickyNoteElement(id) },
            onTextPositionUpdate = { id, newPos -> viewModel.updateTextPosition(id, newPos) },
            onStickyNotePositionUpdate = { id, newPos -> viewModel.updateStickyNotePosition(id, newPos) },
            onCanvasTap = { offset -> viewModel.onCanvasTap(offset) },
            modifier = Modifier.fillMaxSize()
        )

        TopAppBar(
            title = { Text(uiState.currentPage?.name ?: "Infinite Whiteboard") },
            actions = {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.canUndo
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (uiState.canUndo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = uiState.canRedo
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (uiState.canRedo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = { viewModel.resetView() }) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset")
                }

                IconButton(onClick = { viewModel.shareCanvas() }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        )

        Card(modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp)) {
            Text(
                text = "${(uiState.canvasState.transform.scale * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        PagePanel(
            uiState = uiState,
            onAddPage = { viewModel.addPage() },
            onSwitchPage = { viewModel.switchPage(it) },
            onDeletePage = { viewModel.deletePage(it) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = showStrokePicker,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                StrokeSizePicker(
                    currentWidth = uiState.toolSettings.strokeWidth,
                    onWidthChange = { viewModel.updateStrokeWidth(it) }
                )
            }

            AnimatedVisibility(
                visible = showFontSizePicker,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                FontSizePicker(
                    textSize = uiState.toolSettings.textSize,
                    stickyNoteTextSize = uiState.toolSettings.stickyNoteTextSize,
                    onTextSizeChange = { newSize -> viewModel.updateAllTextSizes(newSize) },
                    onStickyNoteTextSizeChange = { newSize -> viewModel.updateAllStickyNoteSizes(newSize) }
                )
            }

            WhiteboardToolbar(
                currentTool = uiState.toolSettings.selectedTool,
                currentColor = uiState.toolSettings.strokeColor,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onToolSelected = { viewModel.selectTool(it) },
                onColorClick = { showColorPicker = true },
                onStrokeClick = { showStrokePicker = !showStrokePicker },
                onFontSizeClick = { showFontSizePicker = !showFontSizePicker },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onClear = { viewModel.clearCanvas() }
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = uiState.toolSettings.strokeColor,
            onColorSelected = { color ->
                viewModel.updateColor(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun StrokeSizePicker(currentWidth: Float, onWidthChange: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Stroke Size: ${currentWidth.toInt()}px",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf(2f, 5f, 10f, 15f, 20f, 30f, 40f)) { size ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onWidthChange(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentWidth == size) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FontSizePicker(
    textSize: Float,
    stickyNoteTextSize: Float,
    onTextSizeChange: (Float) -> Unit,
    onStickyNoteTextSizeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Text Font Size: ${textSize.toInt()}sp",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = textSize,
                onValueChange = onTextSizeChange,
                valueRange = 12f..48f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text(
                text = "Sticky Note Font Size: ${stickyNoteTextSize.toInt()}sp",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = stickyNoteTextSize,
                onValueChange = onStickyNoteTextSizeChange,
                valueRange = 10f..30f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WhiteboardToolbar(
    currentTool: DrawingTool,
    currentColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (DrawingTool) -> Unit,
    onColorClick: () -> Unit,
    onStrokeClick: () -> Unit,
    onFontSizeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = Icons.Default.Brush,
                isSelected = currentTool is DrawingTool.Pen,
                onClick = { onToolSelected(DrawingTool.Pen()) }
            )
            ToolButton(
                icon = Icons.Default.Highlight,
                isSelected = currentTool is DrawingTool.Highlighter,
                onClick = { onToolSelected(DrawingTool.Highlighter()) }
            )
            ToolButton(
                icon = Icons.Default.CropSquare,
                isSelected = currentTool is DrawingTool.Shape.Rectangle,
                onClick = { onToolSelected(DrawingTool.Shape.Rectangle()) }
            )
            ToolButton(
                icon = Icons.Default.RadioButtonUnchecked,
                isSelected = currentTool is DrawingTool.Shape.Circle,
                onClick = { onToolSelected(DrawingTool.Shape.Circle()) }
            )
            ToolButton(
                icon = Icons.Default.TextFields,
                isSelected = currentTool is DrawingTool.Text,
                onClick = { onToolSelected(DrawingTool.Text) }
            )
            ToolButton(
                icon = Icons.Default.NoteAdd,
                isSelected = currentTool is DrawingTool.StickyNote,
                onClick = { onToolSelected(DrawingTool.StickyNote) }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .clickable { onColorClick() }
            )
            ToolButton(
                icon = Icons.Default.LineWeight,
                isSelected = false,
                onClick = onStrokeClick
            )
            ToolButton(
                icon = Icons.Default.FormatSize,
                isSelected = false,
                onClick = onFontSizeClick
            )
            ToolButton(
                icon = Icons.Default.Undo,
                isSelected = false,
                onClick = onUndo,
                enabled = canUndo
            )
            ToolButton(
                icon = Icons.Default.Redo,
                isSelected = false,
                onClick = onRedo,
                enabled = canRedo
            )
            ToolButton(
                icon = Icons.Default.Delete,
                isSelected = false,
                onClick = onClear
            )
        }
    }
}

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.White, Color.Red, Color(0xFFFF9800), Color.Yellow,
        Color.Green, Color.Blue, Color(0xFF9C27B0), Color(0xFFE91E63),
        Color.Cyan, Color.Gray, Color(0xFF795548)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Color") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (color == currentColor) 3.dp else 1.dp,
                                color = if (color == currentColor) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}