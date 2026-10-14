package com.shkurta.pickle.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

/**
 * Material "shared axis" style: screens travel a short distance while cross-fading,
 * instead of sliding a full screen width. Short travel + emphasized easing reads as smooth.
 */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val DURATION = 380
private const val FADE_IN_DURATION = 220
private const val FADE_OUT_DURATION = 120

private fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(DURATION, easing = Emphasized)
private fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(DURATION, easing = Emphasized)

private fun offsetSpec(): FiniteAnimationSpec<IntOffset> = enterSpec()

/** How far (as a fraction of width/height) a screen travels during a transition. */
private const val TRAVEL = 0.15f

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

// Forward: new screen comes in from the right, old one drifts left.
val sharedAxisEnter: Scope.() -> EnterTransition = {
    slideInHorizontally(offsetSpec()) { (it * TRAVEL).toInt() } +
        fadeIn(tween(FADE_IN_DURATION, delayMillis = FADE_OUT_DURATION / 2, easing = Emphasized))
}
val sharedAxisExit: Scope.() -> ExitTransition = {
    slideOutHorizontally(exitSpec()) { -(it * TRAVEL).toInt() } +
        fadeOut(tween(FADE_OUT_DURATION, easing = EmphasizedAccelerate))
}

// Back: everything runs in reverse.
val sharedAxisPopEnter: Scope.() -> EnterTransition = {
    slideInHorizontally(offsetSpec()) { -(it * TRAVEL).toInt() } +
        fadeIn(tween(FADE_IN_DURATION, delayMillis = FADE_OUT_DURATION / 2, easing = Emphasized))
}
val sharedAxisPopExit: Scope.() -> ExitTransition = {
    slideOutHorizontally(exitSpec()) { (it * TRAVEL).toInt() } +
        fadeOut(tween(FADE_OUT_DURATION, easing = EmphasizedAccelerate))
}

// Sheet-like screens (new poll) rise from the bottom and sink back down.
val sheetEnter: Scope.() -> EnterTransition = {
    slideInVertically(offsetSpec()) { (it * 0.25f).toInt() } +
        fadeIn(tween(FADE_IN_DURATION, easing = Emphasized))
}
val sheetPopExit: Scope.() -> ExitTransition = {
    slideOutVertically(tween(260, easing = EmphasizedAccelerate)) { (it * 0.25f).toInt() } +
        fadeOut(tween(200, easing = EmphasizedAccelerate))
}

// Root swaps (log in, sign out): a calm cross-fade, no direction implied.
val fadeThroughEnter: Scope.() -> EnterTransition = {
    fadeIn(tween(300, delayMillis = 90, easing = Emphasized))
}
val fadeThroughExit: Scope.() -> ExitTransition = {
    fadeOut(tween(90, easing = EmphasizedAccelerate))
}

private fun NavBackStackEntry.isSheet() = destination.hasRoute<Screen.CreatePoll>()

// The screen underneath a sheet stays in place and just fades, so only one thing is moving.
val defaultExit: Scope.() -> ExitTransition = {
    if (targetState.isSheet()) fadeOut(tween(DURATION, easing = Emphasized)) else sharedAxisExit()
}
val defaultPopEnter: Scope.() -> EnterTransition = {
    if (initialState.isSheet()) fadeIn(tween(260, easing = Emphasized)) else sharedAxisPopEnter()
}
