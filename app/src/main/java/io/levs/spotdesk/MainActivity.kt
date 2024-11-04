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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import android.widget.Toast

// Add these before class MainActivity
enum class EditableElement {
    FONT_STYLE,
    CONTROLS_STYLE
}

@Composable
fun EditableWrapper(
    isEditMode: Boolean,
    element: EditableElement,
    selectedElement: EditableElement?,
    onSelect: (EditableElement?) -> Unit,
    settings: Any,
    onSettingsChange: (Any) -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .then(
                    if (isEditMode) {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(
                                    alpha = if (selectedElement == element) 0.8f else 0.3f
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                            .clickable { onSelect(element) }
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

@Composable
private fun FrostedGlassBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .then(
                Modifier.border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
            )
    )
}

@Composable
private fun EditMenu(
    selectedElement: EditableElement,
    settings: Any,
    onSettingsChange: (Any) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // iOS-style frosted glass effect menu
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(360.dp)
                .align(Alignment.CenterEnd)
                .background(
                    Color(0xFF1C1C1E).copy(alpha = 0.7f), // More transparent background
                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
        ) {
            // Enhanced blur overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Header
                Text(
                    text = when (selectedElement) {
                        EditableElement.FONT_STYLE -> "Font & Color"
                        EditableElement.CONTROLS_STYLE -> "Controls Style"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                when (selectedElement) {
                    EditableElement.FONT_STYLE -> {
                        val textSettings = settings as TextStyleSettings
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Size slider with iOS style
                            IOSStyleSlider(
                                label = "Size",
                                value = textSettings.fontSize,
                                onValueChange = {
                                    onSettingsChange(textSettings.copy(fontSize = it))
                                }
                            )

                            // Weight selector with iOS style
                            IOSStyleSegmentedControl<FontWeight>(
                                label = "Weight",
                                options = listOf(
                                    FontWeight.Light to "Light",
                                    FontWeight.Normal to "Regular",
                                    FontWeight.Medium to "Medium",
                                    FontWeight.Bold to "Bold"
                                ),
                                selectedOption = textSettings.fontWeight,
                                onOptionSelected = {
                                    onSettingsChange(textSettings.copy(fontWeight = it))
                                }
                            )

                            // Opacity slider with iOS style
                            IOSStyleSlider(
                                label = "Opacity",
                                value = textSettings.opacity,
                                onValueChange = {
                                    onSettingsChange(textSettings.copy(opacity = it))
                                }
                            )
                        }
                    }

                    EditableElement.CONTROLS_STYLE -> {
                        val controlSettings = settings as ControlsStyleSettings

                        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                            // Size Slider
                            IOSStyleSlider(
                                label = "Size",
                                value = controlSettings.size,
                                onValueChange = {
                                    onSettingsChange(controlSettings.copy(size = it))
                                }
                            )

                            // Button Style
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Button Style",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ControlStyle.values().forEach { style ->
                                        Text(
                                            text = when(style) {
                                                ControlStyle.FILLED -> "Filled"
                                                ControlStyle.OUTLINED -> "Outlined"
                                                ControlStyle.MINIMAL -> "Minimal"
                                            },
                                            fontSize = 14.sp,
                                            color = Color.White.copy(
                                                alpha = if (controlSettings.style == style) 1f else 0.5f
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (controlSettings.style == style)
                                                        Color(0xFF48484A)
                                                    else Color(0xFF3A3A3C)
                                                )
                                                .clickable {
                                                    onSettingsChange(controlSettings.copy(style = style))
                                                }
                                                .padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Icon Style
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Icon Style",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconStyle.values().forEach { style ->
                                        Text(
                                            text = when(style) {
                                                IconStyle.REGULAR -> "Regular"
                                                IconStyle.ROUNDED -> "Rounded"
                                                IconStyle.SHARP -> "Sharp"
                                            },
                                            fontSize = 14.sp,
                                            color = Color.White.copy(
                                                alpha = if (controlSettings.iconStyle == style) 1f else 0.5f
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (controlSettings.iconStyle == style)
                                                        Color(0xFF48484A)
                                                    else Color(0xFF3A3A3C)
                                                )
                                                .clickable {
                                                    onSettingsChange(controlSettings.copy(iconStyle = style))
                                                }
                                                .padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Corner Radius
                            IOSStyleSlider(
                                label = "Corner Radius",
                                value = controlSettings.cornerRadius,
                                onValueChange = {
                                    onSettingsChange(controlSettings.copy(cornerRadius = it))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IOSStyleSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Slider(
                value = value,
                onValueChange = { onValueChange((it).coerceIn(0.5f, 2f)) },
                valueRange = 0.5f..2f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Text(
                text = String.format("%.1f", value),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun <T> IOSStyleSegmentedControl(
    label: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Color(0xFF2C2C2E).copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (option, label) ->
                    val isSelected = selectedOption == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = if (isSelected) 1f else 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Color(0xFF48484A).copy(alpha = 0.7f)
                                    else Color.Transparent
                                )
                                .clickable { onOptionSelected(option) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightSelector(
    selected: FontWeight,
    onSelected: (FontWeight) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Weight",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                FontWeight.Light to "Light",
                FontWeight.Normal to "Regular",
                FontWeight.Medium to "Medium",
                FontWeight.Bold to "Bold"
            ).forEach { (weight, label) ->
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = if (selected == weight) 1f else 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected == weight)
                                Color.White.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onSelected(weight) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StyleSelector(
    selected: ControlStyle,
    onSelected: (ControlStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Style",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlStyle.values().forEach { style ->
                Text(
                    text = style.name.lowercase().capitalize(),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = if (selected == style) 1f else 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected == style)
                                Color.White.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onSelected(style) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// Add these data classes to store style settings
data class TextStyleSettings(
    val fontSize: Float = 1f, // multiplier for base size
    val fontWeight: FontWeight = FontWeight.Bold,
    val opacity: Float = 1f
)

data class ControlsStyleSettings(
    val size: Float = 1f,
    val style: ControlStyle = ControlStyle.FILLED,
    val iconStyle: IconStyle = IconStyle.REGULAR,
    val cornerRadius: Float = 1f
)

enum class ControlStyle {
    FILLED, OUTLINED, MINIMAL
}

enum class IconStyle {
    REGULAR, ROUNDED, SHARP
}

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
        // State variables
        var editModeUnlocked by remember { mutableStateOf(false) }
        var selectedElement by remember { mutableStateOf<EditableElement?>(null) }
        var textSettings by remember { mutableStateOf(TextStyleSettings()) }
        var controlsSettings by remember { mutableStateOf(ControlsStyleSettings()) }
        var isAODMode by remember { mutableStateOf(false) }
        var isEditMode by remember { mutableStateOf(false) }

        // Easter egg tap tracking
        var albumArtTapCount by remember { mutableStateOf(0) }
        var lastTapTime by remember { mutableStateOf(0L) }
        val tapTimeout = 3000L // Reset tap count after 3 seconds
        val requiredTaps = 10

        val playerState by viewModel.playerState.collectAsState()
        val albumArt by viewModel.albumArtBitmap.collectAsState()
        val backgroundState = rememberDynamicBackground(
            bitmap = albumArt,
            isPlaying = playerState.isPlaying,
            audioSessionId = playerState.audioSessionId
        )

        // AOD transition
        val aodTransition = updateTransition(targetState = isAODMode, label = "AODTransition")

        // Edit mode transition
        val editTransition = updateTransition(targetState = isEditMode, label = "EditTransition")

        // AOD animations
        val mainAlpha by aodTransition.animateFloat(
            transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
            label = "mainAlpha"
        ) { isAOD -> if (isAOD) 0f else 1f }

        val mainScale by aodTransition.animateFloat(
            transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
            label = "mainScale"
        ) { isAOD -> if (isAOD) 1.1f else 1f }

        val aodAlpha by aodTransition.animateFloat(
            transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
            label = "aodAlpha"
        ) { isAOD -> if (isAOD) 1f else 0f }

        val aodScale by aodTransition.animateFloat(
            transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
            label = "aodScale"
        ) { isAOD -> if (isAOD) 1f else 0.9f }

        // Edit mode animations
        val editScale by editTransition.animateFloat(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            },
            label = "editScale"
        ) { editing -> if (editing) 0.9f else 1f }

        val editOverlayAlpha by editTransition.animateFloat(
            transitionSpec = { tween(300) },
            label = "editOverlayAlpha"
        ) { editing -> if (editing) 1f else 0f }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (editModeUnlocked && !isAODMode) {
                                isEditMode = !isEditMode
                                selectedElement = null
                            }
                        },
                        onDoubleTap = { if (!isEditMode) isAODMode = !isAODMode },
                        onTap = {
                            when {
                                isEditMode && selectedElement != null -> selectedElement = null
                                isEditMode -> isEditMode = false
                                isAODMode -> isAODMode = false
                            }
                        }
                    )
                }
        ) {
            // Main content with combined scale effects
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = mainAlpha
                        scaleX = mainScale * editScale
                        scaleY = mainScale * editScale
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .dynamicBackground(backgroundState.value)
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
                                EditableWrapper(
                                    isEditMode = isEditMode,
                                    element = EditableElement.FONT_STYLE,
                                    selectedElement = selectedElement,
                                    onSelect = { selectedElement = it },
                                    settings = textSettings,
                                    onSettingsChange = { textSettings = it as TextStyleSettings }
                                ) {
                                    Column {
                                        Text(
                                            text = playerState.title,
                                            fontSize = 32.sp * textSettings.fontSize,
                                            fontWeight = textSettings.fontWeight,
                                            letterSpacing = 0.sp,
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
                                            fontSize = 24.sp * textSettings.fontSize,
                                            fontWeight = textSettings.fontWeight,
                                            letterSpacing = 0.sp,
                                            color = Color.White.copy(alpha = textSettings.opacity * 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .padding(end = 16.dp)
                                                .windowInsetsPadding(
                                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                                                )
                                        )
                                    }
                                }
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
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimatedControlButton(
                                        icon = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        onClick = { viewModel.skipToPrevious() },
                                        baseSize = 44.dp,
                                        controlSettings = controlsSettings
                                    )

                                    AnimatedControlButton(
                                        icon = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                        onClick = { viewModel.playPause() },
                                        baseSize = 56.dp,
                                        controlSettings = controlsSettings
                                    )

                                    AnimatedControlButton(
                                        icon = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        onClick = { viewModel.skipToNext() },
                                        baseSize = 44.dp,
                                        controlSettings = controlsSettings
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
                                .clickable {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastTapTime > tapTimeout) {
                                        albumArtTapCount = 1
                                    } else {
                                        albumArtTapCount++
                                        if (albumArtTapCount >= requiredTaps && !editModeUnlocked) {
                                            editModeUnlocked = true
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Edit mode unlocked! Long press anywhere to edit.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    lastTapTime = currentTime
                                }
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

            // AOD Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = aodAlpha
                        scaleX = aodScale
                        scaleY = aodScale
                    }
            ) {
                if (isAODMode || aodAlpha > 0f) {
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

            // Only show edit menu if edit mode is unlocked
            if (editModeUnlocked) {
                AnimatedVisibility(
                    visible = selectedElement != null,
                    enter = slideIn(
                        initialOffset = { IntOffset(it.width, 0) },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOut(
                        targetOffset = { IntOffset(it.width, 0) },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    selectedElement?.let { element ->
                        EditMenu(
                            selectedElement = element,
                            settings = when (element) {
                                EditableElement.FONT_STYLE -> textSettings
                                EditableElement.CONTROLS_STYLE -> controlsSettings
                            },
                            onSettingsChange = {
                                when (element) {
                                    EditableElement.FONT_STYLE -> textSettings = it as TextStyleSettings
                                    EditableElement.CONTROLS_STYLE -> controlsSettings = it as ControlsStyleSettings
                                }
                            },
                            onDismiss = { selectedElement = null }
                        )
                    }
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
        baseSize: Dp,
        controlSettings: ControlsStyleSettings,
        modifier: Modifier = Modifier
    ) {
        val size = baseSize * controlSettings.size
        val iconModifier = Modifier.size(size * 0.55f)
        val cornerRadius = 50f * controlSettings.cornerRadius

        when (controlSettings.style) {
            ControlStyle.FILLED -> {
                IconButton(
                    onClick = onClick,
                    modifier = modifier.size(size)
                ) {
                    Icon(
                        imageVector = when (controlSettings.iconStyle) {
                            IconStyle.REGULAR -> icon
                            IconStyle.ROUNDED -> icon
                            IconStyle.SHARP -> icon
                        },
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = iconModifier
                    )
                }
            }
            ControlStyle.OUTLINED -> {
                IconButton(
                    onClick = onClick,
                    modifier = modifier.size(size)
                ) {
                    Icon(
                        imageVector = when (controlSettings.iconStyle) {
                            IconStyle.REGULAR -> icon
                            IconStyle.ROUNDED -> icon
                            IconStyle.SHARP -> icon
                        },
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = iconModifier
                    )
                }
            }
            ControlStyle.MINIMAL -> {
                IconButton(
                    onClick = onClick,
                    modifier = modifier.size(size)
                ) {
                    Icon(
                        imageVector = when (controlSettings.iconStyle) {
                            IconStyle.REGULAR -> icon
                            IconStyle.ROUNDED -> icon
                            IconStyle.SHARP -> icon
                        },
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = iconModifier
                    )
                }
            }
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

    @Composable
    private fun StyleOption(
        name: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        preview: @Composable () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Color(0xFF2C2C2E).copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                // Enhanced blur overlay for preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    preview()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}