package com.shkurta.pickle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.shkurta.pickle.R

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val GoogleSans = GoogleFont("Google Sans")

val GoogleSansFontFamily = FontFamily(
    Font(googleFont = GoogleSans, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleSans, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleSans, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleSans, fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = GoogleSansFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = GoogleSansFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = GoogleSansFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = GoogleSansFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = GoogleSansFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = GoogleSansFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = GoogleSansFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = GoogleSansFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = GoogleSansFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = GoogleSansFontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = GoogleSansFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = GoogleSansFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = GoogleSansFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = GoogleSansFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = GoogleSansFontFamily)
)
