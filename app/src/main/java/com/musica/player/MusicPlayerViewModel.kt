package com.musica.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musica.player.data.LocalStore
import com.musica.player.data.MediaStoreRepository
import com.musica.player.data.Playlist
import com.musica.player.data.Song
import com.musica.player.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionJob: Job? = null
    private var sleepJob: Job? = null
    private var appContext: Context? = null
    private var songsById: Map<Long, Song> = emptyMap()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Long>>(emptyList())
    val recentlyPlayed: StateFlow<List<Long>> = _recentlyPlayed.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _amoled = MutableStateFlow(false)
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (controllerFuture != null) return
        val appContext = appContext ?: return

        _favorites.value = LocalStore.loadFavorites(appContext)
        _recentlyPlayed.value = LocalStore.loadRecentlyPlayed(appContext)
        _playlists.value = LocalStore.loadPlaylists(appContext)
        _amoled.value = LocalStore.loadAmoled(appContext)

        val sessionToken = SessionToken(
            context, ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                syncFromPlayer()
                startPositionUpdates()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to playback service", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSong()
            rebuildQueue()
            recordRecentlyPlayed()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _durationMs.value = controller?.duration?.takeIf { it > 0 } ?: 0L
            updateCurrentSong()
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _shuffleEnabled.value = enabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            rebuildQueue()
        }
    }

    fun loadSongs(context: Context) {
        viewModelScope.launch {
            val loaded = MediaStoreRepository.loadSongs(context)
            _songs.value = loaded
            songsById = loaded.associateBy { it.id }
            rebuildQueue()
            updateCurrentSong()
        }
    }

    fun playQueue(queue: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (queue.isEmpty()) return
        c.setMediaItems(queue.map { it.toMediaItem() }, startIndex.coerceIn(0, queue.size - 1), 0L)
        c.prepare()
        c.play()
        rebuildQueue()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun seekToNext() {
        controller?.seekToNextMediaItem()
    }

    fun seekToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun jumpToQueueIndex(index: Int) {
        controller?.seekTo(index, 0L)
        controller?.play()
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
            rebuildQueue()
        }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        controller?.setPlaybackSpeed(clamped)
        _playbackSpeed.value = clamped
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0) {
                _sleepRemainingMs.value = remaining
                delay(1_000L)
                remaining -= 1_000L
            }
            _sleepRemainingMs.value = null
            controller?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepRemainingMs.value = null
    }

    fun toggleFavorite(songId: Long) {
        val updated = _favorites.value.toMutableSet()
        if (!updated.add(songId)) updated.remove(songId)
        _favorites.value = updated
        appContext?.let { LocalStore.saveFavorites(it, updated) }
    }

    fun createPlaylist(name: String): Long {
        val id = System.currentTimeMillis()
        _playlists.value = _playlists.value + Playlist(id, name.trim(), emptyList())
        appContext?.let { LocalStore.savePlaylists(it, _playlists.value) }
        return id
    }

    fun deletePlaylist(playlistId: Long) {
        _playlists.value = _playlists.value.filter { it.id != playlistId }
        appContext?.let { LocalStore.savePlaylists(it, _playlists.value) }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId && songId !in playlist.songIds) {
                playlist.copy(songIds = playlist.songIds + songId)
            } else {
                playlist
            }
        }
        appContext?.let { LocalStore.savePlaylists(it, _playlists.value) }
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds - songId)
            } else {
                playlist
            }
        }
        appContext?.let { LocalStore.savePlaylists(it, _playlists.value) }
    }

    fun setAmoled(enabled: Boolean) {
        _amoled.value = enabled
        appContext?.let { LocalStore.saveAmoled(it, enabled) }
    }

    fun recentlyPlayedSongs(): List<Song> =
        _recentlyPlayed.value.mapNotNull { songsById[it] }

    fun refreshPosition() {
        val c = controller ?: return
        if (c.isPlaying) {
            _positionMs.value = c.currentPosition.coerceAtLeast(0L)
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                refreshPosition()
                delay(500)
            }
        }
    }

    private fun rebuildQueue() {
        val c = controller ?: return
        _currentIndex.value = c.currentMediaItemIndex.coerceAtLeast(0)
        _queue.value = (0 until c.mediaItemCount).mapNotNull { i ->
            c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { songsById[it] }
        }
    }

    private fun recordRecentlyPlayed() {
        val id = controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val updated = listOf(id) + _recentlyPlayed.value.filter { it != id }
        _recentlyPlayed.value = updated.take(LocalStore.RECENT_LIMIT)
        appContext?.let { LocalStore.saveRecentlyPlayed(it, updated) }
    }

    private fun updateCurrentSong() {
        val c = controller ?: return
        val mediaId = c.currentMediaItem?.mediaId
        _currentSong.value = mediaId?.toLongOrNull()?.let { songsById[it] }
        _currentIndex.value = c.currentMediaItemIndex.coerceAtLeast(0)
    }

    private fun syncFromPlayer() {
        val c = controller ?: return
        _isPlaying.value = c.isPlaying
        _shuffleEnabled.value = c.shuffleModeEnabled
        _repeatMode.value = c.repeatMode
        _playbackSpeed.value = c.playbackParameters.speed
        _durationMs.value = c.duration.takeIf { it > 0 } ?: 0L
        _positionMs.value = c.currentPosition.coerceAtLeast(0L)
        updateCurrentSong()
        rebuildQueue()
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    companion object {
        private const val TAG = "MusicPlayerViewModel"
    }
}
