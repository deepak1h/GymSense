package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExerciseTemplate
import com.example.data.model.ScheduledExercise
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VoltLime
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSessionSheet(
    scheduledExercise: ScheduledExercise,
    isGirl: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCompleteWorkout: (
        completedSets: Int,
        totalReps: Int,
        weightUsedKg: Int,
        durationSeconds: Int,
        rpeFeedback: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val template = scheduledExercise.template

    var isVideoPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var activeCueIndex by remember { mutableIntStateOf(0) }

    var isTimerRunning by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var restCountdown by remember { mutableIntStateOf(0) }

    val totalTargetSets = scheduledExercise.adjustedSets
    val completedSetsTracker = remember(scheduledExercise) {
        mutableStateListOf<Boolean>().apply {
            repeat(totalTargetSets) { add(false) }
        }
    }
    var weightKg by remember(scheduledExercise) { mutableIntStateOf(scheduledExercise.adjustedWeightKg) }
    var repsPerSet by remember(scheduledExercise) { mutableIntStateOf(scheduledExercise.adjustedReps) }
    var rpeSlider by remember { mutableFloatStateOf(7f) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000L)
            elapsedSeconds += 1
            if (restCountdown > 0) {
                restCountdown -= 1
            }
        }
    }

    LaunchedEffect(isVideoPlaying, template.tutorialSteps.size) {
        if (template.tutorialSteps.isNotEmpty()) {
            while (isVideoPlaying) {
                delay(3200L)
                activeCueIndex = (activeCueIndex + 1) % template.tutorialSteps.size
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = template.muscleGroup.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGirl) Icons.Default.Face3 else Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = template.requiredEquipment,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_session_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Code-Animated Stickman Card (Zero AI credits used)
            StickmanTutorialCard(
                template = template,
                isGirl = isGirl,
                isVideoPlaying = isVideoPlaying,
                playbackSpeed = playbackSpeed,
                activeCueIndex = activeCueIndex,
                onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                onToggleSpeed = {
                    playbackSpeed = when (playbackSpeed) {
                        1.0f -> 0.5f
                        0.5f -> 1.5f
                        else -> 1.0f
                    }
                },
                onSelectCue = { activeCueIndex = it }
            )

            // Compact Live Timer + Rest Bar
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Session timer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = formatDurationMmSs(elapsedSeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (restCountdown > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "REST ${restCountdown}s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { restCountdown = 60 },
                            modifier = Modifier.testTag("start_rest_timer_button")
                        ) {
                            Text("60s Rest")
                        }
                        FilledIconButton(
                            onClick = { isTimerRunning = !isTimerRunning },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Resume",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Icon Steppers for Weight & Reps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepperControlCard(
                    modifier = Modifier.weight(1f),
                    label = if (template.requiredEquipment == "Bodyweight") "EXTRA KG" else "LOAD KG",
                    valueText = if (weightKg == 0) "BW" else "${weightKg}kg",
                    onDecrement = { weightKg = (weightKg - 2).coerceAtLeast(0) },
                    onIncrement = { weightKg = (weightKg + 2).coerceAtMost(400) },
                    decTag = "weight_minus_button",
                    incTag = "weight_plus_button"
                )
                StepperControlCard(
                    modifier = Modifier.weight(1f),
                    label = "REPS / SET",
                    valueText = "$repsPerSet",
                    onDecrement = { repsPerSet = (repsPerSet - 1).coerceAtLeast(1) },
                    onIncrement = { repsPerSet = (repsPerSet + 1).coerceAtMost(100) },
                    decTag = "reps_minus_button",
                    incTag = "reps_plus_button"
                )
            }

            // Horizontal Compact Set Tap Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SETS (${completedSetsTracker.count { it }}/$totalTargetSets)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap set to complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                completedSetsTracker.forEachIndexed { index, isDone ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                completedSetsTracker[index] = !isDone
                                if (!isDone) restCountdown = 60
                            }
                            .testTag("set_row_${index + 1}"),
                        color = if (isDone) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
                                contentDescription = "Set ${index + 1}",
                                tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "S${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Compact RPE Slider
            val rpeInt = rpeSlider.roundToInt()
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "EFFORT (RPE $rpeInt/10)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = when {
                                rpeInt <= 5 -> "+10% Next"
                                rpeInt in 6..8 -> "Optimal"
                                else -> "-10% Deload"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = rpeSlider,
                        onValueChange = { rpeSlider = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("rpe_feedback_slider")
                    )
                }
            }

            val completedCount = completedSetsTracker.count { it }.coerceAtLeast(1)
            val totalRepsDone = completedCount * repsPerSet

            Button(
                onClick = {
                    onCompleteWorkout(
                        completedCount,
                        totalRepsDone,
                        weightKg,
                        elapsedSeconds.coerceAtLeast(30),
                        rpeInt
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("complete_exercise_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "SAVING..." else "DONE • $completedCount SETS ($totalRepsDone REPS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StickmanTutorialCard(
    template: ExerciseTemplate,
    isGirl: Boolean,
    isVideoPlaying: Boolean,
    playbackSpeed: Float,
    activeCueIndex: Int,
    onTogglePlay: () -> Unit,
    onToggleSpeed: () -> Unit,
    onSelectCue: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A111A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color(0xFF080E16))
            ) {
                // Code-Animated Stickman Canvas
                StickmanExerciseCanvas(
                    motionType = template.motionType,
                    isGirl = isGirl,
                    isPlaying = isVideoPlaying,
                    playbackSpeed = playbackSpeed,
                    showGrid = true,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Overlay HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(VoltLime)
                            )
                            Text(
                                text = "${if (isGirl) "GIRL" else "BOY"} STICKMAN • ${template.tempoPrescription}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = Color.White
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { onToggleSpeed() }
                                .testTag("video_speed_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback speed",
                                    tint = VoltLime,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = VoltLime
                                )
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onTogglePlay() }
                                .testTag("video_play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Cue Pill
                if (template.tutorialSteps.isNotEmpty()) {
                    val safeIndex = activeCueIndex.coerceIn(0, template.tutorialSteps.lastIndex)
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${safeIndex + 1}/${template.tutorialSteps.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = VoltLime
                            )
                            Text(
                                text = template.tutorialSteps[safeIndex],
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Compact Step Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                template.tutorialSteps.forEachIndexed { idx, _ ->
                    FilterChip(
                        selected = idx == activeCueIndex,
                        onClick = { onSelectCue(idx) },
                        label = { Text("Cue ${idx + 1}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onSelectCue(0) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart cues",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperControlCard(
    modifier: Modifier = Modifier,
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decTag: String,
    incTag: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag(decTag),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease $label",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold
                )
                FilledIconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag(incTag),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase $label",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatDurationMmSs(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
