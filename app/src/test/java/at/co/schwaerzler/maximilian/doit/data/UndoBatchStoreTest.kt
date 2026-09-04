package at.co.schwaerzler.maximilian.doit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoBatchStoreTest {
    @Test
    fun batches_areIndependentAndCanOnlyBeConsumedOnce() {
        val store = UndoBatchStore<String>()

        val first = store.add(listOf("first"))
        val second = store.add(listOf("second", "third"))

        assertEquals(1, first.count)
        assertEquals(2, second.count)
        assertEquals(listOf("first"), store.take(first.token))
        assertEquals(emptyList<String>(), store.take(first.token))
        assertEquals(listOf("second", "third"), store.take(second.token))
    }

    @Test
    fun discard_removesOnlyTheRequestedBatch() {
        val store = UndoBatchStore<String>()
        val first = store.add(listOf("first"))
        val second = store.add(listOf("second"))

        store.discard(first.token)

        assertTrue(store.take(first.token).isEmpty())
        assertEquals(listOf("second"), store.take(second.token))
    }
}
