package com.oneorigin.studio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * One deep-space navy, a steel-blue accent, and the portal aurora as status colour.
 *
 * The hues are drawn from the Sir Collins portal (ink `#05060F`, steel `#9EC1E6`,
 * teal `#43C6C9`, violet `#9A8CFF`, rose `#FF9FBC`, soft green `#9FD4A8`), retuned
 * for a phone rather than a page: darker surfaces so OLED black stays black, one
 * brighter accent so a primary action is unmistakable at arm's length, and no gold
 * or orange anywhere in the chrome.
 */
object Palette {
    // Surfaces: the page's ink, stepped just enough to separate planes.
    val Bg = Color(0xFF05070E)
    val Card = Color(0xFF0E1322)
    val CardPressed = Color(0xFF161D30)
    val NavBg = Color(0xFF0A0F1C)
    val Field = Color(0xFF101627)
    val Border = Color(0xFF232B45)
    val BorderSoft = Color(0xFF181F33)

    // The one accent. Steel blue, brightened for small-size legibility on ink.
    val Accent = Color(0xFF9EC1E6)
    val AccentStrong = Color(0xFFD8E9FF)
    val AccentDeep = Color(0xFF6A87B8)
    val AccentSoft = Color(0x249EC1E6)

    // Type.
    val Text = Color(0xFFEEF1FF)
    val TextDim = Color(0xFFA7AECB)
    val TextFaint = Color(0xFF7B83A3)
    val OnAccent = Color(0xFF05070E)

    // Semantic colour, all taken from the portal aurora.
    val Teal = Color(0xFF43C6C9)
    val Violet = Color(0xFF9A8CFF)
    val Blue = Color(0xFF7AA6D8)
    val Green = Color(0xFF9FD4A8)
    val Yellow = Color(0xFFE6BE8A) // Caution only; never brand chrome.
    val Pink = Color(0xFFFF9FBC)

    val FolderTints = listOf(Accent, Teal, Violet, Pink, Green, Blue)

    // Terminal surface.
    val TermOut = Color(0xFFD5DAF2)
}

/** Maps a stored tint index onto the palette, tolerating stale indices. */
fun folderTint(index: Int): Color =
    Palette.FolderTints[index.mod(Palette.FolderTints.size)]
