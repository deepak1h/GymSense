package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ExerciseCatalog
import com.example.data.model.ScheduledExercise
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutUser
import com.example.ui.components.ExerciseSessionSheet
import com.example.ui.components.StickmanExerciseCanvas
import com.example.ui.screens.AiCoachAndVisualLabScreen
import com.example.ui.screens.AnalyticsPortalScreen
import com.example.ui.screens.GoalsAndMilestonesScreen
import com.example.ui.screens.SetupAndVitalsScreen
import com.example.ui.screens.TodayScheduleScreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppVersionInfo
import com.example.util.GymSenseReminderScheduler
import com.example.util.configureAppCheck
import com.example.viewmodel.AiStudioViewModel
import com.example.viewmodel.AutoScheduleDraft
import com.example.viewmodel.DataUiState
import com.example.viewmodel.WorkoutViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val PREFS_NAME = "gymsense_mode_prefs"
private const val KEY_LOCAL_ONLY_MODE = "use_local_database_only"
private const val KEY_DARK_THEME_MODE = "use_dark_theme_mode"
private const val LOCAL_OFFLINE_USER_ID = "local_offline_athlete"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY STEP 1: Configure App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            var isDarkTheme by rememberSaveable {
                mutableStateOf(prefs.getBoolean(KEY_DARK_THEME_MODE, true))
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = {
                            val next = !isDarkTheme
                            isDarkTheme = next
                            prefs.edit().putBoolean(KEY_DARK_THEME_MODE, next).apply()
                        }
                    )
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var useLocalDbWithoutGoogle by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_LOCAL_ONLY_MODE, false))
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user != null) {
        GymSenseHub(
            currentUserId = user.uid,
            defaultDisplayName = user.displayName ?: "Athlete",
            isLocalOnlyMode = false,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            onSwitchToSignInOrOut = {
                prefs.edit().putBoolean(KEY_LOCAL_ONLY_MODE, false).apply()
                useLocalDbWithoutGoogle = false
            }
        )
    } else if (useLocalDbWithoutGoogle) {
        GymSenseHub(
            currentUserId = LOCAL_OFFLINE_USER_ID,
            defaultDisplayName = "Athlete",
            isLocalOnlyMode = true,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            onSwitchToSignInOrOut = {
                prefs.edit().putBoolean(KEY_LOCAL_ONLY_MODE, false).apply()
                useLocalDbWithoutGoogle = false
            }
        )
    } else {
        AuthScreen(
            onAuthSuccess = {
                prefs.edit().putBoolean(KEY_LOCAL_ONLY_MODE, false).apply()
                useLocalDbWithoutGoogle = false
            },
            onContinueWithLocalDatabase = {
                prefs.edit().putBoolean(KEY_LOCAL_ONLY_MODE, true).apply()
                useLocalDbWithoutGoogle = true
            }
        )
    }
}

