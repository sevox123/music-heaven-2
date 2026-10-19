package com.musicheaven.app

import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TAU = 6.2831853f

private class Seed(val x: Float, val y: Float, val k: Int, val s: Float, val ph: Float, val m: Int)

private val SEEDS = Random(11).let { r ->
    List(64) { Seed(r.nextFloat(), r.nextFloat(), 1 + r.nextInt(2), 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * TAU, 2 + r.nextInt(4)) }
}
private val PETAL_COLORS = listOf(Color(0xFFB79CF0), Color(0xFFC8A8F8), Color(0xFFDDB8F5), Color(0xFF9F86E8))

private fun DrawScope.flower(c: Offset, r: Float, rot: Float, col: Color) {
    rotate(rot, c) {
        for (k in 0 until 5) {
            rotate(k * 72f, c) { drawOval(col, Offset(c.x - r * 0.3f, c.y - r), Size(r * 0.6f, r)) }
        }
        drawCircle(Color.White.copy(alpha = 0.7f * col.alpha), r * 0.14f, c)
    }
}

private fun DrawScope.spark(c: Offset, r: Float, col: Color) {
    drawLine(col, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 2f, StrokeCap.Round)
    drawLine(col, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 2f, StrokeCap.Round)
    drawCircle(col, r * 0.35f, c)
}

/** Purple flowers drifting slowly downward. u loops 0..1 seamlessly. */
fun DrawScope.drawPetals(u: Float, n: Int) {
    for (i in 0 until n.coerceIn(0, SEEDS.size)) {
        val p = SEEDS[i]
        val r = size.minDimension * 0.024f * (0.7f + p.s * 0.6f)
        val y = (p.y + u * p.k) % 1f
        val x = p.x + 0.05f * sin(TAU * u * p.m + p.ph)
        val rot = 360f * u * p.k * (if (i % 2 == 0) 1f else -1f) + p.ph * 20f
        flower(Offset(x * size.width, y * (size.height + 2 * r) - r), r, rot, PETAL_COLORS[i % 4].copy(alpha = 0.5f + 0.3f * (p.s - 0.6f)))
    }
}

fun DrawScope.drawAmbient(kind: Int, u: Float, n: Int, c1: Color, c2: Color) {
    val w = size.width; val h = size.height
    for (i in 0 until n.coerceIn(0, SEEDS.size)) {
        val p = SEEDS[(i + 20) % SEEDS.size]
        when (kind) {
            1 -> { // dust
                val y = (((p.y - u * p.k) % 1f) + 1f) % 1f
                val x = p.x + 0.02f * sin(TAU * u * 2 + p.ph)
                drawCircle(Color.White.copy(alpha = 0.25f + 0.25f * sin(TAU * u * 3 + p.ph)), 1.5f + p.s * 1.6f, Offset(x * w, y * h))
            }
            2 -> { // stars
                val a = 0.2f + 0.8f * (0.5f + 0.5f * sin(TAU * u * p.m + p.ph))
                spark(Offset(p.x * w, p.y * h * 0.92f), 2.5f + p.s * 2.5f, c2.copy(alpha = a))
            }
            3 -> { // fireflies
                val c = Offset((p.x + 0.07f * sin(TAU * u * p.m + p.ph)) * w, (p.y + 0.05f * cos(TAU * u * (1 + p.k) + p.ph)) * h)
                val g = 0.35f + 0.65f * (0.5f + 0.5f * sin(TAU * u * p.m * 2 + p.ph))
                val r = 4f + 3f * p.s
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0).copy(alpha = 0.8f * g), Color.Transparent), c, r * 4f), r * 4f, c)
                drawCircle(Color.White.copy(alpha = 0.9f * g), r * 0.6f, c)
            }
            4 -> { // bubbles rising
                val y = 1f - ((p.y + u * p.k) % 1f)
                val r = 6f + 8f * p.s
                val c = Offset((p.x + 0.03f * sin(TAU * u * p.m + p.ph)) * w, y * (h + 2 * r) - r)
                drawCircle(c2.copy(alpha = 0.4f), r, c, style = Stroke(2f))
                drawCircle(Color.White.copy(alpha = 0.55f), r * 0.2f, Offset(c.x - r * 0.35f, c.y - r * 0.35f))
            }
            5 -> { // moonfall: soft falling specks
                val y = (p.y + u * (p.k + 1)) % 1f
                drawCircle(Color.White.copy(alpha = 0.75f), 1.5f + p.s, Offset((p.x + 0.03f * sin(TAU * u * p.m + p.ph)) * w, y * h))
            }
            6 -> { // sparkles
                val s = max(0f, sin(TAU * u * p.m * 2 + p.ph))
                val a = s * s * s
                spark(Offset(p.x * w, p.y * h), 4f + 6f * p.s * 0.6f, lerp(c1, Color.White, 0.5f).copy(alpha = a))
            }
        }
    }
}

