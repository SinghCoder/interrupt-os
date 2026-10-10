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

val Graphite = Color(0xFF1D2420)
val InkMuted = Color(0xFF68716B)
val Canvas = Color(0xFFF3F5F1)
val SurfaceWarm = Color(0xFFFBFCFA)
val Stone = Color(0xFFE4E9E4)
val Amber = Color(0xFF9A6A27)
val AmberSoft = Color(0xFFF3E8D5)
val Moss = Color(0xFF4D685A)
val MossSoft = Color(0xFFDDE8E0)
val Brick = Color(0xFF985549)

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = SurfaceWarm,
    secondary = Graphite,
    onSecondary = SurfaceWarm,
    background = Canvas,
    onBackground = Graphite,
    surface = SurfaceWarm,
    onSurface = Graphite,
    surfaceVariant = Stone,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFC5CDC6),
    error = Brick,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF3EFE5),
    onPrimary = Graphite,
    secondary = Color(0xFFD9E2DC),
    background = Color(0xFF151916),
    onBackground = Color(0xFFF3EFE5),
    surface = Color(0xFF1D221F),
    onSurface = Color(0xFFF3EFE5),
    surfaceVariant = Color(0xFF29302B),
    onSurfaceVariant = Color(0xFFC9C3B8),
    outline = Color(0xFF59635C),
    error = Color(0xFFF0A595),
)

private val InterruptTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 47.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
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
