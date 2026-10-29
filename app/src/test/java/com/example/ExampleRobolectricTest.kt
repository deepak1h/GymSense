package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExerciseCatalog
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kinesis BioLab", appName)
    }

    @Test
    fun `dynamic schedule adjusts to equipment and rpe feedback`() {
        val lowRpeProfile = UserProfile(
            userId = "test_user",
            heightCm = 180L,
            weightKg = 80L,
            equipmentCsv = "Dumbbells,Barbell",
            lastFeedbackRpe = 4L
        )
        val highRpeProfile = lowRpeProfile.copy(lastFeedbackRpe = 9L)

        val lowRpeSchedule = ExerciseCatalog.buildDynamicSchedule(lowRpeProfile, emptyList())
        val highRpeSchedule = ExerciseCatalog.buildDynamicSchedule(highRpeProfile, emptyList())

        assertTrue(lowRpeSchedule.isNotEmpty())
        assertTrue(lowRpeSchedule.first().recommendedWeightKg > highRpeSchedule.first().recommendedWeightKg)
        assertEquals(24.7, lowRpeProfile.bmiValue, 0.1)
    }
}
