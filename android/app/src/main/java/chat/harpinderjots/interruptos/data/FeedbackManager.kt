package chat.harpinderjots.interruptos.data

import android.content.Context
import chat.harpinderjots.interruptos.network.DecisionClient
import chat.harpinderjots.interruptos.notification.ReplacementNotifier
import java.util.concurrent.Executors

object FeedbackManager {
    private val executor = Executors.newSingleThreadExecutor()

    fun submit(context: Context, decisionId: String, feedback: DecisionFeedback) {
        DecisionStore.initialize(context.applicationContext)
        val decision = DecisionStore.recordFeedback(decisionId, feedback) ?: return
        ReplacementNotifier.dismissDecision(context, decisionId)
        ReplacementNotifier.refreshReviewRecap(context)
        executor.submit {
            runCatching { DecisionClient(context.applicationContext).submitFeedback(decision, feedback) }
                .onSuccess { DecisionStore.markFeedbackSynced(decisionId) }
        }
    }

    fun syncPending(context: Context) {
        DecisionStore.initialize(context.applicationContext)
        DecisionStore.snapshot()
            .filter { it.feedback != null && !it.feedbackSynced }
            .forEach { decision ->
                executor.submit {
                    runCatching { DecisionClient(context.applicationContext).submitFeedback(decision, decision.feedback!!) }
                        .onSuccess { DecisionStore.markFeedbackSynced(decision.id) }
                }
            }
    }
}
