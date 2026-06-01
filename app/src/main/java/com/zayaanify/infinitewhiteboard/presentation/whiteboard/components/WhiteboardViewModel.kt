// presentation/whiteboard/WhiteboardViewModel.kt
// কেন: UI এর সব logic এখানে থাকবে। Screen এ শুধু UI code।
// StateFlow use করি কারণ Compose collect করে automatically re-compose করে।

package com.zayaanify.whiteboard.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.whiteboard.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel এর সব UI state এক data class এ
data class WhiteboardUiState(
    val pages: List<BoardPage> = listOf(BoardPage(name = "Page 1")),
    val currentPageId: String = "",
    val canvasState: CanvasState = CanvasState(),
    val toolSettings: ToolSettings = ToolSettings(),
    val undoStack: List<List<DrawingElement>> = emptyList(),
    val redoStack: List<List<DrawingElement>> = emptyList(),
    val isToolbarExpanded: Boolean = true,
    val isLoading: Boolean = false,
    val exportSuccess: Boolean = false
) {
    val currentPage: BoardPage?
        get() = pages.find { it.id == currentPageId }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    val currentElements: List<DrawingElement>
        get() = canvasState.elements.filter { it.pageId == currentPageId }
}

@HiltViewModel
class WhiteboardViewModel @Inject constructor(
    // Future: inject use cases here
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    init {
        // প্রথম page select করো
        val firstPageId = _uiState.value.pages.first().id
        _uiState.update { it.copy(currentPageId = firstPageId) }
    }

    // ─── Tool Selection ────────────────────────────────────────────────

    fun selectTool(tool: DrawingTool) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(tool = tool)
            )
        }
    }

    fun updateColor(color: Color) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(strokeColor = color)
            )
        }
    }

    fun updateStrokeWidth(width: Float) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(strokeWidth = width)
            )
        }
    }

    // ─── Drawing ───────────────────────────────────────────────────────

    fun onDrawStart(offset: Offset) {
        val state = _uiState.value
        val canvasOffset = state.canvasState.transform.screenToCanvas(offset)
        val tool = state.toolSettings.tool

        when (tool) {
            is DrawingTool.Pen, DrawingTool.Highlighter -> {
                val newPath = PathElement(
                    pageId = state.currentPageId,
                    zIndex = state.currentElements.size,
                    points = listOf(canvasOffset),
                    color = state.toolSettings.strokeColor,
                    strokeWidth = state.toolSettings.strokeWidth,
                    opacity = if (tool == DrawingTool.Highlighter) 0.4f else 1f,
                    tool = tool
                )
                _uiState.update { it.copy(
                    canvasState = it.canvasState.copy(
                        isDrawing = true,
                        currentPath = newPath
                    )
                )}
            }
            is DrawingTool.Eraser -> {
                val eraserPath = PathElement(
                    pageId = state.currentPageId,
                    zIndex = state.currentElements.size,
                    points = listOf(canvasOffset),
                    color = Color.White,
                    strokeWidth = state.toolSettings.strokeWidth * 3,
                    isEraser = true
                )
                _uiState.update { it.copy(
                    canvasState = it.canvasState.copy(
                        isDrawing = true,
                        currentPath = eraserPath
                    )
                )}
            }
            is DrawingTool.Shape -> {
                val newShape = ShapeElement(
                    pageId = state.currentPageId,
                    zIndex = state.currentElements.size,
                    shapeType = tool,
                    startOffset = canvasOffset,
                    endOffset = canvasOffset,
                    color = state.toolSettings.strokeColor,
                    strokeWidth = state.toolSettings.strokeWidth
                )
                _uiState.update { it.copy(
                    canvasState = it.canvasState.copy(
                        isDrawing = true,
                        currentShape = newShape
                    )
                )}
            }
            else -> {}
        }
    }

    fun onDrawMove(offset: Offset) {
        val state = _uiState.value
        if (!state.canvasState.isDrawing) return
        val canvasOffset = state.canvasState.transform.screenToCanvas(offset)

        _uiState.update { s ->
            val canvas = s.canvasState
            s.copy(
                canvasState = canvas.copy(
                    currentPath = canvas.currentPath?.copy(
                        points = canvas.currentPath.points + canvasOffset
                    ),
                    currentShape = canvas.currentShape?.copy(
                        endOffset = canvasOffset
                    )
                )
            )
        }
    }

    fun onDrawEnd() {
        val state = _uiState.value
        if (!state.canvasState.isDrawing) return

        // Undo stack এ current state save করো
        saveToUndoStack()

        val newElements = buildList {
            addAll(state.canvasState.elements)
            state.canvasState.currentPath?.let { add(it) }
            state.canvasState.currentShape?.let { add(it) }
        }

        _uiState.update { s ->
            s.copy(
                canvasState = s.canvasState.copy(
                    isDrawing = false,
                    elements = newElements,
                    currentPath = null,
                    currentShape = null
                )
            )
        }

        // Auto-save (debounced — 2 sec পরে save হবে)
        scheduleAutoSave()
    }

    // ─── Canvas Transform (Zoom & Pan) ─────────────────────────────────

    fun onZoom(centroid: Offset, zoom: Float) {
        _uiState.update { state ->
            val currentTransform = state.canvasState.transform
            val newScale = (currentTransform.scale * zoom)
                .coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)

            // Centroid point ঠিক রেখে zoom করো
            val newOffset = Offset(
                x = centroid.x - (centroid.x - currentTransform.offset.x) * (newScale / currentTransform.scale),
                y = centroid.y - (centroid.y - currentTransform.offset.y) * (newScale / currentTransform.scale)
            )

            state.copy(
                canvasState = state.canvasState.copy(
                    transform = currentTransform.copy(
                        scale = newScale,
                        offset = newOffset
                    )
                )
            )
        }
    }

    fun onPan(delta: Offset) {
        _uiState.update { state ->
            val current = state.canvasState.transform
            state.copy(
                canvasState = state.canvasState.copy(
                    transform = current.copy(
                        offset = current.offset + delta
                    )
                )
            )
        }
    }

    fun resetView() {
        _uiState.update { state ->
            state.copy(
                canvasState = state.canvasState.copy(
                    transform = CanvasTransform.INITIAL
                )
            )
        }
    }

    // ─── Undo / Redo ───────────────────────────────────────────────────

    private fun saveToUndoStack() {
        _uiState.update { state ->
            val newUndoStack = (state.undoStack + listOf(state.canvasState.elements))
                .takeLast(50) // Max 50 undo steps, memory efficient
            state.copy(
                undoStack = newUndoStack,
                redoStack = emptyList() // Redo clear হয় নতুন action এ
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.undoStack.isEmpty()) return

        val previousElements = state.undoStack.last()
        _uiState.update { s ->
            s.copy(
                canvasState = s.canvasState.copy(elements = previousElements),
                undoStack = s.undoStack.dropLast(1),
                redoStack = s.redoStack + listOf(s.canvasState.elements)
            )
        }
    }

    fun redo() {
        val state = _uiState.value
        if (state.redoStack.isEmpty()) return

        val nextElements = state.redoStack.last()
        _uiState.update { s ->
            s.copy(
                canvasState = s.canvasState.copy(elements = nextElements),
                redoStack = s.redoStack.dropLast(1),
                undoStack = s.undoStack + listOf(s.canvasState.elements)
            )
        }
    }

    fun clearCanvas() {
        saveToUndoStack()
        _uiState.update { state ->
            val filteredElements = state.canvasState.elements
                .filter { it.pageId != state.currentPageId }
            state.copy(
                canvasState = state.canvasState.copy(elements = filteredElements)
            )
        }
    }

    // ─── Page Management ───────────────────────────────────────────────

    fun addPage() {
        val newPage = BoardPage(
            name = "Page ${_uiState.value.pages.size + 1}",
            order = _uiState.value.pages.size
        )
        _uiState.update { state ->
            state.copy(
                pages = state.pages + newPage,
                currentPageId = newPage.id
            )
        }
    }

    fun switchPage(pageId: String) {
        _uiState.update { it.copy(currentPageId = pageId) }
    }

    fun deletePage(pageId: String) {
        val state = _uiState.value
        if (state.pages.size <= 1) return // কমপক্ষে 1 page থাকতে হবে

        val newPages = state.pages.filter { it.id != pageId }
        val newCurrentPageId = if (state.currentPageId == pageId) {
            newPages.first().id
        } else {
            state.currentPageId
        }

        _uiState.update { s ->
            s.copy(
                pages = newPages,
                currentPageId = newCurrentPageId,
                canvasState = s.canvasState.copy(
                    elements = s.canvasState.elements.filter { it.pageId != pageId }
                )
            )
        }
    }

    // ─── Sticky Notes ──────────────────────────────────────────────────

    fun addStickyNote(position: Offset) {
        val state = _uiState.value
        val canvasPosition = state.canvasState.transform.screenToCanvas(position)
        val note = StickyNoteElement(
            pageId = state.currentPageId,
            zIndex = state.currentElements.size,
            position = canvasPosition
        )
        saveToUndoStack()
        _uiState.update { s ->
            s.copy(
                canvasState = s.canvasState.copy(
                    elements = s.canvasState.elements + note
                )
            )
        }
    }

    fun addTextElement(position: Offset) {
        val state = _uiState.value
        val canvasPosition = state.canvasState.transform.screenToCanvas(position)
        val text = TextElement(
            pageId = state.currentPageId,
            zIndex = state.currentElements.size,
            position = canvasPosition,
            isEditing = true
        )
        saveToUndoStack()
        _uiState.update { s ->
            s.copy(
                canvasState = s.canvasState.copy(
                    elements = s.canvasState.elements + text,
                    selectedElementId = text.id
                )
            )
        }
    }

    // ─── Auto Save ─────────────────────────────────────────────────────

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // 2 second debounce
            // TODO Phase 5: Room database save
        }
    }
}