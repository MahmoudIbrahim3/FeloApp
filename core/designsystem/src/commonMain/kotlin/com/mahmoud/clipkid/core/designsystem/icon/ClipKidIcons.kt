package com.mahmoud.clipkid.core.designsystem.icon

import androidx.compose.runtime.Immutable
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ic_book
import com.mahmoud.clipkid.core.designsystem.resources.ic_chart_column
import com.mahmoud.clipkid.core.designsystem.resources.ic_check
import com.mahmoud.clipkid.core.designsystem.resources.ic_chevron_left
import com.mahmoud.clipkid.core.designsystem.resources.ic_chevron_right
import com.mahmoud.clipkid.core.designsystem.resources.ic_delete
import com.mahmoud.clipkid.core.designsystem.resources.ic_heart
import com.mahmoud.clipkid.core.designsystem.resources.ic_heart_filled
import com.mahmoud.clipkid.core.designsystem.resources.ic_house
import com.mahmoud.clipkid.core.designsystem.resources.ic_image
import com.mahmoud.clipkid.core.designsystem.resources.ic_info
import com.mahmoud.clipkid.core.designsystem.resources.ic_leaf
import com.mahmoud.clipkid.core.designsystem.resources.ic_lock
import com.mahmoud.clipkid.core.designsystem.resources.ic_moon
import com.mahmoud.clipkid.core.designsystem.resources.ic_play
import com.mahmoud.clipkid.core.designsystem.resources.ic_play_filled
import com.mahmoud.clipkid.core.designsystem.resources.ic_plus
import com.mahmoud.clipkid.core.designsystem.resources.ic_rotate_ccw
import com.mahmoud.clipkid.core.designsystem.resources.ic_star
import com.mahmoud.clipkid.core.designsystem.resources.ic_timer
import com.mahmoud.clipkid.core.designsystem.resources.ic_x
import org.jetbrains.compose.resources.DrawableResource

/**
 * A bundled vector icon. [autoMirror] icons are drawn for LTR and flipped in RTL
 * (direction-dependent glyphs like back / forward chevrons).
 */
@Immutable
data class ClipKidIcon(val resource: DrawableResource, val autoMirror: Boolean = false)

/** Icons from the Lucide set (ISC license, see core/designsystem/licenses). */
object ClipKidIcons {
    /** Points to the start edge: right in RTL. */
    val Back = ClipKidIcon(Res.drawable.ic_chevron_left, autoMirror = true)
    /** Points to the end edge: left in RTL ("see all"). */
    val Forward = ClipKidIcon(Res.drawable.ic_chevron_right, autoMirror = true)
    val Close = ClipKidIcon(Res.drawable.ic_x)
    val Lock = ClipKidIcon(Res.drawable.ic_lock)
    val Home = ClipKidIcon(Res.drawable.ic_house)
    val Heart = ClipKidIcon(Res.drawable.ic_heart)
    val HeartFilled = ClipKidIcon(Res.drawable.ic_heart_filled)
    /** Media icons are not mirrored in RTL. */
    val Play = ClipKidIcon(Res.drawable.ic_play)
    val PlayFilled = ClipKidIcon(Res.drawable.ic_play_filled)
    val Replay = ClipKidIcon(Res.drawable.ic_rotate_ccw)
    val Plus = ClipKidIcon(Res.drawable.ic_plus)
    val Check = ClipKidIcon(Res.drawable.ic_check)
    /** PIN pad backspace; the pad is always LTR so this never mirrors. */
    val Backspace = ClipKidIcon(Res.drawable.ic_delete)
    val Star = ClipKidIcon(Res.drawable.ic_star)
    val Timer = ClipKidIcon(Res.drawable.ic_timer)
    val Chart = ClipKidIcon(Res.drawable.ic_chart_column)
    val Info = ClipKidIcon(Res.drawable.ic_info)
    val Image = ClipKidIcon(Res.drawable.ic_image)
    val Book = ClipKidIcon(Res.drawable.ic_book)
    val Leaf = ClipKidIcon(Res.drawable.ic_leaf)
    val Moon = ClipKidIcon(Res.drawable.ic_moon)

    /** Icons usable as a collection cover mark, keyed by `CoverMark.Icon.key`. */
    private val coverIcons: Map<String, ClipKidIcon> = mapOf(
        "book" to Book,
        "heart" to Heart,
        "leaf" to Leaf,
        "moon" to Moon,
        "star" to Star,
        "play" to Play,
        "home" to Home,
        "timer" to Timer,
    )

    /** Unknown keys fall back to [Star] so a new backend key never crashes the app. */
    fun forCoverKey(key: String): ClipKidIcon = coverIcons[key.lowercase()] ?: Star
}
