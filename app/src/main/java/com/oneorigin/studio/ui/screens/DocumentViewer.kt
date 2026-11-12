package com.oneorigin.studio.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneorigin.studio.R
import com.oneorigin.studio.ui.components.BareIconButton
import com.oneorigin.studio.ui.theme.Mono
import com.oneorigin.studio.ui.theme.OriginStudioIcons
import com.oneorigin.studio.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A real PDF reader: pages rendered by Android's own PDF engine, no download, no web view.
 * One page at a time, which is what a phone is good at.
 */
@Composable
fun PdfViewer(file: File, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var page by remember(file) { mutableIntStateOf(0) }
    var count by remember(file) { mutableIntStateOf(0) }
    var failed by remember(file) { mutableStateOf(false) }

    val bitmap by produceState<Bitmap?>(initialValue = null, file, page) {
        val rendered = withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (renderer.pageCount == 0) return@use null to 0
                        val safe = page.coerceIn(0, renderer.pageCount - 1)
                        val bitmap = renderer.openPage(safe).use { current ->
                            val targetWidth = 1600
                            val scale = targetWidth.toFloat() / current.width.coerceAtLeast(1)
                            val height = (current.height * scale).toInt().coerceAtLeast(1)
                            val output = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                            output.eraseColor(AndroidColor.WHITE)
                            current.render(output, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            output
                        }
                        bitmap to renderer.pageCount
                    }
                }
            }.getOrNull() ?: (null to 0)
        }
        count = rendered.second
        if (rendered.second > 0 && page >= rendered.second) page = rendered.second - 1
        failed = rendered.first == null
        value = rendered.first
    }

    Column(modifier.fillMaxSize().background(Palette.Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BareIconButton(OriginStudioIcons.Back, stringResource(R.string.action_close), onClose, tint = Palette.TextDim, size = 38)
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall, color = Palette.Text, maxLines = 1)
                Text(
                    text = if (count > 0) stringResource(R.string.preview_pdf_page, page + 1, count) else file.absolutePath,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = Mono),
                    color = Palette.TextFaint,
                    maxLines = 1,
                )
            }
            BareIconButton(
                OriginStudioIcons.ChevronUp,
                stringResource(R.string.preview_pdf_previous),
                { if (page > 0) page-- },
                tint = if (page > 0) Palette.Text else Palette.TextFaint.copy(alpha = 0.35f),
                size = 38,
            )
            BareIconButton(
                OriginStudioIcons.ChevronDown,
                stringResource(R.string.preview_pdf_next),
                { if (page + 1 < count) page++ },
                tint = if (page + 1 < count) Palette.Text else Palette.TextFaint.copy(alpha = 0.35f),
                size = 38,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
            when {
                failed -> Text(
                    text = stringResource(R.string.preview_pdf_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextDim,
                )
                bitmap == null -> CircularProgressIndicator(color = Palette.Accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                else -> Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.preview_pdf_page, page + 1, count),
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String, val ordered: Boolean, val number: Int) : MarkdownBlock
    data class Code(val text: String, val language: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

private fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()
    val code = StringBuilder()
    var inCode = false
    var language = ""

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks.add(MarkdownBlock.Paragraph(paragraph.toString().trim()))
        paragraph.clear()
    }

    source.lineSequence().forEach { raw ->
        val line = raw.trimEnd()
        if (line.trimStart().startsWith("```")) {
            if (inCode) {
                blocks.add(MarkdownBlock.Code(code.toString().trimEnd(), language))
                code.clear()
                language = ""
            } else {
                flushParagraph()
                language = line.trim().removePrefix("```").trim()
            }
            inCode = !inCode
            return@forEach
        }
        if (inCode) {
            code.appendLine(line)
            return@forEach
        }
        val trimmed = line.trimStart()
        when {
            trimmed.isBlank() -> flushParagraph()
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length
                if (level in 1..6) {
                    flushParagraph()
                    blocks.add(MarkdownBlock.Heading(level, trimmed.drop(level).trim().trimEnd('#')))
                }
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Quote(trimmed.removePrefix("> ")))
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Rule)
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Bullet(trimmed.drop(2), ordered = false, number = 0))
            }
            Regex("^\\d+[.)]\\s+").containsMatchIn(trimmed) -> {
                flushParagraph()
                val number = trimmed.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                blocks.add(MarkdownBlock.Bullet(trimmed.substringAfter(' ').trim(), ordered = true, number = number))
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
    }
    flushParagraph()
    if (inCode && code.isNotEmpty()) blocks.add(MarkdownBlock.Code(code.toString(), language))
    return blocks
}

