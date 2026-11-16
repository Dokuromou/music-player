package com.musica.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

data class ArtistInfo(
    val name: String,
    val artworkUri: String,
    val songCount: Int
)

fun buildArtists(songs: List<Song>): List<ArtistInfo> =
    songs.groupBy { it.artist }
        .map { (name, artistSongs) ->
            ArtistInfo(
                name = name,
                artworkUri = artistSongs.first().artworkUri,
                songCount = artistSongs.size
            )
        }
        .sortedBy { it.name.lowercase() }

@Composable
fun ArtistsScreen(viewModel: MusicPlayerViewModel, onOpenArtist: (String) -> Unit, modifier: Modifier = Modifier) {
    val songs by viewModel.songs.collectAsState()
    val artists = remember(songs) { buildArtists(songs) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Artists",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (artists.isEmpty()) {
            EmptyLibrary(hasAnySongs = songs.isEmpty())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(artists, key = { it.name }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist.name) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            AsyncImage(
                                model = artist.artworkUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artist.songCount} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
