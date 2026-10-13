package com.louaysadok.redgif

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louaysadok.redgif.data.settings.ThemeMode
import com.louaysadok.redgif.ui.AppRoot
import com.louaysadok.redgif.ui.theme.RedGifTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RedGifApp).container

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            RedGifTheme(mode = settings?.theme ?: ThemeMode.SYSTEM) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(container = container, onExit = { finish() })
                }
            }
        }
    }
}
