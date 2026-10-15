package com.shkurta.pickle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.shkurta.pickle.R

val GoogleSansFontFamily = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
    Font(R.font.google_sans_regular_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.google_sans_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.google_sans_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.google_sans_bold_italic, FontWeight.Bold, FontStyle.Italic),
)
private val DefaultTypography = Typography()

private fun TextStyle.googleSans(weight: FontWeight? = null) =
    copy(fontFamily = GoogleSansFontFamily, fontWeight = weight ?: fontWeight)

val Typography = Typography(
    // Google Sans supports weights 400–700; use bold for display and headline styles.
    displayLarge = DefaultTypography.displayLarge.googleSans(FontWeight.Bold),
    displayMedium = DefaultTypography.displayMedium.googleSans(FontWeight.Bold),
    displaySmall = DefaultTypography.displaySmall.googleSans(FontWeight.Bold),
    headlineLarge = DefaultTypography.headlineLarge.googleSans(FontWeight.Bold),
    headlineMedium = DefaultTypography.headlineMedium.googleSans(FontWeight.Bold).copy(fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = DefaultTypography.headlineSmall.googleSans(FontWeight.Bold),
    titleLarge = DefaultTypography.titleLarge.googleSans(FontWeight.Bold),
    titleMedium = DefaultTypography.titleMedium.googleSans(FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.googleSans(FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge.googleSans(),
    bodyMedium = DefaultTypography.bodyMedium.googleSans(),
    bodySmall = DefaultTypography.bodySmall.googleSans(),
    labelLarge = DefaultTypography.labelLarge.googleSans(FontWeight.SemiBold),
    labelMedium = DefaultTypography.labelMedium.googleSans(FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.googleSans(FontWeight.Medium),
)
