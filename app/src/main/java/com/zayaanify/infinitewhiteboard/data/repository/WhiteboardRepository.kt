package com.zayaanify.infinitewhiteboard.data.repository

import com.zayaanify.infinitewhiteboard.data.local.BoardPageDao
import com.zayaanify.infinitewhiteboard.domain.model.BoardPageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhiteboardRepository @Inject constructor(
    private val boardPageDao: BoardPageDao
) {

    fun getAllPages(): Flow<List<BoardPageEntity>> = boardPageDao.getAllPages()

    suspend fun getPageById(pageId: String): BoardPageEntity? = boardPageDao.getPageById(pageId)

    suspend fun savePage(page: BoardPageEntity) = boardPageDao.insertPage(page)

    suspend fun updatePage(page: BoardPageEntity) = boardPageDao.updatePage(page)

    suspend fun deletePage(page: BoardPageEntity) = boardPageDao.deletePage(page)

    suspend fun deleteAllPages() = boardPageDao.deleteAllPages()

    suspend fun getPageCount(): Int = boardPageDao.getPageCount()
}