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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import at.co.schwaerzler.maximilian.doit.DoItApplication
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.db.TodoRepository
import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag

/**
 * ViewModel for the add/edit screen.
 *
 * Handles both creating a new todo (`id == null`) and editing an existing one (`id != null`).
 */
class EditTodoViewModel(
    private val appContext: Context,
    private val repository: TodoRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow<EditTodoUiState>(EditTodoUiState())

    /** Current form state exposed to the UI. */
    val uiState = _uiState.asStateFlow()

    val allTags = repository.getAllTagsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tagsEnabled = appPreferences.tagsEnabled

    val appLockEnabled = appPreferences.appLockEnabled
    val appLockPin = appPreferences.appLockPin
    val biometricUnlockEnabled = appPreferences.biometricUnlockEnabled

    private var originalTodo: Todo? = null
    private var originalTitle = ""
    private var originalDescription = ""
    private var originalDeadline: Instant? = null
    private var originalIsPinned = false
    private var originalIsLocked = false
    private var originalTagId: Int? = null

    val notificationDialogSuppressed = appPreferences.notificationDialogSuppressed

    fun saveNotificationDialogSuppressed(suppressed: Boolean) {
        appPreferences.saveNotificationDialogSuppressed(suppressed)
    }

    val isModified = uiState.map { state ->
        state.title != originalTitle ||
                state.description != originalDescription ||
                state.deadline != originalDeadline ||
                state.isPinned != originalIsPinned ||
                state.isLocked != originalIsLocked ||
                state.tagId != originalTagId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun updateTitle(newTitle: String) {
        _uiState.update {
            it.copy(
                title = newTitle,
                titleError = if (newTitle.isNotBlank()) null else it.titleError
            )
        }
    }

    fun updateDescription(newDescription: String) {
        _uiState.update {
            it.copy(
                description = newDescription
            )
        }
    }

    fun updateDeadline(newDeadline: Instant?) {
        _uiState.update {
            it.copy(
                deadline = newDeadline
            )
        }
    }

    fun togglePinned() {
        _uiState.update {
            it.copy(
                isPinned = !it.isPinned
            )
        }
    }

    fun updateLocked(isLocked: Boolean) {
        _uiState.update {
            it.copy(
                isLocked = isLocked
            )
        }
    }

    fun updateTag(tagId: Int?) {
        _uiState.update {
            it.copy(
                tagId = tagId
            )
        }
    }

    fun createTag(tag: Tag, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertTag(tag)
            _uiState.update { it.copy(tagId = id.toInt()) }
            onCreated(id)
        }
    }

    fun duplicateTodo(onDone: () -> Unit) {
        val current = _uiState.value
        if (current.title.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Todo(
                    id = 0,
                    title = current.title,
                    description = current.description.ifBlank { null },
                    deadlineDateTime = current.deadline,
                    isPinned = current.isPinned,
                    isLocked = current.isLocked,
                    tagId = current.tagId
                )
            )
            onDone()
        }
    }

    fun saveTodo(id: Int?): Boolean {
        if (_uiState.value.title.isBlank()) {
            _uiState.update { it.copy(titleError = appContext.getString(R.string.title_is_required)) }
            return false
        }

        if (id == null) {
            viewModelScope.launch {
                repository.insert(
                    Todo(
                        id = 0,
                        title = uiState.value.title,
                        description = uiState.value.description.ifBlank { null },
                        deadlineDateTime = uiState.value.deadline,
                        isPinned = uiState.value.isPinned,
                        isLocked = uiState.value.isLocked,
                        tagId = uiState.value.tagId
                    )
                )
            }
            return true
        } else {
            val original = originalTodo ?: return false
            viewModelScope.launch {
                repository.update(
                    original.copy(
                        title = uiState.value.title,
                        description = uiState.value.description.ifBlank { null },
                        deadlineDateTime = uiState.value.deadline,
                        isPinned = uiState.value.isPinned,
                        isLocked = uiState.value.isLocked,
                        tagId = uiState.value.tagId
                    )
                )
            }
            return true
        }
    }

    fun deleteTodo() {
        val todo = originalTodo ?: return
        viewModelScope.launch {
            repository.delete(todo)
        }
    }

    fun loadTodo(id: Int) {
        viewModelScope.launch {
            val todo = repository.getById(id) ?: return@launch
            originalTodo = todo
            originalTitle = todo.title
            originalDescription = todo.description ?: ""
            originalDeadline = todo.deadlineDateTime
            originalIsPinned = todo.isPinned
            originalIsLocked = todo.isLocked
            originalTagId = todo.tagId
            _uiState.update {
                it.copy(
                    title = todo.title,
                    description = todo.description ?: "",
                    deadline = todo.deadlineDateTime,
                    isPinned = todo.isPinned,
                    isLocked = todo.isLocked,
                    tagId = todo.tagId
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DoItApplication
                EditTodoViewModel(
                    appContext = app.applicationContext,
                    repository = app.repository,
                    appPreferences = app.appPreferences
                )
            }
        }
    }

    data class EditTodoUiState(
        val title: String = "",
        val description: String = "",
        val titleError: String? = null,
        val descriptionError: String? = null,
        val deadline: Instant? = null,
        val isPinned: Boolean = false,
        val isLocked: Boolean = false,
        val tagId: Int? = null
    )
}
