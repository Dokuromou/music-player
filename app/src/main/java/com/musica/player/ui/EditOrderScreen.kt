package com.musica.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musica.player.MusicPlayerViewModel
import com.musica.player.data.Song

data class EditOrderRequest(
    val storageKey: String,
    val title: String,
    val songIds: List<Long>,
    val allowRemove: Boolean
)

@Composable
fun EditOrderScreen(
    viewModel: MusicPlayerViewModel,
    request: EditOrderRequest,
    onDone: () -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    var ids by remember { mutableStateOf(request.songIds) }
    val songById = remember(songs) { songs.associateBy { it.id } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Edit order",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!request.storageKey.startsWith("playlist:") && request.storageKey != "favorites") {
                IconButton(
                    onClick = {
                        viewModel.resetOrder(request.storageKey)
                        onDone()
                    }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset to default order")
                }
            }
            TextButton(
                onClick = {
                    viewModel.applyOrder(request.storageKey, ids)
                    onDone()
                }
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        }

        Text(
            text = "Use the arrows to move songs. Changes apply when you tap Save.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.padding(4.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(ids, key = { _, id -> id }) { index, songId ->
                val song = songById[songId] ?: return@itemsIndexed
                EditRow(
                    song = song,
                    index = index,
                    count = ids.size,
                    allowRemove = request.allowRemove,
                    onMoveUp = {
                        if (index > 0) {
                            ids = ids.toMutableList().apply { add(index - 1, removeAt(index)) }
                        }
                    },
                    onMoveDown = {
                        if (index < ids.size - 1) {
                            ids = ids.toMutableList().apply { add(index + 1, removeAt(index)) }
                        }
                    },
                    onRemove = {
                        ids = ids.filterIndexed { i, _ -> i != index }
                    }
                )
            }
        }
    }
}

@Composable
private fun EditRow(
    song: Song,
    index: Int,
    count: Int,
    allowRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtwork(
            artworkUri = song.artworkUri,
            size = 40.dp,
            cornerRadius = 6.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMoveUp, enabled = index > 0) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Move up",
                tint = if (index > 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onMoveDown, enabled = index < count - 1) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Move down",
                tint = if (index < count - 1) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (allowRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
