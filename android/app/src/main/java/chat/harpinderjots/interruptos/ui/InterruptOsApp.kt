package chat.harpinderjots.interruptos.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import chat.harpinderjots.interruptos.R
import chat.harpinderjots.interruptos.data.DecisionAction
import chat.harpinderjots.interruptos.data.DecisionRecord
import chat.harpinderjots.interruptos.data.RulePreferences
import chat.harpinderjots.interruptos.ui.theme.Amber
import chat.harpinderjots.interruptos.ui.theme.AmberSoft
import chat.harpinderjots.interruptos.ui.theme.Brick
import chat.harpinderjots.interruptos.ui.theme.Graphite
import chat.harpinderjots.interruptos.ui.theme.Moss
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppSection { TODAY, ACTIVITY, RULES }

@Composable
fun InterruptOsApp(
    decisions: StateFlow<List<DecisionRecord>>,
    hasNotificationAccess: () -> Boolean,
    openNotificationAccess: () -> Unit,
    requestPostPermission: () -> Unit,
    clearHistory: () -> Unit,
) {
    var accessGranted by remember { mutableStateOf(hasNotificationAccess()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accessGranted = hasNotificationAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!accessGranted) {
        Onboarding(openNotificationAccess)
    } else {
        LaunchedEffect(Unit) { requestPostPermission() }
        MainShell(decisions, clearHistory)
    }
}

@Composable
private fun Onboarding(openNotificationAccess: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark()
                    Text("Interrupt OS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
            }
            item {
                Image(
                    painter = painterResource(R.drawable.onboarding_signal_filter),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            item {
                Text("Keep the interruptions that matter.", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Interrupt OS checks attention-grabbing claims against current reality, then leaves the rest quiet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PromiseRow(Icons.Outlined.CheckCircle, "Important alerts still get through")
                    PromiseRow(Icons.Outlined.AutoAwesome, "Live evidence, not generic summaries")
                    PromiseRow(Icons.Outlined.Shield, "You choose which kinds of alerts are protected")
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Notification access", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Required to evaluate notifications and replace only the ones covered by your rules. Eligible content is securely processed by the Interrupt OS service.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = openNotificationAccess,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Graphite, contentColor = Color.White),
                ) {
                    Text("Turn on protection")
                }
            }
        }
    }
}

@Composable
private fun MainShell(decisionFlow: StateFlow<List<DecisionRecord>>, clearHistory: () -> Unit) {
    var section by remember { mutableStateOf(AppSection.TODAY) }
    val decisions by decisionFlow.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = section == AppSection.TODAY,
                    onClick = { section = AppSection.TODAY },
                    icon = { Icon(Icons.Outlined.Home, null) },
                    label = { Text("Today") },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = section == AppSection.ACTIVITY,
                    onClick = { section = AppSection.ACTIVITY },
                    icon = { Icon(Icons.Outlined.History, null) },
                    label = { Text("Activity") },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = section == AppSection.RULES,
                    onClick = { section = AppSection.RULES },
                    icon = { Icon(Icons.Outlined.Tune, null) },
                    label = { Text("Rules") },
                    colors = navColors(),
                )
            }
        },
    ) { padding ->
        when (section) {
            AppSection.TODAY -> TodayScreen(decisions, Modifier.padding(padding))
            AppSection.ACTIVITY -> ActivityScreen(decisions, clearHistory, Modifier.padding(padding))
            AppSection.RULES -> RulesScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun TodayScreen(decisions: List<DecisionRecord>, modifier: Modifier = Modifier) {
    val held = decisions.count { it.action == DecisionAction.HELD || it.action == DecisionAction.REPLACED_SILENT }
    val delivered = decisions.count { it.action == DecisionAction.REPLACED_ALERTING }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your attention, protected.", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 5.dp))
                }
                ProtectionPulse()
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(held.toString(), "held back", AmberSoft, Modifier.weight(1f))
                MetricCard(delivered.toString(), "important", Color(0xFFDCE8DF), Modifier.weight(1f))
            }
        }
        item {
            StatusCard()
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent decisions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${decisions.size} checked", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (decisions.isEmpty()) {
            item { EmptyActivity() }
        } else {
            items(decisions.take(4), key = { it.id }) { DecisionCard(it) }
        }
    }
}

