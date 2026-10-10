package chat.harpinderjots.interruptos.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import chat.harpinderjots.interruptos.data.DecisionFeedback
import chat.harpinderjots.interruptos.data.FeedbackManager

class FeedbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val decisionId = intent.getStringExtra(EXTRA_DECISION_ID) ?: return
        val feedback = intent.getStringExtra(EXTRA_FEEDBACK)?.let {
            runCatching { DecisionFeedback.valueOf(it) }.getOrNull()
        } ?: return
        FeedbackManager.submit(context.applicationContext, decisionId, feedback)
    }

    companion object {
        const val EXTRA_DECISION_ID = "decision_id"
        const val EXTRA_FEEDBACK = "feedback"
    }
}
