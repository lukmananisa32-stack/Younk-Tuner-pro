package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.HondaRed
import com.example.ui.theme.HondaRedBright
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun TachometerGauge(
    rpm: Int,
    maxRpm: Int,
    redlineRpm: Int,
    speedKmh: Int,
    gear: String,
    afr: Double,
    modifier: Modifier = Modifier
) {
    val safeMax = maxOf(10000, maxRpm)
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.toFloat().coerceIn(0f, safeMax.toFloat()),
        animationSpec = tween(durationMillis = 110),
        label = "rpmGauge"
    )
    val ratio = animatedRpm / safeMax.toFloat()
    val isShiftLight = rpm >= (redlineRpm * 0.90f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height * 1.15f) * 0.86f
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f + 10.dp.toPx()
            )
            val arcSize = Size(diameter, diameter)
            val strokeWidth = 18.dp.toPx()

            // Base Track (135 deg to +270 deg sweep)
            drawArc(
                color = CarbonBorder,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Redline zone
            val redStartRatio = (redlineRpm.toFloat() / safeMax).coerceIn(0.6f, 0.95f)
            drawArc(
                color = HondaRed.copy(alpha = 0.28f),
                startAngle = 135f + 270f * redStartRatio,
                sweepAngle = 270f * (1f - redStartRatio),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            // Active RPM Sweep
            val activeColor = when {
                rpm >= redlineRpm -> HondaRedBright
                rpm >= redlineRpm * 0.85 -> AmberWarn
                else -> TelemetryCyan
            }
            drawArc(
                color = activeColor,
                startAngle = 135f,
                sweepAngle = 270f * ratio,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Tick Marks & Needle
            val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
            val radius = diameter / 2f
            val ticks = 12
            for (i in 0..ticks) {
                val angleDeg = 135f + (270f / ticks) * i
                val rad = angleDeg * (PI / 180.0)
                val innerR = radius - 22.dp.toPx()
                val outerR = radius - 14.dp.toPx()
                drawLine(
                    color = if (i.toFloat() / ticks >= redStartRatio) HondaRedBright else TextSecondary,
                    start = Offset(
                        center.x + (innerR * cos(rad)).toFloat(),
                        center.y + (innerR * sin(rad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (outerR * cos(rad)).toFloat(),
                        center.y + (outerR * sin(rad)).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Needle tip indicator
            val needleAngle = (135f + 270f * ratio) * (PI / 180.0)
            val needleLen = radius - 26.dp.toPx()
            drawLine(
                color = HondaRedBright,
                start = center,
                end = Offset(
                    center.x + (needleLen * cos(needleAngle)).toFloat(),
                    center.y + (needleLen * sin(needleAngle)).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = CarbonElevated, radius = 10.dp.toPx(), center = center)
            drawCircle(color = HondaRed, radius = 5.dp.toPx(), center = center)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 66.dp)
        ) {
            if (isShiftLight) {
                Surface(
                    color = HondaRed,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "SHIFT!",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = rpm.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Color.White
            )
            Text(
                text = "RPM",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Surface(
                    color = CarbonElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$speedKmh km/h",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = TelemetryCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = CarbonElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "G:$gear",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = CarbonElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "AFR $afr",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = EmeraldOk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
