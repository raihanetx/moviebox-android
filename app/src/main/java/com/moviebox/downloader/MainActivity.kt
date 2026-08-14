package com.moviebox.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.moviebox.downloader.ui.AppNav
import com.moviebox.downloader.ui.theme.MovieBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Capture shared text from other apps (e.g., browser share → MovieBox)
        val sharedText = handleSharedText()

        setContent {
            MovieBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    color = Color.Black
                ) {
                    AppNav(initialUrl = sharedText)
                }
            }
        }
    }

    private fun handleSharedText(): String? {
        val intent = intent ?: return null
        if (intent.action != android.content.Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: return null
        return text.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
