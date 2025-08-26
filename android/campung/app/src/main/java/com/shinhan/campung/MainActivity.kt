package com.shinhan.campung

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.navigation.Route
import com.shinhan.campung.presentation.ui.screens.FullMapScreen
import com.shinhan.campung.presentation.ui.screens.HomeScreen
import com.shinhan.campung.presentation.ui.screens.LoginScreen
import com.shinhan.campung.presentation.ui.screens.SignupScreen
import com.shinhan.campung.presentation.ui.theme.CampungTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authDataStore: AuthDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true // 자동 대비 조정 비활성화
        }
        setContent {
            CampungTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(authDataStore)
                }
            }
        }
    }
}

@Composable
private fun AppNav(authDataStore: AuthDataStore) {
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

    NavHost(
        navController = navController,
        startDestination = start!!,
        // 기본(전역) 전환
        enterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
        exitTransition  = { fadeOut(animationSpec = tween(120)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220, delayMillis = 90)) },
        popExitTransition  = { fadeOut(animationSpec = tween(120)) }
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
                }
            )
        }

        // 풀맵 화면 (슬라이드 업/다운 + 페이드)
        composable(
            route = "map/full",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(280))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(240))
            }
        ) {
            FullMapScreen(navController = navController)
        }
    }
}


//@Composable
//private fun AppNav(authDataStore: AuthDataStore) {
//    val navController = rememberNavController()
//
//    // 처음 1회 토큰 로드 → startDestination 결정
//    var start by remember { mutableStateOf<String?>(null) }
//    LaunchedEffect(Unit) {
//        val token = authDataStore.tokenFlow.first()
//        start = if (token.isNullOrBlank()) Route.LOGIN else Route.HOME
//    }
//
//    if (start == null) {
//        Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
//        return
//    }
//
//    NavHost(navController = navController, startDestination = start!!) {
//
//        // 로그인 라우트에서 회원가입 이동 콜백 연결
//        composable(Route.LOGIN) {
//            LoginScreen(
//                onLoginSuccess = {
//                    navController.navigate(Route.HOME) {
//                        popUpTo(0); launchSingleTop = true
//                    }
//                },
//                onNavigateToSignUp = {
//                    navController.navigate(Route.SIGNUP)
//                }
//            )
//        }
//
//        // 회원가입 라우트 추가
//        composable(Route.SIGNUP) {
//            SignupScreen(
//                onSignupSuccess = {
//                    navController.popBackStack()
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        composable(Route.HOME) {
//            HomeScreen(
//                navController = navController,
//                onLoggedOut = {
//                    // 토큰도 이미 클리어됨 → 로그인 화면으로
//                    navController.navigate(Route.LOGIN) {
//                        popUpTo(0)
//                        launchSingleTop = true
//                    }
//                }
//            )
//        }
//
//        composable("map/full") {
//            FullMapScreen(navController = navController)
//        }
//    }
//}