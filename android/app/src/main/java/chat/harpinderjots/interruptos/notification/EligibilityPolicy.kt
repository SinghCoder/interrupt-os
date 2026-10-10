package chat.harpinderjots.interruptos.notification

import android.app.Notification
import android.service.notification.StatusBarNotification

object EligibilityPolicy {
    private val protectedCategories = setOf(
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_SYSTEM,
    )

    fun shouldEvaluate(sbn: StatusBarNotification, ownPackage: String): Boolean {
        if (sbn.packageName == ownPackage) return false
        if (sbn.isOngoing || !sbn.isClearable) return false
        if (sbn.notification.category in protectedCategories) return false
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        return title.isNotBlank() || text.isNotBlank()
    }
}
