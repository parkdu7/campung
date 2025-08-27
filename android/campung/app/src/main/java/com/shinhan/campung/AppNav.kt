package com.shinhan.campung

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.naver.maps.map.MapView
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.navigation.Route
import com.shinhan.campung.presentation.ui.screens.contentdetail.ContentDetailScreen
import com.shinhan.campung.presentation.ui.screens.FriendScreen
import com.shinhan.campung.presentation.ui.screens.FullMapScreen
import com.shinhan.campung.presentation.ui.screens.HomeScreen
import com.shinhan.campung.presentation.ui.screens.LoginScreen
import com.shinhan.campung.presentation.ui.screens.NotificationScreen
import com.shinhan.campung.presentation.ui.screens.SignupScreen
import kotlinx.coroutines.flow.first

@Composable
fun AppNav(authDataStore: AuthDataStore, sharedMapView: MapView) {
    val navController = rememberNavController()

    var start by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val userId = authDataStore.userIdFlow.first()
        start = if (userId.isNullOrBlank()) Route.LOGIN else Route.HOME
    }
    if (start == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 항상 깔리는 기본 배경 (검은 잔상 방지)
        Surface(color = MaterialTheme.colorScheme.background) {} // 항상 배경 유지

        NavHost(
            navController = navController,
            startDestination = start!!,
            enterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
        ) {
            composable(Route.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Route.HOME) {
                            popUpTo(0); launchSingleTop = true
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Route.SIGNUP) }
                )
            }

            composable(Route.SIGNUP) {
                SignupScreen(
                    onSignupSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.FRIEND) {
                FriendScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }

            // 홈 화면
            composable(
                route = Route.HOME,
                exitTransition = {
                    fadeOut(tween(150)) + scaleOut(
                        targetScale = 0.98f,
                        animationSpec = tween(150)
                    )
                },
                popEnterTransition = {
                    fadeIn(tween(180, delayMillis = 60)) + scaleIn(
                        initialScale = 0.98f,
                        animationSpec = tween(180, delayMillis = 60)
                    )
                }
            ) {
                HomeScreen(
                    navController = navController,
                    onLoggedOut = {
                        navController.navigate(Route.LOGIN) {
                            popUpTo(0); launchSingleTop = true
                        }
                    },
                    sharedMapView = sharedMapView // 전달
                )
            }

            // 풀맵 화면 (자연스럽게 밑에서 위로 슬라이드 + 페이드)
            composable(
                route = "map/full",
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it }, // 밑에서 올라옴
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(300))
                },
                // popExit만 정의 (뒤로 갈 때 내려감)
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(300))
                }
            ) {
                FullMapScreen(navController = navController, mapView = sharedMapView )
            }

            // 알림 화면
            composable(Route.NOTIFICATION) {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 컨텐츠 상세 화면
            composable(
                route = "${Route.CONTENT_DETAIL}/{contentId}",
                arguments = listOf(navArgument("contentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val contentId = backStackEntry.arguments?.getLong("contentId") ?: 0L
                ContentDetailScreen(
                    contentId = contentId,
                    navController = navController
                )
            }
        }
    }
}
