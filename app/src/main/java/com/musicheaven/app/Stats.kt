package com.musicheaven.app

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import kotlin.math.ln
import kotlin.random.Random

/**
 * Exact listening time per song. Driven by the player service, so it keeps counting
 * even when the app screen is closed. Also remembers plays, skips and recently played.
 */
object Stats {
    private var sp: SharedPreferences? = null
    private val playMs = HashMap<Long, Long>()
    private val plays = HashMap<Long, Int>()
    private val skips = HashMap<Long, Int>()
    var recent by mutableStateOf<List<Long>>(emptyList())
    var ver by mutableIntStateOf(0)

    private var cur: Long? = null
    private var startedAt = 0L
    private var session = 0L
    private var counted = false

    private fun now() = SystemClock.elapsedRealtime()

    fun init(ctx: Context) {
        if (sp != null) return
        val s = ctx.getSharedPreferences("stats", Context.MODE_PRIVATE); sp = s
        parse(s.getString("pt", ""), playMs) { it.toLongOrNull() }
        parse(s.getString("pc", ""), plays) { it.toIntOrNull() }
        parse(s.getString("sk", ""), skips) { it.toIntOrNull() }
        recent = (s.getString("rc", "") ?: "").split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun <T> parse(raw: String?, into: HashMap<Long, T>, conv: (String) -> T?) {
        raw.orEmpty().split(",").forEach { e ->
            val p = e.split(":")
            if (p.size == 2) { val k = p[0].toLongOrNull(); val v = conv(p[1]); if (k != null && v != null) into[k] = v }
        }
    }

    private fun ser(m: Map<Long, *>) = m.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun save() {
        sp?.edit()?.putString("pt", ser(playMs))?.putString("pc", ser(plays))?.putString("sk", ser(skips))
            ?.putString("rc", recent.joinToString(","))?.apply()
        ver++
    }

    /** Committed time only (without the currently running slice). */
    fun base(id: Long): Long = playMs[id] ?: 0L
    fun plays(id: Long): Int = plays[id] ?: 0
    fun skipsOf(id: Long): Int = skips[id] ?: 0
    fun allMs(): Long = playMs.values.sum() + (if (startedAt > 0) now() - startedAt else 0L)
    fun allPlays(): Int = plays.values.sum()

    /** Exact time including the slice that is playing right now. */
    fun total(id: Long): Long = base(id) + (if (id == cur && startedAt > 0) now() - startedAt else 0L)

    fun onPlaying(p: Boolean) { if (p) { if (startedAt == 0L) startedAt = now() } else commit() }

    private fun commit() {
        val id = cur
        if (startedAt > 0 && id != null) {
            val d = now() - startedAt
            startedAt = 0
            playMs[id] = (playMs[id] ?: 0L) + d
            session += d
            if (!counted && session >= 30_000) { counted = true; plays[id] = (plays[id] ?: 0) + 1 }
            save()
        } else startedAt = 0
    }

    fun onItem(id: Long?, reason: Int, playing: Boolean) {
        val prev = cur
        commit()
        if (prev != null && prev != id && reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && session < 15_000) {
            skips[prev] = (skips[prev] ?: 0) + 1
        }
        cur = id; session = 0; counted = false
        if (id != null) recent = (listOf(id) + recent.filter { it != id }).take(30)
        if (playing) startedAt = now()
        save()
    }

    fun flush() { commit() }
    fun clearRecent() { recent = emptyList(); save() }
    fun resetAll() { playMs.clear(); plays.clear(); skips.clear(); startedAt = if (startedAt > 0) now() else 0; save() }
}

fun fmtLong(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, sec) else if (m > 0) "%dm %02ds".format(m, sec) else "${sec}s"
}

fun fmtShort(ms: Long): String { val m = ms / 60000; return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m" }

class Recs(val mix: List<Song>, val topArtist: String?, val because: List<Song>, val dust: List<Song>, val fresh: List<Song>, val learned: Boolean)

/**
 * Taste model: every song earns a weight from listening time, completed plays, favorites and skips.
 * Artists and albums inherit the weights of their songs, and each song is then scored by
 * how much you like its artist/album, a bonus for never-played songs, a penalty for skips and
 * for what you just heard, plus a small daily shuffle so the list feels fresh.
 */
fun buildRecs(songs: List<Song>, favs: Set<Long>, salt: Int = 0): Recs {
    if (songs.isEmpty()) return Recs(emptyList(), null, emptyList(), emptyList(), emptyList(), false)
    val recentTop = Stats.recent.take(8).toSet()
    val recent20 = Stats.recent.take(20).toSet()
    fun w(s: Song): Double = Stats.base(s.id) / 60000.0 + 2.0 * Stats.plays(s.id) + (if (s.id in favs) 3.0 else 0.0) - 1.5 * Stats.skipsOf(s.id)
    val artistAff = HashMap<String, Double>(); val albumAff = HashMap<String, Double>()
    songs.forEach { s ->
        val x = w(s).coerceAtLeast(0.0)
        artistAff[s.artist] = (artistAff[s.artist] ?: 0.0) + x
        if (s.album.isNotBlank()) albumAff[s.album] = (albumAff[s.album] ?: 0.0) + x
    }
    val maxA = (artistAff.values.maxOrNull() ?: 0.0).coerceAtLeast(1e-6)
    val maxAl = (albumAff.values.maxOrNull() ?: 0.0).coerceAtLeast(1e-6)
    val learned = songs.sumOf { Stats.base(it.id) } > 60_000L
    val day = System.currentTimeMillis() / 86_400_000L
    fun score(s: Song): Double {
        var v = 0.55 * (artistAff[s.artist] ?: 0.0) / maxA + 0.15 * (albumAff[s.album] ?: 0.0) / maxAl
        if (s.id in favs) v += 0.15
        if (Stats.plays(s.id) == 0 && Stats.base(s.id) < 10_000) v += 0.2
        v -= 0.2 * minOf(Stats.skipsOf(s.id), 3) / 3.0
        if (s.id in recentTop) v -= 0.5
        return v + Random(day * 31 + s.id + salt * 7919L).nextDouble() * 0.12
    }
    val ranked = songs.sortedByDescending { score(it) }
    val top = if (learned) artistAff.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key else null
    val because = if (top == null) emptyList() else songs.filter { it.artist == top }.sortedBy { w(it) }.take(12)
    val dust = songs.filter { (Stats.plays(it.id) > 0 || it.id in favs) && it.id !in recent20 }.sortedByDescending { w(it) }.take(12)
    val fresh = ranked.filter { Stats.plays(it.id) == 0 }.take(12)
    return Recs(ranked.take(15), top, because, dust, fresh, learned)
}
