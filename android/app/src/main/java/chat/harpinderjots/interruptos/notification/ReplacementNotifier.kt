package chat.harpinderjots.interruptos.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import chat.harpinderjots.interruptos.MainActivity
import chat.harpinderjots.interruptos.R
import chat.harpinderjots.interruptos.data.DecisionAction
import chat.harpinderjots.interruptos.data.DecisionRecord
import chat.harpinderjots.interruptos.data.DecisionFeedback
import chat.harpinderjots.interruptos.data.DecisionStore

object ReplacementNotifier {
    fun notify(context: Context, decision: DecisionRecord, contentIntent: PendingIntent?) {
        val alerting = decision.action == DecisionAction.REPLACED_ALERTING
        val channel = if (alerting) "interruptions" else "quiet_decisions"
        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (alerting) "Action needed · ${decision.sourceLabel}" else "Checked · ${decision.sourceLabel}")
            .setContentText(decision.summary)
            .setStyle(Notification.BigTextStyle().bigText(decision.reason))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setOnlyAlertOnce(true)
        contentIntent?.let(builder::setContentIntent)
        builder.addAction(feedbackAction(
            context,
            decision,
            if (alerting) DecisionFeedback.SHOULD_STAY_QUIET else DecisionFeedback.SHOULD_INTERRUPT,
            if (alerting) "Keep quiet next time" else "Should alert",
        ))
        context.getSystemService(NotificationManager::class.java)
            .notify(decision.id.hashCode(), builder.build())
    }

    fun deliverOriginal(
        context: Context,
        decision: DecisionRecord,
        contentIntent: PendingIntent?,
    ) {
        val builder = Notification.Builder(context, "interruptions")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (decision.title.isBlank()) decision.sourceLabel else decision.title)
            .setContentText(decision.body)
            .setStyle(Notification.BigTextStyle().bigText(decision.body))
            .setSubText(decision.sourceLabel)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        contentIntent?.let(builder::setContentIntent)
        if (decision.domain == "commerce" || decision.domain == "travel") {
            builder.addAction(feedbackAction(context, decision, DecisionFeedback.SHOULD_STAY_QUIET, "Keep quiet next time"))
        }
        context.getSystemService(NotificationManager::class.java)
            .notify(decision.id.hashCode(), builder.build())
    }

    fun refreshReviewRecap(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val pending = DecisionStore.snapshot().filter { it.reviewRecommended && it.feedback == null }
        if (pending.isEmpty()) {
            manager.cancel(REVIEW_NOTIFICATION_ID)
            return
        }
        val launchIntent = Intent(context, MainActivity::class.java).putExtra("open_review", true)
        val launch = PendingIntent.getActivity(
            context,
            REVIEW_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val count = pending.size
        val builder = Notification.Builder(context, "quiet_decisions")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (count == 1) "One decision could use your input" else "$count decisions could use your input")
            .setContentText(pending.first().summary)
            .setStyle(Notification.InboxStyle().also { style -> pending.take(4).forEach { style.addLine(it.summary) } })
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
        manager.notify(REVIEW_NOTIFICATION_ID, builder.build())
    }

    fun dismissDecision(context: Context, decisionId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(decisionId.hashCode())
    }

    private fun feedbackAction(
        context: Context,
        decision: DecisionRecord,
        feedback: DecisionFeedback,
        label: String,
    ): Notification.Action {
        val intent = Intent(context, FeedbackReceiver::class.java)
            .putExtra(FeedbackReceiver.EXTRA_DECISION_ID, decision.id)
            .putExtra(FeedbackReceiver.EXTRA_FEEDBACK, feedback.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (decision.id + feedback.name).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(R.drawable.ic_notification, label, pendingIntent).build()
    }

    private const val REVIEW_NOTIFICATION_ID = 0x49524F53
}
