package chat.harpinderjots.fixture.market

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.random.Random

class FixtureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")?.takeIf { it.isNotBlank() } ?: return
        val body = intent.getStringExtra("body")?.takeIf { it.isNotBlank() } ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("offers", "Offers", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = Notification.Builder(context, "offers")
            .setSmallIcon(R.drawable.ic_store)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_PROMO)
            .setAutoCancel(true)
            .build()
        manager.notify(Random.nextInt(), notification)
    }
}
