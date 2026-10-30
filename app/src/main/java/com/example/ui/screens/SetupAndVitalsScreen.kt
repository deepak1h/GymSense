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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupAndVitalsScreen(
    currentProfile: UserProfile,
    customRoutineIds: List<String>,
    defaultDisplayName: String,
    isFirstTimeSetup: Boolean = false,
    isGoogleLinked: Boolean = false,
    isSaving: Boolean,
    onSaveCustomRoutine: (List<String>) -> Unit,
    onSkipSetup: () -> Unit = {},
    onLinkGoogleAccount: () -> Unit = {},
    onSaveSetup: (
        displayName: String,
        ageYears: Int,
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
        wantsProteinShake: Boolean,
        isWeek1To2Warmup: Boolean,
        lastFeedbackRpe: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember(currentProfile) {
        mutableStateOf(currentProfile.displayName.ifBlank { defaultDisplayName.ifBlank { "Athlete" } })
    }
    var isGirl by remember(currentProfile) { mutableStateOf(currentProfile.isGirl) }
    var ageSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.ageYears.toFloat()) }
    var wantsProteinShake by remember(currentProfile) { mutableStateOf(currentProfile.wantsProteinShake) }
    var isWeek1To2Warmup by remember(currentProfile) { mutableStateOf(currentProfile.isWeek1To2Warmup) }

    var heightSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.heightCm.toFloat()) }
    var weightSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.weightKg.toFloat()) }
    var targetWeightSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.targetWeightKg.toFloat()) }
    var weeklyGoalSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.weeklyWorkoutGoal.toFloat()) }
    var weeklyCalorieSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.targetCaloriesPerWeek.toFloat()) }
    var rhrSlider by remember(currentProfile) { mutableFloatStateOf(currentProfile.restingHeartRateBpm.toFloat()) }
    var vo2Slider by remember(currentProfile) { mutableFloatStateOf(currentProfile.vo2MaxEstimate.toFloat()) }
    var fitnessLevel by remember(currentProfile) { mutableStateOf(currentProfile.fitnessLevel) }

    var primaryFocus by remember(currentProfile) {
        val defaultGoal = if (currentProfile.isGirl) {
            if (currentProfile.primaryFocus in ExerciseCatalog.girlGoalOptions) currentProfile.primaryFocus else "Glutes & Booty"
        } else {
            if (currentProfile.primaryFocus in ExerciseCatalog.boyGoalOptions) currentProfile.primaryFocus else "V-Taper & Muscle"
        }
        mutableStateOf(defaultGoal)
    }

    val selectedEquip = remember(currentProfile) {
        mutableStateListOf<String>().apply {
            addAll(currentProfile.equipmentList)
        }
    }

    val pickedCustomExercises = remember(customRoutineIds, currentProfile) {
        mutableStateListOf<String>().apply {
            val seed = customRoutineIds.ifEmpty {
                currentProfile.aiPinnedExerciseIds.ifEmpty {
                    if (currentProfile.isGirl) {
                        listOf(
                            "ex_warmup_jumping_jacks",
                            "ex_glute_bridge_iso",
                            "ex_cable_glute_kickback",
                            "ex_sumo_dumbbell_squat",
                            "ex_curtsy_lunge",
                            "ex_pilates_hundred_core"
                        )
                    } else {
                        listOf(
                            "ex_warmup_jumping_jacks",
                            "ex_goblet_squat",
                            "ex_lat_pulldown",
                            "ex_pushup_tempo",
                            "ex_plank_hollow_hold"
                        )
                    }
                }
            }
            addAll(seed)
        }
    }
    var exerciseFilter by remember { mutableStateOf("All") }

    val liveHeightMeters = (heightSlider / 100f).coerceAtLeast(1.0f)
    val liveBmi = ((weightSlider / (liveHeightMeters * liveHeightMeters)) * 10f).roundToInt() / 10f
    val liveBmiCategory = when {
        liveBmi < 18.5f -> "Lean"
        liveBmi < 25.0f -> "Optimal"
        liveBmi < 30.0f -> "High"
        else -> "High"
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val cardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("setup_vitals_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Minimal Header + Skip Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFirstTimeSetup) "Quick Setup" else "Plan & Gear",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Profile, goals & gym equipment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = onSkipSetup,
                    label = { Text("Skip", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Setup",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("skip_setup_button")
                )
            }
        }

        // 0B. Google Privacy & AI Link Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGoogleLinked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isGoogleLinked) primaryColor.copy(alpha = 0.5f) else cardBorderColor,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isGoogleLinked) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = "Privacy & Google AI Link",
                            tint = if (isGoogleLinked) primaryColor else secondaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isGoogleLinked) {
                                    "Google Account Linked"
                                } else {
                                    "Link Google for Privacy & AI"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGoogleLinked) {
                                    "Private cloud sync & GymBae AI active"
                                } else {
                                    "Encrypted sync & private GymBae AI"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isGoogleLinked) {
                        Surface(
                            color = primaryColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Linked",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onLinkGoogleAccount,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("setup_link_google_button")
                        ) {
                            Text(
                                text = "Link Google",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 1. Profile: Name, Sex (Boy/Girl), Goals & Stickman Preview
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
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
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    if (isGirl) secondaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.4f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StickmanExerciseCanvas(
                                motionType = when {
                                    isGirl && primaryFocus == "Glutes & Booty" -> BiomechanicMotionType.GLUTE_BRIDGE
                                    isGirl -> BiomechanicMotionType.LUNGE_SPLIT
                                    isWeek1To2Warmup -> BiomechanicMotionType.WARMUP_JACKS
                                    else -> BiomechanicMotionType.SQUAT_DRIVE
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
                                    onClick = {
                                        isGirl = false
                                        if (primaryFocus !in ExerciseCatalog.boyGoalOptions) {
                                            primaryFocus = "V-Taper & Muscle"
                                        }
                                    },
                                    label = { Text("Boy") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Boy",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = primaryColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isGirl,
                                    onClick = {
                                        isGirl = true
                                        if (primaryFocus !in ExerciseCatalog.girlGoalOptions) {
                                            primaryFocus = "Glutes & Booty"
                                        }
                                    },
                                    label = { Text("Girl") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Face3,
                                            contentDescription = "Girl",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        selectedLeadingIconColor = secondaryColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Dynamic Goal Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isGirl) secondaryColor else primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isGirl) "Goal (Glutes & Sculpt)" else "Goal (Muscle & Strength)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isGirl) secondaryColor else primaryColor
                            )
                        }

                        val activeGoals = if (isGirl) ExerciseCatalog.girlGoalOptions else ExerciseCatalog.boyGoalOptions
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeGoals.forEach { goal ->
                                val selected = primaryFocus == goal
                                FilterChip(
                                    selected = selected,
                                    onClick = { primaryFocus = goal },
                                    label = { Text(goal, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isGirl) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                        selectedLabelColor = if (isGirl) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    ),
                                    modifier = Modifier.testTag("goal_chip_${goal.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    // Protein Shake & Week 1-2 Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalDrink,
                                        contentDescription = null,
                                        tint = secondaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Shake",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = wantsProteinShake,
                                    onCheckedChange = { wantsProteinShake = it },
                                    modifier = Modifier.testTag("protein_shake_switch")
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Wk 1-2",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
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
        }

        // 2. Age, Weight, Height & Days/Week Sliders
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VitalsStatColumn("AGE", "${ageSlider.roundToInt()}y", MaterialTheme.colorScheme.onSurface)
                        VitalsStatColumn("BMI", "$liveBmi ($liveBmiCategory)", primaryColor)
                        VitalsStatColumn("WT", "${weightSlider.roundToInt()}→${targetWeightSlider.roundToInt()}kg", MaterialTheme.colorScheme.onSurface)
                        VitalsStatColumn("DAYS", "${weeklyGoalSlider.roundToInt()}d/wk", secondaryColor)
                    }

                    CompactVitalSlider(
                        icon = Icons.Default.Cake,
                        label = "Age",
                        valueText = "${ageSlider.roundToInt()} yrs",
                        value = ageSlider,
                        range = 14f..75f,
                        onValueChange = { ageSlider = it },
                        testTag = "slider_age"
                    )
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
                        range = 38f..160f,
                        onValueChange = { weightSlider = it },
                        testTag = "slider_weight"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.MonitorWeight,
                        label = "Target Wt",
                        valueText = "${targetWeightSlider.roundToInt()} kg",
                        value = targetWeightSlider,
                        range = 38f..150f,
                        onValueChange = { targetWeightSlider = it },
                        testTag = "slider_target_weight"
                    )
                    CompactVitalSlider(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Days/Wk",
                        valueText = "${weeklyGoalSlider.roundToInt()} days",
                        value = weeklyGoalSlider,
                        range = 2f..7f,
                        steps = 4,
                        onValueChange = {
                            weeklyGoalSlider = it
                            weeklyCalorieSlider = (it * 450f).coerceIn(900f, 5000f)
                        },
                        testTag = "slider_weekly_goal"
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
                }
            }
        }

        // 3. Available Equipment (20 Machines + No-Gear)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
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
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Equipment (${selectedEquip.size}/${ExerciseCatalog.supportedEquipment.size})",
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
                                label = { Text("No-Gear") },
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
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = primaryColor
                                ),
                                modifier = Modifier.testTag("equip_chip_${equip.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        // 4. Save Button
        item {
            Button(
                onClick = {
                    onSaveSetup(
                        displayName,
                        ageSlider.roundToInt(),
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
                        wantsProteinShake,
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
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "SAVING..." else "SAVE & START",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Custom Schedule Exercise Builder
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = primaryColor.copy(alpha = 0.45f),
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
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Custom Routine (${pickedCustomExercises.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pick exercises for Custom split",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                containerColor = primaryColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("save_custom_schedule_button")
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Glutes", "Bodyweight", "Legs", "Back", "Push").forEach { cat ->
                            FilterChip(
                                selected = exerciseFilter == cat,
                                onClick = { exerciseFilter = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    val visibleCatalog = ExerciseCatalog.allExercises
                        .filter { it.id != "ex_protein_shake_recovery" }
                        .filter { ex ->
                            when (exerciseFilter) {
                                "Glutes" -> WorkoutSplitDay.GLUTE_TONE_SCULPT in ex.splitTags || ex.muscleGroup.contains("Glute", ignoreCase = true)
                                "Bodyweight" -> ex.requiredEquipment.equals("Bodyweight", ignoreCase = true)
                                "Legs" -> WorkoutSplitDay.LEGS_GLUTES in ex.splitTags
                                "Back" -> WorkoutSplitDay.BACK_BICEPS in ex.splitTags
                                "Push" -> WorkoutSplitDay.PUSH_DAY in ex.splitTags
                                else -> true
                            }
                        }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleCatalog.take(12).forEach { ex ->
                            val isPicked = pickedCustomExercises.contains(ex.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isPicked) primaryColor.copy(alpha = 0.45f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isPicked && pickedCustomExercises.size > 1) {
                                            pickedCustomExercises.remove(ex.id)
                                        } else if (!isPicked) {
                                            pickedCustomExercises.add(ex.id)
                                        }
                                    },
                                color = if (isPicked) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                                            text = "${ex.requiredEquipment} • ${ex.muscleGroup}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isPicked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isPicked) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
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
                fontWeight = FontWeight.Bold,
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
