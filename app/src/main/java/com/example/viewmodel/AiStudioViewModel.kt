package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExerciseCatalog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

private const val TAG = "AiStudioViewModel"

data class ChatTurn(
    val id: String,
    val role: String,
    val text: String,
    val modelTag: String = ""
)

data class BioscienceReport(
    val metabolicSummary: String,
    val bmiAndVitalAnalysis: String,
    val hypertrophyPrescription: String,
    val recoveryProtocol: String,
    val actionItems: List<String>
)

data class AutoScheduleDraft(
    val splitTitle: String,
    val selectedExerciseIds: List<String>,
    val fitnessLevel: String,
    val equipmentSummary: String
)

sealed interface BioscienceUiState {
    data object Idle : BioscienceUiState
    data object Loading : BioscienceUiState
    data class Success(val report: BioscienceReport) : BioscienceUiState
    data class Error(val message: String) : BioscienceUiState
}

enum class CoachModelTier(val modelId: String, val label: String, val subtitle: String) {
    FAST("gemini-3.5-flash-lite", "Fast", "Quick Cues"),
    GENERAL("gemini-3.8-flash", "Smart", "GymBae Pro"),
    DEEP_BIOSCIENCE("gemini-4-argon", "Deep", "Full Analysis");

    fun nextTier(): CoachModelTier = when (this) {
        FAST -> GENERAL
        GENERAL -> DEEP_BIOSCIENCE
        DEEP_BIOSCIENCE -> FAST
    }
}

class AiStudioViewModel : ViewModel() {

    private val systemPrompt = content {
        text(
            "You are GymBae, a friendly, smart, and motivating personal workout coach inside the GymSense app. " +
                "Keep answers short, bulleted, and easy to scan."
        )
    }

