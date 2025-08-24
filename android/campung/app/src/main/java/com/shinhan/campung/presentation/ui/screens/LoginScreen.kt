package com.shinhan.campung.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.messaging.FirebaseMessaging
import com.shinhan.campung.presentation.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    var fcmToken by remember { mutableStateOf<String?>(null) }

    // FCM 토큰 가져오기 (권한 없어도 토큰 조회 가능)
    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken = it }
            .addOnFailureListener { fcmToken = null }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("로그인") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = vm.userId.value,
                onValueChange = { vm.userId.value = it },
                label = { Text("아이디") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = vm.password.value,
                onValueChange = { vm.password.value = it },
                label = { Text("비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { vm.login(fcmToken, onLoginSuccess) },
                enabled = !vm.loading.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (vm.loading.value) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("로그인")
            }

            TextButton(onClick = { /* nav to signup */ onNavigateToSignUp() }) {
                Text("회원가입")
            }

            vm.error.value?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(msg, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
