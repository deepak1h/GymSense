package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.todayDayKey
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class ConsistencyGoalItem(
    val title: String,
    val subtitle: String,
    val targetDays: Int,
    val accent: Color
)

@Composable
fun GoalsAndMilestonesScreen(
    profile: UserProfile,
    workoutLogs: List<WorkoutLog>
) {
    val logsByDay = remember(workoutLogs) {
        workoutLogs.groupBy { it.dayKey }
    }
    val activeDaysCount = logsByDay.keys.size
    val totalExercisesDone = workoutLogs.size
    val totalSetsDone = workoutLogs.sumOf { it.completedSets }
    val totalRepsDone = workoutLogs.sumOf { it.totalReps }
    val totalCaloriesDone = workoutLogs.sumOf { it.caloriesBurned }

    val todayKey = todayDayKey()
    var selectedCalendarDayKey by remember { mutableStateOf(todayKey) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val cardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val consistencyGoals = listOf(
        ConsistencyGoalItem(
            title = "1 Week Streak",
            subtitle = "7 active workout days",
            targetDays = 7,
            accent = primaryColor
        ),
        ConsistencyGoalItem(
            title = "2 Weeks Streak",
            subtitle = "14 active workout days",
            targetDays = 14,
            accent = secondaryColor
        ),
        ConsistencyGoalItem(
            title = "30 Days Habit",
            subtitle = "30 active workout days",
            targetDays = 30,
            accent = tertiaryColor
        ),
        ConsistencyGoalItem(
            title = "100 Days Club",
            subtitle = "100 completed workout days",
            targetDays = 100,
            accent = Color(0xFFE11D48)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("goals_and_milestones_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Summary Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$activeDaysCount / 100 Days",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatSummaryMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "WORKOUTS",
                        value = "$totalExercisesDone",
                        color = primaryColor
                    )
                    StatSummaryMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "REPS",
                        value = "$totalRepsDone",
                        color = secondaryColor
                    )
                    StatSummaryMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "SETS",
                        value = "$totalSetsDone",
                        color = tertiaryColor
                    )
                    StatSummaryMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "KCAL",
                        value = "$totalCaloriesDone",
                        color = Color(0xFFE11D48)
                    )
                }
            }
        }

        // 2. Monthly Consistency Calendar
        ConsistencyCalendarCard(
            logsByDay = logsByDay,
            todayKey = todayKey,
            selectedDayKey = selectedCalendarDayKey,
            onSelectDay = { selectedCalendarDayKey = it }
        )

        // 3. Milestones Section
        Text(
            text = "Milestones",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        consistencyGoals.forEach { goal ->
            val ratio = (activeDaysCount.toFloat() / goal.targetDays.toFloat()).coerceIn(0f, 1f)
            val animRatio by animateFloatAsState(
                targetValue = ratio,
                animationSpec = tween(600),
                label = goal.title
            )
            val isUnlocked = activeDaysCount >= goal.targetDays

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isUnlocked) goal.accent.copy(alpha = 0.55f) else cardBorderColor,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) {
                        goal.accent.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(goal.accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.LocalFireDepartment,
                                    contentDescription = goal.title,
                                    tint = goal.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = goal.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = if (isUnlocked) "DONE" else "$activeDaysCount/${goal.targetDays}d",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = goal.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = goal.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 4. 1,000 Reps Goal
        val repTarget = 1000L
        val repProgress = (totalRepsDone.toFloat() / repTarget.toFloat()).coerceIn(0f, 1f)
        GoalMiniBarCard(
            icon = Icons.Default.FitnessCenter,
            title = "1,000 Lifetime Reps",
            progressText = "$totalRepsDone / $repTarget",
            progress = repProgress,
            accent = primaryColor
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatSummaryMiniBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConsistencyCalendarCard(
    logsByDay: Map<String, List<WorkoutLog>>,
    todayKey: String,
    selectedDayKey: String,
    onSelectDay: (String) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val monthTitle = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)
    }
    val year = calendar.get(Calendar.YEAR)
    val monthZeroIndexed = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val firstDayCal = remember {
        (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val leadingEmptyCells = ((firstDayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("consistency_calendar_card"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Tap day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayHeader ->
                    Text(
                        text = dayHeader,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Calendar Grid Rows
            val totalCells = leadingEmptyCells + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - leadingEmptyCells + 1
                        if (dayNum in 1..daysInMonth) {
                            val dayKey = "%04d-%02d-%02d".format(year, monthZeroIndexed + 1, dayNum)
                            val dayLogs = logsByDay[dayKey].orEmpty()
                            val hasProgress = dayLogs.isNotEmpty()
                            val isToday = dayKey == todayKey
                            val isSelected = dayKey == selectedDayKey

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            hasProgress -> primaryColor.copy(alpha = 0.22f)
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected || isToday) 1.5.dp else 0.dp,
                                        color = when {
                                            isSelected -> primaryColor
                                            isToday -> MaterialTheme.colorScheme.secondary
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectDay(dayKey) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = if (hasProgress || isToday) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (hasProgress) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasProgress) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Done",
                                            tint = primaryColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Selected Day Details Box
            val selectedLogs = logsByDay[selectedDayKey].orEmpty()
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    val dayReps = selectedLogs.sumOf { it.totalReps }
                    val daySets = selectedLogs.sumOf { it.completedSets }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedDayKey == todayKey) "Today" else selectedDayKey,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = if (selectedLogs.isEmpty()) "Rest day" else "${selectedLogs.size} ex • $daySets sets • $dayReps reps",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = if (selectedLogs.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    selectedLogs.take(3).forEach { log ->
                        Text(
                            text = "✓ ${log.exerciseName}: ${log.completedSets}× (${log.totalReps} reps)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalMiniBarCard(
    icon: ImageVector,
    title: String,
    progressText: String,
    progress: Float,
    accent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
