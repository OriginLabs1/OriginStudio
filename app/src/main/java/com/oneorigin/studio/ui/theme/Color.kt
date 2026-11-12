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

/**
 * Premium finishes added for the Studio surface in 0.0.10.
 *
 * These are not a second brand: Ink stays the default everywhere. They are calmer or
 * warmer grounds for the Studio and its new surfaces, each checked for contrast against
 * its own text colour. The accents are the portal aurora pushed further apart so a
 * selected environment, an environment badge and a running automation never share a hue.
 */
object Premium {
    val AuroraViolet = Color(0xFFC3B2FF)
    val AuroraTeal = Color(0xFF4FD8D9)
    val AuroraRose = Color(0xFFFFA8C8)
    val AuroraGreen = Color(0xFFA8E6C0)

    val GraphiteBg = Color(0xFF0B0C0E)
    val GraphiteCard = Color(0xFF15171B)
    val GraphiteBorder = Color(0xFF2B2F36)
    val GraphiteAccent = Color(0xFFA8C6EA)
    val GraphiteText = Color(0xFFF1F3F7)
    val GraphiteDim = Color(0xFFAEB4BF)

    val VerdantBg = Color(0xFF050B09)
    val VerdantCard = Color(0xFF0D1714)
    val VerdantBorder = Color(0xFF20362E)
    val VerdantAccent = Color(0xFF8FE3C2)
    val VerdantText = Color(0xFFECFFF7)
    val VerdantDim = Color(0xFFA3C4B5)

    val EmberBg = Color(0xFF0C0709)
    val EmberCard = Color(0xFF1A1015)
    val EmberBorder = Color(0xFF3A2430)
    val EmberAccent = Color(0xFFFFB4C8)
    val EmberText = Color(0xFFFFF1F4)
    val EmberDim = Color(0xFFC9A9B6)

    /** A finish: the ground, the surface, the line and the one accent. */
    data class Finish(
        val id: String,
        val name: String,
        val note: String,
        val bg: Color,
        val card: Color,
        val border: Color,
        val accent: Color,
        val text: Color,
        val dim: Color,
    )

    val Finishes = listOf(
        Finish("ink", "Ink", "The original deep-space navy.", Palette.Bg, Palette.Card, Palette.Border, Palette.Accent, Palette.Text, Palette.TextDim),
        Finish("aurora", "Aurora", "Violet leads, teal answers.", Color(0xFF070511), Color(0xFF140F24), Color(0xFF2C2347), AuroraViolet, Color(0xFFF2EDFF), Color(0xFFB4A9D6)),
        Finish("graphite", "Graphite", "Neutral, for daylight.", GraphiteBg, GraphiteCard, GraphiteBorder, GraphiteAccent, GraphiteText, GraphiteDim),
        Finish("verdant", "Verdant", "Green-black, calm all day.", VerdantBg, VerdantCard, VerdantBorder, VerdantAccent, VerdantText, VerdantDim),
        Finish("ember", "Ember", "Rose-copper for the evening.", EmberBg, EmberCard, EmberBorder, EmberAccent, EmberText, EmberDim),
    )

    fun finish(id: String): Finish = Finishes.firstOrNull { it.id == id } ?: Finishes.first()
}
