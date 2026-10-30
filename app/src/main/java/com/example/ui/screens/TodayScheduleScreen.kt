package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
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
import com.example.util.GymSenseReminderScheduler

private const val PROTEIN_SHAKE_ID = "ex_protein_shake_recovery"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    profile: UserProfile,
    schedule: List<ScheduledExercise>,
    allLogs: List<WorkoutLog>,
    todayExtraIds: List<String>,
    isGeneratingAiSchedule: Boolean,
    onSelectExercise: (ScheduledExercise) -> Unit,
    onToggleWeek1To2: (Boolean) -> Unit,
    onToggleWaterAlerts: (Boolean) -> Unit,
    onToggleProteinShake: (Boolean) -> Unit,
    onToggleWaterSlot: (Int) -> Unit,
    onTestReminderNotification: () -> Unit,
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
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Shift +1 Day?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Missed a workout? Shift your split cycle by +1 day without losing streak progress.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShiftConfirmDialog = false
                        onShiftMissedDay { resultMessage ->
                            shiftResultPopupMessage = resultMessage
                        }
                    },
                    modifier = Modifier.testTag("confirm_shift_day_popup_button")
                ) {
                    Text("Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Shift Result Popup Confirmation
    if (shiftResultPopupMessage != null) {
        AlertDialog(
            onDismissRequest = { shiftResultPopupMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Schedule Shifted", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = shiftResultPopupMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { shiftResultPopupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // 3. Add Extra Exercise Bottom Sheet
    if (showAddExtraExerciseSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddExtraExerciseSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddExtraExerciseBottomSheetContent(
                isGirl = profile.isGirl,
                todayExtraIds = todayExtraIds,
                onToggleExtra = onToggleExtraExercise,
                onClose = { showAddExtraExerciseSheet = false }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_schedule_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP HERO CARD: Today's Split + Water Alert / Shake / Wk 1-2 Toggles + Minimal Action Bar
        item {
            TodaySplitTopHero(
                profile = profile,
                resolvedSplit = resolvedSplit,
                totalLifetimeReps = totalLifetimeReps,
                completionRatio = completionRatio,
                isGeneratingAiSchedule = isGeneratingAiSchedule,
                onToggleWaterAlerts = onToggleWaterAlerts,
                onToggleProteinShake = onToggleProteinShake,
                onToggleWeek1To2 = onToggleWeek1To2,
                onOpenShiftDialog = { showShiftConfirmDialog = true },
                onOpenCustomBuilder = onOpenCustomScheduleBuilder,
                onRequestAiSchedule = onRequestAiSchedule
            )
        }

        // WAKING HOURS (6AM - 12AM) 3-HOUR WATER REMINDER & PROTEIN BAR
        item {
            HydrationAndReminderStrip(
                waterSlotsMask = profile.waterSlotsMask,
                waterAlertsEnabled = profile.waterAlertsEnabled,
                wantsProteinShake = profile.wantsProteinShake,
                onToggleWaterSlot = onToggleWaterSlot,
                onTestReminderNotification = onTestReminderNotification
            )
        }

        // OVERRIDE SPLIT SELECTOR ROW (Short 1-word labels)
        item {
            SplitOverrideStrip(
                activeSplit = resolvedSplit,
                isWeek1To2 = profile.isWeek1To2Warmup,
                onSelectSplit = onSelectSplit,
                onToggleWeek1To2 = onToggleWeek1To2
            )
        }

        // SECTION HEADER: TODAY'S TASKS
        item {
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
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (profile.isGirl) "Today's Sculpt & Tone" else "Today's Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$completedCount/${schedule.size}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // EXERCISE CARDS WITH STICKMAN ANIMATION (OR PROTEIN SHAKE CARD)
        items(schedule, key = { it.template.id }) { item ->
            val isExtra = item.template.id in todayExtraIds
            ExerciseStickmanCard(
                scheduledExercise = item,
                isGirl = profile.isGirl,
                isExtraExercise = isExtra,
                onStartClick = { onSelectExercise(item) },
                onRemoveExtra = if (isExtra) {
                    { onToggleExtraExercise(item.template.id) }
                } else null
            )
        }

        // BOTTOM CARD: "+ Extra"
        item {
            AddExtraExerciseFooterCard(
                extraCount = todayExtraIds.size,
                onClick = { showAddExtraExerciseSheet = true }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HydrationAndReminderStrip(
    waterSlotsMask: Int,
    waterAlertsEnabled: Boolean,
    wantsProteinShake: Boolean,
    onToggleWaterSlot: (Int) -> Unit,
    onTestReminderNotification: () -> Unit
) {
    val waterLabels = GymSenseReminderScheduler.wakingWaterLabels
    val completedGlasses = (0 until waterLabels.size).count { idx ->
        (waterSlotsMask and (1 shl idx)) != 0
    }
    val cyanAccent = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Water 3h Schedule",
                        tint = cyanAccent,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Water (3h • 6am–12am)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = cyanAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$completedGlasses/${waterLabels.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = cyanAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Minimal Reminder Status / Test Bell button
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTestReminderNotification)
                        .testTag("test_reminder_bell_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (waterAlertsEnabled || wantsProteinShake) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = "Reminders Status",
                            tint = if (waterAlertsEnabled || wantsProteinShake) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                waterAlertsEnabled && wantsProteinShake -> "3h + Shake ON"
                                waterAlertsEnabled -> "3h Water ON"
                                wantsProteinShake -> "Shake ON"
                                else -> "Alerts OFF"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 7 Waking Hour Water Slots (6am, 9am, 12pm, 3pm, 6pm, 9pm, 12am)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (index in waterLabels.indices) {
                    val labelText = waterLabels[index]
                    val isDrunk = (waterSlotsMask and (1 shl index)) != 0
                    val bg by animateColorAsState(
                        targetValue = if (isDrunk) cyanAccent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        animationSpec = tween(250),
                        label = "waterSlotBg_$index"
                    )
                    val borderCol by animateColorAsState(
                        targetValue = if (isDrunk) cyanAccent else Color.Transparent,
                        animationSpec = tween(250),
                        label = "waterSlotBorder_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                            .clickable { onToggleWaterSlot(index) }
                            .padding(vertical = 6.dp)
                            .testTag("water_slot_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isDrunk) Icons.Default.Check else Icons.Default.WaterDrop,
                                contentDescription = labelText,
                                tint = if (isDrunk) cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = if (isDrunk) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isDrunk) cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaySplitTopHero(
    profile: UserProfile,
    resolvedSplit: WorkoutSplitDay,
    totalLifetimeReps: Long,
    completionRatio: Float,
    isGeneratingAiSchedule: Boolean,
    onToggleWaterAlerts: (Boolean) -> Unit,
    onToggleProteinShake: (Boolean) -> Unit,
    onToggleWeek1To2: (Boolean) -> Unit,
    onOpenShiftDialog: () -> Unit,
    onOpenCustomBuilder: () -> Unit,
    onRequestAiSchedule: () -> Unit
) {
    val activeTitle = when {
        resolvedSplit == WorkoutSplitDay.CUSTOM_MY_PLAN -> "Custom Routine"
        profile.aiPinnedExerciseIds.isNotEmpty() -> "GymBae AI Split"
        profile.isWeek1To2Warmup -> "Warmup & Light"
        else -> "${resolvedSplit.shortTitle} Split"
    }

    val activeSubtitle = when {
        resolvedSplit == WorkoutSplitDay.CUSTOM_MY_PLAN -> "Your selected exercises"
        profile.isWeek1To2Warmup -> "Week 1–2 Joint Prep & Light Full Body"
        profile.isGirl -> "${profile.primaryFocus} • Sculpt & Tone"
        else -> "${profile.primaryFocus} • ${resolvedSplit.subtitle}"
    }

    val gradientColors = if (profile.isGirl) {
        listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Top Row: Today's Split Title + Progress Ring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (profile.isGirl) "GIRL SCULPT" else "BOY SPLIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${profile.fitnessLevel} • $totalLifetimeReps reps",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Compact Progress Ring
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 5.dp
                        )
                        CircularProgressIndicator(
                            progress = { completionRatio },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp
                        )
                        Text(
                            text = "${(completionRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Minimal Reminder & Warmup Toggle Strip: Water Alert On/Off | Shake On/Off | Wk 1-2
                // (Boy/Girl selector is in the top header bar only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactModePill(
                        selected = profile.waterAlertsEnabled,
                        icon = Icons.Default.WaterDrop,
                        label = if (profile.waterAlertsEnabled) "Water: ON" else "Water: OFF",
                        onClick = { onToggleWaterAlerts(!profile.waterAlertsEnabled) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_water_alerts_button")
                    )
                    CompactModePill(
                        selected = profile.wantsProteinShake,
                        icon = Icons.Default.LocalDrink,
                        label = if (profile.wantsProteinShake) "Shake: ON" else "Shake: OFF",
                        onClick = { onToggleProteinShake(!profile.wantsProteinShake) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_protein_shake_button")
                    )
                    CompactModePill(
                        selected = profile.isWeek1To2Warmup,
                        icon = Icons.Default.SelfImprovement,
                        label = "Wk 1-2",
                        onClick = { onToggleWeek1To2(!profile.isWeek1To2Warmup) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_week1to2_button")
                    )
                }

                // 3 Separate Action Boxes with Minimal 1-Word Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionBoxTile(
                        icon = Icons.Default.EventRepeat,
                        title = "+1 Day",
                        subtitle = "Shift",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onOpenShiftDialog,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shift_missed_day_button")
                    )
                    ActionBoxTile(
                        icon = Icons.Default.EditCalendar,
                        title = "Plan",
                        subtitle = "Custom",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenCustomBuilder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_custom_builder_button")
                    )
                    ActionBoxTile(
                        icon = Icons.Default.AutoAwesome,
                        title = if (isGeneratingAiSchedule) "Wait..." else "GymBae",
                        subtitle = "Auto Plan",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        isLoading = isGeneratingAiSchedule,
                        onClick = onRequestAiSchedule,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_build_schedule_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactModePill(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionBoxTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
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
}

@Composable
private fun SplitOverrideStrip(
    activeSplit: WorkoutSplitDay,
    isWeek1To2: Boolean,
    onSelectSplit: (WorkoutSplitDay) -> Unit,
    onToggleWeek1To2: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WorkoutSplitDay.entries.forEach { split ->
            val isSelected = if (split == WorkoutSplitDay.WARMUP_LIGHT) {
                isWeek1To2
            } else {
                !isWeek1To2 && activeSplit == split
            }
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (split == WorkoutSplitDay.WARMUP_LIGHT) {
                        onToggleWeek1To2(true)
                    } else {
                        onSelectSplit(split)
                    }
                },
                label = {
                    Text(
                        text = split.shortTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("split_chip_${split.code}")
            )
        }
    }
}

@Composable
private fun ExerciseStickmanCard(
    scheduledExercise: ScheduledExercise,
    isGirl: Boolean,
    isExtraExercise: Boolean,
    onStartClick: () -> Unit,
    onRemoveExtra: (() -> Unit)? = null
) {
    val template = scheduledExercise.template
    val isProteinShakeItem = template.id == PROTEIN_SHAKE_ID
    val displayTitle = template.name
    val isNoEquipment = template.requiredEquipment.equals("Bodyweight", ignoreCase = true)
    val estCalories = scheduledExercise.targetSets * template.caloriesPerSet

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (scheduledExercise.isCompletedToday) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onStartClick)
            .testTag("exercise_card_${template.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (scheduledExercise.isCompletedToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else if (isProteinShakeItem) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isProteinShakeItem) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = "Protein Shake",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                // Animated Code Stickman Preview
                StickmanExerciseCanvas(
                    motionType = template.motionType,
                    isGirl = isGirl,
                    modifier = Modifier.size(72.dp),
                    showGrid = false
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = template.muscleGroup.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }

                    Surface(
                        color = if (isNoEquipment) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isNoEquipment) Icons.Default.AccessibilityNew else Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (isNoEquipment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isNoEquipment) "No Equip" else template.requiredEquipment,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNoEquipment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isExtraExercise) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+EXTRA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Icon Metric Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProteinShakeItem) {
                        MiniIconMetric(
                            icon = Icons.Default.Bolt,
                            text = "25-30g Protein"
                        )
                    } else {
                        MiniIconMetric(
                            icon = Icons.Default.Bolt,
                            text = "${scheduledExercise.targetSets}×${scheduledExercise.targetRepsPerSet}"
                        )
                        if (scheduledExercise.recommendedWeightKg > 0) {
                            MiniIconMetric(
                                icon = Icons.Default.MonitorWeight,
                                text = "${scheduledExercise.recommendedWeightKg}kg"
                            )
                        }
                    }
                    MiniIconMetric(
                        icon = Icons.Default.LocalFireDepartment,
                        text = "${estCalories}kcal"
                    )
                }
            }

            if (onRemoveExtra != null && !scheduledExercise.isCompletedToday) {
                IconButton(
                    onClick = onRemoveExtra,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Extra",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (scheduledExercise.isCompletedToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (scheduledExercise.isCompletedToday) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (scheduledExercise.isCompletedToday) "Completed" else "Start",
                    tint = if (scheduledExercise.isCompletedToday) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
private fun AddExtraExerciseFooterCard(
    extraCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .testTag("add_extra_exercise_footer_button"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Extra Exercise",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "+ Extra Exercise",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (extraCount > 0) {
                            "$extraCount extra added today"
                        } else {
                            "Pick any exercise from catalog"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddExtraExerciseBottomSheetContent(
    isGirl: Boolean,
    todayExtraIds: List<String>,
    onToggleExtra: (String) -> Unit,
    onClose: () -> Unit
) {
    var filterGroup by remember { mutableStateOf("All") }
    val muscleGroups = remember {
        listOf("All", "Glutes", "Legs", "Back", "Chest", "Shoulders", "Arms", "Core", "Warmup", "Full Body")
    }
    val allCatalog = remember {
        ExerciseCatalog.allExercises.filter { it.id != PROTEIN_SHAKE_ID }
    }
    val filtered = remember(filterGroup, allCatalog) {
        if (filterGroup == "All") allCatalog
        else allCatalog.filter {
            it.muscleGroup.contains(filterGroup, ignoreCase = true) ||
                (filterGroup == "Glutes" && WorkoutSplitDay.GLUTE_TONE_SCULPT in it.splitTags)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Add Extra Exercise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any exercise to add or remove from Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) {
                Text("Done")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            muscleGroups.forEach { group ->
                FilterChip(
                    selected = filterGroup == group,
                    onClick = { filterGroup = group },
                    label = { Text(group, maxLines = 1) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { ex ->
                val isAdded = ex.id in todayExtraIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleExtra(ex.id) }
                        .testTag("extra_picker_item_${ex.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAdded) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            StickmanExerciseCanvas(
                                motionType = ex.motionType,
                                isGirl = isGirl,
                                modifier = Modifier.size(48.dp),
                                showGrid = false
                            )
                            Column {
                                Text(
                                    text = ex.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ex.muscleGroup} • ${ex.requiredEquipment}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isAdded) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                            contentDescription = if (isAdded) "Added" else "Add",
                            tint = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniIconMetric(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
