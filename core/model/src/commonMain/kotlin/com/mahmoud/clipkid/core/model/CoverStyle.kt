package com.mahmoud.clipkid.core.model

/**
 * Unified visual style of a collection cover: a background, a foreground and
 * one mark (a short glyph such as "أ ب" or an icon key such as "book").
 * Colors are ARGB, e.g. `0xFFFFE6B0`.
 */
data class CoverStyle(
    val bg: Long,
    val fg: Long,
    val mark: CoverMark,
)

sealed interface CoverMark {
    /** Short text drawn in the display font, e.g. "أ ب" or "١٢٣". */
    data class Glyph(val text: String) : CoverMark

    /** Key of a bundled icon, e.g. "book", "heart", "leaf", "moon". */
    data class Icon(val key: String) : CoverMark
}
