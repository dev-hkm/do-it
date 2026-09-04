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

package at.co.schwaerzler.maximilian.doit.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import at.co.schwaerzler.maximilian.doit.DoItApplication
import at.co.schwaerzler.maximilian.doit.data.db.TodoRepository
import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary
import at.co.schwaerzler.maximilian.doit.util.SortOrder
import at.co.schwaerzler.maximilian.doit.util.sortTodos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** ViewModel for the home screen, exposing the todo lists, tag filters, search filter, and bulk-action operations. */
class HomeViewModel(
    private val repository: TodoRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {
    val sortOrder = appPreferences.homeScreenSortOrder
    val tagsEnabled = appPreferences.tagsEnabled
    val searchQuery = MutableStateFlow("")
    val selectedTagId = MutableStateFlow<Int?>(null)

    val allTags = repository.getAllTagsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unlockedTodoIds = MutableStateFlow<Set<Int>>(emptySet())
    val isAppUnlocked = MutableStateFlow<Boolean>(false)

    val appLockEnabled = appPreferences.appLockEnabled
    val appLockPin = appPreferences.appLockPin
    val biometricUnlockEnabled = appPreferences.biometricUnlockEnabled

    val sortedTodos = combine(
        repository.getAllSummaries(),
        sortOrder,
        searchQuery,
        selectedTagId,
        tagsEnabled
    ) { list, order, query, tagId, isTagsEnabled ->
        var filtered = list
        if (isTagsEnabled && tagId != null) {
            filtered = filtered.filter { it.tagId == tagId }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(query, ignoreCase = true) }
        }
        val sorted = order.sortTodos(filtered)
        sorted.partition { it.state != TodoState.DONE }
    }

    fun selectTag(tagId: Int?) {
        selectedTagId.value = tagId
    }

    fun unlockTodo(todoId: Int) {
        unlockedTodoIds.value = unlockedTodoIds.value + todoId
    }

    fun setAppUnlocked(unlocked: Boolean) {
        isAppUnlocked.value = unlocked
    }

    fun createTag(tag: Tag) {
        viewModelScope.launch {
            repository.insertTag(tag)
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            if (selectedTagId.value == tag.id) {
                selectedTagId.value = null
            }
            repository.deleteTag(tag)
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun rotateSortOrder(): SortOrder {
        val newOrder = when (sortOrder.value) {
            SortOrder.DEADLINE_SOONEST_FIRST -> SortOrder.ALPHABETICAL_A_Z
            SortOrder.ALPHABETICAL_A_Z -> SortOrder.ALPHABETICAL_Z_A
            SortOrder.ALPHABETICAL_Z_A -> SortOrder.CREATION_DATE_NEWEST_FIRST
            SortOrder.CREATION_DATE_NEWEST_FIRST -> SortOrder.CREATION_DATE_OLDEST_FIRST
            SortOrder.CREATION_DATE_OLDEST_FIRST -> SortOrder.DEADLINE_SOONEST_FIRST
        }

        setSortOrder(newOrder)
        return newOrder
    }

    fun setSortOrder(order: SortOrder) {
        appPreferences.saveHomeScreenSortOrder(order)
    }

    private val undoMutex = Mutex()
    private val undoBatches = UndoBatchStore<Todo>()
    private val _undoEvents = MutableSharedFlow<UndoBatch>(extraBufferCapacity = 32)
    val undoEvents = _undoEvents.asSharedFlow()

    val todosDone = appPreferences.todosDone
    val widgetDialogSuppressed = appPreferences.widgetDialogSuppressed

    fun suppressWidgetDialog() {
        appPreferences.saveWidgetDialogSuppressed(true)
    }

    fun toggleTodoDone(todo: TodoSummary) {
        viewModelScope.launch {
            val newState = if (todo.state == TodoState.OPEN) TodoState.DONE else TodoState.OPEN
            repository.updateState(todo.id, newState)
            if (newState == TodoState.DONE) {
                appPreferences.incrementTodosDone()
            }
        }
    }

    fun togglePinTodos(ids: List<Int>, shouldPin: Boolean) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.updatePinned(id, shouldPin)
            }
        }
    }

    fun deleteTodosByIds(ids: List<Int>, onDeleted: () -> Unit = {}) {
        val uniqueIds = ids.distinct()
        if (uniqueIds.isEmpty()) return

        viewModelScope.launch {
            val deletedTodos = repository.deleteByIdsReturnContents(uniqueIds)
            if (deletedTodos.isEmpty()) return@launch

            val undoBatch = undoMutex.withLock {
                undoBatches.add(deletedTodos)
            }
            _undoEvents.emit(undoBatch)
            onDeleted()
        }
    }

    fun undoDeleteTodos(batch: UndoBatch) {
        viewModelScope.launch {
            val todos = undoMutex.withLock { undoBatches.take(batch.token) }
            if (todos.isNotEmpty()) {
                repository.reinsertTodos(todos)
            }
        }
    }

    fun clearPendingTodos(batch: UndoBatch) {
        viewModelScope.launch {
            undoMutex.withLock { undoBatches.discard(batch.token) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DoItApplication
                HomeViewModel(
                    repository = app.repository,
                    appPreferences = app.appPreferences
                )
            }
        }
    }
}
