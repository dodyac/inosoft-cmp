package com.ahmaddody.newsreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ahmaddody.newsreader.debug.DebugTools
import com.ahmaddody.newsreader.presentation.navigation.NewsNavHost
import com.ahmaddody.newsreader.presentation.theme.NewsReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsReaderTheme {
                NewsNavHost()
                // Sibling of the app content, and a no-op in release builds.
                DebugTools.Overlay()
            }
        }
    }
}
