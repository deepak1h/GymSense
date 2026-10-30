package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ScheduledExercise
import com.example.data.model.UserProfile
import com.example.data.model.VitalLog
import com.example.data.model.WorkoutLog
import com.example.ui.components.Process3DChartCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.BioscienceUiState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private data class MuscleGroupStat(
    val muscleGroup: String,
    val totalSets: Long,
    val totalReps: Long,
    val totalVolumeKg: Long,
    val shareOfSets: Float
)

private data class ExercisePrStat(
    val exerciseName: String,
    val equipment: String,
    val maxWeightKg: Long,
    val estimatedOneRepMaxKg: Int,
    val bestReps: Long,
    val sessionsCount: Int
)

@Composable
fun AnalyticsPortalScreen(
    profile: UserProfile,
    schedule: List<ScheduledExercise> = emptyList(),
    workoutLogs: List<WorkoutLog>,
    vitalLogs: List<VitalLog>,
    bioscienceState: BioscienceUiState,
    onGenerateBioscienceReport: () -> Unit,
    onQuickAddWaterToday: () -> Unit = {},
    onLogQuickVitals: (weightKg: Long, restingHr: Long, recoveryScore: Long, notes: String) -> Unit,
    onDeleteWorkoutLog: (String) -> Unit
) {
    var showVitalsDialog by remember { mutableStateOf(false) }
    var showAllSessionLogs by remember { mutableStateOf(false) }
    var showAllPrRecords by remember { mutableStateOf(false) }

    val totalWorkouts = workoutLogs.size
    val totalSets = workoutLogs.sumOf { it.completedSets }
    val totalReps = workoutLogs.sumOf { it.totalReps }
    val totalCalories = workoutLogs.sumOf { it.caloriesBurned }
    val totalVolumeKg = workoutLogs.sumOf { log ->
        log.totalReps * log.weightUsedKg.coerceAtLeast(1L)
    }
    val totalDurationMinutes = (workoutLogs.sumOf { it.durationSeconds } / 60.0).roundToInt()
    val avgDurationMinutes = if (totalWorkouts > 0) (totalDurationMinutes / totalWorkouts).coerceAtLeast(1) else 0
    val activeDaysCount = workoutLogs.map { it.dayKey }.distinct().size
    val latestRecoveryScore = vitalLogs.firstOrNull()?.recoveryScore ?: 85L
    val calorieGoalProgress = (totalCalories.toFloat() / profile.targetCaloriesPerWeek.coerceAtLeast(500L).toFloat())
        .coerceIn(0f, 1f)

    val avgRpe = if (workoutLogs.isNotEmpty()) {
        workoutLogs.map { it.rpeFeedback }.average()
    } else {
        profile.lastFeedbackRpe.toDouble()
    }

    val muscleGroupStats = remember(workoutLogs) {
        val nonRecovery = workoutLogs.filter { !it.muscleGroup.equals("Recovery", ignoreCase = true) }
        val totalMuscleSets = nonRecovery.sumOf { it.completedSets }.coerceAtLeast(1L).toFloat()
        nonRecovery
            .groupBy { it.muscleGroup.ifBlank { "Full Body" } }
            .map { (group, logs) ->
                val sets = logs.sumOf { it.completedSets }
                val reps = logs.sumOf { it.totalReps }
                val vol = logs.sumOf { it.totalReps * it.weightUsedKg.coerceAtLeast(1L) }
                MuscleGroupStat(
                    muscleGroup = group,
                    totalSets = sets,
                    totalReps = reps,
                    totalVolumeKg = vol,
                    shareOfSets = (sets / totalMuscleSets).coerceIn(0.05f, 1f)
                )
            }
            .sortedByDescending { it.totalSets }
    }

    val exercisePrStats = remember(workoutLogs) {
        workoutLogs
            .filter { !it.muscleGroup.equals("Recovery", ignoreCase = true) }
            .groupBy { it.exerciseName.ifBlank { "Workout" } }
            .map { (name, logs) ->
                val bestLog = logs.maxByOrNull { it.weightUsedKg * 1000L + it.totalReps } ?: logs.first()
                val repsPerSet = (bestLog.totalReps.toFloat() / bestLog.completedSets.coerceAtLeast(1L)).coerceIn(1f, 20f)
                val est1Rm = if (bestLog.weightUsedKg > 0L) {
                    (bestLog.weightUsedKg * (1f + repsPerSet / 30f)).roundToInt()
                } else {
                    0
                }
                ExercisePrStat(
                    exerciseName = name,
                    equipment = bestLog.equipmentUsed.ifBlank { "Bodyweight" },
                    maxWeightKg = bestLog.weightUsedKg,
                    estimatedOneRepMaxKg = est1Rm,
                    bestReps = logs.maxOf { it.totalReps },
                    sessionsCount = logs.size
                )
            }
            .sortedWith(compareByDescending<ExercisePrStat> { it.estimatedOneRepMaxKg }.thenByDescending { it.bestReps })
    }

    val topPr = exercisePrStats.firstOrNull()
    val cardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("analytics_portal_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Minimal Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$activeDaysCount active days • $totalWorkouts workouts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { showVitalsDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("log_vitals_checkin_button")
            ) {
                Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Vitals", fontWeight = FontWeight.Bold)
            }
        }

        // 2. Top 3 Vitals Row (Uniform Height & Crisp Alignment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsKpiCard(
                label = "BMI",
                value = "${profile.bmiValue}",
                sub = profile.bmiCategory,
                accent = primaryColor,
                modifier = Modifier.weight(1f)
            )
            AnalyticsKpiCard(
                label = "HEART",
                value = "${profile.restingHeartRateBpm} bpm",
                sub = "VO2 ${profile.vo2MaxEstimate}",
                accent = secondaryColor,
                modifier = Modifier.weight(1f)
            )
            AnalyticsKpiCard(
                label = "EFFORT",
                value = "${"%.1f".format(avgRpe)}/10",
                sub = "$totalSets sets",
                accent = tertiaryColor,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Overview Card (Symmetric 2x2 Grid + Weekly Burn Bar)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Readiness $latestRecoveryScore%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Symmetric Row 1: Volume | Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DbInsightTile(
                        icon = Icons.Default.FitnessCenter,
                        title = "Volume",
                        value = if (totalVolumeKg >= 1000L) {
                            "${"%.1f".format(totalVolumeKg / 1000.0)}t"
                        } else {
                            "$totalVolumeKg kg"
                        },
                        subtitle = "$totalReps reps • $totalSets sets",
                        accent = primaryColor,
                        modifier = Modifier.weight(1f)
                    )
                    DbInsightTile(
                        icon = Icons.Default.Timer,
                        title = "Active Time",
                        value = "$totalDurationMinutes min",
                        subtitle = if (totalWorkouts > 0) "Avg $avgDurationMinutes min" else "No sessions yet",
                        accent = secondaryColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Symmetric Row 2: Best Lift | Daily Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DbInsightTile(
                        icon = Icons.Default.EmojiEvents,
                        title = "Best Lift",
                        value = when {
                            topPr != null && topPr.maxWeightKg > 0 -> "${topPr.maxWeightKg} kg"
                            topPr != null -> "${topPr.bestReps} reps"
                            else -> "—"
                        },
                        subtitle = topPr?.exerciseName ?: "Complete a set",
                        accent = tertiaryColor,
                        modifier = Modifier.weight(1f)
                    )
                    DbInsightTile(
                        icon = Icons.Default.WaterDrop,
                        title = "Water & TDEE",
                        value = "${profile.todayWaterGlassesCount}/7 cups",
                        subtitle = "${profile.estimatedDailyTdeeKcal} kcal/day",
                        accent = secondaryColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Weekly Burn Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Weekly Burn",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "$totalCalories / ${profile.targetCaloriesPerWeek} kcal",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { calorieGoalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 4. 3D Progress & Hydration Card
        Process3DChartCard(
            profile = profile,
            schedule = schedule,
            allLogs = workoutLogs,
            onQuickAddWaterToday = onQuickAddWaterToday
        )

        // 5. Muscle Split Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Muscle Split",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalSets sets",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (muscleGroupStats.isEmpty()) {
                    Text(
                        text = "Complete a workout on Today's tab to see your muscle split.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    muscleGroupStats.forEach { stat ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stat.muscleGroup,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${stat.totalSets} sets • ${stat.totalReps} reps (${(stat.shareOfSets * 100).roundToInt()}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { stat.shareOfSets },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = secondaryColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 6. Personal Bests (PRs) Card with Show More / Show Less
        if (exercisePrStats.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Personal Bests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${exercisePrStats.size} exercises",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val visiblePrs = if (showAllPrRecords) exercisePrStats else exercisePrStats.take(3)
                    visiblePrs.forEach { pr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pr.exerciseName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pr.equipment} • Best ${pr.bestReps} reps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (pr.maxWeightKg > 0L) {
                                        "${pr.maxWeightKg} kg (1RM ~${pr.estimatedOneRepMaxKg}kg)"
                                    } else {
                                        "${pr.bestReps} reps"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (exercisePrStats.size > 3) {
                        TextButton(
                            onClick = { showAllPrRecords = !showAllPrRecords },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_show_more_prs_button")
                        ) {
                            Icon(
                                imageVector = if (showAllPrRecords) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAllPrRecords) {
                                    "Show Less"
                                } else {
                                    "Show More (${exercisePrStats.size - 3})"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 7. Recent Workouts Card (With Show More / Show Less Button)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Workouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalWorkouts done • $totalCalories kcal",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (workoutLogs.isEmpty()) {
                    Text(
                        text = "No workouts logged yet. Start an exercise on the Today tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val recentBars = workoutLogs.take(7).reversed()
                    val maxCal = (recentBars.maxOfOrNull { it.caloriesBurned } ?: 200L).coerceAtLeast(100L).toFloat()

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                    ) {
                        val count = recentBars.size
                        val slotWidth = size.width / count.coerceAtLeast(1)
                        val barWidth = (slotWidth * 0.52f).coerceAtMost(64f)

                        recentBars.forEachIndexed { idx, log ->
                            val ratio = (log.caloriesBurned / maxCal).coerceIn(0.12f, 1f)
                            val barH = ratio * (size.height - 12f)
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val y = size.height - barH

                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }

                    val visibleSessionLogs = if (showAllSessionLogs) {
                        workoutLogs
                    } else {
                        workoutLogs.take(3)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleSessionLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.exerciseName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${log.completedSets} sets • ${log.totalReps} reps @ ${log.weightUsedKg}kg • ${log.caloriesBurned} kcal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (log.id.isNotBlank()) {
                                    IconButton(
                                        onClick = { onDeleteWorkoutLog(log.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete workout log",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (workoutLogs.size > 3) {
                        FilledTonalButton(
                            onClick = { showAllSessionLogs = !showAllSessionLogs },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_show_more_workout_logs_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (showAllSessionLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAllSessionLogs) {
                                    "Show Less"
                                } else {
                                    "Show More (${workoutLogs.size - 3})"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 8. Weight Trend Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weight Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${profile.weightKg} kg → Goal ${profile.targetWeightKg} kg",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                val points = if (vitalLogs.isNotEmpty()) {
                    vitalLogs.take(10).reversed().map { it.weightKg.toFloat() }
                } else {
                    listOf(profile.weightKg.toFloat(), profile.targetWeightKg.toFloat())
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val minW = (points.minOrNull() ?: 50f) - 4f
                    val maxW = (points.maxOrNull() ?: 90f) + 4f
                    val span = (maxW - minW).coerceAtLeast(5f)

                    val path = Path()
                    points.forEachIndexed { idx, value ->
                        val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * (w - 32f) + 16f
                        val y = h - ((value - minW) / span) * (h - 28f) - 14f
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(
                            color = secondaryColor,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                    if (points.size > 1) {
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        // 9. Clean AI Insights Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
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
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "AI Summary",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "AI Coach Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personalized tips from your stats",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onGenerateBioscienceReport,
                        enabled = bioscienceState !is BioscienceUiState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("generate_bioscience_report_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Analyze", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                when (bioscienceState) {
                    is BioscienceUiState.Idle -> Unit
                    is BioscienceUiState.Loading -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analyzing your workouts...",
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryColor
                            )
                        }
                    }
                    is BioscienceUiState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bioscienceState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is BioscienceUiState.Success -> {
                        val rep = bioscienceState.report
                        Spacer(modifier = Modifier.height(10.dp))
                        ReportSection("Calories & Metabolism", rep.metabolicSummary)
                        ReportSection("Vitals & Heart", rep.bmiAndVitalAnalysis)
                        ReportSection("Training Advice", rep.hypertrophyPrescription)
                        ReportSection("Recovery", rep.recoveryProtocol)
                        if (rep.actionItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Next Steps",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            rep.actionItems.forEach { item ->
                                Text(
                                    text = "• $item",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showVitalsDialog) {
        QuickVitalsDialog(
            initialWeightKg = profile.weightKg,
            initialRestingHr = profile.restingHeartRateBpm,
            onDismiss = { showVitalsDialog = false },
            onConfirm = { w, hr, rec, notes ->
                showVitalsDialog = false
                onLogQuickVitals(w, hr, rec, notes)
            }
        )
    }
}

@Composable
private fun DbInsightTile(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReportSection(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QuickVitalsDialog(
    initialWeightKg: Long,
    initialRestingHr: Long,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Long, restingHr: Long, recoveryScore: Long, notes: String) -> Unit
) {
    var weight by remember { mutableFloatStateOf(initialWeightKg.toFloat()) }
    var hr by remember { mutableFloatStateOf(initialRestingHr.toFloat()) }
    var recovery by remember { mutableFloatStateOf(85f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Vitals", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Weight: ${weight.roundToInt()} kg", style = MaterialTheme.typography.labelLarge)
                Slider(value = weight, onValueChange = { weight = it }, valueRange = 40f..180f)
                Text("Resting HR: ${hr.roundToInt()} bpm", style = MaterialTheme.typography.labelLarge)
                Slider(value = hr, onValueChange = { hr = it }, valueRange = 40f..110f)
                Text("Readiness: ${recovery.roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                Slider(value = recovery, onValueChange = { recovery = it }, valueRange = 20f..100f)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        weight.roundToLong(),
                        hr.roundToLong(),
                        recovery.roundToLong(),
                        notes
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
