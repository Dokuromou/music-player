package com.musica.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musica.player.MusicPlayerViewModel

private enum class LibraryFilter(val label: String) {
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    SONGS("Songs")
}

@Composable
fun LibraryScreen(
    viewModel: MusicPlayerViewModel,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(LibraryFilter.PLAYLISTS) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            androidx.compose.foundation.lazy.items(LibraryFilter.entries.toList()) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.padding(4.dp))
        when (filter) {
            LibraryFilter.PLAYLISTS -> PlaylistsScreen(
                viewModel = viewModel,
                onOpenPlaylist = onOpenPlaylist,
                showHeader = false,
                modifier = Modifier.weight(1f)
            )
            LibraryFilter.ARTISTS -> ArtistsScreen(
                viewModel = viewModel,
                onOpenArtist = onOpenArtist,
                showHeader = false,
                modifier = Modifier.weight(1f)
            )
            LibraryFilter.ALBUMS -> AlbumsScreen(
                viewModel = viewModel,
                onOpenAlbum = onOpenAlbum,
                showHeader = false,
                modifier = Modifier.weight(1f)
            )
            LibraryFilter.SONGS -> SongsScreen(
                viewModel = viewModel,
                showHeader = false,
                title = "Songs",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
