package io.levs.spotdesk.ui.theme

import androidx.compose.ui.graphics.Color

// Spotify brand colors
val SpotifyGreen = Color(0xFF1DB954)
val SpotifyBlack = Color(0xFF191414)
val SpotifyDarkGrey = Color(0xFF282828)
val SpotifyLightGrey = Color(0xFF404040)
val SpotifyWhite = Color(0xFFFFFFFF)

// Additional UI colors
val BackgroundDark = SpotifyBlack
val SurfaceDark = SpotifyDarkGrey
val OnSurfaceLight = SpotifyWhite.copy(alpha = 0.9f)
val OnSurfaceMedium = SpotifyWhite.copy(alpha = 0.7f)
val OnSurfaceDisabled = SpotifyWhite.copy(alpha = 0.5f)