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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.co.schwaerzler.maximilian.doit.R

/**
 * Ultra-fast, lightweight, crash-safe Markdown renderer for Jetpack Compose.
 * Supports headers, bold, italic, strikethrough, inline code, code blocks,
 * blockquotes, task lists, and bullet lists.
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    if (markdown.isBlank()) {
        Text(
            text = "(No description)",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val (style, topSpace) = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp) to 8.dp
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp) to 6.dp
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp) to 4.dp
                    }
                    Spacer(Modifier.height(topSpace))
                    Text(
                        text = parseInlineMarkdown(block.content, textColor),
                        style = style,
                        color = textColor
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.content, textColor),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        color = textColor
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = (block.indent * 12).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 10.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = parseInlineMarkdown(block.content, textColor),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.TaskItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = (block.indent * 12).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (block.checked) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = if (!block.checked) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(18.dp)
                        ) {
                            if (block.checked) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = parseInlineMarkdown(block.content, textColor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                textDecoration = if (block.checked) TextDecoration.LineThrough else null
                            ),
                            color = if (block.checked) MaterialTheme.colorScheme.onSurfaceVariant else textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(block.content, MaterialTheme.colorScheme.onSurfaceVariant),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Header(val level: Int, val content: String) : MarkdownBlock
    data class Paragraph(val content: String) : MarkdownBlock
    data class BulletItem(val content: String, val indent: Int = 0) : MarkdownBlock
    data class TaskItem(val content: String, val checked: Boolean, val indent: Int = 0) : MarkdownBlock
    data class Quote(val content: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock
    object Divider : MarkdownBlock
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val lines = text.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var inCodeBlock = false
    val codeBuffer = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeBuffer.append(line).append("\n")
            continue
        }

        if (trimmed.isEmpty()) {
            continue
        }

        when {
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                blocks.add(MarkdownBlock.Divider)
            }

            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length
                val content = trimmed.drop(level).trim()
                blocks.add(MarkdownBlock.Header(level.coerceIn(1, 6), content))
            }

            trimmed.startsWith("> ") || trimmed == ">" -> {
                blocks.add(MarkdownBlock.Quote(trimmed.removePrefix(">").trim()))
            }

            trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                blocks.add(MarkdownBlock.TaskItem(trimmed.substring(6).trim(), checked = false))
            }

            trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
            trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                blocks.add(MarkdownBlock.TaskItem(trimmed.substring(6).trim(), checked = true))
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                val indent = (line.length - line.trimStart().length) / 2
                blocks.add(MarkdownBlock.BulletItem(trimmed.substring(2).trim(), indent))
            }

            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val content = trimmed.substringAfter(". ").trim()
                blocks.add(MarkdownBlock.BulletItem(content))
            }

            else -> {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
            }
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd()))
    }

    return blocks
}

/**
 * Parses inline markdown styles (**bold**, *italic*, ~~strikethrough~~, `code`).
 */
fun parseInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Bold (**text** or __text__)
            if ((text.startsWith("**", i) || text.startsWith("__", i)) && i + 2 < len) {
                val marker = text.substring(i, i + 2)
                val end = text.indexOf(marker, i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Strikethrough (~~text~~)
            if (text.startsWith("~~", i) && i + 2 < len) {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = defaultColor.copy(alpha = 0.7f)))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Inline code (`code`)
            if (text[i] == '`' && i + 1 < len) {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = defaultColor.copy(alpha = 0.1f),
                            color = defaultColor
                        )
                    )
                    append(" ${text.substring(i + 1, end)} ")
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Italic (*text* or _text_)
            if ((text[i] == '*' || text[i] == '_') && i + 1 < len) {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end != -1 && end > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}
