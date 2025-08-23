package com.shinhan.campung.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shinhan.campung.presentation.viewmodel.SignupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: SignupViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("뒤로") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이디 + 중복확인
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.userId.value,
                    onValueChange = {
                        vm.userId.value = it
                        vm.dupChecked.value = false
                        vm.dupAvailable.value = null
                    },
                    label = { Text("아이디") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { vm.checkDuplicate() },
                    enabled = !vm.loading.value
                ) { Text("중복확인") }
            }

            vm.dupAvailable.value?.let { ok ->
                val msg = if (ok) "사용 가능한 아이디입니다." else "이미 사용 중인 아이디입니다."
                val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(msg, color = color)
            }

            OutlinedTextField(
                value = vm.nickname.value,
                onValueChange = { vm.nickname.value = it },
                label = { Text("닉네임") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.password.value,
                onValueChange = { vm.password.value = it },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.password2.value,
                onValueChange = { vm.password2.value = it },
                label = { Text("비밀번호 확인") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.signUp(onSignupSuccess) },
                enabled = !vm.loading.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (vm.loading.value) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("회원가입 완료")
            }

            vm.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
