package com.musica.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musica.player.MusicPlayerViewModel
import com.musica.player.data.Song

data class AlbumInfo(
    val name: String,
    val artist: String,
    val artworkUri: String,
    val songCount: Int
)

fun buildAlbums(songs: List<Song>): List<AlbumInfo> =
    songs.groupBy { it.album }
        .map { (name, albumSongs) ->
            AlbumInfo(
                name = name,
                artist = albumSongs.first().artist,
                artworkUri = albumSongs.first().artworkUri,
                songCount = albumSongs.size
            )
        }
        .sortedBy { it.name.lowercase() }

@Composable
fun AlbumsScreen(viewModel: MusicPlayerViewModel, onOpenAlbum: (String) -> Unit, modifier: Modifier = Modifier) {
    val songs by viewModel.songs.collectAsState()
    val albums = remember(songs) { buildAlbums(songs) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Albums",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (albums.isEmpty()) {
            EmptyLibrary(hasAnySongs = songs.isEmpty())
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 8.dp
                )
            ) {
                items(albums, key = { it.name }) { album ->
                    AlbumCard(album = album, onClick = { onOpenAlbum(album.name) })
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = album.artworkUri,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = Icons.Filled.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
