package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExerciseCatalog
import com.example.data.model.ScheduledExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutSplitDay
import com.example.ui.components.StickmanExerciseCanvas
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VoltLime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    profile: UserProfile,
    schedule: List<ScheduledExercise>,
    allLogs: List<WorkoutLog>,
    todayExtraIds: List<String>,
    isGeneratingAiSchedule: Boolean,
    onSelectExercise: (ScheduledExercise) -> Unit,
    onToggleGender: (Boolean) -> Unit,
    onToggleWeek1To2: (Boolean) -> Unit,
    onSelectSplit: (WorkoutSplitDay) -> Unit,
    onShiftMissedDay: (onResultPopup: (String) -> Unit) -> Unit,
    onRequestAiSchedule: () -> Unit,
    onToggleExtraExercise: (String) -> Unit,
    onOpenCustomScheduleBuilder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = schedule.count { it.isCompletedToday }
    val totalCount = schedule.size.coerceAtLeast(1)
    val completionRatio by animateFloatAsState(
        targetValue = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(550),
        label = "todayProgress"
    )
    val resolvedSplit = ExerciseCatalog.resolveTodaySplit(profile)
    val totalLifetimeReps = allLogs.sumOf { it.totalReps }

    var showShiftConfirmDialog by remember { mutableStateOf(false) }
    var shiftResultPopupMessage by remember { mutableStateOf<String?>(null) }
    var showAddExtraExerciseSheet by remember { mutableStateOf(false) }

    // 1. Shift +1 Day Confirmation Popup Dialog
    if (showShiftConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showShiftConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.EventRepeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            title = {
                Text(
                    text = "Shift Schedule +1 Day?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Missed a workout day or resting today? Shifting +1 day moves your split rotation forward so you never lose your place."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShiftConfirmDialog = false
                        onShiftMissedDay { msg ->
                            shiftResultPopupMessage = msg
                        }
                    },
                    modifier = Modifier.testTag("confirm_shift_day_button")
                ) {
                    Text("Shift +1 Day")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Shift Result Popup Dialog
    if (shiftResultPopupMessage != null) {
        AlertDialog(
            onDismissRequest = { shiftResultPopupMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VoltLime
                )
            },
            title = { Text("Schedule Shifted!", fontWeight = FontWeight.Bold) },
            text = { Text(shiftResultPopupMessage ?: "") },
            confirmButton = {
                Button(onClick = { shiftResultPopupMessage = null }) {
                    Text("Got It")
                }
            }
        )
    }

    // 3. Bottom Sheet to Choose & Add Extra Exercises from All Catalog Exercises
    if (showAddExtraExerciseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddExtraExerciseSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ExtraExercisePickerSheetContent(
                isGirl = profile.isGirl,
                scheduledIds = schedule.map { it.template.id }.toSet(),
                extraIds = todayExtraIds.toSet(),
                onToggleExercise = onToggleExtraExercise,
                onClose = { showAddExtraExerciseSheet = false }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_schedule_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. TODAY'S SCHEDULE HERO CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF132235), Color(0xFF0B131E))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { completionRatio },
                                        modifier = Modifier.fillMaxSize(),
                                        color = VoltLime,
                                        trackColor = Color.White.copy(alpha = 0.12f),
                                        strokeWidth = 5.dp
                                    )
                                    Text(
                                        text = "$completedCount/${schedule.size}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = VoltLime
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = VoltLime.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "TODAY",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily
                                                ),
                                                color = VoltLime,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (profile.missedDaysOffset > 0) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "+${profile.missedDaysOffset}d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = resolvedSplit.shortTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = resolvedSplit.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9AB0C7),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 3 SPACIOUS DEDICATED ACTION BOXES: Shift +1 Day (with Popup), My Plan, GymBae AI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dedicated Box 1: Shift +1 Day (Opens Popup Message)
                            ActionFeatureBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("missed_day_shift_button"),
                                icon = Icons.Default.EventRepeat,
                                title = "Shift +1d",
                                subtitle = "Missed day",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onClick = { showShiftConfirmDialog = true }
                            )

                            // Dedicated Box 2: Build My Own Schedule
                            ActionFeatureBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_custom_schedule_button"),
                                icon = Icons.Default.EditCalendar,
                                title = "My Plan",
                                subtitle = "Pick exercises",
                                accentColor = VoltLime,
                                onClick = onOpenCustomScheduleBuilder
                            )

                            // Dedicated Box 3: GymBae AI Auto-Plan
                            ActionFeatureBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_build_schedule_button"),
                                icon = Icons.Default.AutoAwesome,
                                title = if (isGeneratingAiSchedule) "Wait..." else "GymBae",
                                subtitle = "AI routine",
                                accentColor = Color(0xFFFFB300),
                                onClick = { if (!isGeneratingAiSchedule) onRequestAiSchedule() }
                            )
                        }

                        // Compact Boy/Girl + Wk 1-2 + Local Reps Saved Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = profile.isGirl,
                                onClick = { onToggleGender(!profile.isGirl) },
                                label = { Text(if (profile.isGirl) "Girl" else "Boy") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (profile.isGirl) Icons.Default.Face3 else Icons.Default.Face,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6EC7).copy(alpha = 0.22f),
                                    selectedLabelColor = Color(0xFFFF9CE3),
                                    selectedLeadingIconColor = Color(0xFFFF6EC7)
                                ),
                                modifier = Modifier.testTag("toggle_gender_chip")
                            )

                            FilterChip(
                                selected = profile.isWeek1To2Warmup,
                                onClick = { onToggleWeek1To2(!profile.isWeek1To2Warmup) },
                                label = { Text("Wk 1-2") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltLime.copy(alpha = 0.22f),
                                    selectedLabelColor = VoltLime,
                                    selectedLeadingIconColor = VoltLime
                                ),
                                modifier = Modifier.testTag("toggle_week1to2_chip")
                            )

                            CompactIconPill(
                                icon = Icons.Default.FitnessCenter,
                                text = "$totalLifetimeReps reps saved",
                                tint = VoltLime
                            )

                            CompactIconPill(
                                icon = Icons.Default.MonitorWeight,
                                text = "BMI ${profile.bmiValue}",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // 2. SPLIT SELECTOR CHIPS (Short Labels)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkoutSplitDay.entries.forEach { split ->
                    val isSelected = if (split == WorkoutSplitDay.AUTO) {
                        profile.splitOverrideCode == "AUTO" && !profile.isWeek1To2Warmup
                    } else if (split == WorkoutSplitDay.WARMUP_LIGHT) {
                        profile.isWeek1To2Warmup || profile.splitOverrideCode == WorkoutSplitDay.WARMUP_LIGHT.code
                    } else {
                        profile.splitOverrideCode == split.code && !profile.isWeek1To2Warmup
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSplit(split) },
                        label = { Text(split.shortTitle, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconForSplit(split),
                                contentDescription = split.shortTitle,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("split_chip_${split.code}")
                    )
                }
            }
        }

        // 3. TODAY'S EXERCISES LIST
        items(schedule, key = { it.template.id }) { item ->
            ScheduledExerciseCard(
                item = item,
                isGirl = profile.isGirl,
                onClick = { onSelectExercise(item) }
            )
        }

        // 4. ADD EXTRA EXERCISE CARD AT THE BOTTOM OF TODAY'S SPLIT PAGE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddExtraExerciseSheet = true }
                    .border(
                        width = 1.dp,
                        color = VoltLime.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("add_extra_exercise_button"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(VoltLime.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Extra Exercise",
                                tint = VoltLime
                            )
                        }
                        Column {
                            Text(
                                text = "Add Extra Exercise Today",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pick from all ${ExerciseCatalog.allExercises.size} gym & bodyweight exercises",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = VoltLime.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+ ADD",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = VoltLime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionFeatureBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            ),
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA2B6CC),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExtraExercisePickerSheetContent(
    isGirl: Boolean,
    scheduledIds: Set<String>,
    extraIds: Set<String>,
    onToggleExercise: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "Bodyweight", "Legs", "Back", "Push", "Core")

    val filteredList = remember(selectedFilter) {
        ExerciseCatalog.allExercises.filter { ex ->
            when (selectedFilter) {
                "All" -> true
                "Bodyweight" -> ex.requiredEquipment.equals("Bodyweight", ignoreCase = true)
                "Legs" -> WorkoutSplitDay.LEGS_GLUTES in ex.splitTags
                "Back" -> WorkoutSplitDay.BACK_BICEPS in ex.splitTags
                "Push" -> WorkoutSplitDay.PUSH_DAY in ex.splitTags
                "Core" -> ex.muscleGroup.contains("Core", ignoreCase = true) || ex.isWarmupOrBeginnerFriendly
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Add Extra Exercise to Today",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any exercise to add or remove from today's schedule",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedFilter == cat,
                    onClick = { selectedFilter = cat },
                    label = { Text(cat) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { ex ->
                val isAlreadyInSchedule = ex.id in scheduledIds || ex.id in extraIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExercise(ex.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlreadyInSchedule) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF09111B)),
                            contentAlignment = Alignment.Center
                        ) {
                            StickmanExerciseCanvas(
                                motionType = ex.motionType,
                                isGirl = isGirl,
                                isPlaying = true,
                                showGrid = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ex.requiredEquipment} • ${ex.muscleGroup} • ${ex.baseSets}×${ex.baseRepsPerSet}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = if (isAlreadyInSchedule) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                            contentDescription = if (isAlreadyInSchedule) "Added" else "Add",
                            tint = if (isAlreadyInSchedule) VoltLime else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactIconPill(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.07f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = Color.White
            )
        }
    }
}

private fun iconForSplit(split: WorkoutSplitDay): ImageVector {
    return when (split) {
        WorkoutSplitDay.AUTO -> Icons.Default.AutoAwesome
        WorkoutSplitDay.WARMUP_LIGHT -> Icons.Default.SelfImprovement
        WorkoutSplitDay.LEGS_GLUTES -> Icons.Default.DirectionsRun
        WorkoutSplitDay.BACK_BICEPS -> Icons.Default.FitnessCenter
        WorkoutSplitDay.PUSH_DAY -> Icons.Default.Bolt
        WorkoutSplitDay.COMBO_SPLIT -> Icons.Default.LocalFireDepartment
        WorkoutSplitDay.ZERO_EQUIP -> Icons.Default.AccessibilityNew
        WorkoutSplitDay.CUSTOM_MY_PLAN -> Icons.Default.EditCalendar
    }
}

@Composable
private fun ScheduledExerciseCard(
    item: ScheduledExercise,
    isGirl: Boolean,
    onClick: () -> Unit
) {
    val template = item.template
    val isNoEquip = template.requiredEquipment.equals("Bodyweight", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("exercise_card_${template.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompletedToday) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF09111B))
                    .border(
                        width = 1.dp,
                        color = if (item.isCompletedToday) {
                            VoltLime.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                StickmanExerciseCanvas(
                    motionType = template.motionType,
                    isGirl = isGirl,
                    isPlaying = !item.isCompletedToday,
                    playbackSpeed = 1.0f,
                    showGrid = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = template.muscleGroup.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (isNoEquip) {
                            Color(0xFF4CD964).copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = if (isNoEquip) "NO-EQUIP" else template.requiredEquipment,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNoEquip) Color(0xFF4CD964) else MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.adjustedSets}×${item.adjustedReps}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (item.adjustedWeightKg > 0) "${item.adjustedWeightKg}kg" else "BW",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${item.adjustedSets * template.caloriesPerSet} kcal",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isCompletedToday) {
                            VoltLime.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isCompletedToday) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = if (item.isCompletedToday) "Done" else "Start",
                    tint = if (item.isCompletedToday) VoltLime else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
