package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.DynoPoint
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.HondaRed
import com.example.ui.theme.HondaRedBright
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

fun heatMapColor(value: Double, minVal: Double, maxVal: Double): Color {
    val t = ((value - minVal) / max(1.0, maxVal - minVal)).toFloat().coerceIn(0f, 1f)
    return when {
        t < 0.33f -> {
            val k = t / 0.33f
            Color(
                red = 0.02f + k * 0.04f,
                green = 0.71f + k * 0.09f,
                blue = 0.83f - k * 0.33f
            )
        }
        t < 0.66f -> {
            val k = (t - 0.33f) / 0.33f
            Color(
                red = 0.06f + k * 0.90f,
                green = 0.80f - k * 0.18f,
                blue = 0.50f - k * 0.45f
            )
        }
        else -> {
            val k = (t - 0.66f) / 0.34f
            Color(
                red = 0.96f - k * 0.06f,
                green = 0.62f - k * 0.50f,
                blue = 0.05f + k * 0.23f
            )
        }
    }
}

@Composable
fun Curve2DGraph(
    matrix: List<List<Double>>,
    rpmAxis: List<Int>,
    isFuel: Boolean,
    selectedRow: Int,
    modifier: Modifier = Modifier
) {
    val minVal = if (isFuel) 50.0 else 4.0
    val maxVal = if (isFuel) 130.0 else 42.0
    val rowsToShow = listOf(0, 5, 10, 15, selectedRow).distinct()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height
            // Grid lines
            for (i in 0..4) {
                val y = h * (i / 4f)
                drawLine(CarbonBorder, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }
            for (c in 0..15 step 3) {
                val x = w * (c / 15f)
                drawLine(CarbonBorder, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
            }

            rowsToShow.forEachIndexed { idx, rowIdx ->
                val rowData = matrix.getOrNull(rowIdx) ?: return@forEachIndexed
                val path = Path()
                rowData.forEachIndexed { colIdx, value ->
                    val x = w * (colIdx / 15f)
                    val norm = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                    val y = h - (norm * h)
                    if (colIdx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val lineColor = when {
                    rowIdx == selectedRow -> HondaRedBright
                    idx == 0 -> TelemetryCyan
                    idx == 1 -> EmeraldOk
                    else -> AmberWarn.copy(alpha = 0.7f)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = if (rowIdx == selectedRow) 3.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${rpmAxis.firstOrNull() ?: 900} RPM", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Kurva TPS Terpilih: Baris #${selectedRow + 1}", style = MaterialTheme.typography.labelSmall, color = HondaRedBright)
            Text("${rpmAxis.lastOrNull() ?: 12500} RPM", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun Surface3DGraph(
    matrix: List<List<Double>>,
    isFuel: Boolean,
    modifier: Modifier = Modifier
) {
    var yawDeg by remember { mutableFloatStateOf(-38f) }
    var pitchDeg by remember { mutableFloatStateOf(28f) }
    val minVal = if (isFuel) 50.0 else 4.0
    val maxVal = if (isFuel) 130.0 else 42.0

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yawDeg = (yawDeg + dragAmount.x * 0.4f).coerceIn(-85f, 85f)
                        pitchDeg = (pitchDeg + dragAmount.y * 0.35f).coerceIn(10f, 70f)
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height * 0.56f
            val scale = min(size.width, size.height) * 0.044f
            val yaw = yawDeg * (PI / 180.0)
            val pitch = pitchDeg * (PI / 180.0)

            fun project(r: Int, c: Int, v: Double): Offset {
                val nx = c - 7.5
                val ny = r - 7.5
                val nz = ((v - minVal) / (maxVal - minVal)).coerceIn(0.0, 1.2) * 8.0 - 3.0

                val rx = nx * cos(yaw) - ny * sin(yaw)
                val ry = nx * sin(yaw) + ny * cos(yaw)
                val py = ry * cos(pitch) - nz * sin(pitch)

                return Offset(
                    cx + (rx * scale).toFloat(),
                    cy + (py * scale * 0.72).toFloat()
                )
            }

            // Draw mesh rows and columns
            for (r in 0 until 16) {
                for (c in 0 until 16) {
                    val v = matrix[r][c]
                    val p0 = project(r, c, v)
                    val col = heatMapColor(v, minVal, maxVal)
                    if (c < 15) {
                        val pRight = project(r, c + 1, matrix[r][c + 1])
                        drawLine(col, p0, pRight, strokeWidth = 1.4.dp.toPx())
                    }
                    if (r < 15) {
                        val pDown = project(r + 1, c, matrix[r + 1][c])
                        drawLine(col, p0, pDown, strokeWidth = 1.4.dp.toPx())
                    }
                    drawCircle(col, radius = 2.dp.toPx(), center = p0)
                }
            }
        }
        Text(
            text = "Geser grafik untuk memutar sudut kamera 3D (Yaw ${yawDeg.toInt()}° / Pitch ${pitchDeg.toInt()}°)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun DynoCrossoverGraph(
    points: List<DynoPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val maxHp = maxOf(points.maxOf { it.tunedHp }, points.maxOf { it.stockHp }, 10.0) * 1.15
    val maxTq = maxOf(points.maxOf { it.tunedTorqueNm }, points.maxOf { it.stockTorqueNm }, 10.0) * 1.15
    val scaleMax = maxOf(maxHp, maxTq)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height
            for (i in 0..4) {
                val y = h * (i / 4f)
                drawLine(CarbonBorder, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            fun buildCurve(selector: (DynoPoint) -> Double): Path {
                val p = Path()
                points.forEachIndexed { idx, pt ->
                    val x = w * (idx.toFloat() / maxOf(1, points.size - 1))
                    val norm = (selector(pt) / scaleMax).toFloat().coerceIn(0f, 1f)
                    val y = h - norm * h
                    if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                return p
            }

            drawPath(buildCurve { it.stockHp }, color = TelemetryCyan.copy(alpha = 0.45f), style = Stroke(2.dp.toPx()))
            drawPath(buildCurve { it.stockTorqueNm }, color = AmberWarn.copy(alpha = 0.45f), style = Stroke(2.dp.toPx()))
            drawPath(buildCurve { it.tunedHp }, color = HondaRedBright, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            drawPath(buildCurve { it.tunedTorqueNm }, color = EmeraldOk, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("— Tuned HP", style = MaterialTheme.typography.labelSmall, color = HondaRedBright)
            Text("— Tuned Nm", style = MaterialTheme.typography.labelSmall, color = EmeraldOk)
            Text("— Stock HP/Nm", style = MaterialTheme.typography.labelSmall, color = TelemetryCyan)
        }
    }
}
