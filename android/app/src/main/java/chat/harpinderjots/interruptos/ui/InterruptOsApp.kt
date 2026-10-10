package chat.harpinderjots.interruptos.ui

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import chat.harpinderjots.interruptos.R
import chat.harpinderjots.interruptos.data.DecisionAction
import chat.harpinderjots.interruptos.data.DecisionFeedback
import chat.harpinderjots.interruptos.data.DecisionRecord
import chat.harpinderjots.interruptos.data.RulePreferences
import chat.harpinderjots.interruptos.ui.theme.Amber
import chat.harpinderjots.interruptos.ui.theme.AmberSoft
import chat.harpinderjots.interruptos.ui.theme.Brick
import chat.harpinderjots.interruptos.ui.theme.Graphite
import chat.harpinderjots.interruptos.ui.theme.Moss
import chat.harpinderjots.interruptos.ui.theme.MossSoft
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AppScreen { HOME, REVIEW, ACTIVITY, SETTINGS }

@Composable
fun InterruptOsApp(
    decisions: StateFlow<List<DecisionRecord>>,
    hasNotificationAccess: () -> Boolean,
    openNotificationAccess: () -> Unit,
    requestPostPermission: () -> Unit,
    clearHistory: () -> Unit,
    initiallyOpenReview: Boolean,
    submitFeedback: (String, DecisionFeedback) -> Unit,
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
        MainShell(decisions, clearHistory, initiallyOpenReview, submitFeedback)
    }
}

@Composable
private fun Onboarding(openNotificationAccess: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark()
                Text("Interrupt OS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 11.dp))
            }

            Column {
                Image(
                    painter = painterResource(R.drawable.onboarding_signal_filter),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    contentScale = ContentScale.Fit,
                )
                Text("Less noise.\nNothing important missed.", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Interrupt OS checks selected notifications against what is happening now, then stays out of your way.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrustPill(Icons.Outlined.Shield, "You stay in control", Modifier.weight(1f))
                    TrustPill(Icons.Outlined.NotificationsNone, "Quiet by default", Modifier.weight(1f))
                }
            }

            Column {
                Text(
                    "Android will ask you to allow notification access. You can turn it off at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                Button(
                    onClick = openNotificationAccess,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Moss, contentColor = Color.White),
                ) {
                    Text("Turn on Interrupt OS")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    decisionFlow: StateFlow<List<DecisionRecord>>,
    clearHistory: () -> Unit,
    initiallyOpenReview: Boolean,
    submitFeedback: (String, DecisionFeedback) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(if (initiallyOpenReview) AppScreen.REVIEW else AppScreen.HOME) }
    var selectedDecision by remember { mutableStateOf<DecisionRecord?>(null) }
    val decisions by decisionFlow.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                decisions = decisions,
                onOpenReview = { screen = AppScreen.REVIEW },
                onOpenActivity = { screen = AppScreen.ACTIVITY },
                onOpenSettings = { screen = AppScreen.SETTINGS },
                onDecision = { selectedDecision = it },
                modifier = Modifier.padding(padding),
            )
            AppScreen.REVIEW -> ReviewScreen(
                decisions = decisions.filter { it.reviewRecommended && it.feedback == null },
                onBack = { screen = AppScreen.HOME },
                onDecision = { selectedDecision = it },
                modifier = Modifier.padding(padding),
            )
            AppScreen.ACTIVITY -> ActivityScreen(
                decisions = decisions,
                clearHistory = clearHistory,
                onBack = { screen = AppScreen.HOME },
                onDecision = { selectedDecision = it },
                modifier = Modifier.padding(padding),
            )
            AppScreen.SETTINGS -> SettingsScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.padding(padding),
            )
        }
    }

    selectedDecision?.let { decision ->
        DecisionSheet(
            decision = decisions.firstOrNull { it.id == decision.id } ?: decision,
            onDismiss = { selectedDecision = null },
            onFeedback = { feedback -> submitFeedback(decision.id, feedback) },
        )
    }
}

