package com.musicheaven.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class Vm(private val app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private var raw: List<Song> = emptyList()

    var songs by mutableStateOf<List<Song>>(emptyList())
    var metas by mutableStateOf<Map<Long, Meta>>(emptyMap())
    var editing by mutableStateOf<Song?>(null)
    var playlists by mutableStateOf(store.playlists())
    var favSongs by mutableStateOf(store.longs("fs").toSet())
    var favLists by mutableStateOf(store.strs("fl").toSet())
    var ctrl: MediaController? = null
    var nowId by mutableStateOf<Long?>(null)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(0)
    var ctxPl by mutableStateOf<String?>(null)
    var pos by mutableLongStateOf(0L)
    var dur by mutableLongStateOf(1L)
    var lyricsVer by mutableIntStateOf(0)

    init {
        Stats.init(app); AppTheme.init(app)
        metas = store.metas()
    }

    val recent: List<Long> get() = Stats.recent
    val now: Song? get() = nowId?.let { id -> songs.firstOrNull { it.id == id } }
    fun song(id: Long) = songs.firstOrNull { it.id == id }

    fun load() = viewModelScope.launch {
        raw = withContext(Dispatchers.IO) { scanSongs(app) }
        rebuild()
    }

    private fun rebuild() {
        songs = raw.map { s ->
            val m = metas[s.id] ?: return@map s
            s.copy(
                title = m.title ?: s.title, artist = m.artist ?: s.artist, album = m.album ?: s.album,
                artUri = m.cover?.let { Uri.fromFile(File(it)) } ?: s.artUri, edited = true
            )
        }
    }

    fun editSong(id: Long, title: String, artist: String, album: String, cover: String?) {
        val o = raw.firstOrNull { it.id == id } ?: return
        val m = Meta(
            title.trim().takeIf { it.isNotBlank() && it != o.title },
            artist.trim().takeIf { it.isNotBlank() && it != o.artist },
            album.trim().takeIf { it.isNotBlank() && it != o.album },
            cover
        )
        metas = if (m.title == null && m.artist == null && m.album == null && m.cover == null) metas - id else metas + (id to m)
        store.saveMetas(metas); rebuild()
    }

    fun resetSong(id: Long) { metas = metas - id; store.saveMetas(metas); rebuild() }

    fun connect() {
        if (ctrl != null) return
        val f = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlayerService::class.java))).buildAsync()
        f.addListener({
            val c = f.get(); ctrl = c
            c.addListener(object : Player.Listener {
                override fun onMediaItemTransition(m: MediaItem?, r: Int) { nowId = m?.mediaId?.toLongOrNull() }
                override fun onIsPlayingChanged(p: Boolean) { playing = p }
                override fun onShuffleModeEnabledChanged(s: Boolean) { shuffle = s }
                override fun onRepeatModeChanged(r: Int) { repeat = r }
            })
            playing = c.isPlaying; shuffle = c.shuffleModeEnabled; repeat = c.repeatMode
            nowId = c.currentMediaItem?.mediaId?.toLongOrNull()
        }, ContextCompat.getMainExecutor(app))
    }

    fun tick() { ctrl?.let { pos = it.currentPosition; dur = it.duration.takeIf { d -> d > 0 } ?: 1L } }

    fun play(list: List<Song>, index: Int, pl: String? = null) {
        val c = ctrl ?: return
        if (list.isEmpty()) return
        ctxPl = pl
        val items = list.map { s ->
            MediaItem.Builder().setMediaId(s.id.toString()).setUri(s.uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album).setArtworkUri(s.artUri).build())
                .build()
        }
        c.setMediaItems(items, index, 0L); c.prepare(); c.play()
        nowId = list[index].id
    }

    fun toggle() { ctrl?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { ctrl?.seekToNextMediaItem() }
    fun prev() { ctrl?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seek(ms: Long) { ctrl?.seekTo(ms); pos = ms }
    fun toggleShuffle() { ctrl?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        ctrl?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleFav(id: Long) { favSongs = if (id in favSongs) favSongs - id else favSongs + id; store.putLongs("fs", favSongs) }
    fun toggleFavPl(id: String) { favLists = if (id in favLists) favLists - id else favLists + id; store.putStrs("fl", favLists) }

    private fun savePl(l: List<Playlist>) { playlists = l; store.savePlaylists(l) }
    fun newPlaylist(name: String): String {
        val p = Playlist(UUID.randomUUID().toString(), name.trim().ifBlank { "untitled dream" })
        savePl(playlists + p); return p.id
    }
    fun updatePl(p: Playlist) = savePl(playlists.map { if (it.id == p.id) p else it })
    fun deletePl(id: String) { savePl(playlists.filter { it.id != id }); favLists = favLists - id; store.putStrs("fl", favLists) }
    fun importImage(uri: Uri) = store.importImage(uri)

    fun lyrics(id: Long) = store.lyrics(id)
    fun saveLyrics(id: Long, uri: Uri) { store.saveLyrics(id, uri); lyricsVer++ }
}
