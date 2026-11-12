package com.oneorigin.studio.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneorigin.studio.R
import com.oneorigin.studio.acp.AcpAgent
import com.oneorigin.studio.acp.AcpRegistry
import com.oneorigin.studio.studio.Cadence
import com.oneorigin.studio.studio.StudioAccount
import com.oneorigin.studio.studio.StudioAutomation
import com.oneorigin.studio.studio.StudioStore
import com.oneorigin.studio.studio.scheduleFrom
import com.oneorigin.studio.ui.components.BrandMark
import com.oneorigin.studio.ui.components.GroupLabel
import com.oneorigin.studio.ui.theme.Mono
import com.oneorigin.studio.ui.theme.OriginStudioIcons
import com.oneorigin.studio.ui.theme.Palette
import com.oneorigin.studio.ui.theme.Premium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class EnvironmentChoice(
    val id: String,
    val name: String,
    val version: String,
    val caption: String,
    val summary: String,
    val storage: String,
    val bestFor: List<String>,
    val limits: List<String>,
    val hosted: Boolean,
)

private val environments = listOf(
    EnvironmentChoice(
        id = "debian",
        name = "Debian 13",
        version = "Trixie, latest point release",
        caption = "Recommended on phones",
        summary = "A full Debian userland inside Origin Studio. Works offline, needs no root and no account.",
        storage = "About 400 MB once; tools you add use more.",
        bestFor = listOf("Working on a phone or tablet without a laptop", "Offline code, edits, builds and agent sessions"),
        limits = listOf("PRoot is not a virtual machine: no systemd or kernel modules", "Android may stop long background runs; keep them on screen"),
        hosted = false,
    ),
    EnvironmentChoice(
        id = "dakota",
        name = "Dakota OS",
        version = "Bluefin Dakota, latest stream",
        caption = "Most Recommended on laptops and hosts",
        summary = "Bluefin's atomic, GNOME OS-based Dakota image, paired as a host you control. Same workspace, same approvals.",
        storage = "Lives on the host; the phone stays a thin client.",
        bestFor = listOf("Long builds, emulators and desktop-class tooling", "Atomic updates and rollback on a machine you own"),
        limits = listOf("Not unpacked into PRoot on the phone; it runs on a paired host", "Dakota is alpha software from the Bluefin community"),
        hosted = true,
    ),
)

private data class AgentRow(val name: String, val tagline: String, val badge: String)

private val agentRows = listOf(
    AgentRow("Origin Agent", "The built-in profile: your workspace, your chosen agent, one approvals sheet.", "Built in"),
    AgentRow("Claude Code", "Anthropic's terminal agent, with your files and browser beside it.", "ACP"),
    AgentRow("Codex", "OpenAI's coding agent, using the login already in the guest.", "ACP"),
    AgentRow("OpenCode", "Open, provider-agnostic, with a large model menu.", "ACP"),
    AgentRow("Cursor", "For people already living in that workflow.", "ACP"),
    AgentRow("Gemini CLI", "Google's terminal agent, with a generous free tier.", "ACP"),
    AgentRow("Devin", "Cognition's agent, for work you hand off.", "ACP"),
    AgentRow("Pi", "Small, fast, good on a phone.", "ACP"),
    AgentRow("omp", "Standalone ACP build, no Node dependency.", "ACP"),
    AgentRow("Any CLI agent", "A shell session running whichever agent you already trust.", "Shell"),
)

/**
 * Studio: the one screen that explains what this workspace is, lets the user choose the
 * environment and the premium ground, switches the default agent without signing out,
 * schedules real shell commands, and shows what actually differs from other tools.
 */
@Composable
fun StudioScreen(
    onJump: () -> Unit,
    onRunAutomation: (StudioAutomation) -> Unit,
    wide: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by StudioStore.state.collectAsState()
    val finish = Premium.finish(state.finish)
    var agents by remember { mutableStateOf<List<AcpAgent>>(emptyList()) }
    var running by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        StudioStore.init(context)
        agents = runCatching { AcpRegistry.all(context) }.getOrDefault(emptyList())
    }

    // Scheduling itself lives in the app root, so a command still runs when the Studio tab is
    // not the one on screen. Manual runs from here navigate to the session they create.
    val edge = if (wide) 30.dp else 16.dp
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = edge, end = edge, top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StudioHero(
                finish = finish,
                environmentName = environments.firstOrNull { it.id == state.environment }?.name ?: "Debian 13",
                onJump = onJump,
            )
        }

        item {
            GroupLabel(stringResource(R.string.studio_environments))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                environments.forEach { environment ->
                    EnvironmentCard(
                        environment = environment,
                        selected = state.environment == environment.id,
                        accent = finish.accent,
                        onClick = { StudioStore.selectEnvironment(environment.id) },
                    )
                }
            }
        }

        item {
            GroupLabel(stringResource(R.string.studio_why))
            WhyCard(finish.accent)
        }

        item {
            GroupLabel(stringResource(R.string.studio_agents))
            AgentListCard(
                agents = agents,
                activeId = state.activeAgentId,
                onSelect = { StudioStore.selectAgent(it) },
            )
        }

        item {
            GroupLabel(stringResource(R.string.studio_usage))
            UsageCard(
                used = state.usedToday,
                budget = state.dailyBudget,
                accent = finish.accent,
                onBudget = { StudioStore.setDailyBudget(it) },
            )
        }

        item {
            GroupLabel(stringResource(R.string.studio_automations))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.automations.forEach { automation ->
                    AutomationCard(
                        automation = automation,
                        running = running == automation.id,
                        accent = finish.accent,
                        onToggle = { StudioStore.setAutomationEnabled(automation.id, it) },
                        onRun = {
                            onRunAutomation(automation)
                            StudioStore.markRun(automation.id)
                        },
                    )
                }
            }
        }

        item {
            GroupLabel(stringResource(R.string.studio_lab))
            LabCards()
        }

        item {
            GroupLabel(stringResource(R.string.studio_finishes))
            FinishPicker(selected = state.finish, onSelect = { StudioStore.selectFinish(it) })
        }

        item {
            Text(
                text = stringResource(R.string.studio_core_footer),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono, fontSize = 11.sp),
                color = Palette.TextFaint,
                modifier = Modifier.padding(top = 4.dp, start = 6.dp),
            )
        }
    }
}

