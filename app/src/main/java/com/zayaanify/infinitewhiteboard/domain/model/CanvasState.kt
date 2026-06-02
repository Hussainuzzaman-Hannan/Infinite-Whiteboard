package com.zayaanify.infinitewhiteboard.domain.model

import androidx.compose.ui.geometry.Offset

data class CanvasState(
    val transform: Transform = Transform(),  // Transform from separate file
    val elements: List<CanvasElement> = emptyList(),
    val currentPath: PathElement? = null,
    val currentShape: ShapeElement? = null,
    val isDrawing: Boolean = false,
    val selectedElementId: String? = null
) {
    fun addElement(element: CanvasElement): CanvasState {
        return copy(elements = elements + element)
    }

    fun removeElement(elementId: String): CanvasState {
        return copy(elements = elements.filter { it.id != elementId })
    }

    fun updateElement(elementId: String, update: (CanvasElement) -> CanvasElement): CanvasState {
        return copy(
            elements = elements.map {
                if (it.id == elementId) update(it) else it
            }
        )
    }

    fun clearPage(pageId: String): CanvasState {
        return copy(elements = elements.filter { it.pageId != pageId })
    }
}

// Remove Transform class from here if you have separate Transform.kt file