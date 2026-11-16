package com.musica.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musica.player.MusicPlayerViewModel
import com.musica.player.data.Song

const val FAVORITES_ID = -1L

sealed interface DetailScreen {
    data class Album(val name: String) : DetailScreen
    data class Artist(val name: String) : DetailScreen
    data class Playlist(val id: Long) : DetailScreen
}

@Composable
fun DetailScreenHost(
    viewModel: MusicPlayerViewModel,
    detail: DetailScreen,
    onBack: () -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    when (detail) {
        is DetailScreen.Album -> {
            val albumSongs = songs.filter { it.album == detail.name }
                .sortedBy { it.title.lowercase() }
            DetailContent(
                viewModel = viewModel,
                title = detail.name,
                subtitle = "Album",
                artworkUri = albumSongs.firstOrNull()?.artworkUri.orEmpty(),
                songs = albumSongs,
                favorites = favorites,
                onBack = onBack
            )
        }
        is DetailScreen.Artist -> {
            val artistSongs = songs.filter { it.artist == detail.name }
                .sortedBy { it.title.lowercase() }
            DetailContent(
                viewModel = viewModel,
                title = detail.name,
                subtitle = "Artist",
                artworkUri = artistSongs.firstOrNull()?.artworkUri.orEmpty(),
                songs = artistSongs,
                favorites = favorites,
                onBack = onBack
            )
        }
        is DetailScreen.Playlist -> {
            if (detail.id == FAVORITES_ID) {
                val favSongs = songs.filter { it.id in favorites }
                DetailContent(
                    viewModel = viewModel,
                    title = "Favorites",
                    subtitle = "Playlist",
                    artworkUri = favSongs.firstOrNull()?.artworkUri.orEmpty(),
                    songs = favSongs,
                    favorites = favorites,
                    onBack = onBack,
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            } else {
                val playlist = playlists.find { it.id == detail.id }
                val playlistSongs = playlist?.songIds
                    ?.mapNotNull { id -> songs.find { it.id == id } }
                    ?: emptyList()
                DetailContent(
                    viewModel = viewModel,
                    title = playlist?.name ?: "Playlist",
                    subtitle = "Playlist",
                    artworkUri = playlistSongs.firstOrNull()?.artworkUri.orEmpty(),
                    songs = playlistSongs,
                    favorites = favorites,
                    onBack = onBack,
                    playlistId = detail.id,
                    onRemoveFromPlaylist = { viewModel.removeFromPlaylist(detail.id, it) },
                    onDeletePlaylist = {
                        viewModel.deletePlaylist(detail.id)
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    viewModel: MusicPlayerViewModel,
    title: String,
    subtitle: String,
    artworkUri: String,
    songs: List<Song>,
    favorites: Set<Long>,
    onBack: () -> Unit,
    playlistId: Long? = null,
    onToggleFavorite: ((Long) -> Unit)? = null,
    onRemoveFromPlaylist: ((Long) -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (onDeletePlaylist != null) {
                IconButton(onClick = onDeletePlaylist) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete playlist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.playQueue(songs, 0) },
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Play all")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (songs.isEmpty()) {
            EmptyLibrary(hasAnySongs = true)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    if (playlistId != null && onRemoveFromPlaylist != null) {
                        SongRow(
                            song = song,
                            isFavorite = song.id in favorites,
                            onClick = { viewModel.playQueue(songs, index) },
                            onLongClick = { onRemoveFromPlaylist(song.id) },
                            onToggleFavorite = { onToggleFavorite?.invoke(song.id) },
                            trailing = {
                                IconButton(onClick = { onRemoveFromPlaylist(song.id) }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove from playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    } else {
                        SongRow(
                            song = song,
                            isFavorite = song.id in favorites,
                            onClick = { viewModel.playQueue(songs, index) },
                            onLongClick = { },
                            onToggleFavorite = {
                                onToggleFavorite?.invoke(song.id)
                                    ?: viewModel.toggleFavorite(song.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
