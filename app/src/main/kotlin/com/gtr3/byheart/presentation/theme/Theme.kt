package com.gtr3.byheart.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Brand colours ────────────────────────────────────────────────────────────
val AppleYellow      = Color(0xFFFFD60A)
private val AppleRed        = Color(0xFFFF3B30)
private val AppleRedDark    = Color(0xFFFF453A)

// ─── Light — warm paper (Apple Notes feel) ───────────────────────────────────
private val LightColors = lightColorScheme(
    primary                = AppleYellow,
    onPrimary              = Color(0xFF1C1C1E),
    primaryContainer       = Color(0xFFFFF3B0),
    onPrimaryContainer     = Color(0xFF2E2800),
    secondary              = Color(0xFF6C6C70),
    onSecondary            = Color.White,
    secondaryContainer     = Color(0xFFE8E8ED),
    onSecondaryContainer   = Color(0xFF1C1C1E),
    tertiary               = Color(0xFF007AFF),
    onTertiary             = Color.White,
    tertiaryContainer      = Color(0xFFD6EEFF),
    onTertiaryContainer    = Color(0xFF001C3A),
    background             = Color(0xFFF5F4F0),   // Warm parchment — actual Apple Notes bg
    onBackground           = Color(0xFF1C1C1E),
    surface                = Color(0xFFFFFFFF),
    onSurface              = Color(0xFF1C1C1E),
    surfaceVariant         = Color(0xFFEBEBED),
    onSurfaceVariant       = Color(0xFF6C6C70),
    outline                = Color(0xFFC6C6C8),
    outlineVariant         = Color(0xFFE5E5EA),
    error                  = AppleRed,
    onError                = Color.White,
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),
)

// ─── Dark — true OLED black (Apple Notes dark) ───────────────────────────────
private val DarkColors = darkColorScheme(
    primary                = AppleYellow,
    onPrimary              = Color(0xFF1C1C1E),
    primaryContainer       = Color(0xFF3A3000),
    onPrimaryContainer     = AppleYellow,
    secondary              = Color(0xFFAEAEB2),
    onSecondary            = Color(0xFF1C1C1E),
    secondaryContainer     = Color(0xFF3A3A3C),
    onSecondaryContainer   = Color(0xFFE5E5EA),
    tertiary               = Color(0xFF0A84FF),
    onTertiary             = Color.White,
    tertiaryContainer      = Color(0xFF00315C),
    onTertiaryContainer    = Color(0xFFD6EEFF),
    background             = Color(0xFF000000),   // True OLED black
    onBackground           = Color(0xFFFFFFFF),
    surface                = Color(0xFF1C1C1E),   // Apple's primary elevated dark surface
    onSurface              = Color(0xFFFFFFFF),
    surfaceVariant         = Color(0xFF2C2C2E),
    onSurfaceVariant       = Color(0xFFAEAEB2),
    outline                = Color(0xFF48484A),
    outlineVariant         = Color(0xFF3A3A3C),
    error                  = AppleRedDark,
    onError                = Color.White,
    errorContainer         = Color(0xFF93000A),
    onErrorContainer       = Color(0xFFFFDAD6),
)

// ─── Shapes ───────────────────────────────────────────────────────────────────
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(14.dp),
    large      = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// ─── Typography — tighter, more Apple-like spacing ───────────────────────────
private val AppTypography = Typography(
    displayLarge   = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp,  letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp,  letterSpacing = (-0.25).sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.1.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.15.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.25.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.4.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp,  letterSpacing = 0.4.sp),
)

@Composable
fun ByHeartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes      = AppShapes,
        typography  = AppTypography,
        content     = content
    )
}
