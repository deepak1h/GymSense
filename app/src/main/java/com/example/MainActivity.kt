package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ScheduledExercise
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.ExerciseSessionSheet
import com.example.ui.screens.AiCoachAndVisualLabScreen
import com.example.ui.screens.AnalyticsPortalScreen
import com.example.ui.screens.GoalsAndMilestonesScreen
import com.example.ui.screens.SetupAndVitalsScreen
import com.example.ui.screens.TodayScheduleScreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoltLime
import com.example.util.AppVersionInfo
import com.example.util.configureAppCheck
import com.example.viewmodel.AiStudioViewModel
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
private const val LOCAL_OFFLINE_USER_ID = "local_offline_athlete"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY STEP 1: Configure App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
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
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
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
            onSwitchToSignInOrOut = {
                prefs.edit().putBoolean(KEY_LOCAL_ONLY_MODE, false).apply()
                useLocalDbWithoutGoogle = false
            }
        )
    } else if (useLocalDbWithoutGoogle) {
        GymSenseHub(
            currentUserId = LOCAL_OFFLINE_USER_ID,
            defaultDisplayName = "Local Athlete",
            isLocalOnlyMode = true,
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
    onSwitchToSignInOrOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    val chatError by aiViewModel.chatError.collectAsStateWithLifecycle()
    val bioscienceState by aiViewModel.bioscienceState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainNavDestination.TODAY) }
    var activeExerciseSession by remember { mutableStateOf<ScheduledExercise?>(null) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showThreeFeatureMenu by remember { mutableStateOf(false) }
    var activeBundleFeaturePopup by remember { mutableStateOf<String?>(null) }

    if (activeExerciseSession == null && currentTab != MainNavDestination.TODAY) {
        BackHandler {
            currentTab = MainNavDestination.TODAY
        }
    }

    val loadedProfile = (profileState as? DataUiState.Success)?.data
    val profile = loadedProfile ?: UserProfile(
        userId = currentUserId,
        displayName = defaultDisplayName,
        setupCompleted = true
    )
    val vitalLogs = (vitalLogsState as? DataUiState.Success)?.data ?: emptyList()

    // Version Dialog
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

    // 3-Button Feature Bundle Popup Dialog
    if (activeBundleFeaturePopup != null) {
        val totalReps = workoutLogs.sumOf { it.totalReps }
        val totalSets = workoutLogs.sumOf { it.completedSets }
        val activeDays = workoutLogs.map { it.dayKey }.distinct().size
        AlertDialog(
            onDismissRequest = { activeBundleFeaturePopup = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = VoltLime
                )
            },
            title = {
                Text(
                    text = activeBundleFeaturePopup ?: "GymSense Feature",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Storage Mode: ${if (isLocalOnlyMode) "Local Room SQLite DB (No Google)" else "Local DB + Google Cloud Sync"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoltLime
                        )
                        Text(
                            text = "• Completed Exercises: ${workoutLogs.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Total Lifetime Reps: $totalReps reps ($totalSets sets)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Active Consistency Days: $activeDays / 100 days",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Custom Routine Saved: ${customRoutineIds.size} exercises",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            // Clickable Version Badge (v1.0.2 -> next 1.0.3)
                            Surface(
                                color = VoltLime.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = VoltLime.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showVersionDialog = true }
                                    .testTag("version_badge_button")
                            ) {
                                Text(
                                    text = "v${AppVersionInfo.CURRENT_VERSION}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = VoltLime,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (isLocalOnlyMode) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "LOCAL DB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily
                                        ),
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Quick Girl/Boy Switch Icon
                        IconButton(
                            onClick = { workoutViewModel.toggleGender(!profile.isGirl) },
                            modifier = Modifier.testTag("top_bar_gender_button")
                        ) {
                            Icon(
                                imageVector = if (profile.isGirl) Icons.Default.Face3 else Icons.Default.Face,
                                contentDescription = if (profile.isGirl) "Girl Mode" else "Boy Mode",
                                tint = if (profile.isGirl) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }

                        // 3-Button Feature Bundle Menu (Top-Right 3-dot Menu)
                        Box {
                            IconButton(
                                onClick = { showThreeFeatureMenu = true },
                                modifier = Modifier.testTag("three_feature_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Feature Bundle Menu"
                                )
                            }
                            DropdownMenu(
                                expanded = showThreeFeatureMenu,
                                onDismissRequest = { showThreeFeatureMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("1. Local DB Progress Report") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Assessment, contentDescription = null)
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        activeBundleFeaturePopup = "Local DB Progress & Reps Report"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("2. Superset & HIIT Timer Bundle") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Timer, contentDescription = null)
                                    },
                                    onClick = {
                                        showThreeFeatureMenu = false
                                        activeBundleFeaturePopup = "Superset & HIIT Timer Bundle"
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isLocalOnlyMode) "3. Connect Google Cloud Sync"
                                            else "3. Backup & Storage Status"
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
                                            activeBundleFeaturePopup = "Local + Cloud Storage Status"
                                        }
                                    }
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
                if (!isExpandedScreen) {
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
                if (isExpandedScreen) {
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
                                    onToggleGender = { isGirl -> workoutViewModel.toggleGender(isGirl) },
                                    onToggleWeek1To2 = { enabled -> workoutViewModel.toggleWeek1To2Warmup(enabled) },
                                    onSelectSplit = { split -> workoutViewModel.selectSplitOverride(split) },
                                    onShiftMissedDay = { onPopup ->
                                        workoutViewModel.shiftScheduleForMissedDay(1, onPopup)
                                    },
                                    onRequestAiSchedule = {
                                        aiViewModel.generateAiEquipmentSchedule(
                                            profile = profile,
                                            onSuccess = { ids, title ->
                                                workoutViewModel.applyAiCuratedSchedule(ids, title)
                                            },
                                            onError = {}
                                        )
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
                                    onSaveCustomRoutine = { ids ->
                                        workoutViewModel.saveCustomUserRoutine(ids)
                                        currentTab = MainNavDestination.TODAY
                                    },
                                    onSkipSetup = {
                                        workoutViewModel.skipSetupToDefault()
                                        currentTab = MainNavDestination.TODAY
                                    },
                                    onSaveSetup = { name, h, w, tw, wg, tc, rhr, vo2, level, focus, equip, isGirl, isWk12, rpe ->
                                        workoutViewModel.saveSetupAndVitals(
                                            displayName = name,
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
                                            lastFeedbackRpe = rpe,
                                            onComplete = { currentTab = MainNavDestination.TODAY }
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
                                    workoutLogs = workoutLogs,
                                    vitalLogs = vitalLogs,
                                    bioscienceState = bioscienceState,
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
                                    onSelectTier = { aiViewModel.selectCoachTier(it) },
                                    onSendChat = { msg -> aiViewModel.sendChatMessage(msg, profile) },
                                    onAutoBuildSchedule = {
                                        aiViewModel.generateAiEquipmentSchedule(
                                            profile = profile,
                                            onSuccess = { ids, title ->
                                                workoutViewModel.applyAiCuratedSchedule(ids, title)
                                                currentTab = MainNavDestination.TODAY
                                            },
                                            onError = {}
                                        )
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
