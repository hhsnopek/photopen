package dev.hhsnopek.photopen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.hhsnopek.photopen.ui.PhotoPenRoot
import dev.hhsnopek.photopen.ui.theme.PhotoPenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoPenTheme {
                PhotoPenRoot()
            }
        }
    }
}
