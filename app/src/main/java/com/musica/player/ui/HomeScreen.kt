package com.musica.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val recentSongs = recentlyPlayed.mapNotNull { id -> songs.find { it.id == id } }
    val albums = rememberSongs(songs) { buildAlbums(it) }
    val artists = rememberSongs(songs) { buildArtists(it) }
    val favoriteSongs = songs.filter { it.id in favorites }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = greeting(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        if (recentSongs.isEmpty() && songs.isEmpty()) {
            EmptyLibrary(hasAnySongs = false)
            return@Column
        }

        if (recentSongs.isNotEmpty()) {
            Text(
                text = "Jump back in",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ShortcutGrid(
                songs = recentSongs.take(4),
                onPlay = { song ->
                    val idx = recentSongs.indexOf(song)
                    viewModel.playQueue(recentSongs, idx)
                }
            )
        }

        if (albums.isNotEmpty()) {
            Shelf(title = "Your albums") {
                items(albums, key = { "album:${it.name}" }) { album ->
                    ShelfCard(
                        artworkUri = album.artworkUri,
                        label = album.name,
                        sublabel = album.artist,
                        onClick = { onOpenAlbum(album.name) }
                    )
                }
            }
        }

        if (favoriteSongs.isNotEmpty()) {
            Shelf(title = "Your favorites") {
                items(favoriteSongs.take(10), key = { "fav:${it.id}" }) { song ->
                    ShelfCard(
                        artworkUri = song.artworkUri,
                        label = song.title,
                        sublabel = song.artist,
                        onClick = {
                            val idx = favoriteSongs.indexOf(song)
                            viewModel.playQueue(favoriteSongs, idx)
                        }
                    )
                }
            }
        }

        if (artists.isNotEmpty()) {
            Shelf(title = "Artists you listen to") {
                items(artists, key = { "artist:${it.name}" }) { artist ->
                    ShelfCard(
                        artworkUri = artist.artworkUri,
                        label = artist.name,
                        sublabel = "${artist.songCount} songs",
                        onClick = { onOpenArtist(artist.name) }
                    )
                }
            }
        }

        if (playlists.isNotEmpty()) {
            Shelf(title = "Your playlists") {
                items(playlists, key = { "pl:${it.id}" }) { playlist ->
                    ShelfCard(
                        artworkUri = playlist.songIds.firstOrNull()
                            ?.let { id -> songs.find { it.id == id } }?.artworkUri ?: "",
                        label = playlist.name,
                        sublabel = "${playlist.songIds.size} songs",
                        onClick = { onOpenPlaylist(playlist.id) }
                    )
                }
            }
        }

        if (recentSongs.size > 4) {
            Shelf(title = "Recently played") {
                items(recentSongs, key = { "recent:${it.id}" }) { song ->
                    ShelfCard(
                        artworkUri = song.artworkUri,
                        label = song.title,
                        sublabel = song.artist,
                        onClick = {
                            val idx = recentSongs.indexOf(song)
                            viewModel.playQueue(recentSongs, idx)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun <T> rememberSongs(songs: List<Song>, build: (List<Song>) -> T): T =
    androidx.compose.runtime.remember(songs) { build(songs) }

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Late night listening"
    }
}

@Composable
private fun ShortcutGrid(songs: List<Song>, onPlay: (Song) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(((songs.size + 1) / 2 * 64).dp),
        userScrollEnabled = false
    ) {
        items(songs, key = { it.id }) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(song) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    artworkUri = song.artworkUri,
                    size = 52.dp,
                    cornerRadius = 6.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Shelf(
    title: String,
    content: LazyListScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ShelfCard(
    artworkUri: String,
    label: String,
    sublabel: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
