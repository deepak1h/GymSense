package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.BiomechanicMotionType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure-code procedural 60fps Stickman Exercise Animator.
 * Uses zero AI credits and personalizes visual appearance for Boy and Girl athletes.
 */
@Composable
fun StickmanExerciseCanvas(
    motionType: BiomechanicMotionType,
    isGirl: Boolean,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    playbackSpeed: Float = 1.0f,
    showGrid: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stickmanLoop")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2200 / playbackSpeed).roundToInt().coerceAtLeast(700),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "stickmanPhase"
    )

    // Normalized oscillation t in [0f..1f] where 0 = start/lockout, 1 = peak flexion/contraction
    val t = if (isPlaying) ((1f - cos(phase)) * 0.5f) else 0.65f

    val primaryColor = if (isGirl) Color(0xFFFF4081) else MaterialTheme.colorScheme.primary
    val secondaryColor = if (isGirl) Color(0xFF00B8D4) else MaterialTheme.colorScheme.secondary
    val boneColor = MaterialTheme.colorScheme.onSurface
    val equipColor = Color(0xFFFFA000)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h)
        val strokeBone = (s * 0.055f).coerceIn(3.5f, 11f)
        val headRadius = (s * 0.085f).coerceIn(5f, 18f)

        if (showGrid) {
            val cols = 6
            for (i in 1 until cols) {
                drawLine(
                    color = boneColor.copy(alpha = 0.08f),
                    start = Offset(w * i / cols, 0f),
                    end = Offset(w * i / cols, h),
                    strokeWidth = 1f
                )
            }
            drawLine(
                color = boneColor.copy(alpha = 0.2f),
                start = Offset(w * 0.1f, h * 0.86f),
                end = Offset(w * 0.9f, h * 0.86f),
                strokeWidth = 2f
            )
        }

        val cx = w * 0.5f
        val floorY = h * 0.84f

        when (motionType) {
            BiomechanicMotionType.WARMUP_JACKS -> {
                val jumpOffset = t * (s * 0.06f)
                val spreadX = (0.08f + t * 0.18f) * s
                val hip = Offset(cx, floorY - s * 0.34f - jumpOffset)
                val shoulder = Offset(cx, hip.y - s * 0.24f)
                val head = Offset(cx, shoulder.y - headRadius * 1.45f)

                val footL = Offset(cx - spreadX, floorY - jumpOffset)
                val footR = Offset(cx + spreadX, floorY - jumpOffset)
                val kneeL = Offset(cx - spreadX * 0.6f, (hip.y + footL.y) * 0.5f)
                val kneeR = Offset(cx + spreadX * 0.6f, (hip.y + footR.y) * 0.5f)

                val armAngle = (-0.3f + t * 1.15f) * PI.toFloat()
                val handL = Offset(
                    shoulder.x - cos(armAngle * 0.4f) * s * 0.26f,
                    shoulder.y - sin(armAngle * 0.5f) * s * 0.26f
                )
                val handR = Offset(
                    shoulder.x + cos(armAngle * 0.4f) * s * 0.26f,
                    shoulder.y - sin(armAngle * 0.5f) * s * 0.26f
                )
                val elbowL = Offset((shoulder.x + handL.x) * 0.5f - s * 0.04f, (shoulder.y + handL.y) * 0.5f)
                val elbowR = Offset((shoulder.x + handR.x) * 0.5f + s * 0.04f, (shoulder.y + handR.y) * 0.5f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbowL,
                    handL = handL,
                    elbowR = elbowR,
                    handR = handR,
                    kneeL = kneeL,
                    footL = footL,
                    kneeR = kneeR,
                    footR = footR,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.SQUAT_DRIVE -> {
                val drop = t * (s * 0.20f)
                val ankleL = Offset(cx - s * 0.08f, floorY)
                val ankleR = Offset(cx + s * 0.08f, floorY)
                val kneeL = Offset(cx - s * 0.14f - t * s * 0.06f, floorY - s * 0.20f + drop * 0.35f)
                val kneeR = Offset(cx + s * 0.14f + t * s * 0.06f, floorY - s * 0.20f + drop * 0.35f)
                val hip = Offset(cx, floorY - s * 0.38f + drop)
                val shoulder = Offset(cx, hip.y - s * 0.24f + drop * 0.12f)
                val head = Offset(cx, shoulder.y - headRadius * 1.45f)

                val elbowL = Offset(cx - s * 0.16f, shoulder.y + s * 0.07f)
                val elbowR = Offset(cx + s * 0.16f, shoulder.y + s * 0.07f)
                val handL = Offset(cx - s * 0.24f, shoulder.y)
                val handR = Offset(cx + s * 0.24f, shoulder.y)

                // Barbell across shoulders
                drawLine(
                    color = equipColor,
                    start = Offset(cx - s * 0.34f, shoulder.y),
                    end = Offset(cx + s * 0.34f, shoulder.y),
                    strokeWidth = strokeBone * 0.85f,
                    cap = StrokeCap.Round
                )
                drawCircle(equipColor, radius = headRadius * 0.7f, center = Offset(cx - s * 0.32f, shoulder.y))
                drawCircle(equipColor, radius = headRadius * 0.7f, center = Offset(cx + s * 0.32f, shoulder.y))

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbowL,
                    handL = handL,
                    elbowR = elbowR,
                    handR = handR,
                    kneeL = kneeL,
                    footL = ankleL,
                    kneeR = kneeR,
                    footR = ankleR,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.LUNGE_SPLIT -> {
                val drop = t * (s * 0.16f)
                val hip = Offset(cx, floorY - s * 0.36f + drop)
                val shoulder = Offset(cx, hip.y - s * 0.24f)
                val head = Offset(cx, shoulder.y - headRadius * 1.45f)

                val footFront = Offset(cx + s * 0.20f, floorY)
                val kneeFront = Offset(cx + s * 0.20f, hip.y + s * 0.10f)
                val footBack = Offset(cx - s * 0.24f, floorY)
                val kneeBack = Offset(cx - s * 0.12f, hip.y + s * 0.15f + drop * 0.3f)

                val elbowL = Offset(cx - s * 0.06f, shoulder.y + s * 0.12f)
                val handL = Offset(cx - s * 0.08f, hip.y + s * 0.04f)
                val elbowR = Offset(cx + s * 0.06f, shoulder.y + s * 0.12f)
                val handR = Offset(cx + s * 0.08f, hip.y + s * 0.04f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbowL,
                    handL = handL,
                    elbowR = elbowR,
                    handR = handR,
                    kneeL = kneeBack,
                    footL = footBack,
                    kneeR = kneeFront,
                    footR = footFront,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.HINGE_DEADLIFT -> {
                val hingeAngle = t * 0.95f
                val ankle = Offset(cx + s * 0.02f, floorY)
                val knee = Offset(cx + s * 0.08f, floorY - s * 0.19f)
                val hip = Offset(cx - s * 0.06f - t * s * 0.10f, floorY - s * 0.38f + t * s * 0.05f)
                val shoulder = Offset(
                    hip.x + sin(0.35f + hingeAngle) * s * 0.25f,
                    hip.y - cos(0.35f + hingeAngle) * s * 0.25f
                )
                val head = Offset(
                    shoulder.x + sin(0.35f + hingeAngle) * headRadius * 1.4f,
                    shoulder.y - cos(0.35f + hingeAngle) * headRadius * 1.4f
                )
                val hand = Offset(shoulder.x, shoulder.y + s * 0.24f)
                val elbow = Offset(shoulder.x, (shoulder.y + hand.y) * 0.5f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbow,
                    handL = hand,
                    elbowR = elbow,
                    handR = hand,
                    kneeL = knee,
                    footL = ankle,
                    kneeR = knee,
                    footR = ankle,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
                // Kettlebell / Barbell at hand
                drawCircle(equipColor, radius = headRadius * 0.75f, center = hand)
            }

            BiomechanicMotionType.BENCH_PRESS -> {
                val benchY = floorY - s * 0.22f
                // Draw Bench
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(cx - s * 0.32f, benchY),
                    end = Offset(cx + s * 0.22f, benchY),
                    strokeWidth = strokeBone * 1.1f,
                    cap = StrokeCap.Round
                )
                val hip = Offset(cx + s * 0.06f, benchY - strokeBone)
                val shoulder = Offset(cx - s * 0.18f, benchY - strokeBone)
                val head = Offset(shoulder.x - headRadius * 1.3f, shoulder.y - headRadius * 0.2f)
                val knee = Offset(cx + s * 0.22f, benchY)
                val foot = Offset(cx + s * 0.24f, floorY)

                val pressHeight = (1f - t) * (s * 0.22f) + s * 0.06f
                val hand = Offset(shoulder.x, shoulder.y - pressHeight)
                val elbow = Offset(shoulder.x + s * 0.06f, shoulder.y - pressHeight * 0.45f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbow,
                    handL = hand,
                    elbowR = elbow,
                    handR = hand,
                    kneeL = knee,
                    footL = foot,
                    kneeR = knee,
                    footR = foot,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
                drawCircle(equipColor, radius = headRadius * 0.75f, center = hand)
            }

            BiomechanicMotionType.CORE_PLANK -> {
                // Push-up / Plank animation
                val chestDrop = t * (s * 0.13f)
                val foot = Offset(cx - s * 0.28f, floorY)
                val shoulder = Offset(cx + s * 0.16f, floorY - s * 0.22f + chestDrop)
                val hip = Offset((foot.x + shoulder.x) * 0.5f, (foot.y + shoulder.y) * 0.5f)
                val knee = Offset((foot.x + hip.x) * 0.5f, (foot.y + hip.y) * 0.5f)
                val head = Offset(shoulder.x + headRadius * 1.35f, shoulder.y - headRadius * 0.35f)
                val hand = Offset(cx + s * 0.18f, floorY)
                val elbow = Offset(shoulder.x - s * 0.05f * t, (shoulder.y + hand.y) * 0.5f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbow,
                    handL = hand,
                    elbowR = elbow,
                    handR = hand,
                    kneeL = knee,
                    footL = foot,
                    kneeR = knee,
                    footR = foot,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.GLUTE_BRIDGE -> {
                val lift = (1f - t) * (s * 0.16f)
                val shoulder = Offset(cx - s * 0.20f, floorY - strokeBone)
                val head = Offset(shoulder.x - headRadius * 1.25f, floorY - headRadius * 0.8f)
                val foot = Offset(cx + s * 0.24f, floorY)
                val knee = Offset(cx + s * 0.14f, floorY - s * 0.22f)
                val hip = Offset(cx - s * 0.02f, floorY - s * 0.06f - lift)
                val hand = Offset(hip.x, hip.y)
                val elbow = Offset(cx - s * 0.10f, floorY - s * 0.04f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbow,
                    handL = hand,
                    elbowR = elbow,
                    handR = hand,
                    kneeL = knee,
                    footL = foot,
                    kneeR = knee,
                    footR = foot,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.CABLE_PULL, BiomechanicMotionType.PULL_UP -> {
                // Vertical Pull / Lat Pulldown / Pull-Up
                val barY = h * 0.16f
                drawLine(
                    color = equipColor,
                    start = Offset(cx - s * 0.28f, barY),
                    end = Offset(cx + s * 0.28f, barY),
                    strokeWidth = strokeBone * 0.8f,
                    cap = StrokeCap.Round
                )
                val bodyLift = if (motionType == BiomechanicMotionType.PULL_UP) (1f - t) * (s * 0.14f) else 0f
                val pullDownY = if (motionType == BiomechanicMotionType.CABLE_PULL) t * (s * 0.18f) else 0f

                val hip = Offset(cx, floorY - s * 0.24f - bodyLift)
                val shoulder = Offset(cx, hip.y - s * 0.24f)
                val head = Offset(cx, shoulder.y - headRadius * 1.45f)
                val kneeL = Offset(cx - s * 0.09f, hip.y + s * 0.13f)
                val kneeR = Offset(cx + s * 0.09f, hip.y + s * 0.13f)
                val footL = Offset(cx - s * 0.09f, hip.y + s * 0.24f)
                val footR = Offset(cx + s * 0.09f, hip.y + s * 0.24f)

                val handL = Offset(cx - s * 0.18f, barY + pullDownY)
                val handR = Offset(cx + s * 0.18f, barY + pullDownY)
                val elbowL = Offset(cx - s * 0.22f, (shoulder.y + handL.y) * 0.5f + t * s * 0.08f)
                val elbowR = Offset(cx + s * 0.22f, (shoulder.y + handR.y) * 0.5f + t * s * 0.08f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbowL,
                    handL = handL,
                    elbowR = elbowR,
                    handR = handR,
                    kneeL = kneeL,
                    footL = footL,
                    kneeR = kneeR,
                    footR = footR,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.ROW_ERG -> {
                // Horizontal Row / Seated Row / Ergometer
                val pullBack = (1f - t) * (s * 0.14f)
                val hip = Offset(cx - s * 0.06f - pullBack * 0.4f, floorY - s * 0.12f)
                val shoulder = Offset(hip.x - pullBack * 0.3f, hip.y - s * 0.24f)
                val head = Offset(shoulder.x, shoulder.y - headRadius * 1.45f)
                val foot = Offset(cx + s * 0.26f, floorY - s * 0.08f)
                val knee = Offset(cx + s * 0.10f, floorY - s * 0.18f - t * s * 0.06f)
                val hand = Offset(shoulder.x + s * 0.04f + t * s * 0.18f, shoulder.y + s * 0.10f)
                val elbow = Offset(shoulder.x - s * 0.08f + t * s * 0.08f, shoulder.y + s * 0.12f)

                // Cable line
                drawLine(
                    color = equipColor.copy(alpha = 0.7f),
                    start = Offset(cx + s * 0.34f, hand.y),
                    end = hand,
                    strokeWidth = strokeBone * 0.45f
                )

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbow,
                    handL = hand,
                    elbowR = elbow,
                    handR = hand,
                    kneeL = knee,
                    footL = foot,
                    kneeR = knee,
                    footR = foot,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
            }

            BiomechanicMotionType.OVERHEAD_PRESS -> {
                val lift = (1f - t) * (s * 0.20f)
                val hip = Offset(cx, floorY - s * 0.36f)
                val shoulder = Offset(cx, hip.y - s * 0.24f)
                val head = Offset(cx, shoulder.y - headRadius * 1.45f)
                val footL = Offset(cx - s * 0.10f, floorY)
                val footR = Offset(cx + s * 0.10f, floorY)
                val kneeL = Offset(cx - s * 0.10f, floorY - s * 0.18f)
                val kneeR = Offset(cx + s * 0.10f, floorY - s * 0.18f)

                val handL = Offset(cx - s * 0.18f, shoulder.y - s * 0.04f - lift)
                val handR = Offset(cx + s * 0.18f, shoulder.y - s * 0.04f - lift)
                val elbowL = Offset(cx - s * 0.20f, shoulder.y + s * 0.06f - lift * 0.5f)
                val elbowR = Offset(cx + s * 0.20f, shoulder.y + s * 0.06f - lift * 0.5f)

                drawStickFigure(
                    head = head,
                    headRadius = headRadius,
                    shoulder = shoulder,
                    hip = hip,
                    elbowL = elbowL,
                    handL = handL,
                    elbowR = elbowR,
                    handR = handR,
                    kneeL = kneeL,
                    footL = footL,
                    kneeR = kneeR,
                    footR = footR,
                    isGirl = isGirl,
                    boneColor = boneColor,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = strokeBone
                )
                drawCircle(equipColor, radius = headRadius * 0.55f, center = handL)
                drawCircle(equipColor, radius = headRadius * 0.55f, center = handR)
            }
        }
    }
}

private fun DrawScope.drawStickFigure(
    head: Offset,
    headRadius: Float,
    shoulder: Offset,
    hip: Offset,
    elbowL: Offset,
    handL: Offset,
    elbowR: Offset,
    handR: Offset,
    kneeL: Offset,
    footL: Offset,
    kneeR: Offset,
    footR: Offset,
    isGirl: Boolean,
    boneColor: Color,
    primaryColor: Color,
    secondaryColor: Color,
    strokeWidth: Float
) {
    // Legs
    drawLine(boneColor, hip, kneeL, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(secondaryColor, kneeL, footL, strokeWidth = strokeWidth * 0.9f, cap = StrokeCap.Round)
    drawLine(boneColor, hip, kneeR, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(secondaryColor, kneeR, footR, strokeWidth = strokeWidth * 0.9f, cap = StrokeCap.Round)

    // Torso
    drawLine(primaryColor, shoulder, hip, strokeWidth = strokeWidth * 1.25f, cap = StrokeCap.Round)

    // If Girl, add athletic waist/skirt flare or ponytail detail; if Boy, add broad shoulder bar + headband
    if (isGirl) {
        val ponytailPath = Path().apply {
            moveTo(head.x + headRadius * 0.7f, head.y - headRadius * 0.2f)
            quadraticTo(
                head.x + headRadius * 1.9f,
                head.y - headRadius * 0.7f,
                head.x + headRadius * 1.7f,
                head.y + headRadius * 0.8f
            )
        }
        drawPath(
            path = ponytailPath,
            color = primaryColor,
            style = Stroke(width = strokeWidth * 0.75f, cap = StrokeCap.Round)
        )
    } else {
        // Athletic headband stripe on Boy stickman
        drawLine(
            color = secondaryColor,
            start = Offset(head.x - headRadius * 0.9f, head.y - headRadius * 0.35f),
            end = Offset(head.x + headRadius * 0.9f, head.y - headRadius * 0.35f),
            strokeWidth = strokeWidth * 0.55f,
            cap = StrokeCap.Round
        )
    }

    // Arms
    drawLine(boneColor, shoulder, elbowL, strokeWidth = strokeWidth * 0.9f, cap = StrokeCap.Round)
    drawLine(primaryColor, elbowL, handL, strokeWidth = strokeWidth * 0.85f, cap = StrokeCap.Round)
    drawLine(boneColor, shoulder, elbowR, strokeWidth = strokeWidth * 0.9f, cap = StrokeCap.Round)
    drawLine(primaryColor, elbowR, handR, strokeWidth = strokeWidth * 0.85f, cap = StrokeCap.Round)

    // Head
    drawCircle(color = boneColor, radius = headRadius, center = head)
    drawCircle(color = primaryColor, radius = strokeWidth * 0.55f, center = hip)
    drawCircle(color = secondaryColor, radius = strokeWidth * 0.5f, center = kneeL)
    drawCircle(color = secondaryColor, radius = strokeWidth * 0.5f, center = kneeR)
}
