package com.musicheaven.app

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Background playback, lock screen + notification controls, listening stats, beat analysis. */
class PlayerService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Stats.init(this)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Stats.onItem(mediaItem?.mediaId?.toLongOrNull(), reason, player.isPlaying)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { Stats.onPlaying(isPlaying) }
            override fun onAudioSessionIdChanged(audioSessionId: Int) { Spectrum.setSession(audioSessionId) }
        })
        Spectrum.setSession(player.audioSessionId)
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        Stats.flush()
        Spectrum.stop()
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
