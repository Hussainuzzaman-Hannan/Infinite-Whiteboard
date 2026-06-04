package com.zayaanify.infinitewhiteboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zayaanify.infinitewhiteboard.domain.model.CanvasElement

@Entity(tableName = "board_pages")
@TypeConverters(Converters::class)
data class BoardPageEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val order: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val elements: List<CanvasElement>
)