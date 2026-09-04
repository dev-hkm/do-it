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

package at.co.schwaerzler.maximilian.doit.util

import androidx.annotation.StringRes
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoSummary
import kotlin.time.Instant

enum class SortOrder(@param:StringRes val labelRes: Int) {
    DEADLINE_SOONEST_FIRST(R.string.sort_label_deadline_soonest_first),
    ALPHABETICAL_A_Z(R.string.sort_label_alphabetical_a_z),
    ALPHABETICAL_Z_A(R.string.sort_label_alphabetical_z_a),
    CREATION_DATE_NEWEST_FIRST(R.string.sort_label_creation_date_newest_first),
    CREATION_DATE_OLDEST_FIRST(R.string.sort_label_creation_date_oldest_first);

    companion object {
        fun parsePreference(value: String?): SortOrder =
            value?.let { storedValue ->
                runCatching { valueOf(storedValue) }.getOrNull()
            } ?: DEADLINE_SOONEST_FIRST
    }
}

fun SortOrder.sortTodos(todos: List<TodoSummary>): List<TodoSummary> {
    val primaryComparator = compareByDescending<TodoSummary> { it.isPinned }
    val secondaryComparator = when (this) {
        SortOrder.DEADLINE_SOONEST_FIRST -> compareBy<TodoSummary, Instant?>(nullsLast()) { it.deadlineDateTime }
        SortOrder.ALPHABETICAL_A_Z -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        SortOrder.ALPHABETICAL_Z_A -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
        SortOrder.CREATION_DATE_NEWEST_FIRST -> compareByDescending { it.creationDateTime }
        SortOrder.CREATION_DATE_OLDEST_FIRST -> compareBy { it.creationDateTime }
    }
    return todos.sortedWith(primaryComparator.then(secondaryComparator))
}
