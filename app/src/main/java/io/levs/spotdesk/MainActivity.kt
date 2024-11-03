package io.levs.spotdesk

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.ripple.rememberRipple
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Show explanation dialog
            showPermissionExplanationDialog()
        }
    }

    private fun checkAndRequestPermissions() {
        // Check for notification listener permission
        if (!isNotificationServiceEnabled()) {
            showNotificationListenerDialog()
        }

        // Check for audio recording permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission granted, do nothing
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO) -> {
                showPermissionExplanationDialog()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    @Composable
    private fun PermissionDialog(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permissions Required") },
            text = {
                Text("This app needs access to audio recording for beat detection and notification access for media control. Without these permissions, some features won't work properly.")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun showPermissionExplanationDialog() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun showNotificationListenerDialog() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions at startup
        checkAndRequestPermissions()

        // Setup edge-to-edge and immersive mode
        setupFullscreen()

        setContent {
            var showPermissionDialog by remember { mutableStateOf(false) }

            if (showPermissionDialog) {
                PermissionDialog(
                    onDismiss = { showPermissionDialog = false },
                    onConfirm = {
                        showPermissionDialog = false
                        showPermissionExplanationDialog()
                    }
                )
            }

            MainScreen()
        }
    }

    @Composable
    private fun MainScreen() {
        var isAODMode by remember { mutableStateOf(false) }
        val playerState by viewModel.playerState.collectAsState()
        val albumArt by viewModel.albumArtBitmap.collectAsState()
        val backgroundState = rememberDynamicBackground(
            bitmap = albumArt,
            isPlaying = playerState.isPlaying,
            audioSessionId = playerState.audioSessionId
        )

        // Enhanced fade and scale animations
        val transition = updateTransition(targetState = isAODMode, label = "AODTransition")

        val mainAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            },
            label = "mainAlpha"
        ) { isAOD -> if (isAOD) 0f else 1f }

        val mainScale by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            },
            label = "mainScale"
        ) { isAOD -> if (isAOD) 1.1f else 1f }

        val aodAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            },
            label = "aodAlpha"
        ) { isAOD -> if (isAOD) 1f else 0f }

        val aodScale by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            },
            label = "aodScale"
        ) { isAOD -> if (isAOD) 1f else 0.9f }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { isAODMode = !isAODMode },
                        onTap = { if (isAODMode) isAODMode = false }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = mainAlpha
                        scaleX = mainScale
                        scaleY = mainScale
                    }
                    .dynamicBackground(backgroundState.value)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = mainAlpha
                        scaleX = mainScale
                        scaleY = mainScale
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeContent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = playerState.title,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 40.sp,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .windowInsetsPadding(
                                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                                        )
                                )

                                Text(
                                    text = playerState.artist,
                                    fontSize = 24.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .windowInsetsPadding(
                                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                                        )
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(bottom = 8.dp)
                                    .graphicsLayer {
                                        alpha = if (isAODMode) 0f else 1f
                                    }
                                    .graphicsLayer(alpha = mainAlpha),
                                verticalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .width(IntrinsicSize.Min)
                                        .padding(end = 32.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimatedControlButton(
                                        icon = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        onClick = { viewModel.skipToPrevious() },
                                        size = 44.dp
                                    )

                                    PlayPauseButton(
                                        isPlaying = playerState.isPlaying,
                                        onClick = { viewModel.playPause() },
                                        size = 56.dp,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )

                                    AnimatedControlButton(
                                        icon = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        onClick = { viewModel.skipToNext() },
                                        size = 44.dp
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProgressBar(
                                        progress = if (playerState.duration > 0) {
                                            playerState.currentPosition.toFloat() / playerState.duration
                                        } else 0f,
                                        onProgressChange = {
                                            viewModel.seekTo((it * playerState.duration).toLong())
                                        }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatDuration(playerState.currentPosition),
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = formatDuration(playerState.duration),
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(340.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            albumArt?.let { art ->
                                Image(
                                    bitmap = art.asImageBitmap(),
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = aodAlpha
                        scaleX = aodScale
                        scaleY = aodScale
                    }
            ) {
                if (isAODMode || aodAlpha > 0f) {  // Keep AOD mounted during animation
                    AODScreen(
                        playerState = playerState,
                        albumArt = albumArt,
                        isAODMode = isAODMode,
                        onToggleAOD = { isAODMode = false },
                        onPlayPause = { viewModel.playPause() },
                        onNextTrack = { viewModel.skipToNext() },
                        onPreviousTrack = { viewModel.skipToPrevious() }
                    )
                }
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun setupFullscreen() {
        // Make app edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle cutouts
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        // Make system bars transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configure system bars behavior
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Hide both bars
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Composable
    fun ProgressBar(
        progress: Float,
        onProgressChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
        height: Dp = 6.dp
    ) {
        var isDragging by remember { mutableStateOf(false) }
        var dragProgress by remember { mutableStateOf(progress) }
        var lastProgress by remember { mutableStateOf(progress) }
        val view = LocalView.current

        val baseHeight = height
        val expandedHeight = 12.dp

        val height by animateDpAsState(
            targetValue = if (isDragging) expandedHeight else baseHeight,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f,
                visibilityThreshold = 0.1.dp
            ),
            label = "height"
        )

        val backgroundAlpha by animateFloatAsState(
            targetValue = if (isDragging) 0.4f else 0.3f,
            animationSpec = tween(100),
            label = "alpha"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color.White.copy(alpha = backgroundAlpha))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        },
                        onDragEnd = {
                            isDragging = false
                            onProgressChange(dragProgress)
                            if (abs(lastProgress - dragProgress) > 0.01f) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            lastProgress = dragProgress
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val oldProgress = dragProgress
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)

                            when {
                                dragProgress == 0f && oldProgress > 0f -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                dragProgress == 1f && oldProgress < 1f -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                abs(dragProgress - oldProgress) > 0.02f -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }

                            onProgressChange(dragProgress)
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isDragging) dragProgress else progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height / 2))
                    .background(Color.White)
            )
        }
    }

    @Composable
    fun AnimatedControlButton(
        icon: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
        size: Dp = 36.dp,
        modifier: Modifier = Modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed = interactionSource.collectIsPressedAsState()
        val localView = LocalView.current  // Get it directly here

        val scale by animateFloatAsState(
            targetValue = if (isPressed.value) 0.85f else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            ),
            label = "scale"
        )

        val alpha by animateFloatAsState(
            targetValue = if (isPressed.value) 0.7f else 1f,
            animationSpec = tween(100),
            label = "alpha"
        )

        IconButton(
            onClick = {
                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
            interactionSource = interactionSource,
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(size * 0.8f)
            )
        }
    }

    @Composable
    fun PlayPauseButton(
        isPlaying: Boolean,
        onClick: () -> Unit,
        size: Dp = 48.dp,
        modifier: Modifier = Modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed = interactionSource.collectIsPressedAsState()
        val localView = LocalView.current

        // Simple press animation
        val scale by animateFloatAsState(
            targetValue = if (isPressed.value) 0.85f else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            ),
            label = "scale"
        )

        // Smooth icon transition
        val alpha by animateFloatAsState(
            targetValue = if (isPressed.value) 0.7f else 1f,
            animationSpec = tween(100),
            label = "alpha"
        )

        IconButton(
            onClick = {
                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
            interactionSource = interactionSource,
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }

    @Composable
    private fun AnimatedText(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit,
        color: Color,
        fontWeight: FontWeight = FontWeight.Normal,
        maxLines: Int = 1
    ) {
        val transition = updateTransition(targetState = text, label = "TextTransition")
        val scale by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 300, easing = FastOutSlowInEasing) },
            label = "Scale"
        ) { _ -> 1f }

        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}