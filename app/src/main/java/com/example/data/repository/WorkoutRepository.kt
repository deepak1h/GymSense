package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.VitalLog
import com.example.data.model.WorkoutLog
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WorkoutRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeWorkoutLogs(userId: String): Flow<List<WorkoutLog>> = flow {
        val path = "users/$userId/workout_logs"
        emitAll(
            db.collection("users").document(userId).collection("workout_logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeVitalLogs(userId: String): Flow<List<VitalLog>> = flow {
        val path = "users/$userId/vital_logs"
        emitAll(
            db.collection("users").document(userId).collection("vital_logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VitalLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "displayName" to profile.displayName.take(80).ifBlank { "Athlete" },
                    "heightCm" to profile.heightCm.coerceIn(80L, 250L),
                    "weightKg" to profile.weightKg.coerceIn(25L, 300L),
                    "targetWeightKg" to profile.targetWeightKg.coerceIn(25L, 300L),
                    "weeklyWorkoutGoal" to profile.weeklyWorkoutGoal.coerceIn(1L, 14L),
                    "targetCaloriesPerWeek" to profile.targetCaloriesPerWeek.coerceIn(200L, 25000L),
                    "restingHeartRateBpm" to profile.restingHeartRateBpm.coerceIn(35L, 150L),
                    "vo2MaxEstimate" to profile.vo2MaxEstimate.coerceIn(15L, 95L),
                    "fitnessLevel" to profile.fitnessLevel,
                    "primaryFocus" to profile.primaryFocus,
                    "equipmentCsv" to profile.equipmentCsv.take(500).ifBlank { "Bodyweight" },
                    "lastFeedbackRpe" to profile.lastFeedbackRpe.coerceIn(1L, 10L),
                    "setupCompleted" to profile.setupCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.take(80).ifBlank { "Athlete" },
                    "heightCm" to profile.heightCm.coerceIn(80L, 250L),
                    "weightKg" to profile.weightKg.coerceIn(25L, 300L),
                    "targetWeightKg" to profile.targetWeightKg.coerceIn(25L, 300L),
                    "weeklyWorkoutGoal" to profile.weeklyWorkoutGoal.coerceIn(1L, 14L),
                    "targetCaloriesPerWeek" to profile.targetCaloriesPerWeek.coerceIn(200L, 25000L),
                    "restingHeartRateBpm" to profile.restingHeartRateBpm.coerceIn(35L, 150L),
                    "vo2MaxEstimate" to profile.vo2MaxEstimate.coerceIn(15L, 95L),
                    "fitnessLevel" to profile.fitnessLevel,
                    "primaryFocus" to profile.primaryFocus,
                    "equipmentCsv" to profile.equipmentCsv.take(500).ifBlank { "Bodyweight" },
                    "lastFeedbackRpe" to profile.lastFeedbackRpe.coerceIn(1L, 10L),
                    "setupCompleted" to profile.setupCompleted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun logWorkoutSession(log: WorkoutLog): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("workout_logs")
        val docRef = colRef.document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "exerciseId" to log.exerciseId.take(80).ifBlank { "ex_custom" },
                "exerciseName" to log.exerciseName.take(100).ifBlank { "Workout" },
                "muscleGroup" to log.muscleGroup.take(60).ifBlank { "Full Body" },
                "equipmentUsed" to log.equipmentUsed.take(80).ifBlank { "Bodyweight" },
                "completedSets" to log.completedSets.coerceIn(1L, 20L),
                "totalReps" to log.totalReps.coerceIn(1L, 500L),
                "weightUsedKg" to log.weightUsedKg.coerceIn(0L, 500L),
                "durationSeconds" to log.durationSeconds.coerceIn(10L, 14400L),
                "caloriesBurned" to log.caloriesBurned.coerceIn(5L, 3000L),
                "rpeFeedback" to log.rpeFeedback.coerceIn(1L, 10L),
                "dayKey" to log.dayKey.take(20).ifBlank { "2026-10-29" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun logVitalsCheckIn(vital: VitalLog): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("vital_logs")
        val docRef = colRef.document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "weightKg" to vital.weightKg.coerceIn(25L, 300L),
                "bmiTenths" to vital.bmiTenths.coerceIn(100L, 600L),
                "restingHeartRateBpm" to vital.restingHeartRateBpm.coerceIn(35L, 150L),
                "recoveryScore" to vital.recoveryScore.coerceIn(1L, 100L),
                "notes" to vital.notes.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorkoutLog(logId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workout_logs").document(logId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
