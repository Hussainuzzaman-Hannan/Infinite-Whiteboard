// presentation/whiteboard/WhiteboardScreen.kt
// কেন: সব components একত্রে এখানে compose হয়।

package com.zayaanify.infinitewhiteboard.presentation.whiteboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zayaanify.infinitewhiteboard.domain.model.DrawingTool
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.InfiniteCanvas
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.PagePanel
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.StrokeSizePicker
import com.zayaanify.infinitewhiteboard.presentation.whiteboard.components.WhiteboardToolbar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                // রিফ্লেকশনের জটিলতা এড়িয়ে টাইপ-সেফ চেকিং
                when (uiState.toolSettings.tool) {
                    is DrawingTool.Text -> viewModel.addTextElement(offset)
                    is DrawingTool.StickyNote -> viewModel.addStickyNote(offset)
                    else -> { /* অন্যান্য ট্যাপ অ্যাকশন */ }
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
            // রিফ্লেকশন ছাড়া সরাসরি কারেন্ট পেজের নাম রিড করা হচ্ছে
            Text(
                text = uiState.currentPage?.name ?: "Whiteboard",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset view button - Unresolved reference ফিক্স করতে Icons.Outlined.FilterCenterFocus ব্যবহার করা হয়েছে
                SmallFloatingActionButton(
                    onClick = viewModel::resetView,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Outlined.FilterCenterFocus, "Reset view")
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

        // ── Zoom indicator (top right) ─────────────────────────────────
        // রিফ্লেকশন ছাড়া সরাসরি স্টেট থেকে স্কেল ডেটা রিড করা হচ্ছে
        val currentScale = uiState.canvasState.transform.scale

        Text(
            text = "${(currentScale * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )

        // ── Page Panel (right side) ────────────────────────────────────
        PagePanel(
            uiState = uiState,
            onAddPage = viewModel::addPage,
            onSwitchPage = viewModel::switchPage,
            onDeletePage = viewModel::deletePage,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // ── Bottom Controls (Toolbar + Stroke Picker) ──────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // অ্যানিমেটেড স্ট্রোক সাইজ পিকার প্যানেল (টুলবারের ঠিক উপরে ভাসমান থাকবে)
            AnimatedVisibility(
                visible = showStrokePicker,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                StrokeSizePicker(
                    toolSettings = uiState.toolSettings,
                    onStrokeWidthChange = viewModel::updateStrokeWidth
                )
            }

            // মেইন কাস্টম টুলবার
            WhiteboardToolbar(
                toolSettings = uiState.toolSettings,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onToolSelect = { tool ->
                    viewModel.selectTool(tool)
                    showStrokePicker = false //ツール চেঞ্জ করলে স্লাইডার প্যানেল অটো অফ হবে
                },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClear = viewModel::clearCanvas,
                onColorClick = {
                    showColorPicker = true
                    showStrokePicker = false
                },
                onStrokeClick = {
                    showStrokePicker = !showStrokePicker // টগল লজিক
                }
            )
        }
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
}

// ── Color Picker Dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
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
            FlowRow(
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