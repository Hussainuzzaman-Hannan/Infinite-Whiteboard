package com.zayaanify.infinitewhiteboard.data.local

import androidx.room.*
import com.zayaanify.infinitewhiteboard.domain.model.BoardPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardPageDao {

    @Query("SELECT * FROM board_pages ORDER BY `order` ASC")
    fun getAllPages(): Flow<List<BoardPageEntity>>

    @Query("SELECT * FROM board_pages WHERE id = :pageId")
    suspend fun getPageById(pageId: String): BoardPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: BoardPageEntity)

    @Update
    suspend fun updatePage(page: BoardPageEntity)

    @Delete
    suspend fun deletePage(page: BoardPageEntity)

    @Query("DELETE FROM board_pages")
    suspend fun deleteAllPages()

    @Query("SELECT COUNT(*) FROM board_pages")
    suspend fun getPageCount(): Int
}