@Composable
private fun HomeScreen(
    decisions: List<DecisionRecord>,
    onOpenReview: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onDecision: (DecisionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = decisions.filter { it.createdAt >= startOfToday() }
    val quietCount = today.count { it.action == DecisionAction.HELD || it.action == DecisionAction.REPLACED_SILENT }
    val reviewCount = decisions.count { it.reviewRecommended && it.feedback == null }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { HomeHeader(onOpenSettings) }
        item {
            AmbientStatus(
                title = if (reviewCount == 0) "Working quietly" else "A quick check would help",
                subtitle = when {
                    reviewCount > 0 -> if (reviewCount == 1) "One decision is unusually personal." else "$reviewCount decisions are unusually personal."
                    today.isEmpty() -> "Nothing needs you."
                    quietCount == 0 -> "${today.size} checked today. Nothing held back."
                    else -> "${today.size} checked today · $quietCount kept quiet"
                },
                needsReview = reviewCount > 0,
            )
        }
        if (reviewCount > 0) {
            item { ReviewInvitation(reviewCount, onOpenReview) }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (decisions.isNotEmpty()) TextButton(onClick = onOpenActivity) { Text("See all") }
            }
        }
        if (decisions.isEmpty()) {
            item { EmptyRecent() }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    decisions.take(3).forEachIndexed { index, decision ->
                        DecisionRow(decision) { onDecision(decision) }
                        if (index < minOf(2, decisions.lastIndex)) {
                            HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Interrupt OS only asks when your preference could change what it does next.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Text("Interrupt OS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 11.dp).weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AmbientStatus(title: String, subtitle: String, needsReview: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(116.dp).clip(CircleShape).background(if (needsReview) AmberSoft else MossSoft),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(68.dp).clip(CircleShape).background(if (needsReview) Amber else Moss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (needsReview) Icons.Outlined.NotificationsNone else Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun ReviewInvitation(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AmberSoft),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NotificationsNone, null, tint = Amber)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(if (count == 1) "Review one decision" else "Review $count decisions", style = MaterialTheme.typography.titleMedium)
                Text("Your answer will shape similar choices.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewScreen(
    decisions: List<DecisionRecord>,
    onBack: () -> Unit,
    onDecision: (DecisionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader("Review", onBack) }
        item {
            Text(
                if (decisions.isEmpty()) "Nothing needs your input." else "Only decisions where your preference could meaningfully change the system appear here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (decisions.isEmpty()) item { QuietState() }
        else items(decisions, key = { it.id }) { decision -> ReviewCard(decision) { onDecision(decision) } }
    }
}

@Composable
private fun ReviewCard(decision: DecisionRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(decision)
                Text(decision.sourceLabel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 9.dp).weight(1f))
                Text(formatTime(decision.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(decision.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 14.dp))
            Text(decision.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Text("Tap to answer", style = MaterialTheme.typography.labelLarge, color = Amber, modifier = Modifier.padding(top = 18.dp))
        }
    }
}

@Composable
private fun ActivityScreen(
    decisions: List<DecisionRecord>,
    clearHistory: () -> Unit,
    onBack: () -> Unit,
    onDecision: (DecisionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader("Activity", onBack) }
        if (decisions.isEmpty()) item { QuietState() }
        else {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    decisions.forEachIndexed { index, decision ->
                        DecisionRow(decision) { onDecision(decision) }
                        if (index != decisions.lastIndex) HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    }
                }
            }
            item { TextButton(onClick = clearHistory, modifier = Modifier.fillMaxWidth()) { Text("Clear activity", color = Brick) } }
        }
    }
}

@Composable
private fun DecisionRow(decision: DecisionRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(decision)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(decision.sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(formatTime(decision.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(decision.title.ifBlank { decision.summary }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            Text(decision.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun StatusDot(decision: DecisionRecord) {
    val color = when (decision.action) {
        DecisionAction.HELD, DecisionAction.REPLACED_SILENT -> Amber
        DecisionAction.REPLACED_ALERTING -> Brick
        DecisionAction.PRESERVED -> Moss
        DecisionAction.FAILED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { PageHeader("Settings", onBack) }
        item { Text("PROTECTED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp)) }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                PersistentRuleRow("commerce", "Shopping and offers", "Check claims against current results")
                HorizontalDivider(Modifier.padding(start = 18.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                PersistentRuleRow("travel", "Flights and travel", "Surface changes that require action")
            }
        }
        item { Text("SYSTEM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, null, tint = Moss)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("Notification access", style = MaterialTheme.typography.titleMedium)
                        Text("On · Tap to manage in Android", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        item {
            Text(
                "Other notification categories pass through unchanged. Interrupt OS learns only from corrections you explicitly make.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun PersistentRuleRow(domain: String, title: String, description: String) {
    val context = LocalContext.current
    var checked by remember(domain) { mutableStateOf(RulePreferences.isEnabled(context, domain)) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                RulePreferences.setEnabled(context, domain, it)
            },
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecisionSheet(
    decision: DecisionRecord,
    onDismiss: () -> Unit,
    onFeedback: (DecisionFeedback) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(decision)
                    Text(decision.sourceLabel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 9.dp).weight(1f))
                    Text(formatTime(decision.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Text(decision.title, style = MaterialTheme.typography.headlineMedium)
                Text(decision.reason, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            }
            if (decision.evidence.isNotEmpty()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text("WHAT IT CHECKED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    decision.evidence.forEach { evidence ->
                        Text(evidence.label, style = MaterialTheme.typography.labelLarge)
                        Text(evidence.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                    }
                }
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                if (decision.feedback == null) {
                    Text("Was this the right call?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = {
                                onFeedback(DecisionFeedback.CORRECT)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Yes") }
                        Button(
                            onClick = {
                                onFeedback(oppositeFeedback(decision))
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                        ) { Text(oppositeLabel(decision)) }
                    }
                } else {
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Moss)
                        Text("Preference saved", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
}

private fun oppositeFeedback(decision: DecisionRecord): DecisionFeedback = when (decision.action) {
    DecisionAction.HELD, DecisionAction.REPLACED_SILENT -> DecisionFeedback.SHOULD_INTERRUPT
    else -> DecisionFeedback.SHOULD_STAY_QUIET
}

private fun oppositeLabel(decision: DecisionRecord): String = when (decision.action) {
    DecisionAction.HELD, DecisionAction.REPLACED_SILENT -> "Should alert"
    else -> "Keep quiet"
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun EmptyRecent() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MossSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, tint = Moss)
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text("No decisions yet", style = MaterialTheme.typography.titleMedium)
            Text("There is nothing to review.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuietState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(MossSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, tint = Moss, modifier = Modifier.size(28.dp))
        }
        Text("All quiet", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun TrustPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Moss, modifier = Modifier.size(19.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun BrandMark() {
    Box(Modifier.size(32.dp).clip(CircleShape).background(Graphite), contentAlignment = Alignment.Center) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(MossSoft))
    }
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun formatTime(epochMillis: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
