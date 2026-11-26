package moe.nyarchlinux.catgirldownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import moe.nyarchlinux.catgirldownloader.ui.CatgirlDownloaderApp
import moe.nyarchlinux.catgirldownloader.ui.theme.CatgirlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatgirlTheme {
                CatgirlDownloaderApp()
            }
        }
    }
}
