package com.tricosoft.booknest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.BookNestRoot
import com.tricosoft.booknest.ui.BookNestTheme
import com.tricosoft.booknest.ui.VaultDialogHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val graph = (application as BookNestApp).graph

        setContent {
            val dark = graph.prefs.dark
            // Status bar / nav bar transparent (edge-to-edge); icon er rong app theme onujayi
            SideEffect {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CompositionLocalProvider(LocalGraph provides graph) {
                BookNestTheme(dark = dark) {
                    BookNestRoot()
                    VaultDialogHost()
                }
            }
        }
    }
}
