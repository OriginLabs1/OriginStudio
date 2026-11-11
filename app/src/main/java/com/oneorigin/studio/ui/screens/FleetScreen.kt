package com.oneorigin.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneorigin.studio.R
import com.oneorigin.studio.core.Folder
import com.oneorigin.studio.core.Session
import com.oneorigin.studio.shell.TerminalHost
import com.oneorigin.studio.ui.components.BottomNavSpacer
import com.oneorigin.studio.ui.components.GroupLabel
import com.oneorigin.studio.ui.components.LeadingTile
import com.oneorigin.studio.ui.components.ListRow
import com.oneorigin.studio.ui.components.RowSpacer
import com.oneorigin.studio.ui.components.SectionTitle
import com.oneorigin.studio.ui.components.StatusDot
import com.oneorigin.studio.ui.theme.OriginStudioIcons
import com.oneorigin.studio.ui.theme.Palette
import com.oneorigin.studio.ui.theme.folderTint

/**
 * Every session on the device, grouped by project.
 *
 * Projects are where work is organised; the fleet is where it is *observed*. The screen
 * answers the one question a project list cannot — what is running right now — and opens
 * any session in a tap. It is deliberately local: a session appears here because this
 * device holds it, never because something was uploaded somewhere.
 */
@Composable
fun FleetScreen(
    folders: List<Folder>,
    onOpenSession: (folderId: String, sessionId: String) -> Unit,
    onAddProject: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
) {
    // The terminal host is not observable itself; generation changes when sessions start,
    // exit or switch, which is exactly when a status dot can change.
    val generation by TerminalHost.generation.collectAsState()
    val live = remember(generation) { TerminalHost.liveSessions().associateBy { it.key } }

    val totalSessions = folders.sumOf { it.sessions.size }
    val running = live.values.count { it.running }
    val working = live.values.count { it.working }
    val edge = if (wide) 30.dp else 20.dp

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = edge, end = if (wide) 26.dp else 20.dp, top = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                SectionTitle(text = stringResource(R.string.fleet_title))
                Text(
                    text = stringResource(R.string.fleet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextFaint,
                )
            }
            com.oneorigin.studio.ui.components.PrimaryButton(
                icon = OriginStudioIcons.Plus,
                label = stringResource(R.string.project_new),
                onClick = onAddProject,
                compact = !wide,
            )
        }

        Spacer(Modifier.height(18.dp))

        if (totalSessions == 0) {
            EmptyState(
                title = stringResource(R.string.fleet_empty),
                hint = stringResource(R.string.fleet_empty_hint),
            )
            return@Column
        }

        FleetSummary(
            projects = folders.size,
            sessions = totalSessions,
            running = running,
            working = working,
            modifier = Modifier.padding(horizontal = if (wide) 30.dp else 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (wide) 30.dp else 16.dp,
                end = if (wide) 26.dp else 16.dp,
                bottom = 18.dp,
            ),
        ) {
            folders.forEach { folder ->
                if (folder.sessions.isNotEmpty()) {
                    item(key = "label-${folder.id}") {
                        GroupLabel(folder.name)
                    }
                    items(folder.sessions, key = { "${folder.id}/${it.id}" }) { session ->
                        val key = TerminalHost.sessionKey(folder.id, session.id)
                        val state = live[key]
                        FleetRow(
                            session = session,
                            folder = folder,
                            working = state?.working == true,
                            running = state?.running == true,
                            onClick = { onOpenSession(folder.id, session.id) },
                        )
                        RowSpacer()
                    }
                }
            }
            item { BottomNavSpacer() }
        }
    }
}

/** One session row: its project tint, its name, and an honest live state. */
@Composable
private fun FleetRow(
    session: Session,
    folder: Folder,
    working: Boolean,
    running: Boolean,
    onClick: () -> Unit,
) {
    val status = when {
        working -> stringResource(R.string.fleet_status_working)
        running -> stringResource(R.string.fleet_status_running)
        else -> stringResource(R.string.fleet_status_idle)
    }
    val dot = when {
        working -> Palette.Accent
        running -> Palette.Green
        else -> Palette.TextFaint
    }

    ListRow(
        title = session.name,
        leading = {
            LeadingTile(
                tint = folderTint(folder.tint),
                icon = OriginStudioIcons.Terminal,
            )
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (running) Palette.TextDim else Palette.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusDot(dot)
            }
        },
        onClick = onClick,
    )
}

/** Three numbers that describe the fleet at a glance. */
@Composable
private fun FleetSummary(
    projects: Int,
    sessions: Int,
    running: Int,
    working: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Card)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stat(value = "$projects", label = stringResource(R.string.fleet_projects))
        StatDivider()
        Stat(value = "$sessions", label = stringResource(R.string.fleet_sessions))
        StatDivider()
        Stat(
            value = if (working > 0) "$working" else "$running",
            label = if (working > 0) {
                stringResource(R.string.fleet_working)
            } else {
                stringResource(R.string.fleet_running)
            },
            accent = working > 0,
        )
    }
}

@Composable
private fun Stat(value: String, label: String, accent: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (accent) Palette.Accent else Palette.Text,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextFaint,
        )
    }
}

@Composable
private fun StatDivider() {
    Spacer(
        Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Palette.BorderSoft),
    )
}
