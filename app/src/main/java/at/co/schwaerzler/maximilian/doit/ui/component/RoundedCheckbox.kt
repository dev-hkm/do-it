/*
 * Copyright 2026 Maximilian SchwÃ¤rzler
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A compact, rounded checkbox with the same dynamic colors as the rest of the app. */
@Composable
fun RoundedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(7.dp)
    val containerColor by animateColorAsState(
        targetValue = when {
            checked && enabled -> MaterialTheme.colorScheme.primary
            checked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        label = "roundedCheckboxContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            enabled -> if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        label = "roundedCheckboxBorder"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(23.dp)
                .clip(shape)
                .background(containerColor)
                .border(2.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}