enum class MainNavDestination(val route: String, val labelRes: Int, val icon: ImageVector) {
    TODAY("today", R.string.nav_today, Icons.Default.FitnessCenter),
    SETUP("setup", R.string.nav_setup, Icons.Default.Tune),
    GOALS("goals", R.string.nav_goals, Icons.Default.EmojiEvents),
    ANALYTICS("analytics", R.string.nav_analytics, Icons.Default.Analytics),
    AI_LAB("ai_lab", R.string.nav_ai_lab, Icons.Default.AutoAwesome)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymSenseHub(
    currentUserId: String,
    defaultDisplayName: String,
    isLocalOnlyMode: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onSwitchToSignInOrOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val triggerGoogleLinkSignIn: () -> Unit = {
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                onSwitchToSignInOrOut()
            },
            onAuthError = {
                onSwitchToSignInOrOut()
            },
            scope = scope,
            onAuthCancelled = {}
        )
    }

    val workoutViewModel: WorkoutViewModel = viewModel(
        key = "${currentUserId}_$isLocalOnlyMode",
        factory = WorkoutViewModel.provideFactory(
            currentUserId = currentUserId,
            isLocalOnlyMode = isLocalOnlyMode
        )
    )
    val aiViewModel: AiStudioViewModel = viewModel(key = "ai_$currentUserId")

    val profileState by workoutViewModel.profileState.collectAsStateWithLifecycle()
    val workoutLogs by workoutViewModel.combinedWorkoutLogs.collectAsStateWithLifecycle()
    val customRoutineIds by workoutViewModel.customRoutineIds.collectAsStateWithLifecycle()
    val todayExtraIds by workoutViewModel.todayExtraIds.collectAsStateWithLifecycle()
    val vitalLogsState by workoutViewModel.vitalLogsState.collectAsStateWithLifecycle()
    val todaySchedule by workoutViewModel.todaySchedule.collectAsStateWithLifecycle()
    val isSaving by workoutViewModel.isSaving.collectAsStateWithLifecycle()
    val statusBanner by workoutViewModel.statusBanner.collectAsStateWithLifecycle()

    val chatHistory by aiViewModel.chatHistory.collectAsStateWithLifecycle()
    val selectedTier by aiViewModel.selectedCoachTier.collectAsStateWithLifecycle()
    val isChatStreaming by aiViewModel.isChatStreaming.collectAsStateWithLifecycle()
    val isGeneratingSchedule by aiViewModel.isGeneratingSchedule.collectAsStateWithLifecycle()
    val pendingScheduleDraft by aiViewModel.pendingScheduleDraft.collectAsStateWithLifecycle()
    val chatError by aiViewModel.chatError.collectAsStateWithLifecycle()
    val bioscienceState by aiViewModel.bioscienceState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainNavDestination.TODAY) }
    var activeExerciseSession by remember { mutableStateOf<ScheduledExercise?>(null) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showThreeFeatureMenu by remember { mutableStateOf(false) }
    var activeBundleFeaturePopup by remember { mutableStateOf<String?>(null) }

    val loadedProfile = (profileState as? DataUiState.Success)?.data
    val needsFirstTimeSetup = profileState is DataUiState.Success && (loadedProfile == null || !loadedProfile.setupCompleted)
    val profile = loadedProfile ?: UserProfile(
        userId = currentUserId,
        displayName = defaultDisplayName,
        setupCompleted = false
    )
    val vitalLogs = (vitalLogsState as? DataUiState.Success)?.data ?: emptyList()

    // Request POST_NOTIFICATIONS on Android 13+ & schedule 3-hour water (6am–12am), workout, and protein shake notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        GymSenseReminderScheduler.scheduleAllDailyReminders(
            context = context,
            wantsProteinShake = profile.wantsProteinShake,
            waterRemindersEnabled = profile.waterAlertsEnabled
        )
    }

    LaunchedEffect(profile.wantsProteinShake, profile.waterAlertsEnabled, profile.setupCompleted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        GymSenseReminderScheduler.scheduleAllDailyReminders(
            context = context,
            wantsProteinShake = profile.wantsProteinShake,
            waterRemindersEnabled = profile.waterAlertsEnabled
        )
    }

    if (activeExerciseSession == null && !needsFirstTimeSetup && currentTab != MainNavDestination.TODAY) {
        BackHandler {
            currentTab = MainNavDestination.TODAY
        }
    }

    // Interactive Auto-Schedule Maker Popup (Add / Remove Exercises & Accept to Today Tab)
    val activeDraft = pendingScheduleDraft
    if (activeDraft != null) {
        AutoScheduleReviewPopupDialog(
            draft = activeDraft,
            isGirl = profile.isGirl,
            onToggleExerciseInDraft = { exId ->
                aiViewModel.toggleExerciseInDraft(exId)
            },
            onDismiss = {
                aiViewModel.dismissScheduleDraft()
            },
            onAcceptToToday = {
                workoutViewModel.applyAiCuratedSchedule(
                    exerciseIds = activeDraft.selectedExerciseIds,
                    splitSummary = activeDraft.splitTitle
                )
                aiViewModel.dismissScheduleDraft()
                currentTab = MainNavDestination.TODAY
            }
        )
    }

    // Version Dialog (Opened from inside the 3-dot menu)
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "GymSense v${AppVersionInfo.CURRENT_VERSION}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = AppVersionInfo.shortBadgeText,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    AppVersionInfo.changelog.forEach { entry ->
                        Text(
                            text = "• $entry",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3-Dot Feature Bundle Popup Dialog
    if (activeBundleFeaturePopup != null) {
        val totalReps = workoutLogs.sumOf { it.totalReps }
        val totalSets = workoutLogs.sumOf { it.completedSets }
        val activeDays = workoutLogs.map { it.dayKey }.distinct().size
        val waterGlasses = (0 until GymSenseReminderScheduler.wakingWaterHours.size).count { idx ->
            (profile.waterSlotsMask and (1 shl idx)) != 0
        }
        AlertDialog(
            onDismissRequest = { activeBundleFeaturePopup = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = activeBundleFeaturePopup ?: "GymSense Summary",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${profile.displayName} • ${if (profile.isGirl) "Girl Sculpt" else "Boy Mode"} (${profile.ageYears} yrs)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Focus: ${profile.primaryFocus} (${profile.fitnessLevel})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Completed Exercises: ${workoutLogs.size} ($totalReps reps, $totalSets sets)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Active Consistency: $activeDays / 100 days",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Water (6am–12am every 3h): $waterGlasses / 7 glasses (${if (profile.waterAlertsEnabled) "Alerts ON" else "Alerts OFF"})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Daily Protein Shake: ${if (profile.wantsProteinShake) "ON + 7pm Alert" else "OFF"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeBundleFeaturePopup = null }) {
                    Text("Done")
                }
            }
        )
    }

    val selectedSession = activeExerciseSession
    if (selectedSession != null) {
        ExerciseSessionSheet(
            scheduledExercise = selectedSession,
            isGirl = profile.isGirl,
            isSaving = isSaving,
            onDismiss = { activeExerciseSession = null },
            onCompleteWorkout = { sets, reps, weightKg, durationSec, rpe ->
                workoutViewModel.recordCompletedExercise(
                    scheduled = selectedSession,
                    completedSets = sets,
                    totalReps = reps,
                    weightUsedKg = weightKg,
                    durationSeconds = durationSec,
                    rpeFeedback = rpe,
                    onFinished = { activeExerciseSession = null }
                )
            }
        )
    }

    val isProfileLoading = profileState is DataUiState.Loading

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        // Header Boy / Girl Selector Pill (Single source of truth for Boy/Girl switch)
                        Surface(
                            color = if (profile.isGirl) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { workoutViewModel.toggleGender(!profile.isGirl) }
                                .testTag("top_bar_gender_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (profile.isGirl) Icons.Default.Face3 else Icons.Default.Face,
                                    contentDescription = if (profile.isGirl) "Girl Mode" else "Boy Mode",
                                    tint = if (profile.isGirl) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (profile.isGirl) "Girl" else "Boy",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.isGirl) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        // 3-Dot Menu (Dark/Light Mode + Progress + Reminders + Version v1.0.4)
                        Box {
                            IconButton(
                                onClick = { showThreeFeatureMenu = true },
                                modifier = Modifier.testTag("three_feature_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu"
                                )
                            }
                            DropdownMenu(
                                expanded = showThreeFeatureMenu,
                                onDismissRequest = { showThreeFeatureMenu = false }
                            ) {
                                // Dark / Light Mode Toggle in 3-Dot Menu
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        onToggleDarkTheme()
                                    },
                                    modifier = Modifier.testTag("menu_toggle_theme_button")
                                )
                                DropdownMenuItem(
                                    text = { Text("Progress & Reps") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Assessment, contentDescription = null)
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        activeBundleFeaturePopup = "Progress & Reps Summary"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Water (3h) & Shake Alerts") },
                                    leadingIcon = {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        GymSenseReminderScheduler.scheduleAllDailyReminders(
                                            context = context,
                                            wantsProteinShake = profile.wantsProteinShake,
                                            waterRemindersEnabled = profile.waterAlertsEnabled
                                        )
                                        GymSenseReminderScheduler.triggerTestNotificationNow(
                                            context = context,
                                            wantsProteinShake = profile.wantsProteinShake
                                        )
                                        activeBundleFeaturePopup = "3h Water (6am–12am) & Shake Alerts"
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isLocalOnlyMode) "Cloud Sync"
                                            else "Account Status"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (isLocalOnlyMode) Icons.Default.CloudSync else Icons.Default.Save,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        if (isLocalOnlyMode) {
                                            onSwitchToSignInOrOut()
                                        } else {
                                            activeBundleFeaturePopup = "Profile & Sync Summary"
                                        }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "v${AppVersionInfo.CURRENT_VERSION} • Next ${AppVersionInfo.UP_NEXT_VERSION}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Version Info",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        showVersionDialog = true
                                    },
                                    modifier = Modifier.testTag("version_badge_button")
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isLocalOnlyMode) {
                                    onSwitchToSignInOrOut()
                                } else {
                                    signOutUser(
                                        context = context,
                                        onSignOutComplete = onSwitchToSignInOrOut,
                                        scope = scope
                                    )
                                }
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = if (isLocalOnlyMode) "Switch Account" else "Sign Out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen && !needsFirstTimeSetup) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen && !needsFirstTimeSetup) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("rail_tab_${dest.route}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedVisibility(visible = statusBanner != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { workoutViewModel.dismissBanner() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = statusBanner ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (isProfileLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (needsFirstTimeSetup) {
                        SetupAndVitalsScreen(
                            currentProfile = profile,
                            customRoutineIds = customRoutineIds,
                            defaultDisplayName = defaultDisplayName,
                            isSaving = isSaving,
                            isFirstTimeSetup = true,
                            isGoogleLinked = !isLocalOnlyMode,
                            onLinkGoogleAccount = triggerGoogleLinkSignIn,
                            onSaveCustomRoutine = { ids ->
                                workoutViewModel.saveCustomUserRoutine(ids)
                            },
                            onSkipSetup = {
                                workoutViewModel.skipSetupToDefault()
                                currentTab = MainNavDestination.TODAY
                            },
                            onSaveSetup = { name, age, h, w, tw, wg, tc, rhr, vo2, level, focus, equip, isGirl, isWk12, wantsShake, rpe ->
                                workoutViewModel.saveSetupAndVitals(
                                    displayName = name,
                                    ageYears = age,
                                    heightCm = h,
                                    weightKg = w,
                                    targetWeightKg = tw,
                                    weeklyWorkoutGoal = wg,
                                    targetCaloriesPerWeek = tc,
                                    restingHeartRateBpm = rhr,
                                    vo2MaxEstimate = vo2,
                                    fitnessLevel = level,
                                    primaryFocus = focus,
                                    selectedEquipment = equip,
                                    isGirl = isGirl,
                                    isWeek1To2Warmup = isWk12,
                                    wantsProteinShake = wantsShake,
                                    lastFeedbackRpe = rpe,
                                    onComplete = {
                                        GymSenseReminderScheduler.scheduleAllDailyReminders(
                                            context = context,
                                            wantsProteinShake = wantsShake,
                                            waterRemindersEnabled = profile.waterAlertsEnabled
                                        )
                                        currentTab = MainNavDestination.TODAY
                                    }
                                )
                            }
                        )
                    } else {
                        when (currentTab) {
                            MainNavDestination.TODAY -> {
                                TodayScheduleScreen(
                                    profile = profile,
                                    schedule = todaySchedule,
                                    allLogs = workoutLogs,
                                    todayExtraIds = todayExtraIds,
                                    isGeneratingAiSchedule = isGeneratingSchedule,
                                    onSelectExercise = { activeExerciseSession = it },
                                    onToggleWeek1To2 = { enabled -> workoutViewModel.toggleWeek1To2Warmup(enabled) },
                                    onToggleWaterAlerts = { enabled ->
                                        workoutViewModel.toggleWaterAlertsPreference(enabled)
                                        GymSenseReminderScheduler.scheduleAllDailyReminders(
                                            context = context,
                                            wantsProteinShake = profile.wantsProteinShake,
                                            waterRemindersEnabled = enabled
                                        )
                                    },
                                    onToggleProteinShake = { enabled ->
                                        workoutViewModel.toggleProteinShakePreference(enabled)
                                        GymSenseReminderScheduler.scheduleAllDailyReminders(
                                            context = context,
                                            wantsProteinShake = enabled,
                                            waterRemindersEnabled = profile.waterAlertsEnabled
                                        )
                                    },
                                    onToggleWaterSlot = { slotIdx ->
                                        workoutViewModel.toggleWaterSlot(slotIdx)
                                    },
                                    onTestReminderNotification = {
                                        GymSenseReminderScheduler.scheduleAllDailyReminders(
                                            context = context,
                                            wantsProteinShake = profile.wantsProteinShake,
                                            waterRemindersEnabled = profile.waterAlertsEnabled
                                        )
                                        GymSenseReminderScheduler.triggerTestNotificationNow(
                                            context = context,
                                            wantsProteinShake = profile.wantsProteinShake
                                        )
                                    },
                                    onSelectSplit = { split -> workoutViewModel.selectSplitOverride(split) },
                                    onShiftMissedDay = { onPopup ->
                                        workoutViewModel.shiftScheduleForMissedDay(1, onPopup)
                                    },
                                    onRequestAiSchedule = {
                                        aiViewModel.generateAiEquipmentSchedule(profile = profile)
                                    },
                                    onToggleExtraExercise = { exId ->
                                        workoutViewModel.toggleExtraExerciseToday(exId)
                                    },
                                    onOpenCustomScheduleBuilder = {
                                        currentTab = MainNavDestination.SETUP
                                    }
                                )
                            }

                            MainNavDestination.SETUP -> {
                                SetupAndVitalsScreen(
                                    currentProfile = profile,
                                    customRoutineIds = customRoutineIds,
                                    defaultDisplayName = defaultDisplayName,
                                    isSaving = isSaving,
                                    isFirstTimeSetup = false,
                                    isGoogleLinked = !isLocalOnlyMode,
                                    onLinkGoogleAccount = triggerGoogleLinkSignIn,
                                    onSaveCustomRoutine = { ids ->
                                        workoutViewModel.saveCustomUserRoutine(ids)
                                        currentTab = MainNavDestination.TODAY
                                    },
                                    onSkipSetup = {
                                        workoutViewModel.skipSetupToDefault()
                                        currentTab = MainNavDestination.TODAY
                                    },
                                    onSaveSetup = { name, age, h, w, tw, wg, tc, rhr, vo2, level, focus, equip, isGirl, isWk12, wantsShake, rpe ->
                                        workoutViewModel.saveSetupAndVitals(
                                            displayName = name,
                                            ageYears = age,
                                            heightCm = h,
                                            weightKg = w,
                                            targetWeightKg = tw,
                                            weeklyWorkoutGoal = wg,
                                            targetCaloriesPerWeek = tc,
                                            restingHeartRateBpm = rhr,
                                            vo2MaxEstimate = vo2,
                                            fitnessLevel = level,
                                            primaryFocus = focus,
                                            selectedEquipment = equip,
                                            isGirl = isGirl,
                                            isWeek1To2Warmup = isWk12,
                                            wantsProteinShake = wantsShake,
                                            lastFeedbackRpe = rpe,
                                            onComplete = {
                                                GymSenseReminderScheduler.scheduleAllDailyReminders(
                                                    context = context,
                                                    wantsProteinShake = wantsShake,
                                                    waterRemindersEnabled = profile.waterAlertsEnabled
                                                )
                                                currentTab = MainNavDestination.TODAY
                                            }
                                        )
                                    }
                                )
                            }

                            MainNavDestination.GOALS -> {
                                GoalsAndMilestonesScreen(
                                    profile = profile,
                                    workoutLogs = workoutLogs
                                )
                            }

                            MainNavDestination.ANALYTICS -> {
                                AnalyticsPortalScreen(
                                    profile = profile,
                                    schedule = todaySchedule,
                                    workoutLogs = workoutLogs,
                                    vitalLogs = vitalLogs,
                                    bioscienceState = bioscienceState,
                                    onQuickAddWaterToday = {
                                        val nextUnfilledSlot = (0 until GymSenseReminderScheduler.wakingWaterHours.size)
                                            .firstOrNull { idx -> (profile.waterSlotsMask and (1 shl idx)) == 0 }
                                        if (nextUnfilledSlot != null) {
                                            workoutViewModel.toggleWaterSlot(nextUnfilledSlot)
                                        }
                                    },
                                    onGenerateBioscienceReport = {
                                        aiViewModel.generateBioscienceIntelligenceReport(
                                            profile = profile,
                                            recentLogs = workoutLogs
                                        )
                                    },
                                    onLogQuickVitals = { w, hr, rec, notes ->
                                        workoutViewModel.logQuickVitalCheckIn(w, hr, rec, notes)
                                    },
                                    onDeleteWorkoutLog = { workoutViewModel.deleteWorkoutLog(it) }
                                )
                            }

                            MainNavDestination.AI_LAB -> {
                                AiCoachAndVisualLabScreen(
                                    profile = profile,
                                    chatHistory = chatHistory,
                                    selectedTier = selectedTier,
                                    isChatStreaming = isChatStreaming,
                                    isGeneratingSchedule = isGeneratingSchedule,
                                    chatError = chatError,
                                    isGoogleLinked = !isLocalOnlyMode,
                                    onLinkGoogleAccount = triggerGoogleLinkSignIn,
                                    onCycleTier = { aiViewModel.cycleCoachTier() },
                                    onSendChat = { msg -> aiViewModel.sendChatMessage(msg, profile) },
                                    onAutoBuildSchedule = {
                                        aiViewModel.generateAiEquipmentSchedule(profile = profile)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Popup shown when Auto-Schedule Maker creates a workout plan from the user's
 * available machines and fitness level. Allows adding or removing any exercise before accepting to Today.
 */
@Composable
private fun AutoScheduleReviewPopupDialog(
    draft: AutoScheduleDraft,
    isGirl: Boolean,
    onToggleExerciseInDraft: (String) -> Unit,
    onDismiss: () -> Unit,
    onAcceptToToday: () -> Unit
) {
    var showAddMoreCatalog by remember { mutableStateOf(false) }
    val allCatalog = remember {
        ExerciseCatalog.allExercises.filter { it.id != "ex_protein_shake_recovery" }
    }
    val selectedTemplates = remember(draft.selectedExerciseIds, allCatalog) {
        draft.selectedExerciseIds.mapNotNull { id -> allCatalog.firstOrNull { it.id == id } }
    }
    val unselectedTemplates = remember(draft.selectedExerciseIds, allCatalog) {
        allCatalog.filter { it.id !in draft.selectedExerciseIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = draft.splitTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Level: ${draft.fitnessLevel} • Gear: ${draft.equipmentSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected (${selectedTemplates.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { showAddMoreCatalog = !showAddMoreCatalog },
                        modifier = Modifier.testTag("toggle_add_more_in_popup_button")
                    ) {
                        Text(if (showAddMoreCatalog) "Hide Catalog" else "+ Add Exercise")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Selected exercises first (tap to remove)
                    items(selectedTemplates, key = { "sel_${it.id}" }) { ex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleExerciseInDraft(ex.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    StickmanExerciseCanvas(
                                        motionType = ex.motionType,
                                        isGirl = isGirl,
                                        modifier = Modifier.size(40.dp),
                                        showGrid = false
                                    )
                                    Column {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ex.muscleGroup} • ${ex.requiredEquipment}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Optional Add More section inside the same popup
                    if (showAddMoreCatalog) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to Add to Plan:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        items(unselectedTemplates, key = { "unsel_${it.id}" }) { ex ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleExerciseInDraft(ex.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        StickmanExerciseCanvas(
                                            motionType = ex.motionType,
                                            isGirl = isGirl,
                                            modifier = Modifier.size(38.dp),
                                            showGrid = false
                                        )
                                        Column {
                                            Text(
                                                text = ex.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${ex.muscleGroup} • ${ex.requiredEquipment}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAcceptToToday,
                modifier = Modifier.testTag("accept_auto_schedule_popup_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accept to Today")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