@Composable
private fun ActivityScreen(decisions: List<DecisionRecord>, clearHistory: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Activity", style = MaterialTheme.typography.displayMedium)
            Text(
                "A clear record of what was changed—and why.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )
        }
        if (decisions.isEmpty()) item { EmptyActivity() }
        items(decisions, key = { it.id }) { DecisionCard(it, expanded = true) }
        if (decisions.isNotEmpty()) {
            item {
                Text(
                    "Clear activity",
                    style = MaterialTheme.typography.labelLarge,
                    color = Brick,
                    modifier = Modifier.clickable(onClick = clearHistory).padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun RulesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Rules", style = MaterialTheme.typography.displayMedium)
            Text(
                "Decide where Interrupt OS may step in.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )
        }
        item { PersistentRuleRow("commerce", "Shopping and offers", "Verify relevance against current evidence") }
        item { PersistentRuleRow("travel", "Flights and travel", "Escalate changes with real consequences") }
        item { RuleRow("Other notifications", "Observed without changing delivery", false, null) }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp)) {
                    Text("System access", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Review notification access or revoke protection at any time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                    )
                    Text(
                        "Open Android settings",
                        style = MaterialTheme.typography.labelLarge,
                        color = Amber,
                        modifier = Modifier.clickable { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionCard(decision: DecisionRecord, expanded: Boolean = false) {
    val (label, color) = when (decision.action) {
        DecisionAction.HELD -> "HELD BACK" to Amber
        DecisionAction.REPLACED_SILENT -> "QUIET" to Amber
        DecisionAction.REPLACED_ALERTING -> "ACTION NEEDED" to Brick
        DecisionAction.PRESERVED -> "DELIVERED" to Moss
        DecisionAction.FAILED -> "UNCHANGED" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(start = 8.dp).weight(1f))
                Text(formatTime(decision.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(decision.sourceLabel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            Text(decision.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(decision.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            if (expanded && decision.evidence.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                decision.evidence.forEach { item ->
                    Text(item.label, style = MaterialTheme.typography.labelMedium)
                    Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                }
                Text("${(decision.confidence * 100).toInt()}% confidence · ${decision.latencyMs / 1000.0}s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatusCard() {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Graphite)) {
        Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = Color.White)
            }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text("Protection is active", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Important categories always pass through.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.68f), modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun EmptyActivity() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CheckCircle, null, tint = Moss, modifier = Modifier.size(30.dp))
            Text("Everything is quiet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Text("New decisions will appear here with their evidence.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(20.dp)) {
            Text(value, style = MaterialTheme.typography.displayMedium, color = Graphite)
            Text(label, style = MaterialTheme.typography.labelLarge, color = Graphite.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun PersistentRuleRow(domain: String, title: String, description: String) {
    val context = LocalContext.current
    var checked by remember(domain) { mutableStateOf(RulePreferences.isEnabled(context, domain)) }
    RuleRow(title, description, checked) {
        checked = it
        RulePreferences.setEnabled(context, domain, it)
    }
}

@Composable
private fun RuleRow(title: String, description: String, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

@Composable
private fun PromiseRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(AmberSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Graphite, modifier = Modifier.size(19.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 13.dp))
    }
}

@Composable
private fun BrandMark() {
    Box(Modifier.size(34.dp).clip(CircleShape).background(Graphite), contentAlignment = Alignment.Center) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Amber))
    }
}

@Composable
private fun ProtectionPulse() {
    Box(Modifier.size(52.dp).clip(CircleShape).background(AmberSoft), contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Amber))
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Graphite,
    selectedTextColor = Graphite,
    indicatorColor = AmberSoft,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun formatTime(epochMillis: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
