package com.khatibstudio.noyza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.khatibstudio.noyza.ui.NoyZaApp
import com.khatibstudio.noyza.ui.theme.NoyZaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge — applied globally, insets handled per-screen
        // Lesson from Cyvia: do this ONCE here, never hardcode padding values
        enableEdgeToEdge()

        setContent {
            NoyZaTheme {
                NoyZaApp()
            }
        }
    }
}