/** Inline Markdown: bold, italic, inline code and links, without pretending to be a browser. */
private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val rest = text.substring(index)
        when {
            rest.startsWith("**") -> {
                val end = text.indexOf("**", index + 2)
                if (end > index + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(index + 2, end)) }
                    index = end + 2
                } else {
                    append("**"); index += 2
                }
            }
            rest.startsWith("`") -> {
                val end = text.indexOf('`', index + 1)
                if (end > index + 1) {
                    withStyle(SpanStyle(fontFamily = Mono, color = Palette.Teal, background = Palette.Bg)) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append('`'); index++
                }
            }
            rest.startsWith("[") -> {
                val close = text.indexOf(']', index + 1)
                val open = if (close > 0) text.indexOf('(', close) else -1
                val end = if (open > 0) text.indexOf(')', open) else -1
                if (close > 0 && open == close + 1 && end > open) {
                    withStyle(SpanStyle(color = Palette.Accent, textDecoration = TextDecoration.Underline)) {
                        append(text.substring(index + 1, close))
                    }
                    index = end + 1
                } else {
                    append('['); index++
                }
            }
            rest.startsWith("_") || rest.startsWith("*") -> {
                val marker = rest.first()
                val end = text.indexOf(marker, index + 1)
                if (end > index + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(index + 1, end)) }
                    index = end + 1
                } else {
                    append(marker); index++
                }
            }
            rest.startsWith("~~") -> {
                val end = text.indexOf("~~", index + 2)
                if (end > index + 2) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(text.substring(index + 2, end)) }
                    index = end + 2
                } else {
                    append("~~"); index += 2
                }
            }
            else -> {
                append(text[index]); index++
            }
        }
    }
}

/**
 * Markdown rendered in place, with an outline of its headings. No network, no scripts, and
 * the source stays one tap away in the editor.
 */
@Composable
fun MarkdownViewer(file: File, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val source by produceState(initialValue = "", file) {
        value = withContext(Dispatchers.IO) {
            runCatching { file.readText().take(2 * 1024 * 1024) }.getOrDefault("")
        }
    }
    val blocks = remember(source) { parseMarkdown(source) }
    val headings = remember(blocks) { blocks.filterIsInstance<MarkdownBlock.Heading>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var outlineOpen by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(Palette.Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BareIconButton(OriginStudioIcons.Back, stringResource(R.string.action_close), onClose, tint = Palette.TextDim, size = 38)
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall, color = Palette.Text, maxLines = 1)
                Text(
                    text = stringResource(R.string.preview_markdown) + " · " + file.parentFile?.name.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Palette.TextFaint,
                )
            }
            if (headings.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.preview_outline),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = Palette.Accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { outlineOpen = !outlineOpen }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        if (outlineOpen && headings.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                headings.forEach { heading ->
                    val target = blocks.indexOf(heading)
                    Text(
                        text = heading.text.take(34),
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.TextDim,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Palette.Card)
                            .border(1.dp, Palette.Border, RoundedCornerShape(999.dp))
                            .clickable {
                                outlineOpen = false
                                scope.launch { listState.scrollToItem(target.coerceAtLeast(0)) }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(blocks) { block ->
                when (block) {
                    is MarkdownBlock.Heading -> Text(
                        text = inlineMarkdown(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            3 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        color = Palette.Text,
                        modifier = Modifier.padding(top = if (block.level == 1) 4.dp else 8.dp),
                    )
                    is MarkdownBlock.Paragraph -> Text(
                        text = inlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextDim,
                    )
                    is MarkdownBlock.Bullet -> Row {
                        Text(
                            text = if (block.ordered) "${block.number}." else "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.Accent,
                            modifier = Modifier.width(22.dp),
                        )
                        Text(
                            text = inlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextDim,
                        )
                    }
                    is MarkdownBlock.Code -> Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Mono, fontSize = 12.sp),
                        color = Palette.TermOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Palette.Card)
                            .border(1.dp, Palette.BorderSoft, RoundedCornerShape(10.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                    )
                    is MarkdownBlock.Quote -> Row(Modifier.padding(vertical = 2.dp)) {
                        Box(Modifier.width(3.dp).height(20.dp).background(Palette.Accent.copy(alpha = 0.6f)))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = inlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = Palette.TextFaint,
                        )
                    }
                    MarkdownBlock.Rule -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(vertical = 4.dp)
                            .background(Palette.Border),
                    )
                }
            }
        }
    }
}
