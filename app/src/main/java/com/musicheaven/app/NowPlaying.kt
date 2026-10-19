package com.musicheaven.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NowPlaying(vm: Vm, onClose: () -> Unit) {
    val s = vm.now ?: return
    BackHandler(onBack = onClose)
    val pl = vm.playlists.firstOrNull { it.id == vm.ctxPl }
    val acc = pl?.let { ACCENTS[it.accent] } ?: AppTheme.accent
    val shape = pl?.shape ?: 0
    var lyrics by remember { mutableStateOf(false) }

    // per-frame loop that feeds the beat visualizer and cover bounce
    val frame = remember { mutableLongStateOf(0L) }
    val playingNow by rememberUpdatedState(vm.playing)
    val needFx = AppTheme.viz != 0 || AppTheme.bounce
    LaunchedEffect(needFx) {
        if (needFx) while (true) {
            val n = withFrameNanos { it }
            Spectrum.frame(n / 1e9f, playingNow, AppTheme.vizSens)
            frame.longValue = n
        }
    }

    // slow spin for the orb cover, gentle sway for other shapes
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vm.playing) {
        if (vm.playing) {
            var last = withFrameNanos { it }
            while (true) { val n = withFrameNanos { it }; angle = (angle + (n - last) / 1e9f * 14f) % 360f; last = n }
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val p1 by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "p1")
    val p2 by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, delayMillis = 1800, easing = LinearEasing)), label = "p2")
    val glow by animateFloatAsState(if (vm.playing) 1f else 0.35f, tween(800), label = "glow")

    Box(Modifier.fillMaxSize()) {
        Aurora(acc.a, acc.b, pl?.bgPath, pl?.dim ?: 0.5f)
        if (AppTheme.viz == 1 || AppTheme.viz == 3 || AppTheme.viz == 5) Canvas(Modifier.fillMaxSize()) {
            frame.longValue
            val B = Spectrum.bands; val n = Spectrum.N; val w = size.width; val h = size.height
            when (AppTheme.viz) {
                1 -> {
                    val bw = w / n
                    for (i in 0 until n) {
                        val bh = B[i] * h * 0.28f + 4f
                        drawRoundRect(
                            vizCol(AppTheme.vizColor, i / n.toFloat(), acc.a, acc.b).copy(alpha = 0.6f),
                            Offset(i * bw + bw * 0.18f, h - bh), Size(bw * 0.64f, bh), CornerRadius(bw * 0.32f)
                        )
                    }
                }
                3 -> {
                    val mid = h * 0.86f; val amp = h * 0.10f
                    val path = Path().apply {
                        moveTo(0f, mid)
                        for (i in 0 until n) lineTo(w * i / (n - 1), mid - B[i] * amp)
                        lineTo(w, mid)
                        for (i in n - 1 downTo 0) lineTo(w * i / (n - 1), mid + B[i] * amp)
                        close()
                    }
                    drawPath(path, Brush.horizontalGradient(listOf(acc.a.copy(alpha = 0.55f), acc.b.copy(alpha = 0.55f))))
                }
                else -> {
                    val c = Offset(w / 2, h * 0.62f); val r = w * (0.7f + 0.6f * Spectrum.bass)
                    drawCircle(Brush.radialGradient(listOf(acc.b.copy(alpha = 0.15f + 0.45f * Spectrum.bass), Color.Transparent), c, r), r, c)
                }
            }
        }
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Mist, modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClose))
                Text(pl?.name ?: "now playing", Modifier.weight(1f), color = Lilac, textAlign = TextAlign.Center, fontFamily = FONTS[pl?.font ?: AppTheme.titleFont], fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Rounded.Edit, null, tint = Mist, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { vm.editing = s }.padding(6.dp))
                Icon(Icons.Rounded.Subject, null, tint = if (lyrics) acc.b else Mist, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { lyrics = !lyrics }.padding(4.dp))
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    frame.longValue
                    val c = center; val base = size.minDimension * 0.36f
                    val B = Spectrum.bands; val bass = Spectrum.bass
                    drawCircle(Brush.radialGradient(listOf(acc.b.copy(alpha = 0.45f * glow + 0.3f * bass), Color.Transparent), c, size.minDimension * 0.62f), size.minDimension * 0.62f, c)
                    if (AppTheme.viz == 2) for (i in 0 until 48) {
                        val ang = i / 48f * 6.2832f - 1.5708f
                        val k = ((if (i < 24) i else 47 - i) * (Spectrum.N - 1)) / 23
                        val d = Offset(cos(ang), sin(ang)); val len = base * 0.06f + B[k] * base * 0.5f
                        drawLine(vizCol(AppTheme.vizColor, i / 48f, acc.a, acc.b), c + d * (base * 1.12f), c + d * (base * 1.12f + len), 5f, StrokeCap.Round)
                    }
                    if (AppTheme.viz == 4) for (j in 0..2) {
                        drawCircle(vizCol(AppTheme.vizColor, j / 3f, acc.a, acc.b).copy(alpha = 0.6f - j * 0.15f), base * (1.15f + j * 0.2f + bass * 0.4f), c, style = Stroke(3f))
                    }
                    for (p in listOf(p1, p2)) if (vm.playing) drawCircle(acc.b.copy(alpha = 0.5f * (1f - p)), base * (1f + 0.55f * p), c, style = Stroke(2f))
                    val a = Math.toRadians((angle * 2.5f).toDouble()).toFloat()
                    for (i in 0..2) {
                        val ang = a + i * 2.094f; val r = base * 1.28f
                        drawCircle(Color.White.copy(alpha = 0.7f * glow), 5f - i, Offset(c.x + r * cos(ang), c.y + r * sin(ang) * 0.95f))
                    }
                }
                Crossfade(if (lyrics) -1L else s.id, animationSpec = tween(450), label = "cover") { k ->
                    if (k == -1L) {
                        Box(Modifier.fillMaxSize().padding(4.dp).glass(28.dp, 0.07f).padding(16.dp)) { LyricsView(vm, s, acc.b) }
                    } else {
                        val rot = if (shape == 0) angle else sin(Math.toRadians(angle * 3.0)).toFloat() * 3f
                        Art(
                            s.artUri, coverShape(shape), acc.a, acc.b,
                            Modifier.fillMaxSize(0.72f)
                                .graphicsLayer {
                                    frame.longValue
                                    val sc = if (AppTheme.bounce) 1f + 0.08f * Spectrum.bass else 1f
                                    scaleX = sc; scaleY = sc
                                }
                                .rotate(rot).clickable { lyrics = true },
                            "♪"
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            AnimatedContent(s.id, transitionSpec = { (fadeIn(tween(500)) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(250)) }, label = "title") { _ ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        s.title, color = Mist, fontSize = 26.sp, fontFamily = FONTS[pl?.font ?: AppTheme.titleFont], fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = Shadow(acc.b.copy(0.9f), blurRadius = 28f))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(s.artist, color = Lilac.copy(0.8f), fontSize = 14.sp, maxLines = 1)
                    val listened = remember(s.id, Stats.ver) { Stats.base(s.id) }
                    Text("${Stats.plays(s.id)} plays  ·  ${fmtLong(listened)} listened", color = Lilac.copy(0.5f), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            WaveSeek(vm.pos, vm.dur, vm.playing, acc.b) { vm.seek(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(vm.pos), color = Lilac.copy(0.7f), fontSize = 12.sp)
                Text(fmt(vm.dur), color = Lilac.copy(0.7f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CtrlIcon(Icons.Rounded.Shuffle, if (vm.shuffle) acc.b else Lilac.copy(0.6f)) { vm.toggleShuffle() }
                CtrlIcon(Icons.Rounded.SkipPrevious, Mist, 38.dp) { vm.prev() }
                PlayOrb(vm.playing, acc) { vm.toggle() }
                CtrlIcon(Icons.Rounded.SkipNext, Mist, 38.dp) { vm.next() }
                CtrlIcon(if (vm.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, if (vm.repeat != 0) acc.b else Lilac.copy(0.6f)) { vm.cycleRepeat() }
            }
            Spacer(Modifier.height(14.dp))
            val fav = s.id in vm.favSongs
            val heart by animateFloatAsState(if (fav) 1.25f else 1f, spring(Spring.DampingRatioHighBouncy), label = "heart")
            Icon(
                if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (fav) Rose else Lilac.copy(0.7f),
                modifier = Modifier.size(32.dp).scale(heart).clip(CircleShape).clickable { vm.toggleFav(s.id) }
            )
        }
    }
}

@Composable
fun CtrlIcon(icon: ImageVector, tint: Color, size: Dp = 26.dp, onClick: () -> Unit) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(size + 14.dp).clip(CircleShape).clickable(onClick = onClick).padding(7.dp))
}

@Composable
fun PlayOrb(playing: Boolean, acc: Accent, onClick: () -> Unit) {
    val sc by animateFloatAsState(if (playing) 1f else 0.92f, spring(Spring.DampingRatioMediumBouncy), label = "orb")
    Box(
        Modifier.size(78.dp).scale(sc).clip(CircleShape)
            .background(Brush.linearGradient(listOf(acc.a, acc.b)))
            .border(1.5.dp, Color.White.copy(0.5f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(playing, label = "pp") { p -> Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Abyss, modifier = Modifier.size(38.dp)) }
    }
}

/** Seek bar drawn as a wave that ripples while music plays. */
@Composable
fun WaveSeek(pos: Long, dur: Long, playing: Boolean, color: Color, onSeek: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val ph by rememberInfiniteTransition(label = "w").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "p")
    val amp by animateFloatAsState(if (playing) 1f else 0.15f, tween(600), label = "amp")
    val frac = drag ?: (pos.toFloat() / dur).coerceIn(0f, 1f)
    Canvas(
        Modifier.fillMaxWidth().height(48.dp)
            .pointerInput(dur) { detectTapGestures { onSeek((it.x / size.width * dur).toLong()) } }
            .pointerInput(dur) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { drag?.let { f -> onSeek((f * dur).toLong()) }; drag = null },
                    onDragCancel = { drag = null }
                ) { c, _ -> drag = (c.position.x / size.width).coerceIn(0f, 1f) }
            }
    ) {
        val w = size.width; val h = size.height; val x = w * frac
        fun y(px: Float, a: Float) = h / 2 + a * sin(px / 22f - ph)
        fun wave(from: Float, to: Float, a: Float) = Path().apply {
            var px = from; moveTo(px, y(px, a))
            while (px < to) { px += 3f; lineTo(px, y(px, a)) }
        }
        drawPath(wave(0f, x, 10f * amp), color, style = Stroke(5f, cap = StrokeCap.Round))
        drawPath(wave(x, w, 3f), Color.White.copy(0.22f), style = Stroke(4f, cap = StrokeCap.Round))
        val ty = y(x, 10f * amp)
        drawCircle(color.copy(0.35f), 20f, Offset(x, ty)); drawCircle(Color.White, 9f, Offset(x, ty))
    }
}

