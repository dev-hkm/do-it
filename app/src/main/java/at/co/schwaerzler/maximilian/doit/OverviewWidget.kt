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

package at.co.schwaerzler.maximilian.doit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary
import at.co.schwaerzler.maximilian.doit.util.sortTodos
import kotlin.time.Clock

/**
 * Task list widget with balanced header, quick-add action, and clean typography.
 */
class OverviewWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DoItApplication
        provideContent {
            val todos by app.repository.getOpenSummaries().collectAsState(emptyList())
            val sortOrder = app.appPreferences.homeScreenSortOrder.value
            val sorted = sortOrder.sortTodos(todos)
            GlanceTheme {
                OverviewWidgetContent(sorted)
            }
        }
    }
}

@Composable
fun OverviewWidgetContent(todos: List<TodoSummary>) {
    val context = LocalContext.current
    val newTodoIntent = remember(context) {
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("doit://todo?fromWidget=true")
        ).apply {
            component = ComponentName(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    val openAppIntent = remember(context) {
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Symmetrical, balanced header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable(actionStartActivity(openAppIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    ImageProvider(R.drawable.check_circle_24px),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Centered, well-padded "+" Add button
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity(newTodoIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        ImageProvider(R.drawable.add_24px),
                        contentDescription = context.getString(R.string.add_new_todo_app_bar),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            if (todos.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(openAppIntent)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        ImageProvider(R.drawable.check_circle_24px),
                        modifier = GlanceModifier.size(32.dp).padding(bottom = 6.dp),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                    )
                    Text(
                        text = context.getString(R.string.nothing_to_do_empty_text),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                ) {
                    items(todos, itemId = { it.id.toLong() }) { todo ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(openAppIntent))
                                .padding(vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(3.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = todo.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 14.sp
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun OverviewWidgetPreview() {
    GlanceTheme {
        OverviewWidgetContent(
            todos = listOf(
                TodoSummary(1, "Buy milk", null, TodoState.OPEN, Clock.System.now()),
                TodoSummary(2, "Write code", null, TodoState.OPEN, Clock.System.now())
            )
        )
    }
}
