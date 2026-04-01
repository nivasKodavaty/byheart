package com.gtr3.byheart.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleYellow   = Color(0xFFFFD60A)
private val AppleRed      = Color(0xFFFF3B30)
private val AppleRedDark  = Color(0xFFFF453A)

private val LightColors = lightColorScheme(
    primary            = AppleYellow,
    onPrimary          = Color(0xFF1C1C1E),
    primaryContainer   = Color(0xFFFFF9C4),
    onPrimaryContainer = Color(0xFF1C1C1E),
    secondary          = Color(0xFF8E8E93),
    onSecondary        = Color.White,
    background         = Color(0xFFFFFFFF),
    onBackground       = Color(0xFF1C1C1E),
    surface            = Color(0xFFF2F2F7),
    onSurface          = Color(0xFF1C1C1E),
    surfaceVariant     = Color(0xFFE5E5EA),
    onSurfaceVariant   = Color(0xFF8E8E93),
    outline            = Color(0xFFC6C6C8),
    error              = AppleRed,
)

private val DarkColors = darkColorScheme(
    primary            = AppleYellow,
    onPrimary          = Color(0xFF1C1C1E),
    primaryContainer   = Color(0xFF3A3A1A),
    onPrimaryContainer = AppleYellow,
    secondary          = Color(0xFF636366),
    onSecondary        = Color.White,
    background         = Color(0xFF1C1C1E),
    onBackground       = Color(0xFFFFFFFF),
    surface            = Color(0xFF2C2C2E),
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF3A3A3C),
    onSurfaceVariant   = Color(0xFFAEAEB2),
    outline            = Color(0xFF38383A),
    error              = AppleRedDark,
)

@Composable
fun ByHeartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
