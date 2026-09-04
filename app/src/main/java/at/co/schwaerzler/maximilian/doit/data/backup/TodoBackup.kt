package at.co.schwaerzler.maximilian.doit.data.backup

import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import at.co.schwaerzler.maximilian.doit.data.db.entity.Todo
import at.co.schwaerzler.maximilian.doit.data.db.entity.TodoState
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Clock
import kotlin.time.Instant

data class BackupPayload(
    val todos: List<Todo>,
    val tags: List<Tag> = emptyList()
)

object TodoBackup {
    private const val VERSION = 2

    fun encode(todos: List<Todo>, tags: List<Tag> = emptyList()): String = JSONObject()
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
                    put("isLocked", todo.isLocked)
                    if (todo.tagId != null) {
                        put("tagId", todo.tagId)
                    }
                })
            }
        })
        .put("tags", JSONArray().apply {
            tags.forEach { tag ->
                put(JSONObject().apply {
                    put("id", tag.id)
                    put("name", tag.name)
                    put("iconName", tag.iconName)
                    if (tag.colorArgb != null) {
                        put("colorArgb", tag.colorArgb)
                    }
                    put("orderIndex", tag.orderIndex)
                })
            }
        })
        .toString(2)

    fun decode(json: String): BackupPayload {
        val root = JSONObject(json)
        require(root.optString("format") == "do-it-backup") { "Not a Do It backup file" }
        val version = root.optInt("version", 1)
        require(version in 1..VERSION) { "Unsupported backup version" }
        val todosArray = root.getJSONArray("todos")
        val todos = List(todosArray.length()) { index ->
            val item = todosArray.getJSONObject(index)
            Todo(
                id = item.getInt("id"),
                title = item.getString("title"),
                description = item.optString("description").takeUnless { it == "" || it == "null" },
                deadlineDateTime = if (item.isNull("deadline")) null else Instant.fromEpochMilliseconds(item.getLong("deadline")),
                state = TodoState.valueOf(item.optString("state", TodoState.OPEN.name)),
                creationDateTime = Instant.fromEpochMilliseconds(item.getLong("created")),
                isPinned = item.optBoolean("isPinned", false),
                isLocked = item.optBoolean("isLocked", false),
                tagId = if (item.isNull("tagId")) null else item.getInt("tagId")
            )
        }
        val tags = if (root.has("tags")) {
            val tagsArray = root.getJSONArray("tags")
            List(tagsArray.length()) { index ->
                val item = tagsArray.getJSONObject(index)
                Tag(
                    id = item.getInt("id"),
                    name = item.getString("name"),
                    iconName = item.getString("iconName"),
                    colorArgb = if (item.isNull("colorArgb")) null else item.getInt("colorArgb"),
                    orderIndex = item.optInt("orderIndex", index)
                )
            }
        } else {
            emptyList()
        }
        return BackupPayload(todos = todos, tags = tags)
    }
}

