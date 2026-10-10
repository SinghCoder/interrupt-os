package chat.harpinderjots.interruptos.network

import android.content.Context
import chat.harpinderjots.interruptos.BuildConfig
import chat.harpinderjots.interruptos.data.DecisionAction
import chat.harpinderjots.interruptos.data.DecisionRecord
import chat.harpinderjots.interruptos.data.EvidenceItem
import chat.harpinderjots.interruptos.security.SecureValueStore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class NotificationEnvelope(
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val body: String,
    val category: String?,
    val postedAt: Long,
)

class DecisionClient(
    private val context: Context,
    private val baseUrl: String = BuildConfig.DECISION_API_BASE_URL,
) {
    fun evaluate(envelope: NotificationEnvelope): DecisionRecord {
        val started = System.currentTimeMillis()
        val connection = URL("${baseUrl.trimEnd('/')}/v1/evaluate").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 2_500
        connection.readTimeout = 28_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${installationToken()}")
        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(JSONObject().apply {
                put("eventId", UUID.randomUUID().toString())
                put("sourcePackage", envelope.sourcePackage)
                put("sourceLabel", envelope.sourceLabel)
                put("title", envelope.title)
                put("body", envelope.body)
                put("category", envelope.category)
                put("postedAt", envelope.postedAt)
                put("locale", "en-IN")
                put("currency", "INR")
            }.toString())
        }

        val responseCode = connection.responseCode
        val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
            .bufferedReader().use { it.readText() }
        if (responseCode !in 200..299) error("Decision API returned $responseCode")

        val json = JSONObject(responseText)
        val evidenceJson = json.optJSONArray("evidence")
        val evidence = if (evidenceJson == null) emptyList() else List(evidenceJson.length()) { index ->
            evidenceJson.getJSONObject(index).let {
                EvidenceItem(
                    label = it.getString("label"),
                    detail = it.getString("detail"),
                    source = it.optString("source").ifBlank { null },
                )
            }
        }

        return DecisionRecord(
            id = json.getString("decisionId"),
            domain = json.optString("domain", "other"),
            sourcePackage = envelope.sourcePackage,
            sourceLabel = envelope.sourceLabel,
            title = envelope.title,
            body = envelope.body,
            action = when (json.getString("action")) {
                "hold" -> DecisionAction.HELD
                "replace_silent" -> DecisionAction.REPLACED_SILENT
                "replace_alerting" -> DecisionAction.REPLACED_ALERTING
                else -> DecisionAction.PRESERVED
            },
            reason = json.getString("reason"),
            confidence = json.optDouble("confidence", 0.0),
            createdAt = System.currentTimeMillis(),
            latencyMs = json.optLong("latencyMs", System.currentTimeMillis() - started),
            evidence = evidence,
        )
    }

    private fun installationToken(): String {
        SecureValueStore.get(context, "interrupt_os_installation", "token")?.let { return it }
        val connection = URL("${baseUrl.trimEnd('/')}/v1/installations").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 2_500
        connection.readTimeout = 4_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write("{}".toByteArray()) }
        if (connection.responseCode !in 200..299) error("Installation registration failed")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        return json.getString("token").also { token ->
            SecureValueStore.put(context, "interrupt_os_installation", "token", token)
        }
    }
}
