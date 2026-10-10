package chat.harpinderjots.interruptos.data

import android.content.Context

object RulePreferences {
    private const val PREFS = "interrupt_os_rules"

    fun isEnabled(context: Context, domain: String): Boolean = when (domain) {
        "commerce" -> preferences(context).getBoolean("commerce", true)
        "travel" -> preferences(context).getBoolean("travel", true)
        else -> false
    }

    fun setEnabled(context: Context, domain: String, enabled: Boolean) {
        preferences(context).edit().putBoolean(domain, enabled).apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
