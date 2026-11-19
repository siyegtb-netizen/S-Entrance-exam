package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun DarkModeToggleButton(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onToggle,
        modifier = modifier
            .size(48.dp)
            .testTag("dark_mode_toggle_button"),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode"
        )
    }
}

@Composable
fun StudyHeroIllustration(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w * 0.78f, h * 0.35f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.78f, h * 0.35f)
        )

        // Back PDF sheet
        val docWidth = w * 0.30f
        val docHeight = h * 0.72f
        val docLeft = w * 0.12f
        val docTop = h * 0.16f

        drawRoundRect(
            color = primary.copy(alpha = 0.14f),
            topLeft = Offset(docLeft + 8f, docTop + 8f),
            size = Size(docWidth, docHeight),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = surface,
            topLeft = Offset(docLeft, docTop),
            size = Size(docWidth, docHeight),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(docLeft, docTop),
            size = Size(docWidth, docHeight),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 2.5f)
        )

        // PDF header pill & text lines
        drawRoundRect(
            color = secondary,
            topLeft = Offset(docLeft + docWidth * 0.18f, docTop + docHeight * 0.15f),
            size = Size(docWidth * 0.44f, docHeight * 0.12f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        for (i in 0..2) {
            val lineY = docTop + docHeight * (0.40f + i * 0.16f)
            val lineWidth = if (i == 2) docWidth * 0.48f else docWidth * 0.64f
            drawLine(
                color = primary.copy(alpha = 0.25f),
                start = Offset(docLeft + docWidth * 0.18f, lineY),
                end = Offset(docLeft + docWidth * 0.18f + lineWidth, lineY),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        // Connector spark arrow
        val arrowStart = Offset(docLeft + docWidth + w * 0.03f, h * 0.50f)
        val arrowEnd = Offset(w * 0.54f, h * 0.50f)
        drawLine(
            color = secondary,
            start = arrowStart,
            end = arrowEnd,
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        val arrowHead = Path().apply {
            moveTo(arrowEnd.x + 8f, arrowEnd.y)
            lineTo(arrowEnd.x - 6f, arrowEnd.y - 8f)
            lineTo(arrowEnd.x - 6f, arrowEnd.y + 8f)
            close()
        }
        drawPath(path = arrowHead, color = secondary)

        // Interactive Quiz Slide Card on right
        val cardLeft = w * 0.58f
        val cardTop = h * 0.14f
        val cardWidth = w * 0.32f
        val cardHeight = h * 0.74f

        drawRoundRect(
            color = primary,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(22f, 22f)
        )

        // Option pills inside card (A, B, C)
        for (i in 0..2) {
            val pillTop = cardTop + cardHeight * (0.20f + i * 0.24f)
            val pillColor = if (i == 1) secondary else tertiary.copy(alpha = 0.45f)
            drawRoundRect(
                color = pillColor,
                topLeft = Offset(cardLeft + cardWidth * 0.14f, pillTop),
                size = Size(cardWidth * 0.72f, cardHeight * 0.16f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
    }
}

@Composable
fun ScoreGaugeRing(
    percentage: Int,
    ringColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 900),
        label = "score_gauge_progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        content()
    }
}
