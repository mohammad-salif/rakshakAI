package com.example.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

fun rakshakTypography(elderlyMode: Boolean): Typography {
    val bodySize = if (elderlyMode) 18.sp else 16.sp
    val bodyLine = if (elderlyMode) 26.sp else 23.sp
    val labelSize = if (elderlyMode) 15.sp else 13.sp
    val titleMed = if (elderlyMode) 21.sp else 18.sp
    val titleLarge = if (elderlyMode) 24.sp else 21.sp
    val headSmall = if (elderlyMode) 26.sp else 23.sp
    val headMed = if (elderlyMode) 30.sp else 26.sp

    return Typography(
        headlineMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = headMed,
            lineHeight = headMed * 1.2f
        ),
        headlineSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = headSmall,
            lineHeight = headSmall * 1.25f
        ),
        titleLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleLarge,
            lineHeight = titleLarge * 1.25f
        ),
        titleMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleMed,
            lineHeight = titleMed * 1.25f
        ),
        titleSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (elderlyMode) 18.sp else 15.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = bodySize,
            lineHeight = bodyLine
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = if (elderlyMode) 17.sp else 15.sp,
            lineHeight = if (elderlyMode) 24.sp else 21.sp
        ),
        bodySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = if (elderlyMode) 15.sp else 13.sp,
            lineHeight = if (elderlyMode) 21.sp else 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = labelSize
        ),
        labelMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (elderlyMode) 14.sp else 12.sp
        ),
        labelSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = if (elderlyMode) 13.sp else 11.sp
        )
    )
}
