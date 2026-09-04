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
import androidx.room.Transaction
import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary
import kotlinx.coroutines.flow.Flow

/** Room DAO for all [Todo] CRUD operations. */
@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY is_pinned DESC, deadline_timestamp IS NULL ASC, deadline_timestamp ASC")
    fun getAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE state = 'OPEN' ORDER BY is_pinned DESC, deadline_timestamp IS NULL ASC, deadline_timestamp ASC")
    fun getOpen(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE state = 'DONE' ORDER BY creation_timestamp DESC")
    fun getDone(): Flow<List<Todo>>

    @Query("SELECT id, title, deadline_timestamp, state, creation_timestamp, is_pinned, is_locked, tag_id FROM todos WHERE state = 'OPEN' ORDER BY is_pinned DESC, deadline_timestamp IS NULL ASC, deadline_timestamp ASC")
    fun getOpenSummaries(): Flow<List<TodoSummary>>

    @Query("SELECT id, title, deadline_timestamp, state, creation_timestamp, is_pinned, is_locked, tag_id FROM todos WHERE state = 'DONE' ORDER BY creation_timestamp DESC")
    fun getDoneSummaries(): Flow<List<TodoSummary>>

    @Query("SELECT id, title, deadline_timestamp, state, creation_timestamp, is_pinned, is_locked, tag_id FROM todos")
    fun getAllSummaries(): Flow<List<TodoSummary>>

    @Query("SELECT id, title, deadline_timestamp, state, creation_timestamp, is_pinned, is_locked, tag_id FROM todos WHERE tag_id = :tagId AND state = 'OPEN' ORDER BY is_pinned DESC, deadline_timestamp IS NULL ASC, deadline_timestamp ASC")
    fun getOpenSummariesByTag(tagId: Int): Flow<List<TodoSummary>>

    @Query("SELECT id, title, deadline_timestamp, state, creation_timestamp, is_pinned, is_locked, tag_id FROM todos WHERE tag_id = :tagId AND state = 'DONE' ORDER BY creation_timestamp DESC")
    fun getDoneSummariesByTag(tagId: Int): Flow<List<TodoSummary>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Int): Todo?

    @Query("SELECT * FROM todos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Todo>

    @Query("SELECT * FROM todos")
    suspend fun getAllTodos(): List<Todo>

    @Query("UPDATE todos SET state = :state WHERE id = :id")
    suspend fun updateState(id: Int, state: TodoState)

    @Query("UPDATE todos SET is_pinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Int, isPinned: Boolean)

    @Query("UPDATE todos SET is_locked = :isLocked WHERE id = :id")
    suspend fun updateLocked(id: Int, isLocked: Boolean)

    @Query("UPDATE todos SET tag_id = :tagId WHERE id = :id")
    suspend fun updateTag(id: Int, tagId: Int?)

    @Query("UPDATE todos SET tag_id = NULL WHERE tag_id = :tagId")
    suspend fun clearTagReferences(tagId: Int)

    @Insert
    suspend fun insert(todo: Todo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(todos: List<Todo>)

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Delete
    suspend fun delete(todos: List<Todo>)

    @Query("DELETE FROM todos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(todos: List<Todo>) {
        deleteAll()
        if (todos.isNotEmpty()) insertReplace(todos)
    }
}
