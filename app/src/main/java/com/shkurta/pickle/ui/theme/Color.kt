package com.shkurta.pickle.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand color roles from the Foundations board:
 * Tomato = the leader, Mustard = celebration & highlight,
 * Teal = live state & primary actions, Ink = outlines and text.
 * Dark mode keeps every role, only the values change.
 */
@Immutable
data class PickleColors(
    val canvas: Color,
    val surface: Color,
    val sunken: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkInverse: Color,
    val lineStrong: Color,
    val lineSoft: Color,
    val tomato: Color,
    val tomatoDeep: Color,
    val mustard: Color,
    val teal: Color,
    val tealSoft: Color,
    val textAccent: Color,
    val onBrand: Color,
    val onMustard: Color,
)

val LightPickleColors = PickleColors(
    canvas = Color(0xFFF6EFE3),
    surface = Color(0xFFFFFAF3),
    sunken = Color(0xFFEFE2CF),
    ink = Color(0xFF33201B),
    inkMuted = Color(0xFF6E5A4E),
    inkInverse = Color(0xFFFFFAF3),
    lineStrong = Color(0xFF33201B),
    lineSoft = Color(0xFFE0D3BE),
    tomato = Color(0xFFE0552D),
    tomatoDeep = Color(0xFFB8401F),
    mustard = Color(0xFFF3C24A),
    teal = Color(0xFF1F7A6B),
    tealSoft = Color(0xFFCDE7E0),
    textAccent = Color(0xFFC9442A),
    onBrand = Color(0xFFFFFFFF),
    onMustard = Color(0xFF33201B),
)

val DarkPickleColors = PickleColors(
    canvas = Color(0xFF1C1412),
    surface = Color(0xFF261D19),
    sunken = Color(0xFF332824),
    ink = Color(0xFFF5EBDD),
    inkMuted = Color(0xFFB8A696),
    inkInverse = Color(0xFF1C1412),
    lineStrong = Color(0xFF6E5A4E),
    lineSoft = Color(0xFF3D302A),
    tomato = Color(0xFFE9693F),
    tomatoDeep = Color(0xFFA8401F),
    mustard = Color(0xFFF3C24A),
    teal = Color(0xFF258C78),
    tealSoft = Color(0xFF1F3833),
    textAccent = Color(0xFFF08A66),
    onBrand = Color(0xFFFFFFFF),
    onMustard = Color(0xFF33201B),
)

val LocalPickleColors = staticCompositionLocalOf { LightPickleColors }
