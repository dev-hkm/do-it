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

package at.co.schwaerzler.maximilian.doit.ui.navigation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import at.co.schwaerzler.maximilian.doit.OverviewWidget
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.HomeViewModel
import at.co.schwaerzler.maximilian.doit.data.OverviewWidgetReceiver
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary
import at.co.schwaerzler.maximilian.doit.ui.component.AppChoiceRow
import at.co.schwaerzler.maximilian.doit.ui.component.AppDialogSurface
import at.co.schwaerzler.maximilian.doit.ui.component.AppModalBottomSheet
import at.co.schwaerzler.maximilian.doit.ui.component.AppTagChip
import at.co.schwaerzler.maximilian.doit.ui.component.MaxWidthLayout
import at.co.schwaerzler.maximilian.doit.ui.component.PinAuthDialog
import at.co.schwaerzler.maximilian.doit.ui.component.TagListManagementSheet
import at.co.schwaerzler.maximilian.doit.ui.component.TodoListItem
import at.co.schwaerzler.maximilian.doit.util.BiometricHelper
import at.co.schwaerzler.maximilian.doit.util.LucideIconCatalog
import at.co.schwaerzler.maximilian.doit.util.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onAddTodo: () -> Unit,
    onClickTodo: (id: Int) -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val todos by viewModel.sortedTodos.collectAsStateWithLifecycle(Pair(emptyList(), emptyList()))
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val tagsEnabled by viewModel.tagsEnabled.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    val unlockedTodoIds by viewModel.unlockedTodoIds.collectAsStateWithLifecycle()

    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val appLockPin by viewModel.appLockPin.collectAsStateWithLifecycle()
    val biometricUnlockEnabled by viewModel.biometricUnlockEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

    val (openTodos, doneTodos) = todos

    LaunchedEffect(tagsEnabled) {
        if (!tagsEnabled) viewModel.selectTag(null)
    }

    var selectedTodos by rememberSaveable {
        mutableStateOf<Set<Int>>(emptySet())
    }

    BackHandler(enabled = selectedTodos.isNotEmpty()) {
        selectedTodos = emptySet()
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val resources = LocalResources.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.undoEvents.collectLatest { batch ->
            var undoHandled = false
            try {
                val snackbarResult = snackbarHostState.showSnackbar(
                    resources.getQuantityString(
                        R.plurals.todos_deleted_template,
                        batch.count
                    ),
                    actionLabel = resources.getString(R.string.undo_snackbar_action_label),
                    duration = SnackbarDuration.Long
                )

                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    undoHandled = true
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    viewModel.undoDeleteTodos(batch)
                }
            } finally {
                // A newer deletion replaces the old snackbar. Do not leave an
                // unreachable batch in memory waiting for a future undo.
                if (!undoHandled) {
                    viewModel.clearPendingTodos(batch)
                }
            }
        }
    }

    val todosDoneCount by viewModel.todosDone.collectAsStateWithLifecycle()
    val widgetDialogSuppressed by viewModel.widgetDialogSuppressed.collectAsStateWithLifecycle()

    val sortOrderScope = rememberCoroutineScope()
    var sortSnackbarJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current

    var pendingLockAuthTodoId by remember { mutableStateOf<Int?>(null) }
    var showPinAuthForTodo by remember { mutableStateOf(false) }
    var showPinAuthForStartup by rememberSaveable { mutableStateOf(false) }
    
    // App Lock on Startup
    LaunchedEffect(appLockEnabled, isAppUnlocked, biometricUnlockEnabled) {
        if (!appLockEnabled || isAppUnlocked) {
            showPinAuthForStartup = false
            return@LaunchedEffect
        }

        val activity = context as? FragmentActivity
        if (biometricUnlockEnabled && activity != null && BiometricHelper.isBiometricAvailable(activity)) {
            showPinAuthForStartup = false
            BiometricHelper.promptBiometric(
                activity = activity,
                title = context.getString(R.string.app_lock_title),
                onSuccess = {
                    showPinAuthForStartup = false
                    viewModel.setAppUnlocked(true)
                },
                onError = { _, _ ->
                    // PIN is a deliberate fallback after cancellation or failure.
                    showPinAuthForStartup = true
                }
            )
        } else {
            showPinAuthForStartup = true
        }
    }

    if (appLockEnabled && !isAppUnlocked && showPinAuthForStartup) {
        PinAuthDialog(
            correctPin = appLockPin,
            title = stringResource(R.string.app_lock_title),
            subtitle = stringResource(R.string.enter_pin_to_unlock),
            onSuccess = {
                showPinAuthForStartup = false
                viewModel.setAppUnlocked(true)
            },
            onBiometricRequest = if (biometricUnlockEnabled && context is FragmentActivity && BiometricHelper.isBiometricAvailable(context)) {
                {
                    BiometricHelper.promptBiometric(
                        activity = context,
                        title = context.getString(R.string.app_lock_title),
                        onSuccess = {
                            showPinAuthForStartup = false
                            viewModel.setAppUnlocked(true)
                        }
                    )
                }
            } else null,
            onDismissRequest = { /* Cannot dismiss app lock */ }
        )
    }

    HomeScreenContent(
        openTodos = openTodos,
        doneTodos = doneTodos,
        tags = tags,
        tagsEnabled = tagsEnabled,
        selectedTagId = selectedTagId,
        onSelectTag = { viewModel.selectTag(it) },
        unlockedTodoIds = unlockedTodoIds,
        onAddTag = { viewModel.createTag(it) },
        onUpdateTag = { viewModel.updateTag(it) },
        onDeleteTag = { viewModel.deleteTag(it) },
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onAddTodo = onAddTodo,
        onClickTodo = { id ->
            val clicked = openTodos.find { it.id == id } ?: doneTodos.find { it.id == id }
            if (clicked != null && clicked.isLocked && id !in unlockedTodoIds) {
                pendingLockAuthTodoId = id
                if (biometricUnlockEnabled && context is FragmentActivity && BiometricHelper.isBiometricAvailable(context)) {
                    BiometricHelper.promptBiometric(
                        activity = context,
                        title = context.getString(R.string.unlock_task_title),
                        onSuccess = {
                            viewModel.unlockTodo(id)
                            onClickTodo(id)
                        },
                        onError = { _, _ ->
                            showPinAuthForTodo = true
                        }
                    )
                } else {
                    showPinAuthForTodo = true
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClickTodo(id)
            }
        },
        toggleTodoItemSelection = { id ->
            selectedTodos = if (id in selectedTodos) selectedTodos - id else selectedTodos + id
        },
        onStateToggle = { haptic.performHapticFeedback(HapticFeedbackType.Confirm); viewModel.toggleTodoDone(it) },
        selectedTodos = selectedTodos,
        onClearSelection = {
            selectedTodos = emptySet()
        },
        onDeleteSelection = {
            viewModel.deleteTodosByIds(selectedTodos.toList())
            selectedTodos = emptySet()
        },
        onTogglePinSelection = {
            val allPinned = selectedTodos.all { id ->
                openTodos.find { it.id == id }?.isPinned == true || doneTodos.find { it.id == id }?.isPinned == true
            }
            viewModel.togglePinTodos(selectedTodos.toList(), !allPinned)
            selectedTodos = emptySet()
        },
        onBatchCompleteSelection = {
            viewModel.batchCompleteTodos(selectedTodos.toList())
            selectedTodos = emptySet()
        },
        onBatchReopenSelection = {
            viewModel.batchReopenTodos(selectedTodos.toList())
            selectedTodos = emptySet()
        },
        modifier = modifier,
        onSelectAll = {
            selectedTodos += openTodos.map { it.id }
            selectedTodos += doneTodos.map { it.id }
        },
        onClickSettings = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClickSettings() },
        onDeleteTodo = {
            viewModel.deleteTodosByIds(listOf(it))
        },
        snackbarHostState = snackbarHostState,
        todosDoneCount = todosDoneCount,
        widgetDialogSuppressed = widgetDialogSuppressed,
        onSuppressWidgetDialog = viewModel::suppressWidgetDialog,
        onRotateSortOrder = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            val newOrder = viewModel.rotateSortOrder()
            sortSnackbarJob?.cancel()
            sortSnackbarJob = sortOrderScope.launch {
                snackbarHostState.showSnackbar(
                    resources.getString(newOrder.labelRes)
                )
            }
        },
        sortOrder = sortOrder,
        onSetSortOrder = { order ->
            viewModel.setSortOrder(order)
            sortSnackbarJob?.cancel()
            sortSnackbarJob = sortOrderScope.launch {
                snackbarHostState.showSnackbar(resources.getString(order.labelRes))
            }
        }
    )

    if (showPinAuthForTodo && pendingLockAuthTodoId != null) {
        val targetId = pendingLockAuthTodoId!!
        PinAuthDialog(
            correctPin = appLockPin,
            title = stringResource(R.string.unlock_task_title),
            onSuccess = {
                viewModel.unlockTodo(targetId)
                onClickTodo(targetId)
                showPinAuthForTodo = false
                pendingLockAuthTodoId = null
            },
            onBiometricRequest = if (biometricUnlockEnabled && context is FragmentActivity && BiometricHelper.isBiometricAvailable(context)) {
                {
                    BiometricHelper.promptBiometric(
                        activity = context,
                        title = context.getString(R.string.unlock_task_title),
                        onSuccess = {
                            viewModel.unlockTodo(targetId)
                            onClickTodo(targetId)
                            showPinAuthForTodo = false
                            pendingLockAuthTodoId = null
                        }
                    )
                }
            } else null,
            onDismissRequest = {
                showPinAuthForTodo = false
                pendingLockAuthTodoId = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    openTodos: List<TodoSummary>,
    doneTodos: List<TodoSummary>,
    tags: List<Tag>,
    tagsEnabled: Boolean,
    selectedTagId: Int?,
    onSelectTag: (Int?) -> Unit,
    unlockedTodoIds: Set<Int>,
    onAddTag: (Tag) -> Unit,
    onUpdateTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddTodo: () -> Unit,
    onClickTodo: (id: Int) -> Unit,
    onClickSettings: () -> Unit,
    toggleTodoItemSelection: (id: Int) -> Unit,
    onStateToggle: (TodoSummary) -> Unit,
    selectedTodos: Set<Int>,
    onClearSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onTogglePinSelection: () -> Unit,
    onBatchCompleteSelection: () -> Unit,
    onBatchReopenSelection: () -> Unit,
    onDeleteTodo: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
    onSelectAll: () -> Unit,
    snackbarHostState: SnackbarHostState,
    todosDoneCount: Int,
    widgetDialogSuppressed: Boolean,
    onSuppressWidgetDialog: () -> Unit,
    onRotateSortOrder: () -> Unit,
    sortOrder: SortOrder = SortOrder.DEADLINE_SOONEST_FIRST,
    onSetSortOrder: (SortOrder) -> Unit = {}
) {
    val inSelectionMode = selectedTodos.isNotEmpty()
    val isAllSelected = selectedTodos.size == openTodos.size + doneTodos.size
    var showWidgetPinIncentiveDialog by remember { mutableStateOf(false) }
    var showSortOrderSheet by remember { mutableStateOf(false) }
    var showTagManagerSheet by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var isDoneCollapsed by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val tagMap = remember(tags) { tags.associateBy { it.id } }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(todosDoneCount, widgetDialogSuppressed) {
        if (todosDoneCount < 1) return@LaunchedEffect
        if (widgetDialogSuppressed) return@LaunchedEffect
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(OverviewWidget::class.java)
        if (glanceIds.isNotEmpty()) return@LaunchedEffect
        showWidgetPinIncentiveDialog = true
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onAddTodo()
                },
                text = {
                    Text(
                        stringResource(R.string.add_new_todo_fab),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                icon = {
                    Icon(
                        painterResource(R.drawable.add_24px),
                        contentDescription = stringResource(R.string.add_new_todo_fab)
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            MaxWidthLayout(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 64.dp,
                        bottom = navBarBottom + 112.dp
                    )
                ) {
                    // Tag Filter Bar
                    if (tagsEnabled) {
                        item(key = "tag-filter-bar") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppTagChip(
                                    label = stringResource(R.string.all_tags),
                                    selected = selectedTagId == null,
                                    onClick = {
                                        onSelectTag(null)
                                    },
                                    color = MaterialTheme.colorScheme.primary
                                )

                                tags.forEach { tag ->
                                    val isSelected = selectedTagId == tag.id
                                    val tagColor = tag.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                                    val iconRes = LucideIconCatalog.getIconResId(context, tag.iconName)

                                    AppTagChip(
                                        label = tag.name,
                                        selected = isSelected,
                                        onClick = {
                                            onSelectTag(if (isSelected) null else tag.id)
                                        },
                                        color = tagColor,
                                        icon = painterResource(iconRes)
                                    )
                                }

                                // Manage Tags Button
                                AppTagChip(
                                    label = stringResource(R.string.manage_tags),
                                    selected = false,
                                    onClick = {
                                        showTagManagerSheet = true
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    icon = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_plus)
                                )
                            }
                        }
                    }

                    if (openTodos.isNotEmpty() || doneTodos.isNotEmpty()) {
                        if (openTodos.isNotEmpty()) {
                            item(key = "open-headline") {
                                Text(
                                    stringResource(R.string.open_headline),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp, vertical = 6.dp)
                                        .animateItem()
                                )
                            }

                            items(
                                items = openTodos,
                                key = { it.id }
                            ) { todo ->
                                TodoListItem(
                                    todo = todo,
                                    tag = if (tagsEnabled) todo.tagId?.let { tagMap[it] } else null,
                                    isUnlocked = todo.id in unlockedTodoIds,
                                    onStateToggle = { onStateToggle(todo) },
                                    onClick = {
                                        if (inSelectionMode) {
                                            toggleTodoItemSelection(todo.id)
                                        } else {
                                            onClickTodo(todo.id)
                                        }
                                    },
                                    onLongClick = {
                                        toggleTodoItemSelection(todo.id)
                                    },
                                    onDelete = {
                                        onDeleteTodo(todo.id)
                                    },
                                    selected = todo.id in selectedTodos,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        if (doneTodos.isNotEmpty()) {
                            item(key = "done-headline") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isDoneCollapsed = !isDoneCollapsed
                                        }
                                        .padding(horizontal = 24.dp)
                                        .padding(top = if (openTodos.isNotEmpty()) 18.dp else 6.dp, bottom = 6.dp)
                                        .animateItem(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.done_headline),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "${doneTodos.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        painter = painterResource(
                                            if (isDoneCollapsed) com.composables.icons.lucide.R.drawable.lucide_ic_chevron_down
                                            else com.composables.icons.lucide.R.drawable.lucide_ic_chevron_up
                                        ),
                                        contentDescription = stringResource(
                                            if (isDoneCollapsed) R.string.expand_section else R.string.collapse_section
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (!isDoneCollapsed) {
                                items(
                                    items = doneTodos,
                                    key = { it.id }
                                ) { todo ->
                                    TodoListItem(
                                        todo = todo,
                                        tag = if (tagsEnabled) todo.tagId?.let { tagMap[it] } else null,
                                        isUnlocked = todo.id in unlockedTodoIds,
                                        onStateToggle = { onStateToggle(todo) },
                                        onClick = {
                                            if (inSelectionMode) {
                                                toggleTodoItemSelection(todo.id)
                                            } else {
                                                onClickTodo(todo.id)
                                            }
                                        },
                                        onLongClick = {
                                            toggleTodoItemSelection(todo.id)
                                        },
                                        onDelete = {
                                            onDeleteTodo(todo.id)
                                        },
                                        selected = todo.id in selectedTodos,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "empty-placeholder") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp, horizontal = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) stringResource(R.string.search_no_results)
                                        else if (selectedTagId != null) stringResource(R.string.no_tasks_in_tag)
                                        else stringResource(R.string.nothing_to_do_empty_text),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) searchQuery
                                        else if (selectedTagId != null) stringResource(R.string.tap_to_clear_filter)
                                        else stringResource(R.string.add_new_todo_button),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Header (TODO Title badge or Search Bar + Search, Sort, Settings Icons)
            AnimatedVisibility(
                visible = !inSelectionMode,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchActive) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(searchFocusRequester),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController?.hide()
                                    }),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search_hint),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSearchQueryChange("")
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_x),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    } else {
                        // Clean Title Badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSearchActive = true
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Toggle Button
                        SoftHomeIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) {
                                    onSearchQueryChange("")
                                }
                            },
                            icon = painterResource(
                                if (isSearchActive) com.composables.icons.lucide.R.drawable.lucide_ic_x
                                else com.composables.icons.lucide.R.drawable.lucide_ic_search
                            ),
                            contentDescription = stringResource(R.string.search_hint),
                            active = isSearchActive
                        )

                        // Sort Button (triggers modern bottom sheet)
                        SoftHomeIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSortOrderSheet = true
                            },
                            icon = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_sliders_horizontal),
                            contentDescription = stringResource(R.string.settings_sort_order)
                        )

                        // Settings Button
                        SoftHomeIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClickSettings()
                            },
                            icon = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_settings),
                            contentDescription = stringResource(R.string.settings_app_bar),
                            iconSize = 20.dp
                        )
                    }
                }
            }

            // Selection Mode Floating Action Bar
            AnimatedVisibility(
                visible = inSelectionMode,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.todos_selected_template,
                                selectedTodos.size,
                                selectedTodos.size
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )

                        // Select All Toggle
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isAllSelected) onClearSelection() else onSelectAll()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(if (isAllSelected) R.string.deselect_all else R.string.select_all),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Batch Complete button (if any selected are open)
                        val hasOpenSelected = selectedTodos.any { id -> openTodos.any { it.id == id } }
                        if (hasOpenSelected) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onBatchCompleteSelection()
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.check_circle_24px),
                                    contentDescription = stringResource(R.string.batch_complete),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Batch Reopen button (if any selected are done)
                        val hasDoneSelected = selectedTodos.any { id -> doneTodos.any { it.id == id } }
                        if (hasDoneSelected) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onBatchReopenSelection()
                                }
                            ) {
                                Icon(
                                    painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_undo),
                                    contentDescription = stringResource(R.string.batch_reopen),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Pin / Unpin Selected
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onTogglePinSelection()
                            }
                        ) {
                            Icon(
                                painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_pin),
                                contentDescription = stringResource(R.string.pin_todo),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete Selected
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDeleteSelection()
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.delete_24px),
                                contentDescription = stringResource(R.string.delete_todo),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagManagerSheet) {
        TagListManagementSheet(
            tags = tags,
            onAddTag = onAddTag,
            onUpdateTag = onUpdateTag,
            onDeleteTag = onDeleteTag,
            onDismissRequest = { showTagManagerSheet = false }
        )
    }

    if (showSortOrderSheet) {
        AppModalBottomSheet(
            onDismissRequest = { showSortOrderSheet = false },
            title = stringResource(R.string.settings_sort_order),
            subtitle = stringResource(R.string.settings_sort_order_dialog_title)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOrder.entries.forEach { order ->
                    AppChoiceRow(
                        selected = sortOrder == order,
                        onClick = {
                            onSetSortOrder(order)
                            showSortOrderSheet = false
                        },
                        label = stringResource(order.labelRes)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showWidgetPinIncentiveDialog) {
        AppDialogSurface(
            onDismissRequest = {
                showWidgetPinIncentiveDialog = false
                onSuppressWidgetDialog()
            },
            title = stringResource(R.string.add_widget_dialog),
            supportingText = stringResource(R.string.add_widget_dialog_text),
            actions = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showWidgetPinIncentiveDialog = false
                        onSuppressWidgetDialog()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.do_not_ask_again_dialog))
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        coroutineScope.launch {
                            GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
                                OverviewWidgetReceiver::class.java
                            )
                        }
                        showWidgetPinIncentiveDialog = false
                        onSuppressWidgetDialog()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.yes_dialog))
                }
            }
        )
    }
}

@Composable
private fun SoftHomeIconButton(
    onClick: () -> Unit,
    icon: Painter,
    contentDescription: String,
    active: Boolean = false,
    iconSize: Dp = 19.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "homeIconPressScale",
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                color = if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(iconSize),
        )
    }
}
