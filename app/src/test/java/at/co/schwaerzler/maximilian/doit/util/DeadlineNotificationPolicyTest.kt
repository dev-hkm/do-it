package at.co.schwaerzler.maximilian.doit.util

import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlineNotificationPolicyTest {
    private val now = Instant.fromEpochSeconds(1_000)

    @Test
    fun pastDeadline_isNotScheduled() {
        assertFalse(
            shouldScheduleDeadlineNotification(
                todo = todo(deadline = Instant.fromEpochSeconds(900)),
                now = now,
                leadTime = 30.minutes
            )
        )
    }

    @Test
    fun completedTodo_isNotScheduled() {
        assertFalse(
            shouldScheduleDeadlineNotification(
                todo = todo(deadline = Instant.fromEpochSeconds(5_000), state = TodoState.DONE),
                now = now,
                leadTime = 30.minutes
            )
        )
    }

    @Test
    fun futureOpenTodo_isScheduled() {
        assertTrue(
            shouldScheduleDeadlineNotification(
                todo = todo(deadline = Instant.fromEpochSeconds(5_000)),
                now = now,
                leadTime = 30.minutes
            )
        )
    }

    private fun todo(deadline: Instant, state: TodoState = TodoState.OPEN) = Todo(
        id = 1,
        title = "Test task",
        description = null,
        deadlineDateTime = deadline,
        state = state,
        creationDateTime = now
    )
}
