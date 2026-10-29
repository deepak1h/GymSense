package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.GymSenseLocalDatabase
import com.example.data.local.LocalWorkoutRecordEntity
import com.example.data.local.LocalWorkoutRepository
import com.example.data.model.ExerciseCatalog
import com.example.data.model.ScheduledExercise
import com.example.data.model.UserProfile
import com.example.data.model.VitalLog
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutSplitDay
import com.example.data.model.todayDayKey
import com.example.data.repository.WorkoutRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val localRepository: LocalWorkoutRepository,
    private val currentUserId: String,
    val isLocalOnlyMode: Boolean = false
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val localProfileFlow: Flow<UserProfile?> = localRepository
        .observeProfile(currentUserId)
        .map { it?.toUserProfile() }
        .catch { emit(null) }

    private val cloudProfileFlow: Flow<UserProfile?> = if (isLocalOnlyMode) {
        flowOf(null)
    } else {
        repository.observeUserProfile(currentUserId)
            .catch { e ->
                Log.w(TAG, "Cloud profile unavailable, falling back to local DB", e)
                emit(null)
            }
    }

    val profileState: StateFlow<DataUiState<UserProfile?>> = combine(
        localProfileFlow,
        cloudProfileFlow
    ) { localProf, cloudProf ->
        DataUiState.Success(cloudProf ?: localProf)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DataUiState.Loading
    )

    private val cloudWorkoutLogsFlow: Flow<List<WorkoutLog>> = if (isLocalOnlyMode) {
        flowOf(emptyList())
    } else {
        repository.observeWorkoutLogs(currentUserId)
            .catch { e ->
                Log.w(TAG, "Cloud workout logs unavailable, using local DB", e)
                emit(emptyList())
            }
    }

    val localRecordsState: StateFlow<List<LocalWorkoutRecordEntity>> = localRepository
        .observeRecords(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val customRoutineIds: StateFlow<List<String>> = localRepository
        .observeCustomRoutine(currentUserId)
        .map { entry ->
            entry?.exerciseIdsCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val todayExtraIds: StateFlow<List<String>> = localRepository
        .observeTodayExtraExercises(currentUserId, todayDayKey())
        .map { entry ->
            entry?.exerciseIdsCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val combinedWorkoutLogs: StateFlow<List<WorkoutLog>> = combine(
        cloudWorkoutLogsFlow,
        localRecordsState
    ) { cloudLogs, localList ->
        val localMapped = localList.map { rec ->
            WorkoutLog(
                id = "local_${rec.localId}",
                userId = rec.userId,
                exerciseId = rec.exerciseId,
                exerciseName = rec.exerciseName,
                muscleGroup = rec.muscleGroup,
                equipmentUsed = rec.equipmentUsed,
                completedSets = rec.completedSets.toLong(),
                totalReps = rec.totalReps.toLong(),
                weightUsedKg = rec.weightUsedKg.toLong(),
                durationSeconds = rec.durationSeconds.toLong(),
                caloriesBurned = rec.caloriesBurned.toLong(),
                rpeFeedback = rec.rpeFeedback.toLong(),
                dayKey = rec.dayKey
            )
        }
        val cloudKeys = cloudLogs.map { "${it.dayKey}_${it.exerciseId}_${it.totalReps}_${it.completedSets}" }.toSet()
        val uniqueLocal = localMapped.filter {
            "${it.dayKey}_${it.exerciseId}_${it.totalReps}_${it.completedSets}" !in cloudKeys
        }
        cloudLogs + uniqueLocal
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val localVitalsFlow: Flow<List<VitalLog>> = localRepository
        .observeVitals(currentUserId)
        .map { list -> list.map { it.toVitalLog() } }
        .catch { emit(emptyList()) }

    private val cloudVitalsFlow: Flow<List<VitalLog>> = if (isLocalOnlyMode) {
        flowOf(emptyList())
    } else {
        repository.observeVitalLogs(currentUserId)
            .catch { emit(emptyList()) }
    }

    val vitalLogsState: StateFlow<DataUiState<List<VitalLog>>> = combine(
        cloudVitalsFlow,
        localVitalsFlow
    ) { cloudVitals, localVitals ->
        DataUiState.Success((cloudVitals + localVitals).distinctBy { "${it.weightKg}_${it.restingHeartRateBpm}_${it.notes}" })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DataUiState.Loading
    )

    val todaySchedule: StateFlow<List<ScheduledExercise>> = combine(
        profileState,
        combinedWorkoutLogs,
        customRoutineIds,
        todayExtraIds
    ) { profState, allLogs, customIds, extraIds ->
        val profile = (profState as? DataUiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val today = todayDayKey()
        val todayLogs = allLogs.filter { it.dayKey == today }
        ExerciseCatalog.buildDynamicSchedule(
            profile = profile,
            todayLogs = todayLogs,
            customRoutineIds = customIds,
            todayExtraIds = extraIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun dismissBanner() {
        _statusBanner.value = null
    }

    private fun currentOrDefaultProfile(): UserProfile {
        return (profileState.value as? DataUiState.Success)?.data ?: UserProfile(userId = currentUserId)
    }

    private suspend fun persistProfileBothOrLocal(profile: UserProfile) {
        localRepository.saveProfile(profile)
        if (!isLocalOnlyMode) {
            repository.saveUserProfile(profile)
        }
    }

    fun skipSetupToDefault() {
        viewModelScope.launch {
            val base = currentOrDefaultProfile().copy(
                userId = currentUserId,
                setupCompleted = true
            )
            persistProfileBothOrLocal(base)
            _statusBanner.value = "Setup skipped — using default GymSense schedule!"
        }
    }

    fun toggleExtraExerciseToday(exerciseId: String) {
        viewModelScope.launch {
            val current = todayExtraIds.value.toMutableList()
            val exName = ExerciseCatalog.allExercises.firstOrNull { it.id == exerciseId }?.name ?: "Exercise"
            if (current.contains(exerciseId)) {
                current.remove(exerciseId)
                localRepository.saveTodayExtraExercises(currentUserId, todayDayKey(), current)
                _statusBanner.value = "Removed $exName from today"
            } else {
                current.add(exerciseId)
                localRepository.saveTodayExtraExercises(currentUserId, todayDayKey(), current)
                _statusBanner.value = "Added $exName to today's split!"
            }
        }
    }

    fun saveCustomUserRoutine(exerciseIds: List<String>) {
        viewModelScope.launch {
            localRepository.saveCustomRoutine(currentUserId, exerciseIds)
            val base = currentOrDefaultProfile()
            val updated = base.withUpdatedPreferences(
                splitOverrideCode = WorkoutSplitDay.CUSTOM_MY_PLAN.code,
                isWeek1To2Warmup = false,
                aiPinnedIds = exerciseIds
            ).copy(setupCompleted = true)
            persistProfileBothOrLocal(updated)
            _statusBanner.value = "Saved My Plan (${exerciseIds.size} exercises)!"
        }
    }

    fun toggleGender(isGirl: Boolean) {
        viewModelScope.launch {
            val base = currentOrDefaultProfile()
            val updated = base.withUpdatedPreferences(isGirl = isGirl).copy(setupCompleted = true)
            persistProfileBothOrLocal(updated)
            _statusBanner.value = "Switched to ${if (isGirl) "Girl" else "Boy"} profile"
        }
    }

    fun toggleWeek1To2Warmup(enabled: Boolean) {
        viewModelScope.launch {
            val base = currentOrDefaultProfile()
            val updated = base.withUpdatedPreferences(
                isWeek1To2Warmup = enabled,
                splitOverrideCode = if (enabled) WorkoutSplitDay.WARMUP_LIGHT.code else "AUTO"
            ).copy(setupCompleted = true)
            persistProfileBothOrLocal(updated)
            _statusBanner.value = if (enabled) "Wk 1-2 Warmup active" else "Standard split active"
        }
    }

    fun selectSplitOverride(split: WorkoutSplitDay) {
        viewModelScope.launch {
            val base = currentOrDefaultProfile()
            val updated = base.withUpdatedPreferences(
                splitOverrideCode = split.code,
                isWeek1To2Warmup = (split == WorkoutSplitDay.WARMUP_LIGHT),
                aiPinnedIds = if (split == WorkoutSplitDay.CUSTOM_MY_PLAN) customRoutineIds.value else emptyList()
            ).copy(setupCompleted = true)
            persistProfileBothOrLocal(updated)
            _statusBanner.value = "Split: ${split.shortTitle}"
        }
    }

    fun shiftScheduleForMissedDay(deltaDays: Int = 1, onPopupResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val base = currentOrDefaultProfile()
            val newOffset = base.missedDaysOffset + deltaDays
            val updated = base.withUpdatedPreferences(
                missedDaysOffset = newOffset,
                splitOverrideCode = "AUTO",
                isWeek1To2Warmup = false,
                aiPinnedIds = emptyList()
            ).copy(setupCompleted = true)
            persistProfileBothOrLocal(updated)
            val newSplit = ExerciseCatalog.resolveTodaySplit(updated)
            val msg = "Shifted +$deltaDays day! Today's split is now ${newSplit.shortTitle} (${newSplit.subtitle})."
            _statusBanner.value = msg
            onPopupResult(msg)
        }
    }

    fun applyAiCuratedSchedule(exerciseIds: List<String>, splitSummary: String) {
        viewModelScope.launch {
            localRepository.saveCustomRoutine(currentUserId, exerciseIds)
            val base = currentOrDefaultProfile()
            val updated = base.withUpdatedPreferences(
                splitOverrideCode = "AUTO",
                isWeek1To2Warmup = false,
                aiPinnedIds = exerciseIds
            ).copy(setupCompleted = true)
            persistProfileBothOrLocal(updated)
            _statusBanner.value = "GymBae Plan Ready: $splitSummary"
        }
    }

    fun saveSetupAndVitals(
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
        isGirl: Boolean = currentOrDefaultProfile().isGirl,
        isWeek1To2Warmup: Boolean = currentOrDefaultProfile().isWeek1To2Warmup,
        lastFeedbackRpe: Long = 7L,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val base = currentOrDefaultProfile().copy(
                userId = currentUserId,
                displayName = displayName.ifBlank { "Athlete" },
                heightCm = heightCm.coerceIn(80L, 250L),
                weightKg = weightKg.coerceIn(25L, 300L),
                targetWeightKg = targetWeightKg.coerceIn(25L, 300L),
                weeklyWorkoutGoal = weeklyWorkoutGoal.coerceIn(1L, 14L),
                targetCaloriesPerWeek = targetCaloriesPerWeek.coerceIn(200L, 25000L),
                restingHeartRateBpm = restingHeartRateBpm.coerceIn(35L, 150L),
                vo2MaxEstimate = vo2MaxEstimate.coerceIn(15L, 95L),
                fitnessLevel = fitnessLevel,
                primaryFocus = primaryFocus,
                lastFeedbackRpe = lastFeedbackRpe.coerceIn(1L, 10L),
                setupCompleted = true
            )
            val updatedProfile = base.withUpdatedPreferences(
                equipment = selectedEquipment.ifEmpty { listOf("Bodyweight") },
                isGirl = isGirl,
                isWeek1To2Warmup = isWeek1To2Warmup
            )

            persistProfileBothOrLocal(updatedProfile)
            val vital = VitalLog(
                userId = currentUserId,
                weightKg = updatedProfile.weightKg,
                bmiTenths = updatedProfile.bmiTenths,
                restingHeartRateBpm = updatedProfile.restingHeartRateBpm,
                recoveryScore = 88L,
                notes = "Gear & Vitals Saved"
            )
            localRepository.saveVital(vital)
            if (!isLocalOnlyMode) {
                repository.logVitalsCheckIn(vital)
            }
            _statusBanner.value = "Saved! BMI ${updatedProfile.bmiValue}"
            onComplete()
            _isSaving.value = false
        }
    }

    fun recordCompletedExercise(
        scheduled: ScheduledExercise,
        completedSets: Int,
        totalReps: Int,
        weightUsedKg: Int,
        durationSeconds: Int,
        rpeFeedback: Int,
        onFinished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val calories = (completedSets * scheduled.template.caloriesPerSet).coerceIn(10, 2500)
            val safeSets = completedSets.coerceIn(1, 20)
            val safeReps = totalReps.coerceIn(1, 500)
            val safeWeight = weightUsedKg.coerceIn(0, 500)
            val safeDuration = durationSeconds.coerceIn(10, 14400)
            val safeRpe = rpeFeedback.coerceIn(1, 10)
            val today = todayDayKey()

            // 1. Always save locally in Room SQLite immediately
            localRepository.saveWorkoutRecord(
                LocalWorkoutRecordEntity(
                    userId = currentUserId,
                    exerciseId = scheduled.template.id,
                    exerciseName = scheduled.template.name,
                    muscleGroup = scheduled.template.muscleGroup,
                    equipmentUsed = scheduled.template.requiredEquipment,
                    completedSets = safeSets,
                    totalReps = safeReps,
                    weightUsedKg = safeWeight,
                    durationSeconds = safeDuration,
                    caloriesBurned = calories,
                    rpeFeedback = safeRpe,
                    dayKey = today
                )
            )

            // 2. Also sync to Firestore if signed in with Google
            if (!isLocalOnlyMode) {
                val log = WorkoutLog(
                    userId = currentUserId,
                    exerciseId = scheduled.template.id,
                    exerciseName = scheduled.template.name,
                    muscleGroup = scheduled.template.muscleGroup,
                    equipmentUsed = scheduled.template.requiredEquipment,
                    completedSets = safeSets.toLong(),
                    totalReps = safeReps.toLong(),
                    weightUsedKg = safeWeight.toLong(),
                    durationSeconds = safeDuration.toLong(),
                    caloriesBurned = calories.toLong(),
                    rpeFeedback = safeRpe.toLong(),
                    dayKey = today
                )
                repository.logWorkoutSession(log)
            }

            val currentProfile = currentOrDefaultProfile()
            persistProfileBothOrLocal(
                currentProfile.copy(
                    lastFeedbackRpe = safeRpe.toLong(),
                    setupCompleted = true
                )
            )

            _statusBanner.value = if (isLocalOnlyMode) {
                "Saved to Local DB: $safeSets sets • $safeReps reps!"
            } else {
                "Saved Locally & Cloud: $safeSets sets • $safeReps reps!"
            }
            onFinished()
            _isSaving.value = false
        }
    }

    fun logQuickVitalCheckIn(
        weightKg: Long,
        restingHeartRateBpm: Long,
        recoveryScore: Long,
        notes: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val currentProfile = currentOrDefaultProfile()
            val updatedProfile = currentProfile.copy(
                weightKg = weightKg.coerceIn(25L, 300L),
                restingHeartRateBpm = restingHeartRateBpm.coerceIn(35L, 150L),
                setupCompleted = true
            )
            persistProfileBothOrLocal(updatedProfile)
            val vital = VitalLog(
                userId = currentUserId,
                weightKg = updatedProfile.weightKg,
                bmiTenths = updatedProfile.bmiTenths,
                restingHeartRateBpm = updatedProfile.restingHeartRateBpm,
                recoveryScore = recoveryScore.coerceIn(1L, 100L),
                notes = notes.take(300)
            )
            localRepository.saveVital(vital)
            if (!isLocalOnlyMode) {
                repository.logVitalsCheckIn(vital)
            }
            _statusBanner.value = "Vitals logged (BMI ${updatedProfile.bmiValue})"
            _isSaving.value = false
        }
    }

    fun deleteWorkoutLog(logId: String) {
        viewModelScope.launch {
            if (logId.startsWith("local_")) {
                val localId = logId.removePrefix("local_").toLongOrNull()
                if (localId != null) {
                    localRepository.deleteLocalRecord(localId)
                }
            } else if (!isLocalOnlyMode) {
                repository.deleteWorkoutLog(logId)
            }
        }
    }

    companion object {
        private const val TAG = "WorkoutViewModel"

        fun provideFactory(
            currentUserId: String,
            isLocalOnlyMode: Boolean = false
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDb = GymSenseLocalDatabase.getInstance(app)
                WorkoutViewModel(
                    repository = WorkoutRepository(db),
                    localRepository = LocalWorkoutRepository(localDb.gymSenseDao()),
                    currentUserId = currentUserId,
                    isLocalOnlyMode = isLocalOnlyMode
                )
            }
        }
    }
}
