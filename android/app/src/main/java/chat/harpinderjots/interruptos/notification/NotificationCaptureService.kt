package chat.harpinderjots.interruptos.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import chat.harpinderjots.interruptos.data.DecisionAction
import chat.harpinderjots.interruptos.data.DecisionRecord
import chat.harpinderjots.interruptos.data.DecisionStore
import chat.harpinderjots.interruptos.data.RulePreferences
import chat.harpinderjots.interruptos.network.DecisionClient
import chat.harpinderjots.interruptos.network.NotificationEnvelope
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.UUID

class NotificationCaptureService : NotificationListenerService() {
    private val executor = Executors.newFixedThreadPool(2)
    private val client by lazy { DecisionClient(applicationContext) }
    private val inFlight = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        DecisionStore.initialize(applicationContext)
        createChannels()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!EligibilityPolicy.shouldEvaluate(sbn, packageName)) return
        synchronized(inFlight) {
            if (!inFlight.add(sbn.key)) return
        }

        val original = sbn.notification
        val extras = original.extras
        val envelope = NotificationEnvelope(
            sourcePackage = sbn.packageName,
            sourceLabel = sourceLabel(sbn.packageName),
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            category = original.category,
            postedAt = sbn.postTime,
        )
        cancelNotification(sbn.key)

        executor.submit {
            try {
                val decision = client.evaluate(envelope)
                val effectiveDecision = if (RulePreferences.isEnabled(this, decision.domain)) {
                    decision
                } else {
                    decision.copy(
                        action = DecisionAction.PRESERVED,
                        reason = "Observed without changes because this rule is turned off.",
                    )
                }
                DecisionStore.add(effectiveDecision)
                when (effectiveDecision.action) {
                    DecisionAction.HELD,
                    DecisionAction.REPLACED_SILENT -> ReplacementNotifier.refreshReviewRecap(this)
                    DecisionAction.REPLACED_ALERTING -> ReplacementNotifier.notify(this, effectiveDecision, original.contentIntent)
                    DecisionAction.PRESERVED -> ReplacementNotifier.deliverOriginal(
                        this,
                        effectiveDecision,
                        original.contentIntent,
                    )
                    else -> Unit
                }
            } catch (_: Exception) {
                val failed = DecisionRecord(
                    id = UUID.randomUUID().toString(),
                    domain = "other",
                    sourcePackage = envelope.sourcePackage,
                    sourceLabel = envelope.sourceLabel,
                    title = envelope.title,
                    body = envelope.body,
                    action = DecisionAction.FAILED,
                    reason = "Delivered unchanged because verification could not be completed.",
                    summary = "Delivered unchanged because verification was unavailable.",
                    confidence = 0.0,
                    createdAt = System.currentTimeMillis(),
                    latencyMs = System.currentTimeMillis() - envelope.postedAt,
                    evidence = emptyList(),
                )
                DecisionStore.add(failed)
                ReplacementNotifier.deliverOriginal(
                    this,
                    failed,
                    original.contentIntent,
                )
            } finally {
                synchronized(inFlight) { inFlight.remove(sbn.key) }
            }
        }
    }

    private fun sourceLabel(sourcePackage: String): String = runCatching {
        val info = packageManager.getApplicationInfo(sourcePackage, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(sourcePackage.substringAfterLast('.'))

    private fun createChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(
            listOf(
                NotificationChannel("interruptions", "Important decisions", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("quiet_decisions", "Quiet decisions", NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }

    override fun onDestroy() {
        executor.shutdown()
        executor.awaitTermination(500, TimeUnit.MILLISECONDS)
        super.onDestroy()
    }
}
