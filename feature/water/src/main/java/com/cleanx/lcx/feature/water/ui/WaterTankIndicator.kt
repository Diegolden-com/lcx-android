package com.cleanx.lcx.feature.water.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanx.lcx.core.model.WaterLevelStatus

/** Color for each water status level, matching the PWA. */
fun statusColor(status: WaterLevelStatus): Color = when (status) {
    WaterLevelStatus.CRITICAL -> Color(0xFFEF4444) // red
    WaterLevelStatus.LOW -> Color(0xFFF59E0B)      // yellow/amber
    WaterLevelStatus.NORMAL -> Color(0xFF22C55E)    // green
    WaterLevelStatus.OPTIMAL -> Color(0xFF3B82F6)   // blue
}

/** Spanish label for each status. */
fun statusLabel(status: WaterLevelStatus): String = when (status) {
    WaterLevelStatus.CRITICAL -> "Critico"
    WaterLevelStatus.LOW -> "Bajo"
    WaterLevelStatus.NORMAL -> "Normal"
    WaterLevelStatus.OPTIMAL -> "Optimo"
}

/**
 * Visual tank indicator composable.
 *
 * Draws a rounded-rect tank outline filled to [percentage] with color
 * derived from the current [status]. Reference lines are drawn at
 * 20%, 40%, and 70% marks. The percentage and liters are centered
 * inside the tank.
 */
@Composable
fun WaterTankIndicator(
    percentage: Int,
    liters: Int,
    status: WaterLevelStatus,
    modifier: Modifier = Modifier,
) {
    val fillColor = statusColor(status)
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tankPadding = 40f
            val tankLeft = size.width * 0.2f
            val tankRight = size.width * 0.8f
            val tankTop = tankPadding
            val tankBottom = size.height - tankPadding
            val tankWidth = tankRight - tankLeft
            val tankHeight = tankBottom - tankTop
            val cornerRadius = 16f

            // Draw fill
            val fillFraction = (percentage.coerceIn(0, 100)) / 100f
            val fillHeight = tankHeight * fillFraction
            val fillTop = tankBottom - fillHeight

            drawRoundRect(
                color = fillColor.copy(alpha = 0.3f),
                topLeft = Offset(tankLeft, fillTop),
                size = Size(tankWidth, fillHeight),
                cornerRadius = CornerRadius(
                    if (fillFraction > 0.95f) cornerRadius else 0f,
                    if (fillFraction > 0.95f) cornerRadius else 0f,
                ),
            )

            // Draw tank outline
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(tankLeft, tankTop),
                size = Size(tankWidth, tankHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 3f),
            )

            // Reference lines at 20%, 40%, 70%
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            for (refPercent in listOf(20, 40, 70)) {
                val refY = tankBottom - (tankHeight * refPercent / 100f)
                drawLine(
                    color = outlineColor.copy(alpha = 0.5f),
                    start = Offset(tankLeft + 4f, refY),
                    end = Offset(tankRight - 4f, refY),
                    strokeWidth = 1f,
                    pathEffect = dashEffect,
                )
                // Label to the right of the tank
                drawReferenceLabel(
                    textMeasurer = textMeasurer,
                    text = "${refPercent}%",
                    x = tankRight + 8f,
                    y = refY,
                    color = outlineColor,
                )
            }

            // Center text: percentage
            val percentText = "${percentage}%"
            val percentStyle = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor,
                textAlign = TextAlign.Center,
            )
            val percentLayout = textMeasurer.measure(percentText, percentStyle)
            drawText(
                textLayoutResult = percentLayout,
                topLeft = Offset(
                    x = tankLeft + (tankWidth - percentLayout.size.width) / 2f,
                    y = tankTop + (tankHeight - percentLayout.size.height) / 2f - 16f,
                ),
            )

            // Center text: liters
            val litersFormatted = "%,d L".format(liters)
            val litersStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = onSurfaceColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            val litersLayout = textMeasurer.measure(litersFormatted, litersStyle)
            drawText(
                textLayoutResult = litersLayout,
                topLeft = Offset(
                    x = tankLeft + (tankWidth - litersLayout.size.width) / 2f,
                    y = tankTop + (tankHeight - litersLayout.size.height) / 2f + 24f,
                ),
            )
        }
    }
}

private fun DrawScope.drawReferenceLabel(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
) {
    val style = TextStyle(
        fontSize = 11.sp,
        color = color,
    )
    val layout = textMeasurer.measure(text, style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x, y - layout.size.height / 2f),
    )
}
