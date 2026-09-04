package at.co.schwaerzler.maximilian.doit.data

data class UndoBatch(val token: Long, val count: Int)

/**
 * Stores independent delete batches so a second delete cannot overwrite the first undo action.
 * Callers serialize access when the store is shared across coroutines.
 */
internal class UndoBatchStore<T> {
    private var nextToken = 0L
    private val batches = linkedMapOf<Long, List<T>>()

    fun add(items: List<T>): UndoBatch {
        require(items.isNotEmpty()) { "An undo batch must contain at least one item" }
        val token = ++nextToken
        batches[token] = items
        return UndoBatch(token = token, count = items.size)
    }

    fun take(token: Long): List<T> = batches.remove(token).orEmpty()

    fun discard(token: Long) {
        batches.remove(token)
    }
}
