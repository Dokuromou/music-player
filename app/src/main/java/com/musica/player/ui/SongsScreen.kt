package com.musica.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musica.player.MusicPlayerViewModel
import com.musica.player.data.Song

enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    RECENT("Recently added")
}

@Composable
fun SongsScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    lockedSort: SortOrder? = null,
    title: String = "Musica",
    showHeader: Boolean = true
) {
    val songs by viewModel.songs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    var query by remember { mutableStateOf(initialQuery) }
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    val sourceList: List<Song> = when (lockedSort) {
        SortOrder.RECENT -> recentlyPlayed.mapNotNull { id -> songs.find { it.id == id } }
        else -> songs
    }

    val filteredSongs = remember(sourceList, query) {
        if (query.isBlank()) sourceList
        else sourceList.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    val displayList = remember(filteredSongs, sortOrder, lockedSort) {
        when (lockedSort ?: sortOrder) {
            SortOrder.TITLE -> filteredSongs.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> filteredSongs.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> filteredSongs.sortedBy { it.album.lowercase() }
            SortOrder.DURATION -> filteredSongs.sortedBy { it.durationMs }
            SortOrder.RECENT -> filteredSongs
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${displayList.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lockedSort == null) {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label +
                                            if (order == sortOrder) " ✓" else ""
                                    )
                                },
                                onClick = {
                                    sortOrder = order
                                    sortMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (displayList.isEmpty()) {
            EmptyLibrary(hasAnySongs = songs.isEmpty())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(displayList, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favorites,
                        onClick = { viewModel.playQueue(displayList, index) },
                        onLongClick = { addToPlaylistSong = song },
                        onToggleFavorite = { viewModel.toggleFavorite(song.id) }
                    )
                }
            }
        }
    }

    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(
            viewModel = viewModel,
            song = song,
            onDismiss = { addToPlaylistSong = null }
        )
    }
}

@Composable
fun EmptyLibrary(hasAnySongs: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.padding(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (hasAnySongs) "No results" else "No music found on this device",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}
