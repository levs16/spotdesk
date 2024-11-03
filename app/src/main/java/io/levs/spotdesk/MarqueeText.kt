package io.levs.spotdesk

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    style: TextStyle = TextStyle.Default,
    delayMillis: Int = 2000,
    spacerWidth: Int = 64,
) {
    val density = LocalDensity.current

    val createText = @Composable { localModifier: Modifier ->
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            modifier = localModifier,
            style = style,
            maxLines = 1,
        )
    }

    var offsetX by remember { mutableStateOf(0f) }
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }

    LaunchedEffect(textWidth, containerWidth) {
        if (textWidth <= containerWidth) return@LaunchedEffect

        delay(delayMillis.toLong())
        val duration = ((textWidth + spacerWidth) / 30f).toInt() * 1000

        while (true) {
            animate(
                initialValue = 0f,
                targetValue = -textWidth.toFloat() - spacerWidth,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            ) { value, _ ->
                offsetX = value
            }
            delay(delayMillis.toLong())
        }
    }

    SubcomposeLayout(
        modifier = modifier.clipToBounds()
    ) { constraints ->
        val infiniteWidthConstraints = constraints.copy(maxWidth = Constraints.Infinity)

        var mainText = subcompose("main") {
            createText(Modifier)
        }.first().measure(infiniteWidthConstraints)

        var secondText: Placeable? = null
        textWidth = mainText.width
        containerWidth = constraints.maxWidth

        if (textWidth > containerWidth) {
            secondText = subcompose("second") {
                createText(Modifier)
            }.first().measure(infiniteWidthConstraints)

            mainText = subcompose("mainWithOffset") {
                createText(
                    Modifier.offset(
                        x = with(density) { offsetX.toDp() }
                    )
                )
            }.first().measure(infiniteWidthConstraints)
        }

        layout(
            width = constraints.maxWidth,
            height = mainText.height
        ) {
            mainText.place(0, 0)
            if (textWidth > containerWidth) {
                secondText?.place(textWidth + spacerWidth + offsetX.toInt(), 0)
            }
        }
    }
}