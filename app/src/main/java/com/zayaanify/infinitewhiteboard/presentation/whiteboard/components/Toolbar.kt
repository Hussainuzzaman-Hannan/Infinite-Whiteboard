// presentation/whiteboard/components/Toolbar.kt

package com.zayaanify.infinitewhiteboard.presentation.whiteboard.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zayaanify.infinitewhiteboard.domain.model.DrawingTool
import com.zayaanify.infinitewhiteboard.domain.model.ToolSettings

@Composable
fun WhiteboardToolbar(
    toolSettings: ToolSettings,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelect: (DrawingTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onColorClick: () -> Unit,
    onStrokeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drawing tools
        ToolButton(
            icon = Icons.Outlined.Edit,
            label = "Pen",
            isSelected = toolSettings.tool == DrawingTool.Pen,
            onClick = { onToolSelect(DrawingTool.Pen) }
        )
        ToolButton(
            icon = Icons.Outlined.BorderColor,
            label = "Highlighter",
            isSelected = toolSettings.tool == DrawingTool.Highlighter,
            onClick = { onToolSelect(DrawingTool.Highlighter) }
        )
        ToolButton(
            icon = Icons.Outlined.AutoFixHigh,
            label = "Eraser",
            isSelected = toolSettings.tool == DrawingTool.Eraser,
            onClick = { onToolSelect(DrawingTool.Eraser) }
        )

        VerticalDivider(modifier = Modifier.height(28.dp))

        // Shape tools
        ToolButton(
            icon = Icons.Outlined.Square,
            label = "Rectangle",
            isSelected = toolSettings.tool == DrawingTool.Shape.Rectangle,
            onClick = { onToolSelect(DrawingTool.Shape.Rectangle) }
        )
        ToolButton(
            icon = Icons.Outlined.Circle,
            label = "Circle",
            isSelected = toolSettings.tool == DrawingTool.Shape.Circle,
            onClick = { onToolSelect(DrawingTool.Shape.Circle) }
        )
        ToolButton(
            icon = Icons.Outlined.HorizontalRule,
            label = "Line",
            isSelected = toolSettings.tool == DrawingTool.Shape.Line,
            onClick = { onToolSelect(DrawingTool.Shape.Line) }
        )
        ToolButton(
            icon = Icons.Outlined.ArrowRightAlt,
            label = "Arrow",
            isSelected = toolSettings.tool == DrawingTool.Shape.Arrow,
            onClick = { onToolSelect(DrawingTool.Shape.Arrow) }
        )

        VerticalDivider(modifier = Modifier.height(28.dp))

        // Text + Sticky
        ToolButton(
            icon = Icons.Outlined.TextFields,
            label = "Text",
            isSelected = toolSettings.tool == DrawingTool.Text,
            onClick = { onToolSelect(DrawingTool.Text) }
        )
        ToolButton(
            icon = Icons.Outlined.StickyNote2,
            label = "Sticky",
            isSelected = toolSettings.tool == DrawingTool.StickyNote,
            onClick = { onToolSelect(DrawingTool.StickyNote) }
        )
        ToolButton(
            icon = Icons.Outlined.PanTool,
            label = "Pan",
            isSelected = toolSettings.tool == DrawingTool.Pan,
            onClick = { onToolSelect(DrawingTool.Pan) }
        )

        VerticalDivider(modifier = Modifier.height(28.dp))

        // Color indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(toolSettings.strokeColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { onColorClick() }
        )

        // Stroke width
        ToolButton(
            icon = Icons.Outlined.LineWeight,
            label = "Stroke",
            isSelected = false,
            onClick = onStrokeClick
        )

        VerticalDivider(modifier = Modifier.height(28.dp))

        // Undo/Redo
        ToolButton(
            icon = Icons.Outlined.Undo,
            label = "Undo",
            isSelected = false,
            enabled = canUndo,
            onClick = onUndo
        )
        ToolButton(
            icon = Icons.Outlined.Redo,
            label = "Redo",
            isSelected = false,
            enabled = canRedo,
            onClick = onRedo
        )
        ToolButton(
            icon = Icons.Outlined.Delete,
            label = "Clear",
            isSelected = false,
            tint = MaterialTheme.colorScheme.error,
            onClick = onClear
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val iconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        tint != null -> tint
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}