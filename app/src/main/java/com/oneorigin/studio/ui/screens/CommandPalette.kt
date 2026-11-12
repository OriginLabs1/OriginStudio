package com.oneorigin.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oneorigin.studio.ui.theme.Mono
import com.oneorigin.studio.ui.theme.OriginStudioIcons
import com.oneorigin.studio.ui.theme.Palette

/**
 * The physical-keyboard shortcut for Jump Anywhere. The activity writes here and the root
 * composition consumes it, which keeps the key handling out of every screen.
 */
object JumpShortcut {
    var requested by mutableStateOf(false)
        private set

    fun request() { requested = true }
    fun consume() { requested = false }
}

/** One place the app can go, from any other place. */
data class JumpEntry(
    val title: String,
    val subtitle: String,
    val kind: String,
    val target: String,
    val keywords: String = "",
)

/** Same ranking idea as the Rust core: exact, prefix, word prefix, contains, keywords. */
private fun score(entry: JumpEntry, query: String): Int {
    if (query.isBlank()) return 1
    val q = query.trim().lowercase()
    val title = entry.title.lowercase()
    val subtitle = entry.subtitle.lowercase()
    val keywords = entry.keywords.lowercase()
    return when {
        title == q -> 1000
        title.startsWith(q) -> 900
        title.split(' ', '-', '/', '_').any { it.startsWith(q) } -> 800
        title.contains(q) -> 600
        keywords == q -> 550
        subtitle.contains(q) -> 400
        keywords.contains(q) -> 300
        else -> 0
    }
}

/**
 * Jump Anywhere: a single field over workspaces, sessions, repos, environments, agents and
 * app actions. Opens from the Studio card or with Ctrl/Cmd+K on a keyboard.
 */
@Composable
fun JumpPalette(
    entries: List<JumpEntry>,
    onPick: (JumpEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var highlighted by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }

    val results = remember(query, entries) {
        entries
            .map { it to score(it, query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(40)
    }
    if (highlighted >= results.size) highlighted = 0

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 70.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.Card)
                .border(1.dp, Palette.Border, RoundedCornerShape(22.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(OriginStudioIcons.Search, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        highlighted = 0
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Palette.Text),
                    cursorBrush = SolidColor(Palette.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { results.getOrNull(highlighted)?.let(onPick) }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> {
                                    if (results.isNotEmpty()) highlighted = (highlighted + 1) % results.size
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (results.isNotEmpty()) highlighted = (highlighted - 1 + results.size) % results.size
                                    true
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    results.getOrNull(highlighted)?.let(onPick)
                                    true
                                }
                                Key.Escape -> {
                                    onDismiss(); true
                                }
                                else -> false
                            }
                        },
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.oneorigin.studio.R.string.jump_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Palette.TextFaint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                ) {
                    Icon(OriginStudioIcons.Close, null, tint = Palette.TextFaint, modifier = Modifier.size(16.dp))
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))

            if (results.isEmpty()) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.oneorigin.studio.R.string.jump_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp).padding(vertical = 6.dp),
                ) {
                    itemsIndexed(results) { index, entry ->
                        val active = index == highlighted
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Palette.CardPressed else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { onPick(entry) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = kindMark(entry.kind),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono, fontSize = 10.sp),
                                color = if (active) Palette.Accent else Palette.TextFaint,
                                modifier = Modifier.width(54.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Palette.Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (entry.subtitle.isNotBlank()) {
                                    Text(
                                        text = entry.subtitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Palette.TextFaint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun kindMark(kind: String): String = when (kind) {
    "workspace" -> "WORK"
    "session" -> "CHAT"
    "repo" -> "REPO"
    "file" -> "FILE"
    "host" -> "HOST"
    "agent" -> "AGENT"
    "environment" -> "ENV"
    else -> "GO"
}
