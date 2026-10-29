package com.example.util

/**
 * Tracks GymSense application build versions.
 * Increment history:
 * - v.0.0 : Initial baseline build
 * - 1.0.1 : Stickman animations, Boy/Girl modes, Wk 1-2 warmup
 * - 1.0.2 : Current build — Renamed to GymSense & GymBae AI, local Room SQLite rep/exercise storage,
 *           custom schedule & extra exercise picker, 7/30/100-day consistency calendar, 3-feature bundle menu
 * - 1.0.3 : Up-next scheduled build target
 */
object AppVersionInfo {
    const val INITIAL_VERSION = "v.0.0"
    const val PREVIOUS_VERSION = "1.0.1"
    const val CURRENT_VERSION = "1.0.2"
    const val UP_NEXT_VERSION = "1.0.3"

    const val shortBadgeText = "v.0.0 → 1.0.1 → Now: $CURRENT_VERSION → Next: $UP_NEXT_VERSION"

    val changelog = listOf(
        "v.0.0 — Initial baseline release",
        "1.0.1 — Stickman animations (Boy/Girl), 20+ gym machines & Wk 1-2 warmup mode",
        "1.0.2 (Current) — GymSense + GymBae AI Coach, custom schedule builder, +Add Extra Exercise on Today page, local Room database for all reps/exercises, 1-Week & 100-Day consistency calendar, Shift +1 Day popup & 3-feature bundle menu",
        "1.0.3 (Up Next) — Wearable heart-rate BLE sync & PDF workout export"
    )
}
