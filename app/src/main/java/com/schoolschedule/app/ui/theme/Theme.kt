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

// Brand palette lifted straight from the "دوامي" logo: sky-blue building card,
// warm orange backpack/roof, golden bell/pencil accent. Bright and lively on purpose.
val LogoSkyBlue = Color(0xFF29ABE2)
val LogoDeepBlue = Color(0xFF1B5FA8)
val LogoOrange = Color(0xFFF2994A)
val LogoGold = Color(0xFFF4B400)

val LightColors = lightColorScheme(
    primary = Color(0xFF166DB0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9E9FB),
    onPrimaryContainer = Color(0xFF063A5E),
    secondary = Color(0xFFE07A1F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDFB8),
    onSecondaryContainer = Color(0xFF4A2705),
    tertiary = Color(0xFFAD7F00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFECAD),
    onTertiaryContainer = Color(0xFF3F2E00),
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFFCFEFF),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFE3EFF6),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF74797D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF8FCEF2),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF104E77),
    onPrimaryContainer = Color(0xFFC9E9FB),
    secondary = Color(0xFFFFB874),
    onSecondary = Color(0xFF4A2705),
    secondaryContainer = Color(0xFF6A3C0D),
    onSecondaryContainer = Color(0xFFFFDFB8),
    tertiary = Color(0xFFEFC846),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFECAD),
    background = Color(0xFF10151A),
    onBackground = Color(0xFFE2E5E8),
    surface = Color(0xFF161C21),
    onSurface = Color(0xFFE2E5E8),
    surfaceVariant = Color(0xFF252E34),
    onSurfaceVariant = Color(0xFFC1C9CE),
    outline = Color(0xFF8B9297),
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
