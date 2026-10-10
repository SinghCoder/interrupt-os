package chat.harpinderjots.interruptos.data

import android.annotation.SuppressLint
import android.content.Context
import chat.harpinderjots.interruptos.security.SecureValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("StaticFieldLeak") // This is always the process-wide application context.
object DecisionStore {
    private const val PREFS = "interrupt_os_decisions"
    private const val KEY = "history"
    private lateinit var context: Context
    private val mutableDecisions = MutableStateFlow<List<DecisionRecord>>(emptyList())
    val decisions: StateFlow<List<DecisionRecord>> = mutableDecisions.asStateFlow()

    fun initialize(applicationContext: Context) {
        if (::context.isInitialized) return
        context = applicationContext
        mutableDecisions.value = read()
    }

    @Synchronized
    fun add(record: DecisionRecord) {
        if (!::context.isInitialized) return
        val updated = (listOf(record) + mutableDecisions.value.filterNot { it.id == record.id }).take(100)
        mutableDecisions.value = updated
        write(updated)
    }

    fun clear() {
        if (!::context.isInitialized) return
        mutableDecisions.value = emptyList()
        SecureValueStore.remove(context, PREFS, KEY)
    }

    private fun read(): List<DecisionRecord> = runCatching {
        val raw = SecureValueStore.get(context, PREFS, KEY) ?: "[]"
        val array = JSONArray(raw)
        List(array.length()) { index -> array.getJSONObject(index).toRecord() }
    }.getOrDefault(emptyList())

    private fun write(records: List<DecisionRecord>) {
        val array = JSONArray(records.map { it.toJson() })
        SecureValueStore.put(context, PREFS, KEY, array.toString())
    }

    private fun DecisionRecord.toJson() = JSONObject().apply {
        put("id", id)
        put("domain", domain)
        put("sourcePackage", sourcePackage)
        put("sourceLabel", sourceLabel)
        put("title", title)
        put("body", body)
        put("action", action.name)
        put("reason", reason)
        put("confidence", confidence)
        put("createdAt", createdAt)
        put("latencyMs", latencyMs)
        put("evidence", JSONArray(evidence.map { item ->
            JSONObject().apply {
                put("label", item.label)
                put("detail", item.detail)
                put("source", item.source)
            }
        }))
    }

    private fun JSONObject.toRecord(): DecisionRecord {
        val evidenceArray = optJSONArray("evidence") ?: JSONArray()
        return DecisionRecord(
            id = getString("id"),
            domain = optString("domain", "other"),
            sourcePackage = getString("sourcePackage"),
            sourceLabel = getString("sourceLabel"),
            title = getString("title"),
            body = getString("body"),
            action = DecisionAction.valueOf(getString("action")),
            reason = getString("reason"),
            confidence = getDouble("confidence"),
            createdAt = getLong("createdAt"),
            latencyMs = getLong("latencyMs"),
            evidence = List(evidenceArray.length()) { index ->
                evidenceArray.getJSONObject(index).let {
                    EvidenceItem(it.getString("label"), it.getString("detail"), it.optString("source").ifBlank { null })
                }
            },
        )
    }
}
