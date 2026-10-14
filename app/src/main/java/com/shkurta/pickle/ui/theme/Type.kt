package com.shkurta.pickle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.shkurta.pickle.R

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val Rubik = GoogleFont("Rubik")

val RubikFontFamily = FontFamily(
    Font(googleFont = Rubik, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = Rubik, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = Rubik, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Rubik, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = Rubik, fontProvider = GoogleFontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = Rubik, fontProvider = GoogleFontProvider, weight = FontWeight.Black),
)

private val DefaultTypography = Typography()

private fun TextStyle.rubik(weight: FontWeight? = null) =
    copy(fontFamily = RubikFontFamily, fontWeight = weight ?: fontWeight)

val Typography = Typography(
    // Display + headline: the chunky "sticker" headings (see PickleHeadline)
    displayLarge = DefaultTypography.displayLarge.rubik(FontWeight.Black),
    displayMedium = DefaultTypography.displayMedium.rubik(FontWeight.Black),
    displaySmall = DefaultTypography.displaySmall.rubik(FontWeight.Black),
    headlineLarge = DefaultTypography.headlineLarge.rubik(FontWeight.Black),
    headlineMedium = DefaultTypography.headlineMedium.rubik(FontWeight.Black)
        .copy(fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = DefaultTypography.headlineSmall.rubik(FontWeight.ExtraBold),
    titleLarge = DefaultTypography.titleLarge.rubik(FontWeight.Bold),
    titleMedium = DefaultTypography.titleMedium.rubik(FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.rubik(FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge.rubik(),
    bodyMedium = DefaultTypography.bodyMedium.rubik(),
    bodySmall = DefaultTypography.bodySmall.rubik(),
    labelLarge = DefaultTypography.labelLarge.rubik(FontWeight.SemiBold),
    labelMedium = DefaultTypography.labelMedium.rubik(FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.rubik(FontWeight.Medium),
)
