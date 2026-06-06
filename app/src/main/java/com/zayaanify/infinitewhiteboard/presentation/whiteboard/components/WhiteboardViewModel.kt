package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.infinitewhiteboard.domain.model.*
import com.zayaanify.infinitewhiteboard.data.local.BoardPageEntity
import com.zayaanify.infinitewhiteboard.data.repository.WhiteboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WhiteboardUiState(
    val pages: List<BoardPage> = emptyList(),
    val currentPageId: String = "",
    val canvasState: CanvasState = CanvasState(),
    val toolSettings: ToolSettings = ToolSettings(),
    val undoStack: List<List<CanvasElement>> = emptyList(),
    val redoStack: List<List<CanvasElement>> = emptyList(),
    val isToolbarExpanded: Boolean = true,
    val isLoading: Boolean = false,
    val exportSuccess: Boolean = false
) {
    val currentPage: BoardPage?
        get() = pages.find { it.id == currentPageId }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    val currentElements: List<CanvasElement>
        get() = canvasState.elements.filter { it.pageId == currentPageId }
}

@HiltViewModel
class WhiteboardViewModel @Inject constructor(
    private val repository: WhiteboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("WhiteboardDB", "ViewModel init - loading pages from database")
        loadPagesFromDatabase()
    }

    private fun loadPagesFromDatabase() {
        viewModelScope.launch {
            try {
                android.util.Log.d("WhiteboardDB", "loadPagesFromDatabase called")
                repository.getAllPages().collect { entities ->
                    android.util.Log.d("WhiteboardDB", "Loaded ${entities.size} pages from database")

                    if (entities.isNotEmpty()) {
                        // শুধুমাত্র unique ID এর পেজ নিন
                        val uniqueEntities = entities.distinctBy { it.id }

                        val boardPages = uniqueEntities.mapIndexed { index, entity ->
                            BoardPage(
                                id = entity.id,
                                name = "Page ${index + 1}",
                                order = index,
                                createdAt = entity.createdAt,
                                updatedAt = entity.updatedAt
                            )
                        }

                        val firstPage = uniqueEntities.firstOrNull()
                        if (firstPage != null) {
                            _uiState.update { state ->
                                state.copy(
                                    pages = boardPages,
                                    currentPageId = firstPage.id,
                                    canvasState = state.canvasState.copy(
                                        elements = firstPage.elements
                                    )
                                )
                            }
                        }
                    } else {
                        createDefaultPage()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WhiteboardDB", "Load failed: ${e.message}", e)
            }
        }
    }

    private fun createDefaultPage() {
        val defaultPage = BoardPage(
            id = UUID.randomUUID().toString(),
            name = "Page 1",
            order = 0
        )
        _uiState.update { state ->
            state.copy(
                pages = listOf(defaultPage),
                currentPageId = defaultPage.id
            )
        }
        android.util.Log.d("WhiteboardDB", "Default page created with ID: ${defaultPage.id}")
    }

    private fun saveToDatabase() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val uniquePages = state.pages.distinctBy { it.id }

                uniquePages.forEachIndexed { index, page ->
                    val pageElements = state.canvasState.elements.filter { it.pageId == page.id }
                    val updatedPage = page.copy(order = index, name = "Page ${index + 1}")

                    val entity = BoardPageEntity(
                        id = updatedPage.id,
                        name = updatedPage.name,
                        order = updatedPage.order,
                        createdAt = updatedPage.createdAt,
                        updatedAt = System.currentTimeMillis(),
                        elements = pageElements
                    )
                    repository.savePage(entity)
                }
            } catch (e: Exception) {
                android.util.Log.e("WhiteboardDB", "Save failed: ${e.message}", e)
            }
        }
    }

    fun manualSave() {
        saveToDatabase()
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllPages()
            _uiState.update { state ->
                state.copy(
                    pages = emptyList(),
                    currentPageId = "",
                    canvasState = CanvasState(),
                    undoStack = emptyList(),
                    redoStack = emptyList()
                )
            }
            createDefaultPage()
        }
    }

    // ========== মূল ফাংশন ==========

    fun selectTool(tool: DrawingTool) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(selectedTool = tool)
            )
        }
    }

    fun updateColor(color: Color) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(
                    strokeColor = color,
                    textColor = color
                )
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

    fun onDrawStart(offset: Offset) {
        val state = _uiState.value
        val canvasOffset = state.canvasState.transform.screenToCanvas(offset)
        val tool = state.toolSettings.selectedTool

        when (tool) {
            is DrawingTool.Pen -> {
                val newPath = PathElement(
                    pageId = state.currentPageId,
                    points = listOf(canvasOffset),
                    color = state.toolSettings.strokeColor,
                    strokeWidth = state.toolSettings.strokeWidth,
                    opacity = 1f,
                    isEraser = false
                )
                _uiState.update { it.copy(
                    canvasState = it.canvasState.copy(
                        isDrawing = true,
                        currentPath = newPath
                    )
                )}
            }
            is DrawingTool.Highlighter -> {
                val newPath = PathElement(
                    pageId = state.currentPageId,
                    points = listOf(canvasOffset),
                    color = state.toolSettings.strokeColor,
                    strokeWidth = state.toolSettings.strokeWidth,
                    opacity = 0.4f,
                    isEraser = false
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
                    points = listOf(canvasOffset),
                    color = Color.White,
                    strokeWidth = state.toolSettings.strokeWidth * 3,
                    opacity = 1f,
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

        saveToUndoStack()

        val currentPageId = state.currentPageId
        val newElements = buildList {
            addAll(state.canvasState.elements)
            state.canvasState.currentPath?.let { add(it.copy(pageId = currentPageId)) }
            state.canvasState.currentShape?.let { add(it.copy(pageId = currentPageId)) }
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

        scheduleAutoSave()
    }

    fun onZoom(centroid: Offset, zoom: Float) {
        _uiState.update { state ->
            val currentTransform = state.canvasState.transform
            val newScale = (currentTransform.scale * zoom)
                .coerceIn(0.5f, 5f)

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
                    transform = Transform()
                )
            )
        }
    }

    private fun saveToUndoStack() {
        _uiState.update { state ->
            val newUndoStack = (state.undoStack + listOf(state.canvasState.elements))
                .takeLast(50)
            state.copy(
                undoStack = newUndoStack,
                redoStack = emptyList()
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
        val currentPageId = _uiState.value.currentPageId
        _uiState.update { state ->
            val filteredElements = state.canvasState.elements
                .filter { it.pageId != currentPageId }
            state.copy(
                canvasState = state.canvasState.copy(elements = filteredElements)
            )
        }
    }

    fun addPage() {
        val currentPages = _uiState.value.pages
        val newPageNumber = currentPages.size + 1

        val newPage = BoardPage(
            id = UUID.randomUUID().toString(),
            name = "Page $newPageNumber",
            order = currentPages.size
        )

        _uiState.update { state ->
            state.copy(
                pages = state.pages + newPage,
                currentPageId = newPage.id
            )
        }
        android.util.Log.d("WhiteboardDB", "addPage - New page created: ${newPage.name}, ID: ${newPage.id}")
    }

    fun switchPage(pageId: String) {
        android.util.Log.d("WhiteboardDB", "switchPage called - to: $pageId")

        // সব পেজের elements অক্ষত রেখে শুধু currentPageId পরিবর্তন করুন
        _uiState.update { currentState ->
            currentState.copy(
                currentPageId = pageId,
                canvasState = currentState.canvasState.copy(
                    currentPath = null,
                    currentShape = null
                )
            )
        }

        android.util.Log.d("WhiteboardDB", "switchPage completed - currentPageId: ${_uiState.value.currentPageId}, totalElements: ${_uiState.value.canvasState.elements.size}")
    }

    fun deletePage(pageId: String) {
        val state = _uiState.value
        if (state.pages.size <= 1) {
            android.util.Log.d("WhiteboardDB", "Cannot delete last page")
            return
        }

        // UI থেকে পেজ রিমুভ করুন
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

        // ডাটাবেস থেকে পেজ ডিলিট করুন
        viewModelScope.launch {
            try {
                // ডাটাবেস থেকে সরাসরি ডিলিট
                val allEntities = repository.getAllPages().first()
                val entityToDelete = allEntities.find { it.id == pageId }
                if (entityToDelete != null) {
                    repository.deletePage(entityToDelete)
                    android.util.Log.d("WhiteboardDB", "Page $pageId deleted from database")
                }
            } catch (e: Exception) {
                android.util.Log.e("WhiteboardDB", "Delete page failed: ${e.message}", e)
            }
        }
    }

    fun onCanvasTap(position: Offset) {
        val state = _uiState.value
        when (state.toolSettings.selectedTool) {
            is DrawingTool.Text -> {
                addTextElement(position)
            }
            is DrawingTool.StickyNote -> {
                addStickyNote(position)
            }
            else -> {}
        }
    }

    fun addStickyNote(position: Offset) {
        val state = _uiState.value
        val canvasPosition = state.canvasState.transform.screenToCanvas(position)
        val currentZIndex = state.canvasState.elements.size

        val note = StickyNoteElement(
            pageId = state.currentPageId,
            zIndex = currentZIndex,
            position = canvasPosition,
            text = "New Note",
            fontSize = state.toolSettings.stickyNoteTextSize
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
        val currentZIndex = state.canvasState.elements.size

        val text = TextElement(
            pageId = state.currentPageId,
            zIndex = currentZIndex,
            position = canvasPosition,
            text = "",
            color = state.toolSettings.textColor,
            textSize = state.toolSettings.textSize,
            isBold = false,
            isItalic = false,
            isEditing = true
        )
        saveToUndoStack()
        _uiState.update { s ->
            s.copy(
                canvasState = s.canvasState.copy(
                    elements = s.canvasState.elements + text
                )
            )
        }
    }

    fun updateTextElement(elementId: String, newText: String) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.map { element ->
                if (element is TextElement && element.id == elementId) {
                    element.copy(text = newText, isEditing = false)
                } else element
            }
            state.copy(
                canvasState = state.canvasState.copy(
                    elements = updatedElements,
                    selectedElementId = null
                ),
                toolSettings = state.toolSettings.copy(selectedTool = DrawingTool.Pen())
            )
        }
    }

    fun cancelTextElement(elementId: String) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.filter { element ->
                !(element is TextElement && element.id == elementId && element.isEditing)
            }
            state.copy(
                canvasState = state.canvasState.copy(
                    elements = updatedElements,
                    selectedElementId = null
                ),
                toolSettings = state.toolSettings.copy(selectedTool = DrawingTool.Pen())
            )
        }
    }

    fun updateStickyNote(elementId: String, newText: String) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.map { element ->
                if (element is StickyNoteElement && element.id == elementId) {
                    element.copy(text = newText)
                } else element
            }
            state.copy(
                canvasState = state.canvasState.copy(elements = updatedElements),
                toolSettings = state.toolSettings.copy(selectedTool = DrawingTool.Pen())
            )
        }
    }

    fun cancelStickyNoteElement(elementId: String) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.filter { element ->
                element.id != elementId
            }
            state.copy(
                canvasState = state.canvasState.copy(
                    elements = updatedElements,
                    selectedElementId = null
                ),
                toolSettings = state.toolSettings.copy(selectedTool = DrawingTool.Pen())
            )
        }
    }

    fun updateTextPosition(elementId: String, newPosition: Offset) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.map { element ->
                if (element is TextElement && element.id == elementId) {
                    element.copy(position = newPosition)
                } else element
            }
            state.copy(
                canvasState = state.canvasState.copy(elements = updatedElements)
            )
        }
    }

    fun updateStickyNotePosition(elementId: String, newPosition: Offset) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.map { element ->
                if (element is StickyNoteElement && element.id == elementId) {
                    element.copy(position = newPosition)
                } else element
            }
            state.copy(
                canvasState = state.canvasState.copy(elements = updatedElements)
            )
        }
    }

    fun shareCanvas() {
        _uiState.update { it.copy(exportSuccess = true) }
    }

    // ========== ফন্ট সাইজ ফাংশন ==========

    fun updateTextSize(size: Float) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(textSize = size)
            )
        }
    }

    fun updateStickyNoteTextSize(size: Float) {
        _uiState.update { state ->
            state.copy(
                toolSettings = state.toolSettings.copy(stickyNoteTextSize = size)
            )
        }
    }

    fun updateAllTextSizes(newSize: Float) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.map { element ->
                if (element is TextElement) {
                    element.copy(textSize = newSize)
                } else element
            }
            state.copy(
                canvasState = state.canvasState.copy(elements = updatedElements),
                toolSettings = state.toolSettings.copy(textSize = newSize)
            )
        }
    }

    fun updateAllStickyNoteSizes(newSize: Float) {
        _uiState.update { state ->
            val updatedElements = state.canvasState.elements.map { element ->
                if (element is StickyNoteElement) {
                    element.copy(fontSize = newSize)
                } else element
            }
            state.copy(
                canvasState = state.canvasState.copy(elements = updatedElements),
                toolSettings = state.toolSettings.copy(stickyNoteTextSize = newSize)
            )
        }
    }

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            saveToDatabase()
        }
    }
}