package com.example.madomireader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.madomireader.ui.navigation.AppNavHost
import com.example.madomireader.ui.theme.MadomiReaderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MangaReaderApp).container

        setContent {
            MadomiReaderTheme {
                AppNavHost(container = container)
            }
        }
    }
}
