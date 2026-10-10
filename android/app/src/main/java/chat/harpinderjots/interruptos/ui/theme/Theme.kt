package chat.harpinderjots.interruptos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Graphite = Color(0xFF1B1B18)
val InkMuted = Color(0xFF69675F)
val Canvas = Color(0xFFF5F1E8)
val SurfaceWarm = Color(0xFFFFFCF5)
val Stone = Color(0xFFE7E1D6)
val Amber = Color(0xFFE49A37)
val AmberSoft = Color(0xFFF4DFC1)
val Moss = Color(0xFF456A57)
val Brick = Color(0xFF9A5142)

private val LightColors = lightColorScheme(
    primary = Graphite,
    onPrimary = SurfaceWarm,
    secondary = Amber,
    onSecondary = Graphite,
    background = Canvas,
    onBackground = Graphite,
    surface = SurfaceWarm,
    onSurface = Graphite,
    surfaceVariant = Stone,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFC8C0B3),
    error = Brick,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF3EFE5),
    onPrimary = Graphite,
    secondary = Color(0xFFF0B461),
    background = Color(0xFF141412),
    onBackground = Color(0xFFF3EFE5),
    surface = Color(0xFF1E1E1B),
    onSurface = Color(0xFFF3EFE5),
    surfaceVariant = Color(0xFF2A2925),
    onSurfaceVariant = Color(0xFFC9C3B8),
    outline = Color(0xFF5C5952),
    error = Color(0xFFF0A595),
)

private val InterruptTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 54.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun InterruptOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = InterruptTypography,
        content = content,
    )
}

