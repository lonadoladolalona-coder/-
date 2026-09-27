package org.anmoljeevan.office

import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import org.anmoljeevan.office.ui.admin.AdminRoot
import org.anmoljeevan.office.ui.gate.GateScreen
import org.anmoljeevan.office.ui.media.MediaRoot
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Motion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // the splash icon grows a little and the whole splash fades into the app
        splash.setOnExitAnimationListener { provider ->
            runCatching {
                provider.iconView.animate().scaleX(1.25f).scaleY(1.25f).setDuration(340L).start()
            }
            provider.view.animate()
                .alpha(0f)
                .setDuration(340L)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { provider.remove() }
                .start()
        }
        setContent {
            AjmTheme { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel = viewModel()) {
    AnimatedContent(
        targetState = vm.portal,
        transitionSpec = {
            (fadeIn(tween(420, delayMillis = 90, easing = Motion.EmphasizedDecelerate)) +
                scaleIn(tween(420, delayMillis = 90, easing = Motion.EmphasizedDecelerate), initialScale = 0.94f)) togetherWith
                (fadeOut(tween(160)) + scaleOut(tween(220), targetScale = 1.04f))
        },
        label = "portal",
    ) { p ->
        when (p) {
            Portal.Gate -> GateScreen(vm)
            is Portal.Admin -> AdminRoot(p.store, onLogout = vm::logout)
            is Portal.Media -> MediaRoot(p.store, onLogout = vm::logout)
        }
    }
}
