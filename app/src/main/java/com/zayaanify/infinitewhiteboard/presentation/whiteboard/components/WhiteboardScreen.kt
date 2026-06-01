// presentation/whiteboard/WhiteboardScreen.kt
// কেন: সব components একত্রে এখানে compose হয়।

package com.zayaanify.whiteboard.presentation.whiteboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.whiteboard.domain.model.DrawingTool
import com.yourname.whiteboard.presentation.whiteboard.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteboardScreen(
    viewModel: WhiteboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Infinite Canvas (full screen) ──────────────────────────────
        InfiniteCanvas(
            canvasState = uiState.canvasState,
            toolSettings = uiState.toolSettings,
            modifier = Modifier.fillMaxSize(),
            onDrawStart = viewModel::onDrawStart,
            onDrawMove = viewModel::onDrawMove,
            onDrawEnd = viewModel::onDrawEnd,
            onZoom = viewModel::onZoom,
            onPan = viewModel::onPan,
            onTap = { offset ->
                when (uiState.toolSettings.tool) {
                    DrawingTool.Text -> viewModel.addTextElement(offset)
                    DrawingTool.StickyNote -> viewModel.addStickyNote(offset)
                    else -> {}
                }
            }
        )

        // ── Top Bar ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page indicator
            Text(
                text = uiState.currentPage?.name ?: "Whiteboard",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset view button
                SmallFloatingActionButton(
                    onClick = viewModel::resetView,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Outlined.CenterFocusStrong, "Reset view")
                }

                // Export button
                SmallFloatingActionButton(
                    onClick = { /* Phase 5 */ },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Outlined.FileDownload, "Export")
                }
            }
        }

        // ── Main Toolbar (bottom center) ───────────────────────────────
        WhiteboardToolbar(
            toolSettings = uiState.toolSettings,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            onToolSelect = viewModel::selectTool,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onClear = viewModel::clearCanvas,
            onColorClick = { showColorPicker = true },
            onStrokeClick = { showStrokePicker = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // ── Page Panel (right side) ────────────────────────────────────
        PagePanel(
            pages = uiState.pages,
            currentPageId = uiState.currentPageId,
            onAddPage = viewModel::addPage,
            onSwitchPage = viewModel::switchPage,
            onDeletePage = viewModel::deletePage,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // ── Zoom indicator (top right) ─────────────────────────────────
        Text(
            text = "${(uiState.canvasState.transform.scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )
    }

    // ── Color Picker Dialog ──────────────────────────────────────────────
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

    // ── Stroke Size Picker ───────────────────────────────────────────────
    if (showStrokePicker) {
        StrokeSizeDialog(
            currentSize = uiState.toolSettings.strokeWidth,
            onSizeSelected = { size ->
                viewModel.updateStrokeWidth(size)
                showStrokePicker = false
            },
            onDismiss = { showStrokePicker = false }
        )
    }
}

// ── Color Picker Dialog ──────────────────────────────────────────────────────

@Composable
fun ColorPickerDialog(
    currentColor: androidx.compose.ui.graphics.Color,
    onColorSelected: (androidx.compose.ui.graphics.Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = remember {
        listOf(
            0xFF000000, 0xFFFFFFFF, 0xFFEF4444, 0xFFF97316,
            0xFFEAB308, 0xFF22C55E, 0xFF3B82F6, 0xFF8B5CF6,
            0xFFEC4899, 0xFF14B8A6, 0xFF6B7280, 0xFF92400E
        ).map { androidx.compose.ui.graphics.Color(it.toLong()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Color") },
        text = {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (color == currentColor) 3.dp else 1.dp,
                                color = if (color == currentColor)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}