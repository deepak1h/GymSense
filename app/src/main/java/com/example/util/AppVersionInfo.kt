package com.example.util

/**
 * Tracks GymSense application build versions.
 * Increment history:
 * - v.0.0 : Initial baseline build
 * - 1.0.1 : Stickman animations, Boy/Girl modes, Wk 1-2 warmup
 * - 1.0.2 : GymSense + GymBae AI, local DB, consistency calendar & 3-dot bundle menu
 * - 1.0.3 : First-time onboarding setup, Girl/Boy goals, silent progressive overload & 3h water reminders
 * - 1.0.4 : Dark & Light theme switcher in 3-dot menu, Water Alert On/Off toggle, 1-click GymBae mode & Auto-Schedule popup
 * - 1.0.5 : 3D Weekly Hydration & Multi-Process 3D Visualization
 * - 1.0.6 : Moved 3D Water & Process Graph to Stats tab, instant GymBae replies, and Google Account Privacy & AI Link
 * - 1.0.7 : Expanded GymBae chat screen real estate, added deep DB stats, and Show More/Less toggle for Session Output logs
 * - 1.0.8 : Current build — Simplified & aligned Stats UI, clean minimal UI across all tabs, and higher-contrast soft-slate Light Mode
 * - 1.0.9 : Up-next scheduled build target
 */
object AppVersionInfo {
    const val INITIAL_VERSION = "v.0.0"
    const val PREVIOUS_VERSION = "1.0.7"
    const val CURRENT_VERSION = "1.0.8"
    const val UP_NEXT_VERSION = "1.0.9"

    const val shortBadgeText = "v.0.0 → 1.0.1 → 1.0.2 → 1.0.3 → 1.0.4 → 1.0.5 → 1.0.6 → 1.0.7 → Now: $CURRENT_VERSION → Next: $UP_NEXT_VERSION"

    val changelog = listOf(
        "v.0.0 — Initial baseline release",
        "1.0.1 — Stickman animations (Boy/Girl), 20+ gym machines & Wk 1-2 warmup mode",
        "1.0.2 — GymSense + GymBae AI, custom schedule builder, 100-day calendar & 3-dot menu",
        "1.0.3 — First-time onboarding, silent progressive overload & 3-hour water reminders (6am–12am)",
        "1.0.4 — Dark & Light Mode switcher, Water Alert toggle, 1-click GymBae mode & Auto-Schedule popup",
        "1.0.5 — 3D Weekly Hydration & Multi-Process 3D Visualizer",
        "1.0.6 — Moved 3D Hydration & Process Graph to Stats section, instant GymBae replies & Google Privacy Link",
        "1.0.7 — Maximized GymBae AI chat screen real estate, added DB stats & Show More/Less toggle",
        "1.0.8 (Current) — Simplified & aligned Stats section, clean UI across all screens, and higher-contrast toned Light Mode",
        "1.0.9 (Up Next) — Wearable BLE heart-rate sync & PDF workout export"
    )
}