@Composable
fun LyricsView(vm: Vm, s: Song, accent: Color) {
    val lines = remember(s.id, vm.lyricsVer) { vm.lyrics(s.id) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) vm.saveLyrics(s.id, u) }
    if (lines.isEmpty()) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("no lyrics for this song yet", color = Lilac, fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))
            Pill("add .lrc or .txt", Icons.Rounded.Add, { pick.launch(arrayOf("*/*")) }, strong = true)
        }
        return
    }
    val timed = lines.first().t >= 0
    val idx = if (timed) lines.indexOfLast { it.t <= vm.pos }.coerceAtLeast(0) else -1
    val st = rememberLazyListState()
    LaunchedEffect(idx) { if (idx >= 0) st.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = st, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(lines) { i, l ->
                val on = i == idx
                val a by animateFloatAsState(if (!timed || on) 1f else 0.38f, tween(400), label = "la")
                val sz by animateFloatAsState(if (on) 22f else 17f, tween(400), label = "ls")
                Text(l.text.ifBlank { "·" }, color = (if (on) accent else Mist).copy(alpha = a), fontSize = sz.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp))
            }
        }
        Icon(Icons.Rounded.Edit, null, tint = Lilac.copy(0.7f), modifier = Modifier.align(Alignment.TopEnd).size(30.dp).clip(CircleShape).clickable { pick.launch(arrayOf("*/*")) }.padding(5.dp))
    }
}
