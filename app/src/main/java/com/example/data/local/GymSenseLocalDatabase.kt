package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.UserProfile
import com.example.data.model.VitalLog
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_user_profiles")
data class LocalUserProfileEntity(
    @PrimaryKey val userId: String,
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
    val setupCompleted: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toUserProfile(): UserProfile = UserProfile(
        id = userId,
        userId = userId,
        displayName = displayName,
        heightCm = heightCm,
        weightKg = weightKg,
        targetWeightKg = targetWeightKg,
        weeklyWorkoutGoal = weeklyWorkoutGoal,
        targetCaloriesPerWeek = targetCaloriesPerWeek,
        restingHeartRateBpm = restingHeartRateBpm,
        vo2MaxEstimate = vo2MaxEstimate,
        fitnessLevel = fitnessLevel,
        primaryFocus = primaryFocus,
        equipmentCsv = equipmentCsv,
        lastFeedbackRpe = lastFeedbackRpe,
        setupCompleted = setupCompleted
    )

    companion object {
        fun fromUserProfile(profile: UserProfile): LocalUserProfileEntity = LocalUserProfileEntity(
            userId = profile.userId.ifBlank { "local_offline_athlete" },
            displayName = profile.displayName,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            targetWeightKg = profile.targetWeightKg,
            weeklyWorkoutGoal = profile.weeklyWorkoutGoal,
            targetCaloriesPerWeek = profile.targetCaloriesPerWeek,
            restingHeartRateBpm = profile.restingHeartRateBpm,
            vo2MaxEstimate = profile.vo2MaxEstimate,
            fitnessLevel = profile.fitnessLevel,
            primaryFocus = profile.primaryFocus,
            equipmentCsv = profile.equipmentCsv,
            lastFeedbackRpe = profile.lastFeedbackRpe,
            setupCompleted = profile.setupCompleted
        )
    }
}

@Entity(tableName = "local_vital_logs")
data class LocalVitalLogEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val userId: String,
    val weightKg: Long,
    val bmiTenths: Long,
    val restingHeartRateBpm: Long,
    val recoveryScore: Long,
    val notes: String,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    fun toVitalLog(): VitalLog = VitalLog(
        id = "local_vital_$localId",
        userId = userId,
        weightKg = weightKg,
        bmiTenths = bmiTenths,
        restingHeartRateBpm = restingHeartRateBpm,
        recoveryScore = recoveryScore,
        notes = notes
    )
}

@Entity(tableName = "local_workout_records")
data class LocalWorkoutRecordEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val userId: String,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val equipmentUsed: String,
    val completedSets: Int,
    val totalReps: Int,
    val weightUsedKg: Int,
    val durationSeconds: Int,
    val caloriesBurned: Int,
    val rpeFeedback: Int,
    val dayKey: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_custom_schedule")
data class LocalCustomScheduleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val modeKey: String,
    val dayKey: String,
    val exerciseIdsCsv: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface GymSenseLocalDao {
    @Query("SELECT * FROM local_user_profiles WHERE userId = :userId LIMIT 1")
    fun observeLocalProfile(userId: String): Flow<LocalUserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalProfile(profile: LocalUserProfileEntity)

    @Query("SELECT * FROM local_vital_logs WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeLocalVitals(userId: String): Flow<List<LocalVitalLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalVital(vital: LocalVitalLogEntity): Long

    @Query("SELECT * FROM local_workout_records WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeUserWorkoutRecords(userId: String): Flow<List<LocalWorkoutRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutRecord(record: LocalWorkoutRecordEntity): Long

    @Query("DELETE FROM local_workout_records WHERE localId = :localId")
    suspend fun deleteLocalRecordById(localId: Long)

    @Query("SELECT * FROM local_custom_schedule WHERE id = :id LIMIT 1")
    fun observeScheduleEntry(id: String): Flow<LocalCustomScheduleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleEntry(entry: LocalCustomScheduleEntity)
}

@Database(
    entities = [
        LocalUserProfileEntity::class,
        LocalVitalLogEntity::class,
        LocalWorkoutRecordEntity::class,
        LocalCustomScheduleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GymSenseLocalDatabase : RoomDatabase() {
    abstract fun gymSenseDao(): GymSenseLocalDao

    companion object {
        @Volatile
        private var INSTANCE: GymSenseLocalDatabase? = null

        fun getInstance(context: Context): GymSenseLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymSenseLocalDatabase::class.java,
                    "gymsense_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class LocalWorkoutRepository(private val dao: GymSenseLocalDao) {
    fun observeProfile(userId: String): Flow<LocalUserProfileEntity?> =
        dao.observeLocalProfile(userId)

    suspend fun saveProfile(profile: UserProfile) =
        dao.upsertLocalProfile(LocalUserProfileEntity.fromUserProfile(profile))

    fun observeVitals(userId: String): Flow<List<LocalVitalLogEntity>> =
        dao.observeLocalVitals(userId)

    suspend fun saveVital(vital: VitalLog): Long =
        dao.insertLocalVital(
            LocalVitalLogEntity(
                userId = vital.userId,
                weightKg = vital.weightKg,
                bmiTenths = vital.bmiTenths,
                restingHeartRateBpm = vital.restingHeartRateBpm,
                recoveryScore = vital.recoveryScore,
                notes = vital.notes
            )
        )

    fun observeRecords(userId: String): Flow<List<LocalWorkoutRecordEntity>> =
        dao.observeUserWorkoutRecords(userId)

    fun observeCustomRoutine(userId: String): Flow<LocalCustomScheduleEntity?> =
        dao.observeScheduleEntry("${userId}_custom")

    fun observeTodayExtraExercises(userId: String, dayKey: String): Flow<LocalCustomScheduleEntity?> =
        dao.observeScheduleEntry("${userId}_extra_$dayKey")

    suspend fun saveWorkoutRecord(record: LocalWorkoutRecordEntity): Long =
        dao.insertWorkoutRecord(record)

    suspend fun deleteLocalRecord(localId: Long) =
        dao.deleteLocalRecordById(localId)

    suspend fun saveCustomRoutine(userId: String, exerciseIds: List<String>) {
        dao.upsertScheduleEntry(
            LocalCustomScheduleEntity(
                id = "${userId}_custom",
                userId = userId,
                modeKey = "CUSTOM_ROUTINE",
                dayKey = "ALL",
                exerciseIdsCsv = exerciseIds.distinct().joinToString(",")
            )
        )
    }

    suspend fun saveTodayExtraExercises(userId: String, dayKey: String, exerciseIds: List<String>) {
        dao.upsertScheduleEntry(
            LocalCustomScheduleEntity(
                id = "${userId}_extra_$dayKey",
                userId = userId,
                modeKey = "TODAY_EXTRA",
                dayKey = dayKey,
                exerciseIdsCsv = exerciseIds.distinct().joinToString(",")
            )
        )
    }
}
