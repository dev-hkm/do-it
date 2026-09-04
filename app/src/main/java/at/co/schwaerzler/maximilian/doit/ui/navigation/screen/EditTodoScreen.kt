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

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.EditTodoViewModel
import at.co.schwaerzler.maximilian.doit.data.EditTodoViewModel.EditTodoUiState
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import at.co.schwaerzler.maximilian.doit.ui.component.AppDialogSurface
import at.co.schwaerzler.maximilian.doit.ui.component.AppTagChip
import at.co.schwaerzler.maximilian.doit.ui.component.AppTopBar
import at.co.schwaerzler.maximilian.doit.ui.component.MarkdownContent
import at.co.schwaerzler.maximilian.doit.ui.component.MaxWidthLayout
import at.co.schwaerzler.maximilian.doit.ui.component.NotificationPermissionDialog
import at.co.schwaerzler.maximilian.doit.ui.component.PinAuthDialog
import at.co.schwaerzler.maximilian.doit.ui.component.PinMode
import at.co.schwaerzler.maximilian.doit.ui.component.RoundedCheckbox
import at.co.schwaerzler.maximilian.doit.ui.component.TagEditDialog
import at.co.schwaerzler.maximilian.doit.util.LucideIconCatalog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EditTodoScreen(
    todoId: Int?,
    fromWidget: Boolean = false,
    navigateBack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditTodoViewModel = viewModel(factory = EditTodoViewModel.Factory)
) {
    val activity = LocalActivity.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isModified by viewModel.isModified.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val tagsEnabled by viewModel.tagsEnabled.collectAsStateWithLifecycle()
    val appLockPin by viewModel.appLockPin.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    val notificationDialogSuppressed by viewModel.notificationDialogSuppressed.collectAsStateWithLifecycle()

    LaunchedEffect(todoId) {
        if (todoId != null) {
            viewModel.loadTodo(todoId)
        }
    }

    BackHandler(enabled = isModified) {
        showDiscardDialog = true
    }

    fun finishScreen() {
        if (fromWidget) {
            activity?.finishAfterTransition()
        } else {
            navigateBack()
        }
    }

    if (showDiscardDialog) {
        AppDialogSurface(
            title = stringResource(R.string.discard_changes_question),
            supportingText = stringResource(R.string.unsaved_changes_lost),
            actions = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDiscardDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        showDiscardDialog = false
                        finishScreen()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            onDismissRequest = { showDiscardDialog = false }
        )
    }

    val permissionState: PermissionState? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(
                permission = android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else null

    fun saveTodo() {
        val permissionStateGranted = permissionState?.status?.isGranted ?: true
        if (permissionStateGranted) {
            if (viewModel.saveTodo(todoId)) {
                finishScreen()
            }
        } else {
            showNotificationPermissionDialog = true
        }
    }

    if (showNotificationPermissionDialog && permissionState != null) {
        NotificationPermissionDialog(
            notificationPermissionState = permissionState,
            onDismissRequest = {
                showNotificationPermissionDialog = false
                viewModel.saveTodo(todoId)
                finishScreen()
            },
            onDoNotShowAgain = {
                showNotificationPermissionDialog = false
                viewModel.saveNotificationDialogSuppressed(true)
                viewModel.saveTodo(todoId)
                finishScreen()
            }
        )
    }

    if (showPinSetupDialog) {
        PinAuthDialog(
            correctPin = null,
            mode = PinMode.SETUP_NEW,
            onSuccess = { newPin ->
                // Pin successfully set
                viewModel.updateLocked(true)
                showPinSetupDialog = false
            },
            onDismissRequest = { showPinSetupDialog = false }
        )
    }

    if (showCreateTagDialog) {
        TagEditDialog(
            tag = null,
            onSave = { newTag ->
                viewModel.createTag(newTag)
                showCreateTagDialog = false
            },
            onDismissRequest = { showCreateTagDialog = false }
        )
    }

    val context = LocalContext.current

    EditTodoScreenContent(
        id = todoId,
        uiState = uiState,
        tags = tags,
        tagsEnabled = tagsEnabled,
        onTitleChange = viewModel::updateTitle,
        onDescriptionChange = viewModel::updateDescription,
        onDeadlineChange = viewModel::updateDeadline,
        onTogglePinned = viewModel::togglePinned,
        onToggleLocked = { shouldLock ->
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            if (shouldLock && appLockPin.isNullOrBlank()) {
                showPinSetupDialog = true
            } else {
                viewModel.updateLocked(shouldLock)
            }
        },
        onSelectTag = { tagId ->
            viewModel.updateTag(tagId)
        },
        onCreateTagClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            showCreateTagDialog = true
        },
        onDone = ::saveTodo,
        onCancel = {
            if (isModified) {
                showDiscardDialog = true
            } else {
                finishScreen()
            }
        },
        onDelete = {
            viewModel.deleteTodo()
            finishScreen()
        },
        onDuplicate = {
            viewModel.duplicateTodo {
                finishScreen()
            }
        },
        onShareTodo = {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, uiState.title + "\n\n" + uiState.description)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_todo)))
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTodoScreenContent(
    id: Int?,
    uiState: EditTodoUiState,
    tags: List<Tag>,
    tagsEnabled: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDeadlineChange: (Instant?) -> Unit,
    onTogglePinned: () -> Unit,
    onToggleLocked: (Boolean) -> Unit,
    onSelectTag: (Int?) -> Unit,
    onCreateTagClick: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onShareTodo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val titleFocusRequester = remember { FocusRequester() }
    var isMarkdownPreview by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (id == null) {
            titleFocusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDone()
                },
                icon = {
                    Icon(
                        painterResource(R.drawable.check_24px),
                        contentDescription = stringResource(R.string.done_fab)
                    )
                },
                text = {
                    Text(stringResource(R.string.done_fab))
                }
            )
        },
        topBar = {
            AppTopBar(
                title = {
                    if (id == null) {
                        Text(
                            stringResource(R.string.add_new_todo_app_bar),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Text(
                            stringResource(R.string.edit_todo_app_bar),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCancel()
                        },
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = null)
                    }
                },
                actions = {
                    // Pin / Unpin button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onTogglePinned()
                        },
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(
                            painterResource(if (uiState.isPinned) com.composables.icons.lucide.R.drawable.lucide_ic_pin_off else com.composables.icons.lucide.R.drawable.lucide_ic_pin),
                            contentDescription = if (uiState.isPinned) stringResource(R.string.unpin_todo) else stringResource(R.string.pin_todo),
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (id != null) {
                        // Duplicate button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDuplicate()
                            },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Icon(
                                painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_copy),
                                contentDescription = stringResource(R.string.duplicate_todo)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDelete()
                            },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Icon(
                                painterResource(R.drawable.delete_24px),
                                contentDescription = stringResource(R.string.delete_todo)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShareTodo()
                            },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Icon(
                                painterResource(R.drawable.share_24px),
                                contentDescription = stringResource(R.string.share_todo)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        MaxWidthLayout(Modifier.padding(top = innerPadding.calculateTopPadding())) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .focusRequester(titleFocusRequester),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = {
                        Text(stringResource(R.string.title))
                    },
                    supportingText = if (uiState.titleError != null) {
                        { Text(uiState.titleError) }
                    } else null,
                    isError = uiState.titleError != null,
                    leadingIcon = {
                        Icon(painterResource(R.drawable.title_24px), contentDescription = null)
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                // Tag Selector Row
                if (tagsEnabled) Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        stringResource(R.string.tags_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTagChip(
                            label = stringResource(R.string.no_tag),
                            selected = uiState.tagId == null,
                            onClick = { onSelectTag(null) },
                            color = MaterialTheme.colorScheme.primary
                        )

                        tags.forEach { tag ->
                            val isSelected = uiState.tagId == tag.id
                            val tagColor = tag.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                            val iconRes = LucideIconCatalog.getIconResId(context, tag.iconName)

                            AppTagChip(
                                label = tag.name,
                                selected = isSelected,
                                onClick = { onSelectTag(if (isSelected) null else tag.id) },
                                color = tagColor,
                                icon = painterResource(iconRes)
                            )
                        }

                        // "+ Create Tag" button
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCreateTagClick()
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_plus),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.new_tag),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Description Field with Markdown Toggle
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.description),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                isMarkdownPreview = !isMarkdownPreview
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isMarkdownPreview) com.composables.icons.lucide.R.drawable.lucide_ic_pencil
                                    else com.composables.icons.lucide.R.drawable.lucide_ic_eye
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isMarkdownPreview) stringResource(R.string.edit_mode) else stringResource(R.string.preview_mode),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = isMarkdownPreview,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "markdown_transition"
                    ) { preview ->
                        if (preview) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    if (uiState.description.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.empty_markdown_preview),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        MarkdownContent(
                                            markdown = uiState.description,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = uiState.description,
                                onValueChange = onDescriptionChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                shape = RoundedCornerShape(16.dp),
                                textStyle = LocalTextStyle.current
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Lock Task Switch Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.isLocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_lock),
                                    contentDescription = null,
                                    tint = if (uiState.isLocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.lock_this_task),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.lock_this_task_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = uiState.isLocked,
                            onCheckedChange = onToggleLocked,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                DeadlineSection(
                    deadline = uiState.deadline,
                    onDeadlineChange = onDeadlineChange
                )

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineSection(
    deadline: Instant?,
    onDeadlineChange: (Instant?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDateMillis by remember { mutableLongStateOf(0L) }

    val formattedDeadline = remember(deadline) {
        deadline?.let {
            val local = it.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(local)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.deadline),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.event_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDeadline ?: stringResource(R.string.no_deadline_set),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (deadline != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (deadline != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deadline != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onDeadlineChange(null)
                        },
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_x),
                            contentDescription = stringResource(R.string.clear_deadline),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDatePicker = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(if (deadline == null) R.string.set_deadline else R.string.change_deadline))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Quick Preset Chips (Today, Tomorrow, Next Week, etc.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPresetChip(label = stringResource(R.string.preset_today)) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val todayEvening = LocalDateTime(now.date, LocalTime(18, 0)).toInstant(TimeZone.currentSystemDefault())
                onDeadlineChange(todayEvening)
            }

            QuickPresetChip(label = stringResource(R.string.preset_tomorrow)) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val tomorrow = now.date.plus(1, DateTimeUnit.DAY)
                val tomorrowEvening = LocalDateTime(tomorrow, LocalTime(18, 0)).toInstant(TimeZone.currentSystemDefault())
                onDeadlineChange(tomorrowEvening)
            }

            QuickPresetChip(label = stringResource(R.string.preset_next_week)) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val daysUntilNextMonday = (DayOfWeek.MONDAY.isoDayNumber - now.date.dayOfWeek.isoDayNumber + 7) % 7
                val nextMonday = now.date.plus(if (daysUntilNextMonday == 0) 7 else daysUntilNextMonday, DateTimeUnit.DAY)
                val nextMondayMorning = LocalDateTime(nextMonday, LocalTime(9, 0)).toInstant(TimeZone.currentSystemDefault())
                onDeadlineChange(nextMondayMorning)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
        )
            DatePickerDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        datePickerState.selectedDateMillis?.let { millis ->
                            tempDateMillis = millis
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.next))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 18, initialMinute = 0)
        AppDialogSurface(
            title = stringResource(R.string.select_time),
            actions = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val instantDate = Instant.fromEpochMilliseconds(tempDateMillis)
                        val localDate = instantDate.toLocalDateTime(TimeZone.UTC).date
                        val localTime = LocalTime(timePickerState.hour, timePickerState.minute)
                        val combined = LocalDateTime(localDate, localTime).toInstant(TimeZone.currentSystemDefault())
                        onDeadlineChange(combined)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showTimePicker = false
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@Composable
private fun QuickPresetChip(label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
