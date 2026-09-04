package at.co.schwaerzler.maximilian.doit.data.backup

import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Clock
import kotlin.time.Instant

object TodoBackup {
    private const val VERSION = 1

    fun encode(todos: List<Todo>): String = JSONObject()
        .put("format", "do-it-backup")
        .put("version", VERSION)
        .put("exportedAt", Clock.System.now().toEpochMilliseconds())
        .put("todos", JSONArray().apply {
            todos.forEach { todo ->
                put(JSONObject().apply {
                    put("id", todo.id)
                    put("title", todo.title)
                    put("description", todo.description)
                    put("deadline", todo.deadlineDateTime?.toEpochMilliseconds())
                    put("state", todo.state.name)
                    put("created", todo.creationDateTime.toEpochMilliseconds())
                    put("isPinned", todo.isPinned)
                })
            }
        }).toString(2)

    fun decode(json: String): List<Todo> {
        val root = JSONObject(json)
        require(root.optString("format") == "do-it-backup") { "Not a Do It backup file" }
        require(root.optInt("version") == VERSION) { "Unsupported backup version" }
        val todos = root.getJSONArray("todos")
        return List(todos.length()) { index ->
            val item = todos.getJSONObject(index)
            Todo(
                id = item.getInt("id"),
                title = item.getString("title"),
                description = item.optString("description").takeUnless { it == "" || it == "null" },
                deadlineDateTime = if (item.isNull("deadline")) null else Instant.fromEpochMilliseconds(item.getLong("deadline")),
                state = TodoState.valueOf(item.optString("state", TodoState.OPEN.name)),
                creationDateTime = Instant.fromEpochMilliseconds(item.getLong("created")),
                isPinned = item.optBoolean("isPinned", false)
            )
        }
    }
}
