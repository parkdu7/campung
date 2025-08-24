package com.shinhan.campung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    // 처음 1회 토큰 로드 → startDestination 결정
    var start by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val token = authDataStore.tokenFlow.first()
        start = if (token.isNullOrBlank()) Route.LOGIN else Route.HOME
    }

    if (start == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    NavHost(navController = navController, startDestination = start!!) {

        // 로그인 라우트에서 회원가입 이동 콜백 연결
        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.HOME) {
                        popUpTo(0); launchSingleTop = true
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Route.SIGNUP)
                }
            )
        }

        // 회원가입 라우트 추가
        composable(Route.SIGNUP) {
            SignupScreen(
                onSignupSuccess = {
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.HOME) {
            HomeScreen(
                onLoggedOut = {
                    // 토큰도 이미 클리어됨 → 로그인 화면으로
                    navController.navigate(Route.LOGIN) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}