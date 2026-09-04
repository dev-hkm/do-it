package at.co.schwaerzler.maximilian.doit.util

import kotlin.math.abs

private const val SWIPE_DELETE_THRESHOLD_FRACTION = 0.32f

/** Keeps the destructive swipe deliberate without making it feel heavy. */
internal fun swipeDeleteThreshold(distance: Float): Float =
    (distance.coerceAtLeast(0f) * SWIPE_DELETE_THRESHOLD_FRACTION).coerceAtLeast(1f)

internal fun shouldCommitSwipe(offset: Float, distance: Float): Boolean =
    distance > 0f && abs(offset) >= swipeDeleteThreshold(distance)
