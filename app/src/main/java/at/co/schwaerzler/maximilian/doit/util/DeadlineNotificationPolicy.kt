package at.co.schwaerzler.maximilian.doit.util

import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import kotlin.time.Duration
import kotlin.time.Instant

internal fun shouldScheduleDeadlineNotification(
    todo: Todo,
    now: Instant,
    leadTime: Duration
): Boolean {
    val deadline = todo.deadlineDateTime ?: return false
    if (todo.state == TodoState.DONE) return false
    return !(deadline - now - leadTime).isNegative()
}
