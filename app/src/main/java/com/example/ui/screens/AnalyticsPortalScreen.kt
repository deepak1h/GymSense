package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.VitalLog
import com.example.data.model.WorkoutLog
import com.example.viewmodel.BioscienceUiState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun AnalyticsPortalScreen(
    profile: UserProfile,
    workoutLogs: List<WorkoutLog>,
    vitalLogs: List<VitalLog>,
    bioscienceState: BioscienceUiState,
    onGenerateBioscienceReport: () -> Unit,
    onLogQuickVitals: (weightKg: Long, restingHr: Long, recoveryScore: Long, notes: String) -> Unit,
    onDeleteWorkoutLog: (String) -> Unit
) {
    var showVitalsDialog by remember { mutableStateOf(false) }

    val totalWorkouts = workoutLogs.size
    val totalSets = workoutLogs.sumOf { it.completedSets }
    val totalReps = workoutLogs.sumOf { it.totalReps }
    val totalCalories = workoutLogs.sumOf { it.caloriesBurned }
    val avgRpe = if (workoutLogs.isNotEmpty()) {
        workoutLogs.map { it.rpeFeedback }.average()
    } else {
        profile.lastFeedbackRpe.toDouble()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("analytics_portal_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LONG-TERM HEALTH & ANALYTICS PORTAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bioscience & Telemetry",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedButton(
                onClick = { showVitalsDialog = true },
                modifier = Modifier.testTag("log_vitals_checkin_button")
            ) {
                Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Vitals")
            }
        }

        // Top KPI Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsKpiCard(
                label = "CURRENT BMI",
                value = "${profile.bmiValue}",
                sub = profile.bmiCategory,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            AnalyticsKpiCard(
                label = "VO2 MAX / RHR",
                value = "${profile.vo2MaxEstimate} mL",
                sub = "${profile.restingHeartRateBpm} bpm RHR",
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            AnalyticsKpiCard(
                label = "AVG RPE / SETS",
                value = "${"%.1f".format(avgRpe)}/10",
                sub = "$totalSets sets ($totalReps reps)",
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        // Gemini Structured Bioscience Intelligence Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101D2C)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Bioscience AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini Bioscience Intelligence",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Analyzes your BMI, TDEE (${profile.estimatedDailyTdeeKcal} kcal), VO2 max & RPE logs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onGenerateBioscienceReport,
                        enabled = bioscienceState !is BioscienceUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("generate_bioscience_report_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze")
                    }
                }

                when (bioscienceState) {
                    is BioscienceUiState.Idle -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap Analyze to generate a structured clinical exercise physiology report tailored to your logged workouts and vitals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is BioscienceUiState.Loading -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Synthesizing metabolic & biomechanics telemetry...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is BioscienceUiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = bioscienceState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is BioscienceUiState.Success -> {
                        val rep = bioscienceState.report
                        Spacer(modifier = Modifier.height(14.dp))
                        ReportSection("METABOLIC & TDEE BALANCE", rep.metabolicSummary)
                        ReportSection("BMI & CARDIORESPIRATORY VITALS", rep.bmiAndVitalAnalysis)
                        ReportSection("HYPERTROPHY & RPE PRESCRIPTION", rep.hypertrophyPrescription)
                        ReportSection("CNS & MYOFIBRILLAR RECOVERY", rep.recoveryProtocol)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "PRIORITY ACTION ITEMS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        rep.actionItems.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Chart 1: Longitudinal Weight & BMI Trajectory Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "WEIGHT & BMI LONGITUDINAL TREND",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Biometric Check-In Trajectory (${vitalLogs.size} logs)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                val points = if (vitalLogs.isNotEmpty()) {
                    vitalLogs.take(10).reversed().map { it.weightKg.toFloat() }
                } else {
                    listOf(profile.weightKg.toFloat(), profile.targetWeightKg.toFloat())
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val minW = (points.minOrNull() ?: 50f) - 4f
                    val maxW = (points.maxOrNull() ?: 90f) + 4f
                    val span = (maxW - minW).coerceAtLeast(5f)

                    val path = Path()
                    points.forEachIndexed { idx, value ->
                        val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * (w - 32f) + 16f
                        val y = h - ((value - minW) / span) * (h - 32f) - 16f
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(
                            color = Color(0xFF00B0FF),
                            radius = 9f,
                            center = Offset(x, y)
                        )
                    }
                    if (points.size > 1) {
                        drawPath(
                            path = path,
                            color = Color(0xFF00E676),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Current: ${profile.weightKg} kg (BMI ${profile.bmiValue}) • Goal: ${profile.targetWeightKg} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Chart 2: Session Calorie & Volume Output Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SESSION OUTPUT & CALORIC EXPENDITURE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalWorkouts Sessions Logged • $totalCalories kcal Total Burn",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (workoutLogs.isEmpty()) {
                    Text(
                        text = "No workouts logged yet. Select an exercise on Today's Schedule to record your first session!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val recentBars = workoutLogs.take(7).reversed()
                    val maxCal = (recentBars.maxOfOrNull { it.caloriesBurned } ?: 200L).coerceAtLeast(100L).toFloat()

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val count = recentBars.size
                        val slotWidth = size.width / count.coerceAtLeast(1)
                        val barWidth = (slotWidth * 0.55f).coerceAtMost(70f)

                        recentBars.forEachIndexed { idx, log ->
                            val ratio = (log.caloriesBurned / maxCal).coerceIn(0.1f, 1f)
                            val barH = ratio * (size.height - 20f)
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val y = size.height - barH

                            drawRoundRect(
                                color = Color(0xFF00E676),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "RECENT WORKOUT LOG HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    workoutLogs.take(8).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.exerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${log.completedSets} sets • ${log.totalReps} reps @ ${log.weightUsedKg} kg • ${log.caloriesBurned} kcal • RPE ${log.rpeFeedback}/10",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (log.id.isNotBlank()) {
                                IconButton(onClick = { onDeleteWorkoutLog(log.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete workout log",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
private fun AnalyticsKpiCard(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportSection(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
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
        title = { Text("Log Biometric Vitals Check-In") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weight: ${weight.roundToInt()} kg")
                Slider(value = weight, onValueChange = { weight = it }, valueRange = 40f..180f)
                Text("Resting Heart Rate: ${hr.roundToInt()} bpm")
                Slider(value = hr, onValueChange = { hr = it }, valueRange = 40f..110f)
                Text("CNS Recovery Readiness: ${recovery.roundToInt()}%")
                Slider(value = recovery, onValueChange = { recovery = it }, valueRange = 20f..100f)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Biometric / Soreness Note") },
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
                Text("Save Check-In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
