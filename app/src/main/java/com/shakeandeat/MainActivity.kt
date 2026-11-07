package com.shakeandeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shakeandeat.ui.AppNavigation
import com.shakeandeat.ui.theme.ShakeAndEatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShakeAndEatTheme {
                AppNavigation()
            }
        }
    }
}