@Composable
private fun StudioHero(
    finish: Premium.Finish,
    environmentName: String,
    onJump: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(finish.card, finish.bg.copy(alpha = 0.92f), finish.card),
                ),
            )
            .border(1.dp, finish.border, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.studio_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = finish.text,
                )
                Text(
                    text = stringResource(R.string.studio_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = finish.dim,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroPill(environmentName, finish)
            HeroPill(stringResource(R.string.studio_local_and_host), finish)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(finish.accent.copy(alpha = 0.14f))
                .border(1.dp, finish.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(onClick = onJump)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(OriginStudioIcons.Command, null, tint = finish.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.studio_jump_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = finish.text,
                )
                Text(
                    text = stringResource(R.string.studio_jump_body),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = finish.dim,
                )
            }
            Text(
                text = "Ctrl/⌘ K",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono, fontSize = 11.sp),
                color = finish.accent,
            )
        }
    }
}

@Composable
private fun HeroPill(text: String, finish: Premium.Finish) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = finish.text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(finish.accent.copy(alpha = 0.16f))
            .border(1.dp, finish.accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun EnvironmentCard(
    environment: EnvironmentChoice,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(selected) }
    val border by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.7f) else Palette.Border,
        animationSpec = tween(160),
        label = "environmentBorder",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Palette.CardPressed else Palette.Card)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable {
                onClick()
                expanded = true
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) accent.copy(alpha = 0.2f) else Palette.Field),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (environment.hosted) OriginStudioIcons.Layers else OriginStudioIcons.Chip,
                    contentDescription = null,
                    tint = if (selected) accent else Palette.TextDim,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(environment.name, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                Text(environment.version, style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
            }
            Text(
                text = environment.caption,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                color = if (selected) Palette.OnAccent else Palette.Text,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) accent else Palette.Field)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(environment.summary, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        Spacer(Modifier.height(6.dp))
        Text(environment.storage, style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                environment.bestFor.forEach { line -> Bullet(line, accent) }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.studio_honest_limits),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextFaint,
                )
                environment.limits.forEach { line -> Bullet(line, Palette.TextFaint, marker = "·") }
            }
        }
        if (!expanded) {
            Text(
                text = stringResource(R.string.studio_show_details),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { expanded = true }
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun Bullet(text: String, tint: Color, marker: String = "→") {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            text = marker,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.width(16.dp),
        )
        Text(text, style = MaterialTheme.typography.labelSmall, color = Palette.TextDim)
    }
}

@Composable
private fun WhyCard(accent: Color) {
    val points = listOf(
        "Rust core, one contract" to "Environments, Jump, previews, usage, automations and the agent catalog are specified once and answered by a small Rust core and CLI, with the app mirroring the same answers.",
        "The room, not another agent" to "Origin Agent pairs your workspace tools with the CLI agents you already use. One approvals sheet, no second subscription.",
        "A phone that is not a compromise" to "Dictation, drag-and-drop from other apps, PDF and Markdown previews, and a browser agents can drive, all one-handed.",
        "Two honest environments" to "Debian runs on the phone, offline. Dakota runs on a host you own. The app says which is which.",
        "No account wall" to "Sessions, files and terminals work without signing in. Agent logins stay with the agents.",
        "Visible automation" to "Scheduled commands run as readable sessions, with the same approvals as a typed command.",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        points.forEachIndexed { index, point ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(point.first, style = MaterialTheme.typography.labelLarge, color = Palette.Text)
                    Text(point.second, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
                }
            }
        }
    }
}

