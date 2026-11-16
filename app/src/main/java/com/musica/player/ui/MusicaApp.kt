package com.musica.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.musica.player.MusicPlayerViewModel
import com.musica.player.ui.theme.MusicaTheme

private enum class Tab(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

@Composable
fun MusicaApp(viewModel: MusicPlayerViewModel) {
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission()) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.loadSongs(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.loadSongs(context)
        } else {
            audioPermissionLauncher.launch(audioPermission())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val amoled by viewModel.amoled.collectAsState()

    MusicaTheme(amoled = amoled) {
        if (!hasAudioPermission) {
            PermissionScreen(onRequest = { audioPermissionLauncher.launch(audioPermission()) })
        } else {
            MainScreen(viewModel)
        }
    }
}

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Musica needs access to your music",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Allow access to audio files so your library can be loaded. " +
                "Nothing ever leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text(text = "Grant access")
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MainScreen(viewModel: MusicPlayerViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var detail by remember { mutableStateOf<DetailScreen?>(null) }
    var editRequest by remember { mutableStateOf<EditOrderRequest?>(null) }
    var nowPlayingOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = nowPlayingOpen) { nowPlayingOpen = false }
    BackHandler(enabled = !nowPlayingOpen && editRequest != null) { editRequest = null }
    BackHandler(enabled = !nowPlayingOpen && editRequest == null && detail != null) {
        detail = null
    }

    Scaffold(
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = currentSong != null && !nowPlayingOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    MiniPlayerBar(
                        viewModel = viewModel,
                        onOpenNowPlaying = { nowPlayingOpen = true }
                    )
                }
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t && detail == null && editRequest == null,
                            onClick = {
                                tab = t
                                detail = null
                                editRequest = null
                            },
                            icon = {
                                Icon(
                                    imageVector = when (t) {
                                        Tab.HOME -> Icons.Filled.Home
                                        Tab.SEARCH -> Icons.Filled.Search
                                        Tab.LIBRARY -> Icons.Filled.LibraryMusic
                                        Tab.SETTINGS -> Icons.Filled.Settings
                                    },
                                    contentDescription = t.label
                                )
                            },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                editRequest != null -> EditOrderScreen(
                    viewModel = viewModel,
                    request = editRequest!!,
                    onDone = { editRequest = null }
                )
                detail != null -> DetailScreenHost(
                    viewModel = viewModel,
                    detail = detail!!,
                    onBack = { detail = null },
                    onEdit = { editRequest = it }
                )
                tab == Tab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onOpenAlbum = { detail = DetailScreen.Album(it) },
                    onOpenArtist = { detail = DetailScreen.Artist(it) },
                    onOpenPlaylist = { detail = DetailScreen.Playlist(it) },
                    modifier = Modifier.weight(1f)
                )
                tab == Tab.SEARCH -> SearchScreen(
                    viewModel = viewModel,
                    onOpenAlbum = { detail = DetailScreen.Album(it) },
                    onOpenArtist = { detail = DetailScreen.Artist(it) },
                    modifier = Modifier.weight(1f)
                )
                tab == Tab.LIBRARY -> LibraryScreen(
                    viewModel = viewModel,
                    onOpenAlbum = { detail = DetailScreen.Album(it) },
                    onOpenArtist = { detail = DetailScreen.Artist(it) },
                    onOpenPlaylist = { detail = DetailScreen.Playlist(it) },
                    modifier = Modifier.weight(1f)
                )
                tab == Tab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = nowPlayingOpen,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NowPlayingScreen(viewModel = viewModel, onClose = { nowPlayingOpen = false })
    }
}
