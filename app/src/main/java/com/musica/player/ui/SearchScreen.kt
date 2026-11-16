package com.musica.player.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musica.player.MusicPlayerViewModel

@Composable
fun SearchScreen(
    viewModel: MusicPlayerViewModel,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    var query by remember { mutableStateOf("") }

    val trimmedQuery = query.trim()
    val matchedSongs = remember(songs, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(trimmedQuery, ignoreCase = true)
        }.take(8)
    }
    val matchedAlbums = remember(songs, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else buildAlbums(songs).filter { it.name.contains(trimmedQuery, ignoreCase = true) }
    }
    val matchedArtists = remember(songs, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else buildArtists(songs).filter { it.name.contains(trimmedQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Songs, albums, artists") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { viewModel.addRecentSearch(query) }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (trimmedQuery.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recent searches",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                Text("Clear")
                            }
                        }
                    }
                    items(recentSearches.size) { index ->
                        val term = recentSearches[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = term }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = term, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Text(
                        text = "Browse all",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    BrowseAlbumGrid(
                        viewModel = viewModel,
                        onOpenAlbum = onOpenAlbum
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (matchedSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(matchedSongs.size) { index ->
                        val song = matchedSongs[index]
                        SongRow(
                            song = song,
                            isFavorite = song.id in favorites,
                            onClick = { viewModel.playQueue(matchedSongs, index) },
                            onLongClick = { },
                            onToggleFavorite = { viewModel.toggleFavorite(song.id) }
                        )
                    }
                }
                if (matchedAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(matchedAlbums, key = { "salbum:${it.name}" }) { album ->
                        ResultRow(
                            artworkUri = album.artworkUri,
                            title = album.name,
                            subtitle = "Album • ${album.artist}",
                            icon = Icons.Filled.Album,
                            onClick = { onOpenAlbum(album.name) }
                        )
                    }
                }
                if (matchedArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(matchedArtists, key = { "sartist:${it.name}" }) { artist ->
                        ResultRow(
                            artworkUri = artist.artworkUri,
                            title = artist.name,
                            subtitle = "Artist",
                            icon = Icons.Filled.Person,
                            onClick = { onOpenArtist(artist.name) }
                        )
                    }
                }
                if (matchedSongs.isEmpty() && matchedAlbums.isEmpty() && matchedArtists.isEmpty()) {
                    item {
                        Text(
                            text = "No results for \"$trimmedQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseAlbumGrid(
    viewModel: MusicPlayerViewModel,
    onOpenAlbum: (String) -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    val albums = remember(songs) { buildAlbums(songs) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height((albums.size.coerceAtMost(6) / 2 * 180).dp),
        userScrollEnabled = false
    ) {
        items(albums.take(6), key = { "browse:${it.name}" }) { album ->
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onOpenAlbum(album.name) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    artworkUri: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
