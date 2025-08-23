package com.shinhan.campung.presentation.ui.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.shinhan.campung.presentation.ui.theme.CampungTheme

abstract class BaseComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampungTheme {
                Content()
            }
        }
    }

    @Composable
    abstract fun Content()
}