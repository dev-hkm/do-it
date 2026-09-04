package at.co.schwaerzler.maximilian.doit.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {
    @Test
    fun parsePreference_invalidValue_fallsBackToDeadlineOrder() {
        assertEquals(
            SortOrder.DEADLINE_SOONEST_FIRST,
            SortOrder.parsePreference("removed-or-corrupt-value")
        )
    }
}
