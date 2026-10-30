package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class WorkoutSplitDay(
    val code: String,
    val shortTitle: String,
    val tagLine: String
) {
    AUTO("AUTO", "Auto", "Smart Rotation"),
    WARMUP_LIGHT("WARMUP_LIGHT", "Warmup", "Wk 1-2 Light Full Body"),
    GLUTE_TONE_SCULPT("GLUTE_TONE_SCULPT", "Glutes", "Booty, Hips & Waist Sculpt"),
    LEGS_GLUTES("LEGS_GLUTES", "Legs", "Quads, Glutes & Hamstrings"),
    BACK_BICEPS("BACK_BICEPS", "Back", "Lats, Posture & Arms"),
    PUSH_DAY("PUSH_DAY", "Push", "Chest, Shoulders & Tone"),
    COMBO_SPLIT("COMBO_SPLIT", "Full", "Total Body + Core"),
    ZERO_EQUIP("ZERO_EQUIP", "No-Gear", "100% Bodyweight"),
    CUSTOM_MY_PLAN("CUSTOM_MY_PLAN", "Custom", "Your Selected Routine");

    val subtitle: String
        get() = tagLine
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Athlete",
    val heightCm: Long = 175L,
    val weightKg: Long = 75L,
    val targetWeightKg: Long = 70L,
    val weeklyWorkoutGoal: Long = 4L,
    val targetCaloriesPerWeek: Long = 2000L,
    val restingHeartRateBpm: Long = 60L,
    val vo2MaxEstimate: Long = 45L,
    val fitnessLevel: String = "Beginner",
    val primaryFocus: String = "Hypertrophy",
    val equipmentCsv: String = "Bodyweight,Dumbbells,Barbell,Cable Machine,Lat Pulldown,Leg Press Machine,Bench|G:Boy|W12:1|SP:AUTO|OFF:0",
    val lastFeedbackRpe: Long = 7L,
    val setupCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    private val rawParts: List<String>
        get() = equipmentCsv.split("|")

    val equipmentList: List<String>
        get() = rawParts.firstOrNull()
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("Bodyweight") }

    private fun metaValue(key: String, default: String): String {
        val prefix = "$key:"
        return rawParts.drop(1).firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix) ?: default
    }

    val isGirl: Boolean
        get() = metaValue("G", "Boy").equals("Girl", ignoreCase = true)

    val genderLabel: String
        get() = if (isGirl) "Girl" else "Boy"

    val ageYears: Int
        get() = metaValue("AGE", "24").toIntOrNull()?.coerceIn(13, 90) ?: 24

    val wantsProteinShake: Boolean
        get() = metaValue("PR", "1") == "1"

    val waterAlertsEnabled: Boolean
        get() = metaValue("WA", "1") == "1"

    val waterSlotsMask: Int
        get() = metaValue("WT", "0").toIntOrNull() ?: 0

    val todayWaterGlassesCount: Int
        get() = (0 until 7).count { idx -> (waterSlotsMask and (1 shl idx)) != 0 }

    val waterHistoryMap: Map<String, Int>
        get() = metaValue("WH", "")
            .split(";")
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val day = parts[0].trim()
                    val count = parts[1].trim().toIntOrNull()
                    if (day.isNotEmpty() && count != null) day to count.coerceIn(0, 7) else null
                } else null
            }
            .toMap()

    val isWeek1To2Warmup: Boolean
        get() = metaValue("W12", "0") == "1"

    val splitOverrideCode: String
        get() = metaValue("SP", "AUTO")

    val missedDaysOffset: Int
        get() = metaValue("OFF", "0").toIntOrNull() ?: 0

    val aiPinnedExerciseIds: List<String>
        get() = metaValue("AI", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun withUpdatedPreferences(
        equipment: List<String> = equipmentList,
        isGirl: Boolean = this.isGirl,
        ageYears: Int = this.ageYears,
        wantsProteinShake: Boolean = this.wantsProteinShake,
        waterAlertsEnabled: Boolean = this.waterAlertsEnabled,
        waterSlotsMask: Int = this.waterSlotsMask,
        isWeek1To2Warmup: Boolean = this.isWeek1To2Warmup,
        splitOverrideCode: String = this.splitOverrideCode,
        missedDaysOffset: Int = this.missedDaysOffset,
        aiPinnedIds: List<String> = this.aiPinnedExerciseIds
    ): UserProfile {
        val equipPart = (equipment + "Bodyweight").distinct().joinToString(",")
        val gVal = if (isGirl) "Girl" else "Boy"
        val wVal = if (isWeek1To2Warmup) "1" else "0"
        val prVal = if (wantsProteinShake) "1" else "0"
        val waVal = if (waterAlertsEnabled) "1" else "0"
        val aiVal = aiPinnedIds.take(8).joinToString(",")
        val todayCount = (0 until 7).count { idx -> (waterSlotsMask and (1 shl idx)) != 0 }
        val updatedHistory = waterHistoryMap.toMutableMap().apply {
            put(todayDayKey(), todayCount)
        }
        val whVal = updatedHistory.entries
            .sortedByDescending { it.key }
            .take(7)
            .joinToString(";") { "${it.key}=${it.value}" }

        val combined = "$equipPart|G:$gVal|AGE:${ageYears.coerceIn(13, 90)}|PR:$prVal|WA:$waVal|WT:$waterSlotsMask|WH:$whVal|W12:$wVal|SP:$splitOverrideCode|OFF:$missedDaysOffset|AI:$aiVal"
            .take(500)
        return copy(equipmentCsv = combined)
    }

    val bmiValue: Double
        get() {
            val heightM = (heightCm.coerceAtLeast(100L)) / 100.0
            return ((weightKg / (heightM * heightM)) * 10.0).roundToInt() / 10.0
        }

    val bmiTenths: Long
        get() = (bmiValue * 10.0).roundToLong().coerceIn(100L, 600L)

    val bmiCategory: String
        get() = when {
            bmiValue < 18.5 -> "Lean"
            bmiValue < 25.0 -> "Optimal"
            bmiValue < 30.0 -> "Overweight"
            else -> "High BMI"
        }

    // Mifflin-St Jeor + Activity Factor bioscience estimate (personalized for Boy / Girl & Age)
    val estimatedDailyTdeeKcal: Int
        get() {
            val genderConstant = if (isGirl) -161 else 5
            val bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * ageYears) + genderConstant
            val multiplier = when (weeklyWorkoutGoal) {
                in 1..2 -> 1.375
                in 3..5 -> 1.55
                else -> 1.725
            }
            return (bmr * multiplier).roundToInt()
        }
}