    private val flashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CoachModelTier.GENERAL.modelId,
            systemInstruction = systemPrompt
        )
    }

    private val fastLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CoachModelTier.FAST.modelId,
            systemInstruction = systemPrompt
        )
    }

    private val argonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CoachModelTier.DEEP_BIOSCIENCE.modelId,
            systemInstruction = systemPrompt
        )
    }

    private val bioscienceReportSchema = Schema.obj(
        properties = mapOf(
            "metabolicSummary" to Schema.string(description = "Short TDEE & calorie summary"),
            "bmiAndVitalAnalysis" to Schema.string(description = "Short BMI, HR, and VO2 insight"),
            "hypertrophyPrescription" to Schema.string(description = "Equipment & bodyweight adjustment"),
            "recoveryProtocol" to Schema.string(description = "Recovery tip"),
            "actionItems" to Schema.array(
                items = Schema.string(),
                description = "3 short action bullets"
            )
        )
    )

    private val aiScheduleSchema = Schema.obj(
        properties = mapOf(
            "splitTitle" to Schema.string(description = "Short title of the workout plan (max 22 chars)"),
            "exerciseIds" to Schema.array(
                items = Schema.string(),
                description = "5 to 7 exercise IDs chosen from the catalog"
            )
        )
    )

    private val structuredBioscienceModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = bioscienceReportSchema
            }
        )
    }

    private val structuredScheduleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = aiScheduleSchema
            }
        )
    }

    private val _selectedCoachTier = MutableStateFlow(CoachModelTier.GENERAL)
    val selectedCoachTier: StateFlow<CoachModelTier> = _selectedCoachTier.asStateFlow()

    private val _chatHistory = MutableStateFlow(
        listOf(
            ChatTurn(
                id = "welcome",
                role = "model",
                text = "Hey! I'm GymBae. Ask me for exercise swaps, form tips, diet advice, or tap 'Auto-Build Plan' above!",
                modelTag = CoachModelTier.GENERAL.label
            )
        )
    )
    val chatHistory: StateFlow<List<ChatTurn>> = _chatHistory.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _isGeneratingSchedule = MutableStateFlow(false)
    val isGeneratingSchedule: StateFlow<Boolean> = _isGeneratingSchedule.asStateFlow()

    private val _pendingScheduleDraft = MutableStateFlow<AutoScheduleDraft?>(null)
    val pendingScheduleDraft: StateFlow<AutoScheduleDraft?> = _pendingScheduleDraft.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _bioscienceState = MutableStateFlow<BioscienceUiState>(BioscienceUiState.Idle)
    val bioscienceState: StateFlow<BioscienceUiState> = _bioscienceState.asStateFlow()

    fun selectCoachTier(tier: CoachModelTier) {
        _selectedCoachTier.value = tier
    }

    fun cycleCoachTier() {
        _selectedCoachTier.value = _selectedCoachTier.value.nextTier()
    }

    fun dismissScheduleDraft() {
        _pendingScheduleDraft.value = null
    }

    fun toggleExerciseInDraft(exerciseId: String) {
        val current = _pendingScheduleDraft.value ?: return
        val updatedIds = current.selectedExerciseIds.toMutableList()
        if (exerciseId in updatedIds) {
            if (updatedIds.size > 1) {
                updatedIds.remove(exerciseId)
            }
        } else {
            updatedIds.add(exerciseId)
        }
        _pendingScheduleDraft.value = current.copy(selectedExerciseIds = updatedIds)
    }

    /**
     * Generates a tailored workout plan based on available machines, fitness level, and Boy/Girl focus,
     * and opens the interactive popup where the user can add/remove exercises before accepting.
     */
    fun generateAiEquipmentSchedule(
        profile: UserProfile,
        onSuccess: (exerciseIds: List<String>, splitTitle: String) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        if (_isGeneratingSchedule.value) return
        _isGeneratingSchedule.value = true

        viewModelScope.launch {
            val workoutCatalog = ExerciseCatalog.allExercises.filter { it.id != "ex_protein_shake_recovery" }
            val equipSummary = profile.equipmentList.take(4).joinToString(", ")
            val isGoogleSignedIn = try { Firebase.auth.currentUser != null } catch (_: Exception) { false }

            try {
                var resolvedTitle: String? = null
                var resolvedIds: List<String>? = null

                if (isGoogleSignedIn) {
                    val catalogSummary = workoutCatalog.joinToString("\n") { ex ->
                        "${ex.id}: ${ex.name} (Equip=${ex.requiredEquipment}, Muscle=${ex.muscleGroup})"
                    }
                    val prompt = """
                        Select 6 optimal exercises for today's workout from the catalog below.
                        - Athlete: ${profile.genderLabel}, Level=${profile.fitnessLevel}, Focus=${profile.primaryFocus}
                        - Available Equipment: ${profile.equipmentList.joinToString(", ")}
                        Catalog:
                        $catalogSummary
                    """.trimIndent()

                    val response = withTimeoutOrNull(4_000L) {
                        structuredScheduleModel.generateContent(prompt)
                    }
                    val jsonText = response?.text
                    if (!jsonText.isNullOrBlank()) {
                        val json = JSONObject(jsonText)
                        val title = json.optString("splitTitle", "${profile.fitnessLevel} ${profile.primaryFocus}")
                        val arr = json.optJSONArray("exerciseIds")
                        val ids = mutableListOf<String>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                ids.add(arr.getString(i))
                            }
                        }
                        val valid = ids.filter { id -> workoutCatalog.any { it.id == id } }
                        if (valid.isNotEmpty()) {
                            resolvedTitle = title
                            resolvedIds = valid
                        }
                    }
                } else {
                    delay(180L)
                }

                val finalPair = if (resolvedIds != null && resolvedTitle != null) {
                    resolvedTitle to resolvedIds
                } else {
                    ExerciseCatalog.recommendExercisesForEquipmentAndLevel(profile)
                }

                _pendingScheduleDraft.value = AutoScheduleDraft(
                    splitTitle = finalPair.first,
                    selectedExerciseIds = finalPair.second,
                    fitnessLevel = profile.fitnessLevel,
                    equipmentSummary = equipSummary
                )
                onSuccess(finalPair.second, finalPair.first)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Using smart equipment & level matcher for schedule draft", e)
                val (fallbackTitle, fallbackIds) = ExerciseCatalog.recommendExercisesForEquipmentAndLevel(profile)
                _pendingScheduleDraft.value = AutoScheduleDraft(
                    splitTitle = fallbackTitle,
                    selectedExerciseIds = fallbackIds,
                    fitnessLevel = profile.fitnessLevel,
                    equipmentSummary = equipSummary
                )
                onSuccess(fallbackIds, fallbackTitle)
            } finally {
                _isGeneratingSchedule.value = false
            }
        }
    }

    fun sendChatMessage(userMessage: String, profile: UserProfile?) {
        val cleanMsg = userMessage.trim()
        if (cleanMsg.isEmpty() || _isChatStreaming.value) return

        val tier = _selectedCoachTier.value
        val userTurn = ChatTurn(
            id = "u_${System.currentTimeMillis()}",
            role = "user",
            text = cleanMsg
        )
        val priorTurns = _chatHistory.value
        _chatHistory.value = priorTurns + userTurn
        _chatError.value = null
        _isChatStreaming.value = true

        viewModelScope.launch {
            val responseId = "m_${System.currentTimeMillis()}"
            val isGoogleSignedIn = try { Firebase.auth.currentUser != null } catch (_: Exception) { false }

            try {
                var cloudReply: String? = null
                if (isGoogleSignedIn) {
                    val activeModel = when (tier) {
                        CoachModelTier.FAST -> fastLiteModel
                        CoachModelTier.GENERAL -> flashModel
                        CoachModelTier.DEEP_BIOSCIENCE -> argonModel
                    }
                    val profileContext = if (profile != null) {
                        "[Context: ${profile.genderLabel} (${profile.ageYears}y), Level=${profile.fitnessLevel}, Focus=${profile.primaryFocus}, " +
                            "${profile.heightCm}cm, ${profile.weightKg}kg, BMI=${profile.bmiValue}, TDEE=${profile.estimatedDailyTdeeKcal}kcal, " +
                            "Equip=${profile.equipmentList.joinToString(",")}]\n"
                    } else {
                        ""
                    }

                    val resp = withTimeoutOrNull(4_500L) {
                        activeModel.generateContent(profileContext + cleanMsg)
                    }
                    cloudReply = resp?.text?.takeIf { it.isNotBlank() }
                } else {
                    // Instant response when Google account is not yet linked
                    delay(220L)
                }

                val finalReply = cloudReply ?: buildSmartLocalCoachReply(cleanMsg, profile, tier)
                val modelTurn = ChatTurn(
                    id = responseId,
                    role = "model",
                    text = finalReply,
                    modelTag = tier.label
                )
                _chatHistory.value = priorTurns + userTurn + modelTurn
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Falling back to instant GymBae coach reply", e)
                val fallbackText = buildSmartLocalCoachReply(cleanMsg, profile, tier)
                val modelTurn = ChatTurn(
                    id = responseId,
                    role = "model",
                    text = fallbackText,
                    modelTag = tier.label
                )
                _chatHistory.value = priorTurns + userTurn + modelTurn
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    private fun buildSmartLocalCoachReply(
        query: String,
        profile: UserProfile?,
        tier: CoachModelTier
    ): String {
        val p = profile ?: UserProfile()
        val q = query.lowercase()
        val (_, recIds) = ExerciseCatalog.recommendExercisesForEquipmentAndLevel(p)
        val recExercises = recIds.mapNotNull { id ->
            ExerciseCatalog.allExercises.firstOrNull { it.id == id }
        }

        val depthNote = when (tier) {
            CoachModelTier.FAST -> "⚡ Fast Cue:"
            CoachModelTier.GENERAL -> "✨ Smart Plan (${p.fitnessLevel}):"
            CoachModelTier.DEEP_BIOSCIENCE -> "🧠 Deep Bioscience (${p.genderLabel} • TDEE ${p.estimatedDailyTdeeKcal} kcal):"
        }

        return when {
            q.contains("no-equip") || q.contains("bodyweight") || q.contains("home") -> {
                val bwList = ExerciseCatalog.allExercises
                    .filter { it.requiredEquipment.equals("Bodyweight", ignoreCase = true) }
                    .take(4)
                    .joinToString("\n") { "• ${it.name} (${it.baseSets}×${it.baseRepsPerSet} reps — ${it.muscleGroup})" }
                "$depthNote **No-Equipment Routine for ${p.primaryFocus}**:\n$bwList\n• Rest 45–60s between sets and sip water every 15 mins."
            }
            q.contains("warmup") || q.contains("week 1") -> {
                "$depthNote **Week 1–2 Warmup & Joint Prep (${p.genderLabel})**:\n" +
                    "• Start with 2 mins of Jumping Jacks & dynamic hip openers\n" +
                    "• Keep effort at RPE 6–7 (2–3 reps in reserve)\n" +
                    "• Focus on controlled 3-second lowering before increasing load."
            }
            q.contains("squat") || q.contains("form") || q.contains("glute") || q.contains("leg") -> {
                if (p.isGirl) {
                    "$depthNote **${p.primaryFocus} Lower-Body & Glute Cues**:\n" +
                        "• Drive through your heels with a 15° toe flare for glute activation\n" +
                        "• Pause 1 second at peak contraction on Hip Bridges & Kickbacks\n" +
                        "• Recommended for your gear: Sumo Squat Pulse, Cable Glute Kickbacks & Curtsy Lunges."
                } else {
                    "$depthNote **${p.primaryFocus} Squat & Lower-Body Cues**:\n" +
                        "• Brace 360° core before descending below parallel\n" +
                        "• Track knees aligned with toes and drive forcefully upward\n" +
                        "• Use a 3-0-1 tempo for optimal mechanical tension."
                }
            }
            q.contains("chest") || q.contains("push") || q.contains("shoulder") || q.contains("arm") -> {
                val pushMoves = ExerciseCatalog.allExercises
                    .filter { it.muscleGroup.contains("Chest", true) || it.muscleGroup.contains("Shoulder", true) || it.muscleGroup.contains("Arm", true) }
                    .take(3)
                    .joinToString("\n") { "• ${it.name} (${it.requiredEquipment}) — ${it.baseSets}×${it.baseRepsPerSet}" }
                "$depthNote **Upper-Body Push & Tone (${p.fitnessLevel})**:\n$pushMoves\n• Keep scapulae retracted and elbows at a 45° angle to protect shoulder joints."
            }
            q.contains("back") || q.contains("pull") || q.contains("posture") || q.contains("lat") -> {
                val pullMoves = ExerciseCatalog.allExercises
                    .filter { it.muscleGroup.contains("Back", true) || it.muscleGroup.contains("Lat", true) }
                    .take(3)
                    .joinToString("\n") { "• ${it.name} (${it.requiredEquipment}) — ${it.baseSets}×${it.baseRepsPerSet}" }
                "$depthNote **Back & Posture Sculpt (${p.fitnessLevel})**:\n$pullMoves\n• Initiate every pull by depressing your shoulder blades before bending your elbows."
            }
            q.contains("bmi") || q.contains("diet") || q.contains("protein") || q.contains("water") || q.contains("calorie") || q.contains("weight") -> {
                val proteinTargetG = (p.weightKg * 1.8).toInt()
                "$depthNote **Nutrition & Hydration Plan (${p.genderLabel} • BMI ${p.bmiValue} ${p.bmiCategory})**:\n" +
                    "• Daily TDEE: ~${p.estimatedDailyTdeeKcal} kcal/day (Current: ${p.weightKg}kg → Goal: ${p.targetWeightKg}kg)\n" +
                    "• Protein Target: ~${proteinTargetG}g/day (${if (p.wantsProteinShake) "includes your 25–30g post-workout shake" else "enable Shake on Today tab for 7pm reminder"})\n" +
                    "• Hydration: 7 glasses (300ml every 3h from 6am to 12am)."
            }
            else -> {
                val topMoves = recExercises.take(4).joinToString("\n") {
                    "• ${it.name} (${it.requiredEquipment}) — ${it.baseSets}×${it.baseRepsPerSet}"
                }
                "$depthNote **Personalized Plan for ${p.displayName} (${p.fitnessLevel} • ${p.primaryFocus})**:\n" +
                    "Matched to your ${p.equipmentList.size} available machines (${p.equipmentList.take(4).joinToString(", ")}):\n" +
                    "$topMoves\n" +
                    "• Tap **Auto-Build Plan** above to customize these exercises in a popup and add them to Today!"
            }
        }
    }

    fun generateBioscienceIntelligenceReport(
        profile: UserProfile,
        recentLogs: List<WorkoutLog>
    ) {
        viewModelScope.launch {
            _bioscienceState.value = BioscienceUiState.Loading
            val totalCals = recentLogs.sumOf { it.caloriesBurned }
            val totalSets = recentLogs.sumOf { it.completedSets }
            val avgRpe = if (recentLogs.isNotEmpty()) {
                recentLogs.map { it.rpeFeedback }.average()
            } else {
                profile.lastFeedbackRpe.toDouble()
            }
            val isGoogleSignedIn = try { Firebase.auth.currentUser != null } catch (_: Exception) { false }

            try {
                var cloudReport: BioscienceReport? = null
                if (isGoogleSignedIn) {
                    val prompt = """
                        Concise GymBae Fitness Report for ${profile.genderLabel} athlete:
                        - ${profile.heightCm}cm, ${profile.weightKg}kg -> Goal ${profile.targetWeightKg}kg, BMI=${profile.bmiValue}, TDEE=${profile.estimatedDailyTdeeKcal} kcal
                        - RHR=${profile.restingHeartRateBpm}bpm, VO2=${profile.vo2MaxEstimate}, Equip=${profile.equipmentList.joinToString(",")}
                        - Sessions=${recentLogs.size} ($totalSets sets, $totalCals kcal, Avg RPE=${"%.1f".format(avgRpe)}/10)
                    """.trimIndent()

                    val response = withTimeoutOrNull(4_500L) {
                        structuredBioscienceModel.generateContent(prompt)
                    }
                    val jsonString = response?.text
                    if (!jsonString.isNullOrBlank()) {
                        val json = JSONObject(jsonString)
                        val itemsArray = json.optJSONArray("actionItems")
                        val actions = mutableListOf<String>()
                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                actions.add(itemsArray.getString(i))
                            }
                        }
                        cloudReport = BioscienceReport(
                            metabolicSummary = json.optString("metabolicSummary", "TDEE ${profile.estimatedDailyTdeeKcal} kcal/day aligned with ${profile.primaryFocus}."),
                            bmiAndVitalAnalysis = json.optString("bmiAndVitalAnalysis", "BMI ${profile.bmiValue} (${profile.bmiCategory}) • RHR ${profile.restingHeartRateBpm} bpm."),
                            hypertrophyPrescription = json.optString("hypertrophyPrescription", "Progressive overload active for ${profile.fitnessLevel} level."),
                            recoveryProtocol = json.optString("recoveryProtocol", "3-hour hydration (6am–12am) & 8h sleep."),
                            actionItems = actions.ifEmpty { listOf("Complete ${profile.weeklyWorkoutGoal} workouts/week", "Hit 7 daily water slots", "Log post-workout RPE") }
                        )
                    }
                } else {
                    delay(200L)
                }

                val finalReport = cloudReport ?: BioscienceReport(
                    metabolicSummary = "Daily TDEE ~${profile.estimatedDailyTdeeKcal} kcal • Logged $totalCals kcal across ${recentLogs.size} sessions.",
                    bmiAndVitalAnalysis = "BMI ${profile.bmiValue} (${profile.bmiCategory}) • RHR ${profile.restingHeartRateBpm} bpm • VO2 ${profile.vo2MaxEstimate} ml/kg.",
                    hypertrophyPrescription = "${profile.genderLabel} ${profile.fitnessLevel} plan focused on ${profile.primaryFocus} using ${profile.equipmentList.size} machines.",
                    recoveryProtocol = "Drink water every 3h (6am–12am, ${profile.todayWaterGlassesCount}/7 today)${if (profile.wantsProteinShake) " + 25–30g protein shake post-workout" else ""}.",
                    actionItems = listOf(
                        "Target ${profile.weeklyWorkoutGoal} days/week consistency",
                        "Maintain RPE 7–8 for silent progressive overload",
                        "Complete all 7 waking-hour hydration slots"
                    )
                )
                _bioscienceState.value = BioscienceUiState.Success(finalReport)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Using smart local bioscience report", e)
                val report = BioscienceReport(
                    metabolicSummary = "Daily TDEE ~${profile.estimatedDailyTdeeKcal} kcal • Logged $totalCals kcal across ${recentLogs.size} sessions.",
                    bmiAndVitalAnalysis = "BMI ${profile.bmiValue} (${profile.bmiCategory}) • RHR ${profile.restingHeartRateBpm} bpm • VO2 ${profile.vo2MaxEstimate} ml/kg.",
                    hypertrophyPrescription = "${profile.genderLabel} ${profile.fitnessLevel} plan focused on ${profile.primaryFocus} using ${profile.equipmentList.size} machines.",
                    recoveryProtocol = "Drink water every 3h (6am–12am)${if (profile.wantsProteinShake) " + 25–30g protein shake post-workout" else ""}.",
                    actionItems = listOf(
                        "Target ${profile.weeklyWorkoutGoal} days/week consistency",
                        "Maintain RPE 7–8 for silent progressive overload",
                        "Complete all 7 waking-hour hydration slots"
                    )
                )
                _bioscienceState.value = BioscienceUiState.Success(report)
            }
        }
    }
}
