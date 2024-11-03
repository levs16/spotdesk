package io.levs.spotdesk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyLightGrey,
    tertiary = SpotifyGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = SpotifyBlack,
    onSecondary = SpotifyWhite,
    onBackground = SpotifyWhite,
    onSurface = OnSurfaceLight
)

// We'll keep just the dark theme since it's a Spotify-inspired app
@Composable
fun SpotDeskTheme(
    darkTheme: Boolean = true, // Force dark theme
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to keep Spotify branding
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Always use our dark theme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}