package com.hhc558.passcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hhc558.passcontrol.ui.AppNavHost
import com.hhc558.passcontrol.ui.theme.PassControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PassControlTheme {
                AppNavHost()
            }
        }
    }
}