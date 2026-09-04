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

package at.co.schwaerzler.maximilian.doit.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary
import at.co.schwaerzler.maximilian.doit.ui.theme.DoItTheme
import at.co.schwaerzler.maximilian.doit.util.swipeDeleteThreshold
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Clock
import kotlin.time.Instant

import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import at.co.schwaerzler.maximilian.doit.util.LucideIconCatalog
import androidx.compose.ui.platform.LocalContext

/**
 * A single row in the todo list.
 *
 * Displays the todo title, optional deadline, tag badge, locked indicator, and a completion checkbox.
 * Clean, flat design directly on the natural screen background.
 */
@Composable
fun TodoListItem(
    todo: TodoSummary,
    onStateToggle: (newState: Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tag: Tag? = null,
    isUnlocked: Boolean = false
) {
    val context = LocalContext.current
    val now = Clock.System.now()
    val isOverdue = todo.deadlineDateTime != null && todo.deadlineDateTime <= now && todo.state != TodoState.DONE

    val deadlineText = todo.deadlineDateTime?.let { deadline ->
        val localDeadline = deadline.toLocalDateTime(TimeZone.currentSystemDefault())
        val localNow = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val javaDead = localDeadline.toJavaLocalDateTime()
        val javaNow = localNow.toJavaLocalDateTime()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val timeStr = timeFormatter.format(javaDead)

        when {
            javaDead.toLocalDate() == javaNow.toLocalDate() -> {
                context.getString(R.string.deadline_today) + " " + timeStr
            }
            javaDead.toLocalDate() == javaNow.toLocalDate().plusDays(1) -> {
                context.getString(R.string.deadline_tomorrow) + " " + timeStr
            }
            else -> {
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(javaDead)
            }
        }
    }

    val deadlineColor = if (isOverdue) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val haptic = LocalHapticFeedback.current
    val currentOnDelete by rememberUpdatedState(onDelete)
    var deleteTriggered by remember(todo.id) { mutableStateOf(false) }
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        positionalThreshold = ::swipeDeleteThreshold,
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !deleteTriggered) {
                deleteTriggered = true
                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                currentOnDelete()
            }
            true
        }
    )
    val animatedDeleteIconScale by animateFloatAsState(
        targetValue = if (swipeToDismissBoxState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.25f else 1f,
        label = "deleteIconScale",
    )

    LaunchedEffect(todo.id) {
        deleteTriggered = false
        if (swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
            swipeToDismissBoxState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        modifier = modifier,
        backgroundContent = {
            if (swipeToDismissBoxState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer),
                ) {
                    Icon(
                        painterResource(R.drawable.delete_24px),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp)
                            .size(24.dp)
                            .scale(animatedDeleteIconScale),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        val rowShape = RoundedCornerShape(16.dp)
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(rowShape)
                .combinedClickable(
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClickLabel = stringResource(R.string.select_this_todo)
                )
                .padding(horizontal = 8.dp),
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todo.isPinned && todo.state != TodoState.DONE) {
                        Icon(
                            painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_pin),
                            contentDescription = stringResource(R.string.pin_todo),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(14.dp)
                        )
                    }

                    if (todo.isLocked && !isUnlocked) {
                        Icon(
                            painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_lock),
                            contentDescription = stringResource(R.string.lock_this_task),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(14.dp)
                        )
                    }

                    Text(
                        text = if (todo.isLocked && !isUnlocked) "••••••••••••" else todo.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = if (todo.state == TodoState.DONE) FontWeight.Normal else if (todo.isPinned) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (todo.state == TodoState.DONE) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (todo.state == TodoState.DONE) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (tag != null) {
                        val tagColor = tag.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                        val iconRes = LucideIconCatalog.getIconResId(context, tag.iconName)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tagColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, tagColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = tagColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = tagColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (deadlineText != null) {
                        if (isOverdue) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_clock_alert),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = deadlineText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.event_24px),
                                    contentDescription = null,
                                    tint = deadlineColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = deadlineText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = deadlineColor
                                )
                            }
                        }
                    }
                }
            },
            trailingContent = {
                RoundedCheckbox(
                    checked = todo.state == TodoState.DONE,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onStateToggle(it)
                    }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    Color.Transparent
                }
            )
        )
    }
}

private val previewCreatedAt = Instant.parse("2026-05-01T08:00:00Z")
private val previewDeadline = Instant.parse("2026-05-10T10:00:00Z")

@Preview(showBackground = true)
@Composable
private fun TodoListItemOpenPreview() {
    DoItTheme {
        Surface {
            TodoListItem(
                todo = TodoSummary(1, "Buy groceries", null, TodoState.OPEN, previewCreatedAt),
                onStateToggle = {},
                onClick = {},
                onLongClick = {},
                onDelete = {},
                selected = false
            )
        }
    }
}
