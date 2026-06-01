package com.zayaanify.infinitewhiteboard.domain.model

import java.util.UUID

data class BoardPage(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Page 1",
    val order: Int = 0,
    val thumbnail: String? = null,
    val backgroundColor: Long = 0xFFF8F8F8L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)