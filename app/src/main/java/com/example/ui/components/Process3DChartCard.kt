package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ScheduledExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

enum class Process3DMode(val label: String, val icon: ImageVector) {
    HYDRATION_7D("Water", Icons.Default.WaterDrop),
    WORKOUT_REPS_7D("Reps", Icons.Default.FitnessCenter),
    BODY_SYSTEM_3D("Today", Icons.Default.Bolt)
}

enum class CameraAngle3D(val label: String, val depthFactor: Float, val pitchFactor: Float) {
    ISOMETRIC("30°", 0.34f, 0.22f),
    DEEP_3D("45°", 0.48f, 0.32f),
    ELEVATED("Top", 0.24f, 0.38f);

    fun next(): CameraAngle3D = when (this) {
        ISOMETRIC -> DEEP_3D
        DEEP_3D -> ELEVATED
        ELEVATED -> ISOMETRIC
    }
}

data class DayHydrationAndProcessPoint(
    val dayKey: String,
    val shortLabel: String,
    val waterGlasses: Int,
    val waterMl: Int,
    val exercisesDone: Int,
    val totalReps: Int,
    val caloriesBurned: Int,
    val isToday: Boolean
)

/**
 * Clean, minimal 3D Weekly Hydration & Progress Card for the Stats section.
 */
