/*
 * Copyright 2026 Maximilian Schwärzler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package at.co.schwaerzler.maximilian.doit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for CRUD operations on [Tag].
 */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY order_index ASC, id ASC")
    fun getAllTagsFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY order_index ASC, id ASC")
    suspend fun getAllTags(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Int): Tag?

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<Tag>)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
