package com.musica.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.musica.player.ui.MusicaApp
import com.musica.player.ui.theme.MusicaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicaTheme {
                MusicaApp(viewModel = viewModel)
            }
        }
        viewModel.initialize(this)
    }
}
