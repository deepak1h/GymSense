package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.example.data.model.VitalLog
import com.example.data.model.WorkoutLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WorkoutRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = WorkoutRepository(firestore)

        val profile = UserProfile(
            userId = aliceUid,
            displayName = "Alice Kinesis",
            heightCm = 172L,
            weightKg = 68L,
            targetWeightKg = 64L,
            weeklyWorkoutGoal = 5L,
            targetCaloriesPerWeek = 2400L,
            restingHeartRateBpm = 56L,
            vo2MaxEstimate = 51L,
            fitnessLevel = "Intermediate",
            primaryFocus = "Hypertrophy",
            equipmentCsv = "Dumbbells,Barbell,Cable Machine",
            lastFeedbackRpe = 7L,
            setupCompleted = true
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveUserProfile(profile) }
        assertTrue("Expected saveUserProfile to succeed", saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(aliceUid).first { it != null && it.setupCompleted }
        }
        assertNotNull(observed)
        assertEquals("Alice Kinesis", observed?.displayName)
        assertEquals(68L, observed?.weightKg)
    }

    @Test
    fun logWorkoutAndVitals_authenticatedOwner_emitsInRealtime() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = WorkoutRepository(firestore)

        val logIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logWorkoutSession(
                WorkoutLog(
                    userId = aliceUid,
                    exerciseId = "ex_barbell_bench",
                    exerciseName = "Barbell Biomechanic Bench Press",
                    muscleGroup = "Chest",
                    equipmentUsed = "Barbell",
                    completedSets = 4L,
                    totalReps = 40L,
                    weightUsedKg = 65L,
                    durationSeconds = 360L,
                    caloriesBurned = 130L,
                    rpeFeedback = 8L,
                    dayKey = "2026-10-29"
                )
            )
        }
        assertTrue(logIdResult.isSuccess)
        val logId = logIdResult.getOrThrow()

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkoutLogs(aliceUid).first { list -> list.any { it.id == logId } }
        }
        assertTrue(logs.any { it.id == logId })

        val vitalIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logVitalsCheckIn(
                VitalLog(
                    userId = aliceUid,
                    weightKg = 68L,
                    bmiTenths = 230L,
                    restingHeartRateBpm = 55L,
                    recoveryScore = 90L,
                    notes = "Great HRV recovery"
                )
            )
        }
        assertTrue(vitalIdResult.isSuccess)
    }

    @Test
    fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = WorkoutRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserProfile(
                UserProfile(
                    userId = aliceUid,
                    displayName = "Alice Private",
                    setupCompleted = true
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = WorkoutRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfile(aliceUid)
        }
        assertTrue("Expected cross-user read to fail", crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeWorkoutLogs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = WorkoutRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWorkoutLogs(aliceUid).first()
            }
            fail("Expected unauthenticated observeWorkoutLogs to throw exception")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_kinesis@test.com"
        const val BOB_EMAIL = "bob_kinesis@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
