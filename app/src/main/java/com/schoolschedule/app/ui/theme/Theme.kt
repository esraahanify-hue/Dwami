package com.schoolschedule.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Brand palette: deep school-blue + warm teal accent, tuned for a calm, modern look.
private val SeedBlue = Color(0xFF2C5F8A)
private val SeedTeal = Color(0xFF3E7D6E)
private val SeedAmber = Color(0xFFB0631F)

val LightColors = lightColorScheme(
    primary = Color(0xFF265C87),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FA),
    onPrimaryContainer = Color(0xFF08304F),
    secondary = SeedTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7ECE3),
    onSecondaryContainer = Color(0xFF0E2F26),
    tertiary = SeedAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE2C6),
    onTertiaryContainer = Color(0xFF3E2405),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF1B1E22),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1B1E22),
    surfaceVariant = Color(0xFFE7EDF5),
    onSurfaceVariant = Color(0xFF44474D),
    outline = Color(0xFF767A82),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF9FCBFA),
    onPrimary = Color(0xFF0A3355),
    primaryContainer = Color(0xFF17456A),
    onPrimaryContainer = Color(0xFFD3E4FA),
    secondary = Color(0xFF9FD4C3),
    onSecondary = Color(0xFF0A3327),
    secondaryContainer = Color(0xFF224A3D),
    onSecondaryContainer = Color(0xFFD7ECE3),
    tertiary = Color(0xFFF3BE8B),
    onTertiary = Color(0xFF432B03),
    tertiaryContainer = Color(0xFF5E3E10),
    onTertiaryContainer = Color(0xFFFBE2C6),
    background = Color(0xFF12151A),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF181B20),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF262A30),
    onSurfaceVariant = Color(0xFFC4C7CE),
    outline = Color(0xFF8E9198),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val AppTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

/**
 * Countdown/emphasis numerals — tabular figures so the seconds ticker doesn't jitter.
 */
val CountdownTextStyle: TextStyle
    @Composable get() = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)

@Composable
fun SchoolScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = colorScheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