@Composable
fun Process3DChartCard(
    profile: UserProfile,
    schedule: List<ScheduledExercise>,
    allLogs: List<WorkoutLog>,
    onQuickAddWaterToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeMode by remember { mutableStateOf(Process3DMode.HYDRATION_7D) }
    var cameraAngle by remember { mutableStateOf(CameraAngle3D.ISOMETRIC) }
    var selectedDayIndex by remember { mutableIntStateOf(6) }

    val animatedDepth by animateFloatAsState(
        targetValue = cameraAngle.depthFactor,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
        label = "camDepth3d"
    )
    val animatedPitch by animateFloatAsState(
        targetValue = cameraAngle.pitchFactor,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
        label = "camPitch3d"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "liquidWave3D")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase3d"
    )

    val points7Days = remember(profile.waterSlotsMask, profile.equipmentCsv, allLogs) {
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortDayFormat = SimpleDateFormat("EEE", Locale.US)
        val logsByDay = allLogs.groupBy { it.dayKey }
        val waterMap = profile.waterHistoryMap
        val todayGlasses = profile.todayWaterGlassesCount

        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val key = dayKeyFormat.format(cal.time)
            val label = if (daysAgo == 0) "Today" else shortDayFormat.format(cal.time)
            val dayLogs = logsByDay[key].orEmpty()
            val reps = dayLogs.sumOf { it.totalReps }.toInt()
            val cals = dayLogs.sumOf { it.caloriesBurned }.toInt()
            val exCount = dayLogs.size

            val glasses = when {
                daysAgo == 0 -> todayGlasses
                waterMap.containsKey(key) -> waterMap[key] ?: 0
                exCount > 0 -> (4 + (exCount.coerceAtMost(3))).coerceIn(0, 7)
                else -> 0
            }

            DayHydrationAndProcessPoint(
                dayKey = key,
                shortLabel = label,
                waterGlasses = glasses,
                waterMl = glasses * 300,
                exercisesDone = exCount,
                totalReps = reps,
                caloriesBurned = cals,
                isToday = daysAgo == 0
            )
        }
    }

    val targetRatios7Days = points7Days.map { pt ->
        when (activeMode) {
            Process3DMode.HYDRATION_7D -> (pt.waterGlasses / 7f).coerceIn(0.06f, 1f)
            Process3DMode.WORKOUT_REPS_7D -> (pt.totalReps / 120f).coerceIn(
                if (pt.exercisesDone > 0) 0.18f else 0.06f,
                1f
            )
            Process3DMode.BODY_SYSTEM_3D -> (pt.waterGlasses / 7f).coerceIn(0.06f, 1f)
        }
    }

    val animBar0 by animateFloatAsState(targetRatios7Days[0], spring(0.7f, 260f), label = "b0")
    val animBar1 by animateFloatAsState(targetRatios7Days[1], spring(0.7f, 260f), label = "b1")
    val animBar2 by animateFloatAsState(targetRatios7Days[2], spring(0.7f, 260f), label = "b2")
    val animBar3 by animateFloatAsState(targetRatios7Days[3], spring(0.7f, 260f), label = "b3")
    val animBar4 by animateFloatAsState(targetRatios7Days[4], spring(0.7f, 260f), label = "b4")
    val animBar5 by animateFloatAsState(targetRatios7Days[5], spring(0.7f, 260f), label = "b5")
    val animBar6 by animateFloatAsState(targetRatios7Days[6], spring(0.7f, 260f), label = "b6")
    val animatedBarRatios = listOf(animBar0, animBar1, animBar2, animBar3, animBar4, animBar5, animBar6)

    val completedTasks = schedule.count { it.isCompletedToday }
    val totalTasks = schedule.size.coerceAtLeast(1)
    val todayPoint = points7Days.last()
    val proteinDone = schedule.any { it.template.id == "ex_protein_shake_recovery" && it.isCompletedToday }

    val sysHydrationRatio by animateFloatAsState(
        targetValue = (todayPoint.waterGlasses / 7f).coerceIn(0.08f, 1f),
        animationSpec = spring(0.7f, 260f),
        label = "sysHyd"
    )
    val sysSplitRatio by animateFloatAsState(
        targetValue = (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0.08f, 1f),
        animationSpec = spring(0.7f, 260f),
        label = "sysSplit"
    )
    val sysCalorieRatio by animateFloatAsState(
        targetValue = (todayPoint.caloriesBurned / 450f).coerceIn(0.08f, 1f),
        animationSpec = spring(0.7f, 260f),
        label = "sysCal"
    )
    val sysRecoveryRatio by animateFloatAsState(
        targetValue = when {
            proteinDone -> 1f
            profile.wantsProteinShake -> 0.45f
            else -> 0.3f
        },
        animationSpec = spring(0.7f, 260f),
        label = "sysRec"
    )

    val selectedPoint = points7Days.getOrElse(selectedDayIndex.coerceIn(0, 6)) { todayPoint }
    val weeklyTotalGlasses = points7Days.sumOf { it.waterGlasses }
    val weeklyTotalLiters = "%.1f".format(Locale.US, (weeklyTotalGlasses * 0.3f))

    val cyanBase = MaterialTheme.colorScheme.secondary
    val emeraldBase = MaterialTheme.colorScheme.primary
    val amberBase = MaterialTheme.colorScheme.tertiary
    val surfaceLineColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("weekly_hydration_3d_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Clean Title + 3D Angle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "3D Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = cyanBase.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${weeklyTotalLiters}L / 7d",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = cyanBase,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // 1-Click 3D Camera Angle Rotator
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { cameraAngle = cameraAngle.next() }
                        .testTag("rotate_3d_angle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D Angle",
                            tint = emeraldBase,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = cameraAngle.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3D Mode Switcher Row (Water | Reps | Today)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Process3DMode.entries.forEach { mode ->
                    val isSelected = activeMode == mode
                    Surface(
                        color = if (isSelected) {
                            emeraldBase.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) emeraldBase else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { activeMode = mode }
                            .testTag("process_3d_mode_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.label,
                                tint = if (isSelected) emeraldBase else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) emeraldBase else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3D Isometric Stage Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .pointerInput(activeMode) {
                        detectTapGestures { tapOffset ->
                            if (activeMode != Process3DMode.BODY_SYSTEM_3D && size.width > 0) {
                                val slotWidth = size.width / 7f
                                val tappedIdx = (tapOffset.x / slotWidth).toInt().coerceIn(0, 6)
                                selectedDayIndex = tappedIdx
                                if (tappedIdx == 6 && activeMode == Process3DMode.HYDRATION_7D) {
                                    onQuickAddWaterToday()
                                }
                            }
                        }
                    }
                    .testTag("hydration_3d_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val floorY = h * 0.78f
                    val maxBarHeight = h * 0.52f
                    val isoDx = w * 0.028f * (0.5f + animatedDepth)
                    val isoDy = h * 0.065f * (0.5f + animatedPitch)

                    drawIsometricStageGrid(
                        w = w,
                        floorY = floorY,
                        maxBarHeight = maxBarHeight,
                        isoDx = isoDx,
                        isoDy = isoDy,
                        gridColor = surfaceLineColor.copy(alpha = 0.14f),
                        goalColor = cyanBase.copy(alpha = 0.4f)
                    )

                    if (activeMode != Process3DMode.BODY_SYSTEM_3D) {
                        val count = 7
                        val slotW = (w * 0.86f) / count
                        val startX = w * 0.06f
                        val barHalfW = (slotW * 0.28f).coerceAtLeast(8f)

                        for (i in 0 until count) {
                            val cx = startX + slotW * (i + 0.5f)
                            val ratio = animatedBarRatios[i]
                            val barH = (maxBarHeight * ratio).coerceAtLeast(8f)
                            val isSelected = i == selectedDayIndex
                            val isToday = i == 6

                            val baseColor = when (activeMode) {
                                Process3DMode.HYDRATION_7D -> {
                                    if (isToday) cyanBase
                                    else if (isSelected) emeraldBase
                                    else cyanBase.copy(alpha = 0.78f)
                                }
                                Process3DMode.WORKOUT_REPS_7D -> {
                                    if (isToday) emeraldBase
                                    else if (isSelected) amberBase
                                    else emeraldBase.copy(alpha = 0.78f)
                                }
                                else -> cyanBase
                            }

                            val waveOffset = if (ratio > 0.1f) {
                                sin(wavePhase + i * 0.9f) * 3.0f
                            } else 0f

                            drawIsometricPrism3D(
                                cx = cx,
                                floorY = floorY,
                                halfW = barHalfW,
                                height = (barH + waveOffset).coerceAtLeast(6f),
                                isoDx = isoDx,
                                isoDy = isoDy,
                                baseColor = baseColor,
                                highlight = isSelected || isToday
                            )
                        }
                    } else {
                        val systemRatios = listOf(
                            sysHydrationRatio,
                            sysSplitRatio,
                            sysCalorieRatio,
                            sysRecoveryRatio
                        )
                        val systemColors = listOf(
                            cyanBase,
                            emeraldBase,
                            amberBase,
                            Color(0xFFE91E63)
                        )
                        val slotW = (w * 0.84f) / 4f
                        val startX = w * 0.07f
                        val cubeHalfW = (slotW * 0.28f).coerceAtLeast(12f)

                        for (i in 0 until 4) {
                            val cx = startX + slotW * (i + 0.5f)
                            val ratio = systemRatios[i]
                            val cubeH = (maxBarHeight * ratio).coerceAtLeast(12f)
                            val pulse = sin(wavePhase + i * 1.3f) * 3.5f

                            drawIsometricPrism3D(
                                cx = cx,
                                floorY = floorY,
                                halfW = cubeHalfW,
                                height = (cubeH + pulse).coerceAtLeast(10f),
                                isoDx = isoDx * 1.25f,
                                isoDy = isoDy * 1.15f,
                                baseColor = systemColors[i],
                                highlight = true
                            )
                        }
                    }
                }

                // Bottom X-Axis Labels
                if (activeMode != Process3DMode.BODY_SYSTEM_3D) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        points7Days.forEachIndexed { idx, pt ->
                            val isSel = idx == selectedDayIndex
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDayIndex = idx },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (activeMode == Process3DMode.HYDRATION_7D) {
                                        "${pt.waterGlasses}/7"
                                    } else {
                                        "${pt.totalReps}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (pt.isToday || isSel) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = pt.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (pt.isToday || isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pt.isToday || isSel) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    val processLabels = listOf(
                        "Water ${todayPoint.waterGlasses}/7",
                        "Tasks $completedTasks/$totalTasks",
                        "${todayPoint.caloriesBurned} kcal",
                        if (proteinDone) "Shake ✓" else "Recovery"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        processLabels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Selected Day Compact Footer + "+1 Water" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedPoint.shortLabel}: ${selectedPoint.waterGlasses}/7 water • ${selectedPoint.totalReps} reps • ${selectedPoint.caloriesBurned} kcal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = cyanBase.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, cyanBase.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onQuickAddWaterToday)
                        .testTag("quick_add_3d_water_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Add Water",
                            tint = cyanBase,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+1 Water",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = cyanBase
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawIsometricStageGrid(
    w: Float,
    floorY: Float,
    maxBarHeight: Float,
    isoDx: Float,
    isoDy: Float,
    gridColor: Color,
    goalColor: Color
) {
    val leftX = w * 0.05f
    val rightX = w * 0.92f

    val floorPath = Path().apply {
        moveTo(leftX, floorY)
        lineTo(rightX, floorY)
        lineTo(rightX + isoDx, floorY - isoDy)
        lineTo(leftX + isoDx, floorY - isoDy)
        close()
    }
    drawPath(
        path = floorPath,
        color = gridColor.copy(alpha = 0.08f)
    )
    drawPath(
        path = floorPath,
        color = gridColor,
        style = Stroke(width = 1.5f)
    )

    val levels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
    levels.forEach { lv ->
        val y = floorY - maxBarHeight * lv - isoDy
        drawLine(
            color = if (lv == 1.0f) goalColor else gridColor,
            start = Offset(leftX + isoDx, y),
            end = Offset(rightX + isoDx, y),
            strokeWidth = if (lv == 1.0f) 2.2f else 1f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawIsometricPrism3D(
    cx: Float,
    floorY: Float,
    halfW: Float,
    height: Float,
    isoDx: Float,
    isoDy: Float,
    baseColor: Color,
    highlight: Boolean
) {
    val topY = floorY - height
    val leftX = cx - halfW
    val rightX = cx + halfW

    val frontPath = Path().apply {
        moveTo(leftX, floorY)
        lineTo(rightX, floorY)
        lineTo(rightX, topY)
        lineTo(leftX, topY)
        close()
    }
    drawPath(
        path = frontPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = if (highlight) 0.95f else 0.82f),
                baseColor.copy(alpha = if (highlight) 0.65f else 0.45f)
            ),
            startY = topY,
            endY = floorY
        )
    )

    val rightFacePath = Path().apply {
        moveTo(rightX, floorY)
        lineTo(rightX + isoDx, floorY - isoDy)
        lineTo(rightX + isoDx, topY - isoDy)
        lineTo(rightX, topY)
        close()
    }
    drawPath(
        path = rightFacePath,
        color = baseColor.copy(alpha = if (highlight) 0.58f else 0.40f)
    )

    val topCapPath = Path().apply {
        moveTo(leftX, topY)
        lineTo(rightX, topY)
        lineTo(rightX + isoDx, topY - isoDy)
        lineTo(leftX + isoDx, topY - isoDy)
        close()
    }
    drawPath(
        path = topCapPath,
        color = baseColor.copy(alpha = 1f)
    )
    drawPath(
        path = topCapPath,
        color = Color.White.copy(alpha = if (highlight) 0.45f else 0.22f)
    )

    if (highlight) {
        drawPath(
            path = topCapPath,
            color = Color.White.copy(alpha = 0.65f),
            style = Stroke(width = 1.5f)
        )
    }
}
