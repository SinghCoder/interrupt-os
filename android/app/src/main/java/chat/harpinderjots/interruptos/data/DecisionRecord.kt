package chat.harpinderjots.interruptos.data

enum class DecisionAction {
    PRESERVED,
    HELD,
    REPLACED_SILENT,
    REPLACED_ALERTING,
    FAILED,
}

enum class DecisionFeedback {
    CORRECT,
    SHOULD_INTERRUPT,
    SHOULD_STAY_QUIET,
}

data class EvidenceItem(
    val label: String,
    val detail: String,
    val source: String? = null,
)

data class DecisionRecord(
    val id: String,
    val domain: String,
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val body: String,
    val action: DecisionAction,
    val reason: String,
    val summary: String,
    val confidence: Double,
    val createdAt: Long,
    val latencyMs: Long,
    val evidence: List<EvidenceItem>,
    val reviewRecommended: Boolean = false,
    val feedback: DecisionFeedback? = null,
    val feedbackSynced: Boolean = false,
)
