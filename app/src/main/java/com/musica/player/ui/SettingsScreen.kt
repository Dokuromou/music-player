package com.musica.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.musica.player.MusicPlayerViewModel

@Composable
fun SettingsScreen(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val amoled by viewModel.amoled.collectAsState()
    val sleepRemainingMs by viewModel.sleepRemainingMs.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pure black dark theme", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Saves battery on OLED screens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = amoled, onCheckedChange = { viewModel.setAmoled(it) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Sleep timer", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (sleepRemainingMs != null)
                        "Stops music in ${formatDuration(sleepRemainingMs ?: 0L)}"
                    else "Not active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf(15, 30, 60).forEach { minutes ->
                        Button(
                            onClick = { viewModel.startSleepTimer(minutes) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("${minutes}m")
                        }
                    }
                    Button(onClick = { viewModel.cancelSleepTimer() }) {
                        Text("Off")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Playback speed", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Current: %.2fx".format(playbackSpeed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Button(
                            onClick = { viewModel.setPlaybackSpeed(speed) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("%.2fx".format(speed).removeSuffix("0").removeSuffix("."))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Library", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.loadSongs(context) }) {
                    Text("Rescan device music")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "About", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Musica v1.1 — a fully offline music player. " +
                        "It has no internet permission: your music and data " +
                        "never leave this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
