package at.co.schwaerzler.maximilian.doit.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeDeletePolicyTest {
    @Test
    fun commitThreshold_isPredictableAndReachable() {
        assertFalse(shouldCommitSwipe(offset = 95f, distance = 300f))
        assertTrue(shouldCommitSwipe(offset = 96f, distance = 300f))
    }

    @Test
    fun threshold_scalesWithRowWidth() {
        assertTrue(shouldCommitSwipe(offset = 64f, distance = 200f))
        assertFalse(shouldCommitSwipe(offset = 63f, distance = 200f))
    }
}
