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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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

sealed interface BioscienceUiState {
    data object Idle : BioscienceUiState
    data object Loading : BioscienceUiState
    data class Success(val report: BioscienceReport) : BioscienceUiState
    data class Error(val message: String) : BioscienceUiState
}

enum class CoachModelTier(val modelId: String, val label: String, val subtitle: String) {
    FAST("gemini-3.5-flash-lite", "Fast", "Quick Cues"),
    GENERAL("gemini-3.8-flash", "Smart", "GymBae Pro"),
    DEEP_BIOSCIENCE("gemini-4-argon", "Deep", "Full Analysis")
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
                text = "Hey! I'm GymBae. Ask me for exercise swaps, form tips, or tap 'Auto-Build Schedule' above!",
                modelTag = CoachModelTier.GENERAL.label
            )
        )
    )
    val chatHistory: StateFlow<List<ChatTurn>> = _chatHistory.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _isGeneratingSchedule = MutableStateFlow(false)
    val isGeneratingSchedule: StateFlow<Boolean> = _isGeneratingSchedule.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _bioscienceState = MutableStateFlow<BioscienceUiState>(BioscienceUiState.Idle)
    val bioscienceState: StateFlow<BioscienceUiState> = _bioscienceState.asStateFlow()

    fun selectCoachTier(tier: CoachModelTier) {
        _selectedCoachTier.value = tier
    }

    fun generateAiEquipmentSchedule(
        profile: UserProfile,
        onSuccess: (exerciseIds: List<String>, splitTitle: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isGeneratingSchedule.value) return
        _isGeneratingSchedule.value = true

        viewModelScope.launch {
            val catalogSummary = ExerciseCatalog.allExercises.joinToString("\n") { ex ->
                "${ex.id}: ${ex.name} (Equip=${ex.requiredEquipment}, Muscle=${ex.muscleGroup}, Warmup=${ex.isWarmupOrBeginnerFriendly})"
            }
            val prompt = """
                Select 6 optimal exercises for today's workout from the catalog below.
                - Athlete: ${profile.genderLabel}, Level=${profile.fitnessLevel}, Week1-2Beginner=${profile.isWeek1To2Warmup}, Focus=${profile.primaryFocus}
                - Available Equipment: ${profile.equipmentList.joinToString(", ")} (plus Bodyweight)
                - Always include at least 1 warmup/mobility exercise and at least 2 Bodyweight (no-equipment) exercises alongside equipment exercises.
                Catalog:
                $catalogSummary
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    structuredScheduleModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Empty schedule response")
                val json = JSONObject(jsonText)
                val title = json.optString("splitTitle", "GymBae Custom Plan")
                val arr = json.optJSONArray("exerciseIds")
                val ids = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        ids.add(arr.getString(i))
                    }
                }
                val validIds = ids.filter { id -> ExerciseCatalog.allExercises.any { it.id == id } }
                if (validIds.isNotEmpty()) {
                    onSuccess(validIds, title)
                } else {
                    onError("Could not match exercises from response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "GymBae schedule timed out", e)
                onError("Schedule request timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "GymBae schedule generation failed", e)
                onError(e.localizedMessage ?: "Schedule failed.")
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
            val activeModel = when (tier) {
                CoachModelTier.FAST -> fastLiteModel
                CoachModelTier.GENERAL -> flashModel
                CoachModelTier.DEEP_BIOSCIENCE -> argonModel
            }

            val profileContext = if (profile != null) {
                "[Context: ${profile.genderLabel}, ${profile.heightCm}cm, ${profile.weightKg}kg, BMI=${profile.bmiValue}, " +
                    "Equip=${profile.equipmentList.joinToString(",")}, Wk1-2=${profile.isWeek1To2Warmup}]\n"
            } else {
                ""
            }

            val sdkHistory = priorTurns.map { turn ->
                content(turn.role) { text(turn.text) }
            }

            val responseId = "m_${System.currentTimeMillis()}"
            val accumulated = StringBuilder()

            try {
                val chat = activeModel.startChat(history = sdkHistory)
                withTimeout(60_000L) {
                    chat.sendMessageStream(profileContext + cleanMsg).collect { chunk ->
                        val partText = chunk.text.orEmpty()
                        if (partText.isNotEmpty()) {
                            accumulated.append(partText)
                            val updatedModelTurn = ChatTurn(
                                id = responseId,
                                role = "model",
                                text = accumulated.toString(),
                                modelTag = tier.label
                            )
                            _chatHistory.value = priorTurns + userTurn + updatedModelTurn
                        }
                    }
                }
                if (accumulated.isBlank()) {
                    _chatError.value = "GymBae returned an empty response."
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat streaming timed out", e)
                _chatError.value = "Timed out after 60s."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation failed", e)
                _chatError.value = e.localizedMessage ?: "GymBae failed to respond."
            } finally {
                _isChatStreaming.value = false
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

            val prompt = """
                Concise GymBae Fitness Report for ${profile.genderLabel} athlete:
                - ${profile.heightCm}cm, ${profile.weightKg}kg -> Goal ${profile.targetWeightKg}kg, BMI=${profile.bmiValue}, TDEE=${profile.estimatedDailyTdeeKcal} kcal
                - RHR=${profile.restingHeartRateBpm}bpm, VO2=${profile.vo2MaxEstimate}, Equip=${profile.equipmentList.joinToString(",")}
                - Sessions=${recentLogs.size} ($totalSets sets, $totalCals kcal, Avg RPE=${"%.1f".format(avgRpe)}/10)
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    structuredBioscienceModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Model returned empty JSON.")
                val json = JSONObject(jsonString)
                val itemsArray = json.optJSONArray("actionItems")
                val actions = mutableListOf<String>()
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        actions.add(itemsArray.getString(i))
                    }
                }
                val report = BioscienceReport(
                    metabolicSummary = json.optString("metabolicSummary", "TDEE & caloric burn aligned."),
                    bmiAndVitalAnalysis = json.optString("bmiAndVitalAnalysis", "BMI & vitals tracked."),
                    hypertrophyPrescription = json.optString("hypertrophyPrescription", "Progressive overload active."),
                    recoveryProtocol = json.optString("recoveryProtocol", "8h sleep & hydration."),
                    actionItems = actions.ifEmpty { listOf("Hit weekly target", "Log RPE feedback") }
                )
                _bioscienceState.value = BioscienceUiState.Success(report)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "GymBae report timed out", e)
                _bioscienceState.value = BioscienceUiState.Error("Timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate GymBae report", e)
                _bioscienceState.value = BioscienceUiState.Error(
                    e.localizedMessage ?: "Failed to generate report."
                )
            }
        }
    }
}
