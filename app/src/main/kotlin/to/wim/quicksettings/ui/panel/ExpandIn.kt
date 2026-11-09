package to.wim.quicksettings.ui.panel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/** Material 3 "emphasized decelerate" easing, as used for things entering the screen. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Grows the popup out of the top of the screen, where the tile was, while it fades in: the
 * closest an app can get to Android's own expand-from-tile animation, which only System UI can
 * draw. With animations turned off, Android sets the durations to zero and it simply appears.
 */
@Composable
fun Modifier.expandIn(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 400, easing = EmphasizedDecelerate)) }
    return graphicsLayer {
        val p = progress.value
        alpha = (p * 1.5f).coerceAtMost(1f)
        scaleX = 0.85f + 0.15f * p
        scaleY = 0.6f + 0.4f * p
        translationY = -(1f - p) * size.height * 0.25f
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}
