package com.cleanx.lcx.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cleanx.lcx.core.network.SessionExpiredInterceptor
import com.cleanx.lcx.core.session.SessionManager
import com.cleanx.lcx.feature.auth.ui.LoginScreen
import com.cleanx.lcx.ui.shell.MainScaffold
import kotlinx.coroutines.launch
import timber.log.Timber

private const val NAV_ANIM_DURATION = 300

/**
 * Root-level navigation host.
 *
 * Contains exactly two top-level destinations:
 *  1. **Login** — shown before authentication (no bottom bar).
 *  2. **Main**  — post-login shell with a [MainScaffold] that provides the
 *     bottom navigation bar and per-tab nested nav graphs.
 *
 * The 401 session-expired handler remains at this level because it needs to
 * pop the entire back-stack and navigate to Login regardless of which tab or
 * nested screen the user is on.
 */
@Composable
fun LcxNavHost(
    sessionExpiredInterceptor: SessionExpiredInterceptor? = null,
    sessionManager: SessionManager? = null,
) {
    val rootNavController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Global 401 handler: clear session and redirect to Login when session expires.
    if (sessionExpiredInterceptor != null) {
        LaunchedEffect(Unit) {
            sessionExpiredInterceptor.sessionExpired.collect {
                Timber.tag("AUTH").w("Session expired — redirecting to Login")
                sessionManager?.clearSession()
                rootNavController.navigate(Screen.Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = Screen.Login,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION),
            )
        },
    ) {
        // ── Login (no bottom bar) ───────────────────────────────────
        composable<Screen.Login>(
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
        ) {
            LoginScreen(
                onAuthenticated = {
                    rootNavController.navigate(Screen.Main) {
                        popUpTo<Screen.Login> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ── Main shell (with bottom navigation bar) ─────────────────
        composable<Screen.Main>(
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
        ) {
            MainScaffold(
                onSignOut = {
                    scope.launch {
                        sessionManager?.clearSession()
                        rootNavController.navigate(Screen.Login) {
                            popUpTo<Screen.Main> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}
