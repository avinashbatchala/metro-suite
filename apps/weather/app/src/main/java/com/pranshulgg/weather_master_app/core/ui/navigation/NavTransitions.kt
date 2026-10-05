package com.pranshulgg.weather_master_app.core.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.metro.ui.MetroTransitions

/**
 * WP page-pivot navigation motion shared with the metro suite. A forward push fades the
 * incoming page in from the right with a subtle side slide + scale settle; Back mirrors it.
 *
 * Durations and easings come from [MetroTransitions] so weather drills in and out exactly
 * like calendar / people / dialer ([MetroPagePivotLoad] language), rather than a long fade.
 */
object NavTransitions {

    /** Enter offset as a fraction of viewport width; small so it reads as a settle, not a slide. */
    private const val PUSH_START_FRACTION = 14
    private const val POP_START_FRACTION = 18
    private const val EXIT_FRACTION = 18
    private const val POP_EXIT_FRACTION = 14

    private const val ENTER_SCALE = 0.96f
    private const val EXIT_SCALE = 0.98f

    fun enter(): EnterTransition =
        fadeIn(MetroTransitions.pageTween<Float>()) +
            slideInHorizontally(MetroTransitions.pageTween()) { fullWidth -> fullWidth / PUSH_START_FRACTION } +
            scaleIn(MetroTransitions.pageTween(), initialScale = ENTER_SCALE)

    fun exit(): ExitTransition =
        fadeOut(MetroTransitions.pagePivotExitTween<Float>()) +
            slideOutHorizontally(MetroTransitions.pagePivotExitTween()) { fullWidth -> -fullWidth / EXIT_FRACTION } +
            scaleOut(MetroTransitions.pagePivotExitTween(), targetScale = EXIT_SCALE)

    fun popEnter(): EnterTransition =
        fadeIn(MetroTransitions.pageTween<Float>()) +
            slideInHorizontally(MetroTransitions.pageTween()) { fullWidth -> -fullWidth / POP_START_FRACTION } +
            scaleIn(MetroTransitions.pageTween(), initialScale = EXIT_SCALE)

    fun popExit(): ExitTransition =
        fadeOut(MetroTransitions.pagePivotExitTween<Float>()) +
            slideOutHorizontally(MetroTransitions.pagePivotExitTween()) { fullWidth -> fullWidth / POP_EXIT_FRACTION } +
            scaleOut(MetroTransitions.pagePivotExitTween(), targetScale = ENTER_SCALE)
}
