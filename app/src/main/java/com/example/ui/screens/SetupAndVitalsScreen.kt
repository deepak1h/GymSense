package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.BiomechanicMotionType
import com.example.data.model.ExerciseCatalog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutSplitDay
import com.example.ui.components.StickmanExerciseCanvas
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VoltLime
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupAndVitalsScreen(
    currentProfile: UserProfile,
    customRoutineIds: List<String>,
    defaultDisplayName: String,
    isSaving: Boolean,
    onSaveCustomRoutine: (List<String>) -> Unit,
    onSkipSetup: () -> Unit = {},
    onSaveSetup: (
        displayName: String,
        heightCm: Long,
        weightKg: Long,
        targetWeightKg: Long,
        weeklyWorkoutGoal: Long,
        targetCaloriesPerWeek: Long,
        restingHeartRateBpm: Long,
        vo2MaxEstimate: Long,
        fitnessLevel: String,
        primaryFocus: String,
        selectedEquipment: List<String>,
        isGirl: Boolean,
        isWeek1To2Warmup: Boolean,
        lastFeedbackRpe: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember(currentProfile) {
        mutableStateOf(currentProfile.displayName.ifBlank { defaultDisplayName.ifBlank { "Athlete" } })
    }
    var isGirl by remember(currentProfile) { mutableStateOf(currentProfile.isGirl) }
    var isWeek1To2Warmup by remember(currentProfile) { mutableStateOf(currentProfile.isWeek1To2Warmup) }

    var heightSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.heightCm.toFloat()) }
    var weightSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.weightKg.toFloat()) }
    var targetWeightSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.targetWeightKg.toFloat()) }
    var weeklyGoalSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.weeklyWorkoutGoal.toFloat()) }
    var weeklyCalorieSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.targetCaloriesPerWeek.toFloat()) }
    var rhrSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.restingHeartRateBpm.toFloat()) }
    var vo2Slider by remember(currentProfile) { mutableFloatStateOf(currentProfile.vo2MaxEstimate.toFloat()) }
    var fitnessLevel by remember(currentProfile) { mutableStateOf(currentProfile.fitnessLevel) }
    var primaryFocus by remember(currentProfile) { mutableStateOf(currentProfile.primaryFocus) }

    val selectedEquip = remember(currentProfile) {
        mutableStateListOf<String>().apply {
            addAll(currentProfile.equipmentList)
        }
    }

    val pickedCustomExercises = remember(customRoutineIds, currentProfile) {
        mutableStateListOf<String>().apply {
            val seed = customRoutineIds.ifEmpty {
                currentProfile.aiPinnedExerciseIds.ifEmpty {
                    listOf(
                        "ex_warmup_jumping_jacks",
                        "ex_goblet_squat",
                        "ex_lat_pulldown",
                        "ex_pushup_tempo",
                        "ex_plank_hollow_hold"
                    )
                }
            }
            addAll(seed)
        }
    }
    var exerciseFilter by remember { mutableStateOf("All") }

    val liveHeightMeters = (heightSlider / 100f).coerceAtLeast(1.0f)
    val liveBmi = ((weightSlider / (liveHeightMeters * liveHeightMeters)) * 10f).roundToInt() / 10f
    val liveBmiCategory = when {
        liveBmi < 18.5f -> "Under"
        liveBmi < 25.0f -> "Optimal"
        liveBmi < 30.0f -> "Elevated"
        else -> "High"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("setup_vitals_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. TOP BAR WITH SKIP SETUP OPTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Plan, Gear & Vitals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize exercises or skip to use defaults",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = onSkipSetup,
                    label = { Text("Skip Setup →", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("skip_setup_button")
                )
            }
        }
        // 1. CUSTOM SCHEDULE & EXERCISE BUILDER ("Make My Own Schedule")
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = VoltLime.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(18.dp)
                )
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
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Build My Custom Schedule",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${pickedCustomExercises.size} exercises selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoltLime
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (pickedCustomExercises.isNotEmpty()) {
                                    onSaveCustomRoutine(pickedCustomExercises.toList())
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VoltLime,
                                contentColor = Color(0xFF06150E)
                            ),
                            modifier = Modifier.testTag("save_custom_schedule_button")
                        ) {
                            Text("Apply Plan", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Filter tabs for Custom Schedule Exercise Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Bodyweight", "Legs", "Back", "Push").forEach { cat ->
                            FilterChip(
                                selected = exerciseFilter == cat,
                                onClick = { exerciseFilter = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    val visibleCatalog = ExerciseCatalog.allExercises.filter { ex ->
                        when (exerciseFilter) {
                            "Bodyweight" -> ex.requiredEquipment.equals("Bodyweight", ignoreCase = true)
                            "Legs" -> WorkoutSplitDay.LEGS_GLUTES in ex.splitTags
                            "Back" -> WorkoutSplitDay.BACK_BICEPS in ex.splitTags
                            "Push" -> WorkoutSplitDay.PUSH_DAY in ex.splitTags
                            else -> true
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleCatalog.take(10).forEach { ex ->
                            val isPicked = pickedCustomExercises.contains(ex.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isPicked && pickedCustomExercises.size > 1) {
                                            pickedCustomExercises.remove(ex.id)
                                        } else if (!isPicked) {
                                            pickedCustomExercises.add(ex.id)
                                        }
                                    },
                                color = if (isPicked) {
                                    VoltLime.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ex.requiredEquipment} • ${ex.muscleGroup} (${ex.baseSets}×${ex.baseRepsPerSet})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isPicked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isPicked) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. PERSONALIZATION CARD: GIRL / BOY + WEEK 1-2 WARMUP
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF080F18))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StickmanExerciseCanvas(
                                motionType = if (isWeek1To2Warmup) {
                                    BiomechanicMotionType.WARMUP_JACKS
                                } else {
                                    BiomechanicMotionType.SQUAT_DRIVE
                                },
                                isGirl = isGirl,
                                isPlaying = true,
                                showGrid = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("athlete_name_input")
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !isGirl,
                                    onClick = { isGirl = false },
                                    label = { Text("Boy") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Boy",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isGirl,
                                    onClick = { isGirl = true },
                                    label = { Text("Girl") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Face3,
                                            contentDescription = "Girl",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF6EC7).copy(alpha = 0.22f),
                                        selectedLabelColor = Color(0xFFFF9CE3),
                                        selectedLeadingIconColor = Color(0xFFFF6EC7)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    tint = VoltLime,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "New to Gym (Week 1–2 Mode)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Warmup + light full-body foundation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isWeek1To2Warmup,
                                onCheckedChange = {
                                    isWeek1To2Warmup = it
                                    if (it) fitnessLevel = "Beginner"
                                },
                                modifier = Modifier.testTag("week1to2_warmup_switch")
                            )
                        }
                    }
                }
            }
        }

        // 3. AVAILABLE MACHINES & EQUIPMENT (20 OPTIONS + NO-EQUIPMENT)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "GEAR (${selectedEquip.size}/${ExerciseCatalog.supportedEquipment.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = {
                                    selectedEquip.clear()
                                    selectedEquip.add("Bodyweight")
                                },
                                label = { Text("No-Equip") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccessibilityNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                            AssistChip(
                                onClick = {
                                    selectedEquip.clear()
                                    selectedEquip.addAll(ExerciseCatalog.supportedEquipment)
                                },
                                label = { Text("All") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExerciseCatalog.supportedEquipment.forEach { equip ->
                            val isSelected = selectedEquip.contains(equip)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected && selectedEquip.size > 1) {
                                        selectedEquip.remove(equip)
                                    } else if (!isSelected) {
                                        selectedEquip.add(equip)
                                    }
                                },
                                label = { Text(equip) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("equip_chip_${equip.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        // 4. LIVE BMI & BIOMETRICS SLIDERS
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF142438), Color(0xFF0C1520))
                                )
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VitalsStatColumn("BMI", "$liveBmi ($liveBmiCategory)", VoltLime)
                        VitalsStatColumn("WEIGHT", "${weightSlider.roundToInt()} → ${targetWeightSlider.roundToInt()}kg", Color.White)
                        VitalsStatColumn("RHR", "${rhrSlider.roundToInt()} bpm", Color(0xFF4CD964))
                        VitalsStatColumn("VO2", "${vo2Slider.roundToInt()}", MaterialTheme.colorScheme.secondary)
                    }

                    CompactVitalSlider(
                        icon = Icons.Default.Height,
                        label = "Height",
                        valueText = "${heightSlider.roundToInt()} cm",
                        value = heightSlider,
                        range = 140f..210f,
                        onValueChange = { heightSlider = it },
                        testTag = "slider_height"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.MonitorWeight,
                        label = "Weight",
                        valueText = "${weightSlider.roundToInt()} kg",
                        value = weightSlider,
                        range = 40f..160f,
                        onValueChange = { weightSlider = it },
                        testTag = "slider_weight"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.MonitorWeight,
                        label = "Goal Wt",
                        valueText = "${targetWeightSlider.roundToInt()} kg",
                        value = targetWeightSlider,
                        range = 40f..150f,
                        onValueChange = { targetWeightSlider = it },
                        testTag = "slider_target_weight"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.Favorite,
                        label = "Rest HR",
                        valueText = "${rhrSlider.roundToInt()} bpm",
                        value = rhrSlider,
                        range = 42f..100f,
                        onValueChange = { rhrSlider = it },
                        testTag = "slider_rhr"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.Speed,
                        label = "VO2 Max",
                        valueText = "${vo2Slider.roundToInt()}",
                        value = vo2Slider,
                        range = 25f..75f,
                        onValueChange = { vo2Slider = it },
                        testTag = "slider_vo2"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Days/Wk",
                        valueText = "${weeklyGoalSlider.roundToInt()} d • ${weeklyCalorieSlider.roundToInt()} kcal",
                        value = weeklyGoalSlider,
                        range = 2f..7f,
                        steps = 4,
                        onValueChange = {
                            weeklyGoalSlider = it
                            weeklyCalorieSlider = (it * 450f).coerceIn(900f, 5000f)
                        },
                        testTag = "slider_weekly_goal"
                    )
                }
            }
        }

        // 5. SAVE BUTTON
        item {
            Button(
                onClick = {
                    onSaveSetup(
                        displayName,
                        heightSlider.roundToInt().toLong(),
                        weightSlider.roundToInt().toLong(),
                        targetWeightSlider.roundToInt().toLong(),
                        weeklyGoalSlider.roundToInt().toLong(),
                        weeklyCalorieSlider.roundToInt().toLong(),
                        rhrSlider.roundToInt().toLong(),
                        vo2Slider.roundToInt().toLong(),
                        fitnessLevel,
                        primaryFocus,
                        selectedEquip.toList(),
                        isGirl,
                        isWeek1To2Warmup,
                        currentProfile.lastFeedbackRpe
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_setup_vitals_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "SAVING..." else "SAVE GEAR & VITALS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VitalsStatColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF96ACC2)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun CompactVitalSlider(
    icon: ImageVector,
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