@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val muscleGroup: String = "",
    val equipmentUsed: String = "",
    val completedSets: Long = 3L,
    val totalReps: Long = 30L,
    val weightUsedKg: Long = 20L,
    val durationSeconds: Long = 300L,
    val caloriesBurned: Long = 100L,
    val rpeFeedback: Long = 7L,
    val dayKey: String = todayDayKey(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VitalLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weightKg: Long = 75L,
    val bmiTenths: Long = 245L,
    val restingHeartRateBpm: Long = 60L,
    val recoveryScore: Long = 85L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun todayDayKey(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

enum class BiomechanicMotionType {
    WARMUP_JACKS,
    SQUAT_DRIVE,
    LUNGE_SPLIT,
    HINGE_DEADLIFT,
    BENCH_PRESS,
    CORE_PLANK,
    GLUTE_BRIDGE,
    CABLE_PULL,
    PULL_UP,
    ROW_ERG,
    OVERHEAD_PRESS
}

data class ExerciseTemplate(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val requiredEquipment: String,
    val splitTags: List<WorkoutSplitDay>,
    val isWarmupOrBeginnerFriendly: Boolean = false,
    val baseSets: Int,
    val baseRepsPerSet: Int,
    val baseWeightKg: Int,
    val restSeconds: Int,
    val tempoCode: String,
    val motionType: BiomechanicMotionType,
    val formCues: List<String>,
    val bioscienceNote: String,
    val caloriesPerSet: Int
) {
    val tempoPrescription: String
        get() = tempoCode

    val tutorialSteps: List<String>
        get() = formCues
}

data class ScheduledExercise(
    val template: ExerciseTemplate,
    val targetSets: Int,
    val targetRepsPerSet: Int,
    val recommendedWeightKg: Int,
    val adjustmentReason: String,
    val isCompletedToday: Boolean = false,
    val completedLog: WorkoutLog? = null
) {
    val adjustedSets: Int
        get() = targetSets

    val adjustedReps: Int
        get() = targetRepsPerSet

    val adjustedWeightKg: Int
        get() = recommendedWeightKg
}

object ExerciseCatalog {
    val allEquipmentOptions = listOf(
        "Bodyweight",
        "Dumbbells",
        "Barbell",
        "Cable Machine",
        "Lat Pulldown",
        "Seated Row Machine",
        "Leg Press Machine",
        "Smith Machine",
        "Hack Squat",
        "Leg Extension",
        "Leg Curl Machine",
        "Pec Deck / Fly",
        "Pull-up Bar",
        "Dip Station",
        "Kettlebell",
        "Rowing Ergometer",
        "Battle Ropes",
        "Resistance Bands",
        "Medicine Ball",
        "Bench"
    )

    val supportedEquipment: List<String>
        get() = allEquipmentOptions

    val allExercises = listOf(
        // --- WARMUP & NO-EQUIPMENT BODYWEIGHT FOUNDATION ---
        ExerciseTemplate(
            id = "ex_warmup_jumping_jacks",
            name = "Dynamic Cardio Jacks (Warmup)",
            muscleGroup = "Full Body Warmup",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 2,
            baseRepsPerSet = 25,
            baseWeightKg = 0,
            restSeconds = 30,
            tempoCode = "1-0-1-0",
            motionType = BiomechanicMotionType.WARMUP_JACKS,
            formCues = listOf(
                "Land softly on balls of feet with knees tracking toes",
                "Sweep arms overhead to open glenohumeral capsule",
                "Breathe rhythmically to elevate core temperature"
            ),
            bioscienceNote = "Elevates synovial fluid viscosity and muscle temperature before loading.",
            caloriesPerSet = 22
        ),
        ExerciseTemplate(
            id = "ex_warmup_mobility_lunge",
            name = "Thoracic Lunge Flow (Warmup)",
            muscleGroup = "Hips, Spine & Hamstrings",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.ZERO_EQUIP
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 2,
            baseRepsPerSet = 10,
            baseWeightKg = 0,
            restSeconds = 30,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.LUNGE_SPLIT,
            formCues = listOf(
                "Step into deep runner's lunge and plant opposite hand",
                "Rotate chest and reach fingertips toward ceiling",
                "Extend front knee briefly to mobilize hamstrings"
            ),
            bioscienceNote = "Unlocks hip flexor extension and thoracic rotation simultaneously.",
            caloriesPerSet = 18
        ),
        ExerciseTemplate(
            id = "ex_bw_tempo_squat",
            name = "Bodyweight Tempo Air Squat",
            muscleGroup = "Quads & Glutes",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 0,
            restSeconds = 45,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Reach arms forward for counterbalance while sitting hips back",
                "Pause 1s at parallel with weight evenly on midfoot",
                "Squeeze glutes at top lockout"
            ),
            bioscienceNote = "Builds neuromuscular knee-tracking control for first-time lifters.",
            caloriesPerSet = 25
        ),
        ExerciseTemplate(
            id = "ex_bw_glute_bridge",
            name = "Hip Thrust / Glute Bridge March",
            muscleGroup = "Glutes & Core",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 0,
            restSeconds = 45,
            tempoCode = "2-2-1-0",
            motionType = BiomechanicMotionType.GLUTE_BRIDGE,
            formCues = listOf(
                "Drive through heels until hips form a straight line from knees to shoulders",
                "Hold peak glute squeeze for 2 seconds without overarching lower back",
                "Keep ribs tucked and core braced"
            ),
            bioscienceNote = "High gluteus maximus EMG activation with zero spinal compression.",
            caloriesPerSet = 24
        ),
        ExerciseTemplate(
            id = "ex_bw_kinetic_pushup",
            name = "Tempo Push-Up",
            muscleGroup = "Chest, Shoulders & Core",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.PUSH_DAY,
                WorkoutSplitDay.ZERO_EQUIP
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 12,
            baseWeightKg = 0,
            restSeconds = 60,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.CORE_PLANK,
            formCues = listOf(
                "Brace core and glutes in a straight plank line",
                "Lower chest over 3 seconds with elbows at 45° angle",
                "Press floor away to full protraction"
            ),
            bioscienceNote = "Closed-chain pressing strengthens serratus anterior and shoulder stability.",
            caloriesPerSet = 25
        ),
        ExerciseTemplate(
            id = "ex_bw_reverse_lunge",
            name = "Alternating Reverse Lunge",
            muscleGroup = "Glutes & Quadriceps",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 14,
            baseWeightKg = 0,
            restSeconds = 60,
            tempoCode = "2-1-1-0",
            motionType = BiomechanicMotionType.LUNGE_SPLIT,
            formCues = listOf(
                "Step backward softly and lower back knee 1 inch from floor",
                "Keep front shin vertical to bias glute recruitment",
                "Drive through front heel back to tall stance"
            ),
            bioscienceNote = "Backward step reduces patellofemoral shear stress compared to forward lunges.",
            caloriesPerSet = 26
        ),
        ExerciseTemplate(
            id = "ex_bw_superman_pull",
            name = "Prone Superman Lat Pull",
            muscleGroup = "Upper Back & Erectors",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.BACK_BICEPS,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 0,
            restSeconds = 45,
            tempoCode = "2-2-1-0",
            motionType = BiomechanicMotionType.HINGE_DEADLIFT,
            formCues = listOf(
                "Lie face down, lift chest slightly and reach arms overhead",
                "Pull elbows down into a 'W' squeezing shoulder blades together",
                "Pause 2s at peak contraction"
            ),
            bioscienceNote = "Trains scapular retraction and posterior chain endurance without machines.",
            caloriesPerSet = 20
        ),
        ExerciseTemplate(
            id = "ex_bw_plank_taps",
            name = "Anti-Rotation Plank Taps",
            muscleGroup = "Core & Obliques",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 20,
            baseWeightKg = 0,
            restSeconds = 45,
            tempoCode = "1-1-1-0",
            motionType = BiomechanicMotionType.CORE_PLANK,
            formCues = listOf(
                "Set feet shoulder-width apart in high plank",
                "Tap opposite shoulder slowly without letting hips sway",
                "Exhale on each tap to lock transverse abdominis"
            ),
            bioscienceNote = "Resisting pelvic rotation builds 360° spinal stability.",
            caloriesPerSet = 22
        ),

        // --- LEG & GLUTE DAY MACHINES & WEIGHTS ---
        ExerciseTemplate(
            id = "ex_barbell_back_squat",
            name = "Barbell Back Squat",
            muscleGroup = "Quads, Glutes & Core",
            requiredEquipment = "Barbell",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 4,
            baseRepsPerSet = 8,
            baseWeightKg = 70,
            restSeconds = 90,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Brace 360° core pressure before unlocking hips and knees",
                "Track knees over toes to full depth",
                "Drive chest and hips up together"
            ),
            bioscienceNote = "Recruits high-threshold motor units across the entire lower body.",
            caloriesPerSet = 42
        ),
        ExerciseTemplate(
            id = "ex_leg_press_sled",
            name = "45° Sled Leg Press",
            muscleGroup = "Quads & Glutes",
            requiredEquipment = "Leg Press Machine",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 4,
            baseRepsPerSet = 12,
            baseWeightKg = 120,
            restSeconds = 90,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Keep lower back pressed flat into pad",
                "Lower sled to 90° knee flexion",
                "Press through heels without locking knees"
            ),
            bioscienceNote = "High quad overload with zero axial spinal compression.",
            caloriesPerSet = 38
        ),
        ExerciseTemplate(
            id = "ex_smith_hip_thrust",
            name = "Smith Machine Hip Thrust",
            muscleGroup = "Gluteus Maximus",
            requiredEquipment = "Smith Machine",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 4,
            baseRepsPerSet = 12,
            baseWeightKg = 60,
            restSeconds = 75,
            tempoCode = "2-2-1-0",
            motionType = BiomechanicMotionType.GLUTE_BRIDGE,
            formCues = listOf(
                "Hinge on upper back against bench with shins vertical at top",
                "Hold 2s isometric glute squeeze at full hip extension",
                "Keep chin tucked forward"
            ),
            bioscienceNote = "Peak mechanical tension at shortened hip extension.",
            caloriesPerSet = 35
        ),
        ExerciseTemplate(
            id = "ex_hack_squat_machine",
            name = "Deep Hack Squat",
            muscleGroup = "Quadriceps Sweep",
            requiredEquipment = "Hack Squat",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES),
            baseSets = 4,
            baseRepsPerSet = 10,
            baseWeightKg = 80,
            restSeconds = 90,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Place feet lower on platform for maximum knee flexion",
                "Control 3s descent until hamstrings meet calves",
                "Drive smoothly keeping hips pinned to back pad"
            ),
            bioscienceNote = "Fixed path isolates vastus lateralis and vastus medialis.",
            caloriesPerSet = 40
        ),
        ExerciseTemplate(
            id = "ex_leg_extension_iso",
            name = "Seated Leg Extension",
            muscleGroup = "Rectus Femoris & Quads",
            requiredEquipment = "Leg Extension",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES),
            baseSets = 3,
            baseRepsPerSet = 14,
            baseWeightKg = 45,
            restSeconds = 60,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Align knee joint with machine pivot point",
                "Pause 1s at full extension squeezing quads hard",
                "Lower slowly over 2s"
            ),
            bioscienceNote = "Uniquely loads rectus femoris in full knee extension.",
            caloriesPerSet = 28
        ),
        ExerciseTemplate(
            id = "ex_leg_curl_hamstring",
            name = "Seated / Lying Leg Curl",
            muscleGroup = "Hamstrings",
            requiredEquipment = "Leg Curl Machine",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 3,
            baseRepsPerSet = 12,
            baseWeightKg = 40,
            restSeconds = 60,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.HINGE_DEADLIFT,
            formCues = listOf(
                "Dorsiflex ankles (toes up) during curl",
                "Keep hips pressed firmly into pad",
                "Control eccentric stretch for 2 full seconds"
            ),
            bioscienceNote = "Direct knee-flexion stimulus for biceps femoris and semitendinosus.",
            caloriesPerSet = 28
        ),
        ExerciseTemplate(
            id = "ex_kb_romanian_hinge",
            name = "Kettlebell / DB Romanian Hinge",
            muscleGroup = "Hamstrings & Glutes",
            requiredEquipment = "Kettlebell",
            splitTags = listOf(WorkoutSplitDay.LEGS_GLUTES, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 3,
            baseRepsPerSet = 12,
            baseWeightKg = 24,
            restSeconds = 75,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.HINGE_DEADLIFT,
            formCues = listOf(
                "Push hips straight back with 15° knee bend",
                "Keep weight grazing shins with flat back",
                "Snap hips forward to lockout"
            ),
            bioscienceNote = "Lengthened-state eccentric tension drives hamstring hypertrophy.",
            caloriesPerSet = 34
        ),
        ExerciseTemplate(
            id = "ex_db_goblet_squat",
            name = "Dumbbell Goblet Squat",
            muscleGroup = "Quads, Glutes & Core",
            requiredEquipment = "Dumbbells",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 12,
            baseWeightKg = 20,
            restSeconds = 60,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Hold dumbbell vertically against chest",
                "Sit upright between hips with elbows inside knees",
                "Drive evenly through midfoot"
            ),
            bioscienceNote = "Anterior load encourages upright thoracic posture.",
            caloriesPerSet = 30
        ),

        // --- BACK & BICEPS DAY ---
        ExerciseTemplate(
            id = "ex_lat_pulldown_wide",
            name = "Lat Pulldown Machine",
            muscleGroup = "Latissimus Dorsi",
            requiredEquipment = "Lat Pulldown",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.BACK_BICEPS,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 4,
            baseRepsPerSet = 12,
            baseWeightKg = 45,
            restSeconds = 75,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.CABLE_PULL,
            formCues = listOf(
                "Depress shoulder blades first before bending elbows",
                "Pull bar to upper chest with 15° lean",
                "Control upward stretch over 2 seconds"
            ),
            bioscienceNote = "Targets upper and lower lat fibers through full vertical arc.",
            caloriesPerSet = 30
        ),
        ExerciseTemplate(
            id = "ex_seated_row_machine",
            name = "Seated Row Machine",
            muscleGroup = "Mid-Back, Rhomboids & Lats",
            requiredEquipment = "Seated Row Machine",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.BACK_BICEPS,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 4,
            baseRepsPerSet = 12,
            baseWeightKg = 50,
            restSeconds = 75,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.ROW_ERG,
            formCues = listOf(
                "Brace chest against pad or sit tall with neutral spine",
                "Drive elbows past ribs and squeeze scapulae for 1s",
                "Let shoulders protract slightly on return"
            ),
            bioscienceNote = "Builds mid-trap and rhomboid thickness for posture.",
            caloriesPerSet = 30
        ),
        ExerciseTemplate(
            id = "ex_cable_face_pull",
            name = "Cable Face Pull & Rear Delt",
            muscleGroup = "Rear Delts, Rotator Cuff & Traps",
            requiredEquipment = "Cable Machine",
            splitTags = listOf(WorkoutSplitDay.BACK_BICEPS, WorkoutSplitDay.COMBO_SPLIT),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 20,
            restSeconds = 60,
            tempoCode = "2-2-1-0",
            motionType = BiomechanicMotionType.ROW_ERG,
            formCues = listOf(
                "Pull rope toward forehead while externally rotating thumbs back",
                "Hold 2s double-bicep pose at peak contraction",
                "Keep ribs down"
            ),
            bioscienceNote = "Essential shoulder health exercise balancing pressing volume.",
            caloriesPerSet = 24
        ),
        ExerciseTemplate(
            id = "ex_pullup_strict",
            name = "Strict Pull-Up / Chin-Up",
            muscleGroup = "Lats & Biceps",
            requiredEquipment = "Pull-up Bar",
            splitTags = listOf(WorkoutSplitDay.BACK_BICEPS, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 4,
            baseRepsPerSet = 8,
            baseWeightKg = 0,
            restSeconds = 90,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.PULL_UP,
            formCues = listOf(
                "Start from dead hang with active shoulders",
                "Drive elbows down until chin clears bar",
                "Lower smoothly over 2s"
            ),
            bioscienceNote = "Maximum motor unit recruitment across upper posterior chain.",
            caloriesPerSet = 32
        ),
        ExerciseTemplate(
            id = "ex_db_hammer_curl",
            name = "Dumbbell Incline / Hammer Curl",
            muscleGroup = "Biceps Brachii & Brachialis",
            requiredEquipment = "Dumbbells",
            splitTags = listOf(WorkoutSplitDay.BACK_BICEPS),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 12,
            baseWeightKg = 12,
            restSeconds = 60,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.OVERHEAD_PRESS,
            formCues = listOf(
                "Pin elbows to sides with zero torso swing",
                "Curl to full squeeze and lower for 2 full seconds",
                "Keep wrists neutral"
            ),
            bioscienceNote = "Neutral grip targets brachialis and brachioradialis thickness.",
            caloriesPerSet = 22
        ),
        ExerciseTemplate(
            id = "ex_rower_vo2_intervals",
            name = "Rowing Ergometer Power Intervals",
            muscleGroup = "Back, Legs & VO2 Cardio",
            requiredEquipment = "Rowing Ergometer",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.BACK_BICEPS,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 0,
            restSeconds = 60,
            tempoCode = "1-0-2-0",
            motionType = BiomechanicMotionType.ROW_ERG,
            formCues = listOf(
                "Drive 60% legs, 20% hip hinge, 20% arm pull",
                "Recover forward smoothly at half drive speed",
                "Keep chain horizontal"
            ),
            bioscienceNote = "Boosts VO2 max and posterior chain work capacity.",
            caloriesPerSet = 45
        ),

        // --- PUSH DAY (CHEST, SHOULDERS & TRICEPS) ---
        ExerciseTemplate(
            id = "ex_barbell_bench",
            name = "Barbell Bench Press",
            muscleGroup = "Chest & Triceps",
            requiredEquipment = "Barbell",
            splitTags = listOf(WorkoutSplitDay.PUSH_DAY, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 4,
            baseRepsPerSet = 10,
            baseWeightKg = 55,
            restSeconds = 90,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.BENCH_PRESS,
            formCues = listOf(
                "Retract shoulder blades into bench pad",
                "Lower bar over 3s to lower sternum",
                "Press up and slightly back toward rack"
            ),
            bioscienceNote = "Compound horizontal press for pectoral mechanical tension.",
            caloriesPerSet = 34
        ),
        ExerciseTemplate(
            id = "ex_db_incline_press",
            name = "Dumbbell Shoulder / Incline Press",
            muscleGroup = "Upper Chest & Deltoids",
            requiredEquipment = "Dumbbells",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.PUSH_DAY,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 4,
            baseRepsPerSet = 12,
            baseWeightKg = 18,
            restSeconds = 75,
            tempoCode = "3-0-1-1",
            motionType = BiomechanicMotionType.OVERHEAD_PRESS,
            formCues = listOf(
                "Stack wrists directly over elbows in scapular plane",
                "Lower dumbbells to ear/collarbone level for full stretch",
                "Press overhead without flaring ribs"
            ),
            bioscienceNote = "Independent dumbbells train bilateral shoulder stability.",
            caloriesPerSet = 28
        ),
        ExerciseTemplate(
            id = "ex_pec_deck_fly",
            name = "Pec Deck / Machine Chest Fly",
            muscleGroup = "Pectoralis Major",
            requiredEquipment = "Pec Deck / Fly",
            splitTags = listOf(WorkoutSplitDay.PUSH_DAY),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 14,
            baseWeightKg = 40,
            restSeconds = 60,
            tempoCode = "2-2-2-0",
            motionType = BiomechanicMotionType.BENCH_PRESS,
            formCues = listOf(
                "Keep slight bend in elbows and shoulders pinned down",
                "Bring handles together and hold squeeze for 2s",
                "Return under control into deep chest stretch"
            ),
            bioscienceNote = "Constant resistance through horizontal adduction.",
            caloriesPerSet = 26
        ),
        ExerciseTemplate(
            id = "ex_dip_station_press",
            name = "Parallel Bar Tricep & Chest Dip",
            muscleGroup = "Lower Chest & Triceps",
            requiredEquipment = "Dip Station",
            splitTags = listOf(WorkoutSplitDay.PUSH_DAY),
            baseSets = 3,
            baseRepsPerSet = 10,
            baseWeightKg = 0,
            restSeconds = 75,
            tempoCode = "3-1-1-0",
            motionType = BiomechanicMotionType.PULL_UP,
            formCues = listOf(
                "Lean torso slightly forward and depress shoulders",
                "Lower until elbows reach 90° flexion",
                "Press to full elbow lockout"
            ),
            bioscienceNote = "High stretch-mediated hypertrophy for triceps and lower pecs.",
            caloriesPerSet = 30
        ),
        ExerciseTemplate(
            id = "ex_band_lateral_raise",
            name = "Band / Cable Lateral Raise",
            muscleGroup = "Medial Deltoids",
            requiredEquipment = "Resistance Bands",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.PUSH_DAY,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 8,
            restSeconds = 45,
            tempoCode = "2-1-2-0",
            motionType = BiomechanicMotionType.OVERHEAD_PRESS,
            formCues = listOf(
                "Sweep arms out in 30° scapular plane leading with elbows",
                "Stop at shoulder height with traps relaxed",
                "Resist the band on the way down"
            ),
            bioscienceNote = "Builds shoulder width and V-taper proportions.",
            caloriesPerSet = 22
        ),
        ExerciseTemplate(
            id = "ex_battle_ropes_hiit",
            name = "Battle Ropes Metabolic Waves",
            muscleGroup = "Shoulders, Core & Conditioning",
            requiredEquipment = "Battle Ropes",
            splitTags = listOf(WorkoutSplitDay.PUSH_DAY, WorkoutSplitDay.COMBO_SPLIT),
            baseSets = 3,
            baseRepsPerSet = 30,
            baseWeightKg = 0,
            restSeconds = 45,
            tempoCode = "1-0-1-0",
            motionType = BiomechanicMotionType.ROW_ERG,
            formCues = listOf(
                "Sit into athletic quarter-squat with braced core",
                "Alternate explosive arm waves all the way to anchor",
                "Maintain steady nasal-mouth breathing"
            ),
            bioscienceNote = "High upper-body glycolytic conditioning with zero joint impact.",
            caloriesPerSet = 38
        ),
        ExerciseTemplate(
            id = "ex_medball_rotational_slam",
            name = "Medicine Ball Core Slam",
            muscleGroup = "Core, Lats & Power",
            requiredEquipment = "Medicine Ball",
            splitTags = listOf(
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 12,
            baseWeightKg = 8,
            restSeconds = 45,
            tempoCode = "1-0-1-0",
            motionType = BiomechanicMotionType.OVERHEAD_PRESS,
            formCues = listOf(
                "Reach ball high overhead into triple extension",
                "Slam ball forcefully down using abs and lats",
                "Catch on bounce with hinged hips"
            ),
            bioscienceNote = "Trains fast-twitch core flexion and total-body power.",
            caloriesPerSet = 30
        ),

        // --- GIRL-FOCUSED GLUTE, HIP SCULPT, HOURGLASS & TONING EXERCISES ---
        ExerciseTemplate(
            id = "ex_cable_glute_kickback",
            name = "Cable Glute Kickbacks",
            muscleGroup = "Glutes & Upper Shelf",
            requiredEquipment = "Cable Machine",
            splitTags = listOf(
                WorkoutSplitDay.GLUTE_TONE_SCULPT,
                WorkoutSplitDay.LEGS_GLUTES
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 12,
            restSeconds = 45,
            tempoCode = "2-1-2-1",
            motionType = BiomechanicMotionType.LUNGE_SPLIT,
            formCues = listOf(
                "Hinge torso 30 degrees forward holding cable tower",
                "Drive heel back & slightly out 15 degrees to target glute medius",
                "Squeeze glute at peak for 1 full second without arching lower back"
            ),
            bioscienceNote = "Isolates upper and lower glute fibers with constant cable tension.",
            caloriesPerSet = 28
        ),
        ExerciseTemplate(
            id = "ex_sumo_dumbbell_squat",
            name = "Sumo Squat Pulse",
            muscleGroup = "Glutes & Inner Thighs",
            requiredEquipment = "Dumbbells",
            splitTags = listOf(
                WorkoutSplitDay.GLUTE_TONE_SCULPT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.WARMUP_LIGHT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 15,
            baseWeightKg = 14,
            restSeconds = 50,
            tempoCode = "2-2-1-0",
            motionType = BiomechanicMotionType.SQUAT_DRIVE,
            formCues = listOf(
                "Take a wide stance with toes turned out 45 degrees",
                "Lower vertically keeping knees tracking over toes",
                "Add a small pulse at the bottom before driving up through heels"
            ),
            bioscienceNote = "Maximizes glute and adductor recruitment for lower-body toning.",
            caloriesPerSet = 34
        ),
        ExerciseTemplate(
            id = "ex_curtsy_lunge",
            name = "Sculpting Curtsy Lunges",
            muscleGroup = "Side Glutes & Hips",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.GLUTE_TONE_SCULPT,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.WARMUP_LIGHT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 14,
            baseWeightKg = 0,
            restSeconds = 45,
            tempoCode = "2-1-1-0",
            motionType = BiomechanicMotionType.LUNGE_SPLIT,
            formCues = listOf(
                "Step back leg diagonally behind front heel",
                "Keep hips squared forward and chest tall",
                "Drive through front heel to sculpt outer glute shelf"
            ),
            bioscienceNote = "Targets gluteus medius and hip stabilizers for hourglass symmetry.",
            caloriesPerSet = 26
        ),
        ExerciseTemplate(
            id = "ex_pilates_hundred_core",
            name = "Pilates Core & Waist Sculpt",
            muscleGroup = "Deep Abs & Waistline",
            requiredEquipment = "Bodyweight",
            splitTags = listOf(
                WorkoutSplitDay.GLUTE_TONE_SCULPT,
                WorkoutSplitDay.ZERO_EQUIP,
                WorkoutSplitDay.WARMUP_LIGHT,
                WorkoutSplitDay.COMBO_SPLIT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 20,
            baseWeightKg = 0,
            restSeconds = 40,
            tempoCode = "2-1-2-1",
            motionType = BiomechanicMotionType.CORE_PLANK,
            formCues = listOf(
                "Draw navel toward spine to engage transverse abdominis",
                "Keep ribs knitted down with controlled rhythmic breathing",
                "Extend legs at 45 degrees with long neck posture"
            ),
            bioscienceNote = "Tightens deep abdominal wall without widening the oblique waist.",
            caloriesPerSet = 22
        ),
        ExerciseTemplate(
            id = "ex_band_hip_abduction",
            name = "Banded Glute Abductions",
            muscleGroup = "Upper Glutes & Hips",
            requiredEquipment = "Resistance Bands",
            splitTags = listOf(
                WorkoutSplitDay.GLUTE_TONE_SCULPT,
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.WARMUP_LIGHT
            ),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 3,
            baseRepsPerSet = 18,
            baseWeightKg = 0,
            restSeconds = 40,
            tempoCode = "1-2-1-0",
            motionType = BiomechanicMotionType.GLUTE_BRIDGE,
            formCues = listOf(
                "Place mini-band just above knees in a hip bridge or seated position",
                "Press knees outward against band tension and hold 2 seconds",
                "Maintain pelvic tuck throughout the set"
            ),
            bioscienceNote = "High metabolic stress on gluteus medius for hip rounding and knee alignment.",
            caloriesPerSet = 24
        ),

        // --- PROTEIN SHAKE RECOVERY SCHEDULE ITEM ---
        ExerciseTemplate(
            id = "ex_protein_shake_recovery",
            name = "Protein Shake (25–30g Whey/Plant)",
            muscleGroup = "Recovery & Muscle Synthesis",
            requiredEquipment = "Bodyweight",
            splitTags = emptyList(),
            isWarmupOrBeginnerFriendly = true,
            baseSets = 1,
            baseRepsPerSet = 1,
            baseWeightKg = 0,
            restSeconds = 10,
            tempoCode = "POST-WORKOUT",
            motionType = BiomechanicMotionType.WARMUP_JACKS,
            formCues = listOf(
                "Mix 1 scoop (25–30g) protein with 300ml cold water or milk",
                "Drink within 45 minutes post-workout to spike leucine synthesis",
                "Pair with 300ml water to stay on your 3-hour hydration schedule"
            ),
            bioscienceNote = "Provides essential amino acids for myofibrillar repair.",
            caloriesPerSet = 120
        )
    )

    val girlGoalOptions = listOf(
        "Glutes & Booty",
        "Toned & Lean",
        "Hourglass & Waist",
        "Core & Posture"
    )

    val boyGoalOptions = listOf(
        "V-Taper & Muscle",
        "Strength & Mass",
        "Shred & Athletic",
        "Full Body Tone"
    )

    fun resolveTodaySplit(profile: UserProfile): WorkoutSplitDay {
        val override = WorkoutSplitDay.entries.firstOrNull {
            it.code == profile.splitOverrideCode && it != WorkoutSplitDay.AUTO
        }
        if (override != null) return override

        if (profile.isWeek1To2Warmup) {
            return WorkoutSplitDay.WARMUP_LIGHT
        }

        val rotation = if (profile.isGirl) {
            when (profile.primaryFocus) {
                "Glutes & Booty", "Hourglass & Waist" -> listOf(
                    WorkoutSplitDay.GLUTE_TONE_SCULPT,
                    WorkoutSplitDay.BACK_BICEPS,
                    WorkoutSplitDay.LEGS_GLUTES,
                    WorkoutSplitDay.COMBO_SPLIT
                )
                else -> listOf(
                    WorkoutSplitDay.GLUTE_TONE_SCULPT,
                    WorkoutSplitDay.COMBO_SPLIT,
                    WorkoutSplitDay.LEGS_GLUTES,
                    WorkoutSplitDay.BACK_BICEPS
                )
            }
        } else {
            listOf(
                WorkoutSplitDay.LEGS_GLUTES,
                WorkoutSplitDay.BACK_BICEPS,
                WorkoutSplitDay.PUSH_DAY,
                WorkoutSplitDay.COMBO_SPLIT
            )
        }
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val shifted = Math.floorMod(dayOfYear + profile.missedDaysOffset, rotation.size)
        return rotation[shifted]
    }

    /**
     * Builds an equipment- and fitness-level-aware recommended exercise list for the Auto-Schedule popup.
     */
    fun recommendExercisesForEquipmentAndLevel(profile: UserProfile): Pair<String, List<String>> {
        val availableEquip = (profile.equipmentList + "Bodyweight").map { it.lowercase() }.toSet()
        val workoutCatalog = allExercises.filter { it.id != "ex_protein_shake_recovery" }
        val matchingEquip = workoutCatalog.filter { ex ->
            availableEquip.contains(ex.requiredEquipment.lowercase())
        }.ifEmpty { workoutCatalog }

        val ranked = matchingEquip.sortedByDescending { ex ->
            var score = 0
            if (ex.id == "ex_warmup_jumping_jacks") score += 30
            if (!ex.requiredEquipment.equals("Bodyweight", ignoreCase = true)) score += 12
            if (profile.fitnessLevel.equals("Beginner", ignoreCase = true) && ex.isWarmupOrBeginnerFriendly) {
                score += 8
            }
            if (profile.fitnessLevel.equals("Advanced", ignoreCase = true) && ex.baseWeightKg >= 30) {
                score += 8
            }
            if (profile.isGirl) {
                if (WorkoutSplitDay.GLUTE_TONE_SCULPT in ex.splitTags) score += 16
                if (ex.muscleGroup.contains("Glute", ignoreCase = true) ||
                    ex.muscleGroup.contains("Core", ignoreCase = true) ||
                    ex.muscleGroup.contains("Waist", ignoreCase = true)
                ) score += 12
            } else {
                if (WorkoutSplitDay.PUSH_DAY in ex.splitTags || WorkoutSplitDay.BACK_BICEPS in ex.splitTags || WorkoutSplitDay.LEGS_GLUTES in ex.splitTags) {
                    score += 12
                }
            }
            score
        }

        val pickedIds = ranked.take(6).map { it.id }
        val title = if (profile.isGirl) {
            "${profile.fitnessLevel} ${profile.primaryFocus}"
        } else {
            "${profile.fitnessLevel} ${profile.primaryFocus}"
        }
        return title.take(24) to pickedIds
    }

    fun buildDynamicSchedule(
        profile: UserProfile,
        todayLogs: List<WorkoutLog>,
        allHistoricalLogs: List<WorkoutLog> = todayLogs,
        customRoutineIds: List<String> = emptyList(),
        todayExtraIds: List<String> = emptyList()
    ): List<ScheduledExercise> {
        val availableEquip = (profile.equipmentList + "Bodyweight").map { it.lowercase() }.toSet()
        val activeSplit = resolveTodaySplit(profile)

        val workoutCatalog = allExercises.filter { it.id != "ex_protein_shake_recovery" }
        val equipFiltered = workoutCatalog.filter { ex ->
            availableEquip.contains(ex.requiredEquipment.lowercase())
        }

        val pinnedIds = profile.aiPinnedExerciseIds
        val baseSelection = when {
            activeSplit == WorkoutSplitDay.CUSTOM_MY_PLAN && customRoutineIds.isNotEmpty() -> {
                customRoutineIds.mapNotNull { id -> workoutCatalog.firstOrNull { it.id == id } }
            }
            pinnedIds.isNotEmpty() && profile.splitOverrideCode == "AUTO" && !profile.isWeek1To2Warmup -> {
                val pinned = pinnedIds.mapNotNull { id -> equipFiltered.firstOrNull { it.id == id } }
                val extras = equipFiltered.filter { ex -> ex.id !in pinnedIds }.take(2)
                (pinned + extras).ifEmpty { equipFiltered }
            }
            activeSplit == WorkoutSplitDay.WARMUP_LIGHT -> {
                equipFiltered.filter { it.isWarmupOrBeginnerFriendly }
            }
            activeSplit == WorkoutSplitDay.ZERO_EQUIP -> {
                workoutCatalog.filter { it.requiredEquipment.equals("Bodyweight", ignoreCase = true) }
            }
            else -> {
                val warmup = workoutCatalog.firstOrNull { it.id == "ex_warmup_jumping_jacks" }
                val splitEquip = equipFiltered.filter {
                    activeSplit in it.splitTags && !it.requiredEquipment.equals("Bodyweight", ignoreCase = true)
                }
                val splitBodyweight = workoutCatalog.filter {
                    activeSplit in it.splitTags && it.requiredEquipment.equals("Bodyweight", ignoreCase = true)
                }
                (listOfNotNull(warmup) + splitEquip + splitBodyweight).distinctBy { it.id }
            }
        }.ifEmpty {
            workoutCatalog.filter { it.requiredEquipment.equals("Bodyweight", ignoreCase = true) }
        }

        // Personalize exercise priority for Girl goals vs Boy goals
        val sortedCore = if (activeSplit == WorkoutSplitDay.CUSTOM_MY_PLAN && customRoutineIds.isNotEmpty()) {
            baseSelection
        } else if (profile.isGirl) {
            baseSelection.sortedByDescending { ex ->
                val goalBoost = when (profile.primaryFocus) {
                    "Glutes & Booty" -> if (
                        ex.muscleGroup.contains("Glute", ignoreCase = true) ||
                        ex.motionType == BiomechanicMotionType.GLUTE_BRIDGE ||
                        ex.motionType == BiomechanicMotionType.LUNGE_SPLIT
                    ) 12 else 4
                    "Toned & Lean" -> if (
                        ex.requiredEquipment == "Bodyweight" ||
                        ex.requiredEquipment == "Dumbbells" ||
                        ex.requiredEquipment == "Resistance Bands"
                    ) 10 else 5
                    "Hourglass & Waist" -> if (
                        ex.muscleGroup.contains("Glute", ignoreCase = true) ||
                        ex.muscleGroup.contains("Waist", ignoreCase = true) ||
                        ex.muscleGroup.contains("Lat", ignoreCase = true)
                    ) 11 else 5
                    else -> if (ex.muscleGroup.contains("Core", ignoreCase = true)) 11 else 6
                }
                if (ex.id.startsWith("ex_warmup")) 15 else goalBoost
            }.take(6)
        } else {
            baseSelection.sortedByDescending { ex ->
                val boyBoost = when (profile.primaryFocus) {
                    "Strength & Mass" -> if (ex.baseWeightKg >= 40) 11 else 5
                    "V-Taper & Muscle" -> if (
                        ex.muscleGroup.contains("Lat", ignoreCase = true) ||
                        ex.muscleGroup.contains("Chest", ignoreCase = true) ||
                        ex.muscleGroup.contains("Delt", ignoreCase = true)
                    ) 11 else 5
                    else -> 6
                }
                if (ex.id.startsWith("ex_warmup")) 15 else boyBoost
            }.take(6)
        }

        val extraTemplates = todayExtraIds.mapNotNull { id ->
            allExercises.firstOrNull { it.id == id }
        }
        val proteinTemplate = if (profile.wantsProteinShake) {
            listOfNotNull(allExercises.firstOrNull { it.id == "ex_protein_shake_recovery" })
        } else {
            emptyList()
        }

        val orderedTemplates = (sortedCore + extraTemplates + proteinTemplate).distinctBy { it.id }

        val rpe = profile.lastFeedbackRpe.toInt()
        val levelMultiplier = when {
            profile.isWeek1To2Warmup -> 0.65
            profile.fitnessLevel == "Beginner" -> 0.78
            profile.fitnessLevel == "Advanced" -> 1.20
            else -> 1.00
        }
        // Girl mode uses tailored toning/sculpting weights & higher sculpting reps
        val genderLoadFactor = if (profile.isGirl) 0.68 else 1.00
        val genderRepBonus = if (profile.isGirl && profile.primaryFocus in listOf("Toned & Lean", "Hourglass & Waist", "Glutes & Booty")) 2 else 0

        val (setDelta, repDelta, loadFactor, reason) = when {
            profile.isWeek1To2Warmup -> Quadruple(-1, 2, 0.75, "Wk 1-2 Light")
            rpe <= 5 -> Quadruple(1, 2, 1.10, "Progressive")
            rpe in 6..7 -> Quadruple(0, 1, 1.05, "Optimal")
            rpe == 8 -> Quadruple(0, 0, 1.00, "Target")
            else -> Quadruple(-1, -2, 0.90, "Recovery")
        }

        return orderedTemplates.map { template ->
            if (template.id == "ex_protein_shake_recovery") {
                val completedLog = todayLogs.firstOrNull { it.exerciseId == template.id }
                return@map ScheduledExercise(
                    template = template,
                    targetSets = 1,
                    targetRepsPerSet = 1,
                    recommendedWeightKg = 0,
                    adjustmentReason = "Post-Workout Shake",
                    isCompletedToday = completedLog != null,
                    completedLog = completedLog
                )
            }

            // SILENT CONSISTENCY PROGRESSION:
            // Count how many times the user has completed this exact exercise previously
            val consistencyCount = allHistoricalLogs.count { it.exerciseId == template.id }
            val silentExtraSets = (consistencyCount / 4).coerceIn(0, 2)
            val silentExtraReps = (consistencyCount / 2).coerceIn(0, 6)
            val silentExtraWeightKg = if (profile.isGirl) {
                ((consistencyCount / 2) * 1).coerceIn(0, 12)
            } else {
                ((consistencyCount / 2) * 2).coerceIn(0, 24)
            }

            val adjustedSets = (template.baseSets + setDelta + silentExtraSets).coerceIn(2, 6)
            val adjustedReps = (template.baseRepsPerSet + repDelta + genderRepBonus + silentExtraReps).coerceIn(6, 30)
            val adjustedWeight = if (template.baseWeightKg == 0) {
                0
            } else {
                ((template.baseWeightKg * levelMultiplier * genderLoadFactor * loadFactor).roundToInt() + silentExtraWeightKg)
                    .coerceAtLeast(3)
            }
            val completedLog = todayLogs.firstOrNull { it.exerciseId == template.id }

            ScheduledExercise(
                template = template,
                targetSets = adjustedSets,
                targetRepsPerSet = adjustedReps,
                recommendedWeightKg = adjustedWeight,
                adjustmentReason = reason,
                isCompletedToday = completedLog != null,
                completedLog = completedLog
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
