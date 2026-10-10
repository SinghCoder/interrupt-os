package chat.harpinderjots.interruptos.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import chat.harpinderjots.interruptos.R
import chat.harpinderjots.interruptos.data.DecisionAction
import chat.harpinderjots.interruptos.data.DecisionRecord

object ReplacementNotifier {
    fun notify(context: Context, decision: DecisionRecord, contentIntent: PendingIntent?) {
        val alerting = decision.action == DecisionAction.REPLACED_ALERTING
        val channel = if (alerting) "interruptions" else "quiet_decisions"
        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (alerting) "Action needed · ${decision.sourceLabel}" else "Checked · ${decision.sourceLabel}")
            .setContentText(decision.reason)
            .setStyle(Notification.BigTextStyle().bigText(decision.reason))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setOnlyAlertOnce(true)
        contentIntent?.let(builder::setContentIntent)
        context.getSystemService(NotificationManager::class.java)
            .notify(decision.id.hashCode(), builder.build())
    }

    fun deliverOriginal(
        context: Context,
        sourceLabel: String,
        title: String,
        body: String,
        contentIntent: PendingIntent?,
    ) {
        val builder = Notification.Builder(context, "interruptions")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (title.isBlank()) sourceLabel else title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setSubText(sourceLabel)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        contentIntent?.let(builder::setContentIntent)
        context.getSystemService(NotificationManager::class.java)
            .notify("restored:$sourceLabel:$title:$body".hashCode(), builder.build())
    }
}