fun vizCol(mode: Int, f: Float, a: Color, b: Color): Color = when (mode) {
    1 -> Color.hsv((f * 300f + 200f) % 360f, 0.35f, 1f)
    2 -> Color.White.copy(alpha = 0.85f)
    else -> lerp(a, b, f.coerceIn(0f, 1f))
}

/**
 * Reads the real audio spectrum from the player (needs the microphone/audio permission, nothing is recorded).
 * Without the permission it falls back to a simulated beat so the visuals still move.
 */
object Spectrum {
    const val N = 32
    val bands = FloatArray(N)
    @Volatile var bass = 0f
    private val raw = FloatArray(N)
    @Volatile private var lastData = 0L
    private var viz: Visualizer? = null
    private var session = 0

    fun setSession(id: Int) { session = id; start() }

    fun start() {
        stop()
        if (session == 0) return
        try {
            val v = Visualizer(session)
            v.enabled = false
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) { if (fft != null) process(fft) }
            }, Visualizer.getMaxCaptureRate(), false, true)
            v.enabled = true
            viz = v
        } catch (e: Exception) { viz = null }
    }

    fun stop() {
        try { viz?.enabled = false; viz?.release() } catch (e: Exception) { }
        viz = null
    }

    private fun process(fft: ByteArray) {
        val bins = fft.size / 2
        if (bins < 8) return
        for (b in 0 until N) {
            val lo = Math.pow(bins.toDouble(), b.toDouble() / N).toInt().coerceIn(1, bins - 1)
            val hi = Math.pow(bins.toDouble(), (b + 1).toDouble() / N).toInt().coerceIn(lo + 1, bins)
            var sum = 0f
            for (k in lo until hi) {
                val re = fft[2 * k].toInt(); val im = fft[2 * k + 1].toInt()
                sum += sqrt((re * re + im * im).toFloat())
            }
            raw[b] = sqrt(((sum / (hi - lo)) / 110f).coerceIn(0f, 1f))
        }
        lastData = SystemClock.elapsedRealtime()
    }

    private fun sim(t: Float, i: Int): Float {
        val beat = max(0f, sin(TAU * 2f * t)).let { it * it * it * it }
        return if (i < 6) 0.2f + 0.75f * beat
        else (0.12f + 0.4f * abs(sin(t * (1.7f + i * 0.21f) + i * 1.3f))) * (0.6f + 0.4f * beat)
    }

    /** Call once per frame while a visualizer is shown. */
    fun frame(t: Float, playing: Boolean, sens: Float) {
        val live = SystemClock.elapsedRealtime() - lastData < 400
        for (i in 0 until N) {
            val target = if (!playing) 0f else (if (live) raw[i] else sim(t, i)) * sens
            val cur = bands[i]
            bands[i] = (if (target > cur) cur + (target - cur) * 0.6f else cur * 0.9f).coerceIn(0f, 1f)
        }
        bass = (bands[0] + bands[1] + bands[2] + bands[3]) / 4f
    }
}
