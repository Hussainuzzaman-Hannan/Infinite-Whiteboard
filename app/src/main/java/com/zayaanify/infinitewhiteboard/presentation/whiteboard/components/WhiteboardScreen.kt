package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
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
                // Undo Button
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.canUndo
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (uiState.canUndo) Color.White else Color.Gray
                    )
                }

                // Redo Button
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = uiState.canRedo
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (uiState.canRedo) Color.White else Color.Gray
                    )
                }

                // Delete/Clear Current Page Button (নতুন যোগ করা)
                IconButton(
                    onClick = { viewModel.clearCanvas() }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear Page",
                        tint = Color.White
                    )
                }

                // Reset View Button
                IconButton(onClick = { viewModel.resetView() }) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset", tint = Color.White)
                }

                // Add Page Button (যদি চান, নয়তো সরিয়ে দিন)
                // IconButton(onClick = { viewModel.addPage() }) {
                //     Icon(Icons.Default.Add, contentDescription = "Add Page", tint = Color.White)
                // }

                // Share Button
                IconButton(onClick = { viewModel.shareCanvas() }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
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
                onToolSelected = { viewModel.selectTool(it) },
                onColorClick = { showColorPicker = true },
                onStrokeClick = { showStrokePicker = !showStrokePicker },
                onFontSizeClick = { showFontSizePicker = !showFontSizePicker }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Stroke Size: ${currentWidth.toInt()}px",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf(2f, 5f, 10f, 15f, 20f, 30f, 40f)) { size ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                            .clickable { onWidthChange(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentWidth == size) Color.White
                                    else Color.Gray
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Text Font Size: ${textSize.toInt()}sp",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Slider(
                value = textSize,
                onValueChange = onTextSizeChange,
                valueRange = 12f..48f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(color = Color.Gray)
            Text(
                text = "Sticky Note Font Size: ${stickyNoteTextSize.toInt()}sp",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
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
    onToolSelected: (DrawingTool) -> Unit,
    onColorClick: () -> Unit,
    onStrokeClick: () -> Unit,
    onFontSizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        // প্রথম Row - ড্রইং টুলস
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // পেন
            ToolButton(
                icon = Icons.Default.Brush,
                isSelected = currentTool is DrawingTool.Pen,
                onClick = { onToolSelected(DrawingTool.Pen()) }
            )
            // হাইলাইটার
            ToolButton(
                icon = Icons.Default.Highlight,
                isSelected = currentTool is DrawingTool.Highlighter,
                onClick = { onToolSelected(DrawingTool.Highlighter()) }
            )
            // ইরেজার
            ToolButton(
                icon = Icons.Default.Edit,
                isSelected = currentTool is DrawingTool.Eraser,
                onClick = { onToolSelected(DrawingTool.Eraser()) }
            )
            // রেকট্যাঙ্গেল
            ToolButton(
                icon = Icons.Default.CropSquare,
                isSelected = currentTool is DrawingTool.Shape.Rectangle,
                onClick = { onToolSelected(DrawingTool.Shape.Rectangle()) }
            )
            // সার্কেল
            ToolButton(
                icon = Icons.Default.RadioButtonUnchecked,
                isSelected = currentTool is DrawingTool.Shape.Circle,
                onClick = { onToolSelected(DrawingTool.Shape.Circle()) }
            )
            // টেক্সট
            ToolButton(
                icon = Icons.Default.TextFields,
                isSelected = currentTool is DrawingTool.Text,
                onClick = { onToolSelected(DrawingTool.Text) }
            )
            // স্টিকি নোট
            ToolButton(
                icon = Icons.Default.NoteAdd,
                isSelected = currentTool is DrawingTool.StickyNote,
                onClick = { onToolSelected(DrawingTool.StickyNote) }
            )
        }

        // দ্বিতীয় Row - কন্ট্রোল টুলস
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // কালার পিকার
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable { onColorClick() }
            )
            // স্ট্রোক সাইজ
            ToolButton(
                icon = Icons.Default.LineWeight,
                isSelected = false,
                onClick = onStrokeClick
            )
            // ফন্ট সাইজ
            ToolButton(
                icon = Icons.Default.FormatSize,
                isSelected = false,
                onClick = onFontSizeClick
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
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> Color.Gray
                isSelected -> Color(0xFFBB86FC)
                else -> Color.White
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
    // SoftBoard স্টাইলের হালকা রঙের প্যালেট
    val colors = listOf(
        Color.White,
        Color(0xFFFF6B6B),  // লাইট লাল
        Color(0xFFFFB347),  // কমলা
        Color(0xFFFDFD97),  // লাইট ইয়েলো
        Color(0xFFB5EAD7),  // লাইট গ্রিন
        Color(0xFFA0D2FF),  // লাইট ব্লু
        Color(0xFFD0B2FF),  // লাইট পার্পল
        Color(0xFFE0E0E0)   // লাইট গ্রে
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Color", color = Color.White) },
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
                                color = if (color == currentColor) Color.White else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}