@Composable
private fun AgentListCard(
    agents: List<AcpAgent>,
    activeId: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.studio_agents_body),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
        Spacer(Modifier.height(10.dp))
        AgentRowView(
            name = "Origin Agent",
            tagline = "Your workspace, your chosen agent, one approvals sheet.",
            badge = "Built in",
            selected = activeId == "origin",
            onClick = { onSelect("origin") },
        )
        if (agents.isNotEmpty()) {
            agents.forEach { agent ->
                AgentRowView(
                    name = agent.name,
                    tagline = agent.description.ifBlank { agent.repository },
                    badge = if (agent.id in setOf("omp")) "No Node" else "ACP",
                    selected = activeId == agent.id,
                    onClick = { onSelect(agent.id) },
                )
            }
        } else {
            Text(
                text = stringResource(R.string.studio_agents_offline),
                style = MaterialTheme.typography.labelSmall,
                color = Palette.TextFaint,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.studio_any_cli),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextFaint,
        )
    }
}

@Composable
private fun AgentRowView(
    name: String,
    tagline: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Palette.CardPressed else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) Palette.Accent else Palette.Border),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = Palette.Text, maxLines = 1)
            Text(
                tagline,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Palette.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) Palette.Accent else Palette.TextFaint,
        )
    }
}

@Composable
private fun UsageCard(used: Int, budget: Int, accent: Color, onBudget: (Int) -> Unit) {
    val fraction = if (budget <= 0) 0f else (used.toFloat() / budget).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (budget > 0) stringResource(R.string.studio_usage_today, used, budget) else stringResource(R.string.studio_usage_unlimited, used),
                    style = MaterialTheme.typography.titleSmall,
                    color = Palette.Text,
                )
                Text(
                    text = stringResource(R.string.studio_usage_reset),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextFaint,
                )
            }
            BudgetButton("−", enabled = budget > 10) { onBudget((budget - 10).coerceAtLeast(10)) }
            Text(
                text = if (budget > 0) "$budget" else "∞",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = Mono),
                color = Palette.Text,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            BudgetButton("+", enabled = budget < 500) { onBudget(budget + 10) }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Palette.Field),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (fraction >= 0.9f) Palette.Pink else accent),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.studio_usage_note),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextFaint,
        )
    }
}

@Composable
private fun BudgetButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Palette.Field)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Palette.TextDim else Palette.TextFaint.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun AutomationCard(
    automation: StudioAutomation,
    running: Boolean,
    accent: Color,
    onToggle: (Boolean) -> Unit,
    onRun: () -> Unit,
) {
    val next = remember(automation.nextRunMs) {
        if (!automation.enabled || automation.nextRunMs <= 0L) null
        else DateUtils.getRelativeTimeSpanString(
            automation.nextRunMs,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(automation.name, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                Text(
                    text = describe(automation),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextFaint,
                )
            }
            Toggle(checked = automation.enabled, accent = accent, onChecked = onToggle)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$ ${automation.command}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Mono, fontSize = 12.sp),
            color = Palette.TermOut,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.Bg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    running -> stringResource(R.string.studio_automation_running)
                    next != null -> stringResource(R.string.studio_automation_next, next)
                    automation.enabled -> stringResource(R.string.studio_automation_waiting)
                    else -> stringResource(R.string.studio_automation_paused)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (running) accent else Palette.TextFaint,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.studio_run_now),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRun)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Toggle(checked: Boolean, accent: Color, onChecked: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) accent.copy(alpha = 0.85f) else Palette.Field)
            .clickable { onChecked(!checked) }
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(if (checked) Palette.OnAccent else Palette.TextFaint),
        )
    }
}

private fun describe(automation: StudioAutomation): String {
    val time = String.format(Locale.getDefault(), "%02d:%02d", automation.hour, automation.minute)
    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    return when (automation.cadence) {
        Cadence.INTERVAL -> "Every ${automation.everyMinutes} minutes"
        Cadence.DAILY -> "Every day at $time"
        Cadence.WEEKLY -> "Every ${days.getOrElse(automation.weekday - 1) { "Monday" }} at $time"
        Cadence.MANUAL -> "Only when you press Run"
    }
}

@Composable
private fun LabCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FeatureCard(
            icon = OriginStudioIcons.DevicePhone,
            title = stringResource(R.string.studio_lab_device_title),
            body = stringResource(R.string.studio_lab_device_body),
            modifier = Modifier.weight(1f),
        )
        FeatureCard(
            icon = OriginStudioIcons.Globe,
            title = stringResource(R.string.studio_lab_computer_title),
            body = stringResource(R.string.studio_lab_computer_body),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Icon(icon, null, tint = Palette.Teal, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.labelSmall, color = Palette.TextDim)
    }
}

@Composable
private fun FinishPicker(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.studio_finishes_body),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Premium.Finishes.forEach { finish ->
                val active = finish.id == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) finish.accent.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (active) finish.accent else Palette.Border,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(finish.id) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(finish.accent),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = finish.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (active) Palette.Text else Palette.TextFaint,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = Premium.finish(selected).note,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextFaint,
        )
    }
}
