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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary

/**
 * Compact Quick Action & Stats Widget (2x2 / 3x2).
 */
class QuickAddWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DoItApplication
        provideContent {
            val todos by app.repository.getOpenSummaries().collectAsState(emptyList())
            GlanceTheme {
                QuickAddWidgetContent(openCount = todos.size, todos = todos)
            }
        }
    }
}

@Composable
fun QuickAddWidgetContent(openCount: Int, todos: List<TodoSummary> = emptyList()) {
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
            .padding(14.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openAppIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    ImageProvider(R.drawable.check_circle_24px),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Text(
                text = "$openCount",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = if (openCount == 1) "task to do" else "tasks to do",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (todos.isEmpty()) {
                Spacer(modifier = GlanceModifier.defaultWeight())
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
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(3.dp),
                            ) {}
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = todo.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp,
                                ),
                                modifier = GlanceModifier.defaultWeight(),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Big Quick Add Button Card
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(19.dp)
                    .clickable(actionStartActivity(newTodoIntent)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        ImageProvider(R.drawable.add_24px),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                        modifier = GlanceModifier.size(18.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "New TODO",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
fun QuickAddWidgetPreview() {
    GlanceTheme {
        QuickAddWidgetContent(openCount = 3)
    }
}
