package chat.harpinderjots.interruptos

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import chat.harpinderjots.interruptos.data.DecisionStore
import chat.harpinderjots.interruptos.data.FeedbackManager
import chat.harpinderjots.interruptos.notification.NotificationCaptureService
import chat.harpinderjots.interruptos.ui.InterruptOsApp
import chat.harpinderjots.interruptos.ui.theme.InterruptOsTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DecisionStore.initialize(applicationContext)
        FeedbackManager.syncPending(applicationContext)

        setContent {
            InterruptOsTheme {
                InterruptOsApp(
                    decisions = DecisionStore.decisions,
                    hasNotificationAccess = ::hasNotificationAccess,
                    openNotificationAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    requestPostPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    clearHistory = DecisionStore::clear,
                    initiallyOpenReview = intent.getBooleanExtra("open_review", false),
                    submitFeedback = { decisionId, feedback ->
                        FeedbackManager.submit(applicationContext, decisionId, feedback)
                    },
                )
            }
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        val component = ComponentName(this, NotificationCaptureService::class.java)
        return manager.isNotificationListenerAccessGranted(component)
    }
}
