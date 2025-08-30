package com.shinhan.campung.navigation

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
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
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
import com.shinhan.campung.presentation.ui.screens.WritePostScreen
import kotlinx.coroutines.flow.first

@Composable
fun AppNav(
    authDataStore: AuthDataStore, 
    sharedMapView: MapView,
    onNavControllerReady: (NavController) -> Unit = {},
    initialRoute: String? = null  // FCM에서 전달할 초기 라우트
) {
    val navController = rememberNavController()
    
    // NavController가 준비되면 MainActivity에 전달
    LaunchedEffect(navController) {
        onNavControllerReady(navController)
    }

    // 1) 로딩 상태 관리
    var isInitialLoading by remember { mutableStateOf(true) }
    val userId by authDataStore.userIdFlow.collectAsState(initial = null)
    
    // 2) 첫 번째 값이 도착하면 (null이든 실제값이든) 로딩 완료
    LaunchedEffect(Unit) {
        val firstValue = authDataStore.userIdFlow.first()
        isInitialLoading = false
        android.util.Log.d("AppNav", "DataStore 로딩 완료 - userId: '$firstValue'")
    }
    
    // 3) 초기 로딩 중이면 로딩 화면 표시
    if (isInitialLoading) {
        android.util.Log.d("AppNav", "초기 로딩 중...")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 4) 로딩 완료 후 적절한 화면 결정
    val startRoute = if (userId.isNullOrBlank()) Route.LOGIN else Route.HOME
    
    // 디버깅 로그
    LaunchedEffect(startRoute) {
        android.util.Log.d("AppNav", "라우팅 결정: userId = '$userId', startRoute = '$startRoute'")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 항상 깔리는 기본 배경 (검은 잔상 방지)
        Surface(color = MaterialTheme.colorScheme.background) {} // 항상 배경 유지

        NavHost(
            navController = navController,
            startDestination = startRoute,
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

            // 글쓰기 화면
            composable(Route.WRITE_POST) {
//                WritePostScreen(
//                    onBack = { navController.popBackStack() },
//                    onSubmitted = { navController.popBackStack() } // 등록 → 맵으로 복귀
//                )
                WritePostScreen(
                    onBack = { navController.popBackStack() },
                    onSubmitted = { newId ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("map_refresh_content_id", newId)
                        navController.popBackStack()  // 맵으로 복귀
                    }
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
        
        // FCM 라우트가 있으면 HOME 로드 후 네비게이션 스택에 추가
        // launchMode="singleTop"으로 설정했으므로:
        // - 앱 종료 상태에서 FCM 클릭: onCreate에서만 이 로직 실행
        // - 앱 백그라운드에서 FCM 클릭: onNewIntent에서 처리
        LaunchedEffect(navController, initialRoute) {
            initialRoute?.let { route ->
                if (startRoute == Route.HOME) {
                    // HOME이 완전히 로드되길 기다림
                    kotlinx.coroutines.delay(300)
                    when {
                        route.startsWith("content_detail/") -> {
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                        route == "notification" -> {
                            navController.navigate(Route.NOTIFICATION) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }
}
