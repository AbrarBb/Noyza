package com.khatibstudio.noyza.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Colors ─────────────────────────────────────────────────────────────
// Primary accent: Deep Teal / Cyan
val Teal10 = Color(0xFF001F26)
val Teal20 = Color(0xFF003640)
val Teal30 = Color(0xFF004E5B)
val Teal40 = Color(0xFF006875)  // Primary
val Teal50 = Color(0xFF00828F)
val Teal60 = Color(0xFF009CAA)
val Teal70 = Color(0xFF00B5C4)
val Teal80 = Color(0xFF4DD0DF)  // Primary Container
val Teal90 = Color(0xFF97EAF6)
val Teal95 = Color(0xFFCAF5FB)
val Teal99 = Color(0xFFF4FDFF)

// Secondary: Muted slate-teal
val Slate10 = Color(0xFF111B1E)
val Slate20 = Color(0xFF263034)
val Slate30 = Color(0xFF3C464A)
val Slate40 = Color(0xFF535E62)
val Slate60 = Color(0xFF8B9599)
val Slate80 = Color(0xFFB8C3C7)
val Slate90 = Color(0xFFD4DFE3)
val Slate95 = Color(0xFFE2EDF1)
val Slate99 = Color(0xFFF4F9FB)

// Tertiary: Soft indigo
val Indigo10 = Color(0xFF0D0D3C)
val Indigo20 = Color(0xFF1C1B55)
val Indigo40 = Color(0xFF3D3B8E)
val Indigo80 = Color(0xFFBDBCF5)
val Indigo90 = Color(0xFFE1E0FF)

// Neutral
val NeutralSurface = Color(0xFFF8FAFA)
val NeutralSurfaceDark = Color(0xFF0E1618)
val NeutralBg = Color(0xFFEFF3F5)
val NeutralBgDark = Color(0xFF16202A)
val NeutralCard = Color(0xFFFFFFFF)
val NeutralCardDark = Color(0xFF1E2B30)

// ─── Noise Status Colors ───────────────────────────────────────────────────────
// Never communicate status with color alone — always pair with text
val QuietGreen = Color(0xFF2E7D32)        // ≤ 45 dB
val QuietGreenContainer = Color(0xFFD7F3D8)
val QuietGreenLight = Color(0xFF4CAF50)

val ModerateAmber = Color(0xFFF57F17)     // 46–65 dB
val ModerateAmberContainer = Color(0xFFFFF3CC)
val ModerateAmberLight = Color(0xFFFFB300)

val LoudOrange = Color(0xFFE65100)        // 66–80 dB
val LoudOrangeContainer = Color(0xFFFFE0B2)
val LoudOrangeLight = Color(0xFFFF6D00)

val VeryLoudRed = Color(0xFFC62828)       // > 80 dB
val VeryLoudRedContainer = Color(0xFFFFCDD2)
val VeryLoudRedLight = Color(0xFFD32F2F)

// ─── Score Colors ──────────────────────────────────────────────────────────────
val ScoreExcellent = Color(0xFF1B5E20)    // 90–100
val ScoreGood = Color(0xFF2E7D32)         // 75–89
val ScoreModerate = Color(0xFFF57F17)     // 50–74
val ScorePoor = Color(0xFFE65100)         // 25–49
val ScoreNotRec = Color(0xFFC62828)       // 0–24

// ─── Common ───────────────────────────────────────────────────────────────────
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Transparent = Color(0x00000000)
val SurfaceVariantLight = Color(0xFFE0EDEF)
val SurfaceVariantDark = Color(0xFF29373C)
val OutlineLight = Color(0xFF70787C)
val OutlineDark = Color(0xFF899296)
