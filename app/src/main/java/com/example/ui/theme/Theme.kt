package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CommissionWalaLightScheme = lightColorScheme(
    primary = BrandPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleContainer,
    onPrimaryContainer = BrandOnPurpleContainer,
    secondary = BrandBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandBlueContainer,
    onSecondaryContainer = BrandOnBlueContainer,
    tertiary = BrandCyanSpark,
    onTertiary = Color.White,
    tertiaryContainer = BrandCyanContainer,
    onTertiaryContainer = BrandOnBlueContainer,
    background = AppBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = AppSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = AppOutlineLight,
    outlineVariant = AppOutlineSubtle,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val CommissionWalaDarkScheme = darkColorScheme(
    primary = BrandPurpleDarkPrimary,
    onPrimary = BrandPurpleDeep,
    primaryContainer = BrandPurplePrimary,
    onPrimaryContainer = Color.White,
    secondary = BrandBlueDarkSecondary,
    onSecondary = BrandOnBlueContainer,
    secondaryContainer = BrandBlueSecondary,
    onSecondaryContainer = Color.White,
    background = AppBackgroundDark,
    onBackground = Color(0xFFF8FAFC),
    surface = AppSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D)
)

val CommissionWalaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CommissionWalaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme && isSystemInDarkTheme()) {
        CommissionWalaDarkScheme
    } else {
        CommissionWalaLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CommissionWalaTypography,
        shapes = CommissionWalaShapes,
        content = content
    )
}
