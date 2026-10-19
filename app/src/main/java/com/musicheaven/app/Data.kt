package com.musicheaven.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as M
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Song(
    val id: Long, val title: String, val artist: String, val album: String,
    val durationMs: Long, val uri: Uri, val artUri: Uri, val edited: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val desc: String = "",
    val coverPath: String? = null,
    val bgPath: String? = null,
    val accent: Int = 0,
    val shape: Int = 0,
    val dim: Float = 0.55f,
    val font: Int = 0,
    val songIds: List<Long> = emptyList()
)

class Lrc(val t: Long, val text: String)

/** User edits of a song's info. Only stored inside the app, the audio file is never touched. */
class Meta(val title: String?, val artist: String?, val album: String?, val cover: String?)

class Store(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("heaven", Context.MODE_PRIVATE)

    fun playlists(): List<Playlist> = runCatching {
        val a = JSONArray(sp.getString("pl", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i); val s = o.getJSONArray("songs")
            Playlist(
                o.getString("id"), o.getString("name"), o.optString("desc"),
                o.optString("cover").ifEmpty { null }, o.optString("bg").ifEmpty { null },
                o.optInt("accent").coerceIn(0, ACCENTS.size - 1), o.optInt("shape"), o.optDouble("dim", 0.55).toFloat(), o.optInt("font"),
                (0 until s.length()).map { s.getLong(it) }
            )
        }
    }.getOrDefault(emptyList())

    fun savePlaylists(l: List<Playlist>) {
        val a = JSONArray()
        l.forEach { p ->
            a.put(JSONObject().put("id", p.id).put("name", p.name).put("desc", p.desc)
                .put("cover", p.coverPath ?: "").put("bg", p.bgPath ?: "")
                .put("accent", p.accent).put("shape", p.shape).put("dim", p.dim.toDouble()).put("font", p.font)
                .put("songs", JSONArray(p.songIds)))
        }
        sp.edit().putString("pl", a.toString()).apply()
    }

    fun metas(): Map<Long, Meta> = runCatching {
        val o = JSONObject(sp.getString("meta", "{}")!!)
        o.keys().asSequence().associate { k ->
            val m = o.getJSONObject(k)
            k.toLong() to Meta(m.optString("t").ifEmpty { null }, m.optString("a").ifEmpty { null }, m.optString("al").ifEmpty { null }, m.optString("c").ifEmpty { null })
        }
    }.getOrDefault(emptyMap())

    fun saveMetas(m: Map<Long, Meta>) {
        val o = JSONObject()
        m.forEach { (id, x) ->
            o.put(id.toString(), JSONObject().put("t", x.title ?: "").put("a", x.artist ?: "").put("al", x.album ?: "").put("c", x.cover ?: ""))
        }
        sp.edit().putString("meta", o.toString()).apply()
    }

    fun longs(k: String): List<Long> = sp.getString(k, "")!!.split(",").mapNotNull { it.toLongOrNull() }
    fun putLongs(k: String, v: Collection<Long>) = sp.edit().putString(k, v.joinToString(",")).apply()
    fun strs(k: String): List<String> = sp.getString(k, "")!!.split(",").filter { it.isNotBlank() }
    fun putStrs(k: String, v: Collection<String>) = sp.edit().putString(k, v.joinToString(",")).apply()

    /** Copies a picked image into app storage so it stays available without permissions. */
    fun importImage(uri: Uri): String? = runCatching {
        val f = File(ctx.filesDir, "img_${System.currentTimeMillis()}.jpg")
        ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f.absolutePath
    }.getOrNull()

    private fun lrcFile(id: Long) = File(ctx.filesDir, "lyrics").apply { mkdirs() }.resolve("$id.lrc")
    fun saveLyrics(id: Long, uri: Uri) = runCatching {
        val text = ctx.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).readText()
        lrcFile(id).writeText(text)
    }
    fun lyrics(id: Long): List<Lrc> = runCatching { parseLrc(lrcFile(id).readText()) }.getOrDefault(emptyList())
}

fun parseLrc(raw: String): List<Lrc> {
    val tag = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    val meta = Regex("^\\[[a-zA-Z]+:.*]$")
    val out = mutableListOf<Lrc>()
    raw.removePrefix("\uFEFF").lines().forEach { line ->
        val ms = tag.findAll(line).toList()
        val text = tag.replace(line, "").trim()
        if (ms.isEmpty()) {
            if (line.isNotBlank() && !meta.matches(line.trim())) out.add(Lrc(-1, line.trim()))
        } else ms.forEach { m ->
            val f = m.groupValues[3]
            val frac = if (f.isEmpty()) 0 else f.padEnd(3, '0').take(3).toInt()
            out.add(Lrc(m.groupValues[1].toLong() * 60000 + m.groupValues[2].toLong() * 1000 + frac, text))
        }
    }
    return if (out.any { it.t >= 0 }) out.filter { it.t >= 0 }.sortedBy { it.t } else out
}

fun scanSongs(ctx: Context): List<Song> {
    val out = mutableListOf<Song>()
    val proj = arrayOf(M._ID, M.TITLE, M.ARTIST, M.ALBUM, M.DURATION, M.ALBUM_ID)
    ctx.contentResolver.query(M.EXTERNAL_CONTENT_URI, proj, "${M.IS_MUSIC}!=0 AND ${M.DURATION}>30000", null, "${M.TITLE} COLLATE NOCASE")?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            val artist = c.getString(2)?.takeIf { it != "<unknown>" } ?: "Unknown artist"
            out.add(Song(
                id, c.getString(1) ?: "Untitled", artist, c.getString(3) ?: "", c.getLong(4),
                ContentUris.withAppendedId(M.EXTERNAL_CONTENT_URI, id),
                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), c.getLong(5))
            ))
        }
    }
    return out
}
