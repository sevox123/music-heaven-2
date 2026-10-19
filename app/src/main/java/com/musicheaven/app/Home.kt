package com.musicheaven.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.sin

@Composable
fun HomeTab(vm: Vm, openPl: (String) -> Unit, go: (Int, Int) -> Unit, openArtist: (String) -> Unit) {
    val acc = AppTheme.accent
    val recent = remember(vm.songs, Stats.recent) { Stats.recent.mapNotNull { vm.song(it) }.take(15) }
    val top = remember(vm.songs, Stats.ver) { vm.songs.filter { Stats.base(it.id) > 0 }.sortedByDescending { Stats.base(it.id) }.take(10) }
    val artists = remember(vm.songs) { artistsOf(vm.songs).take(10) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Hero(vm, recent) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile("shuffle", Icons.Rounded.Shuffle) { vm.play(vm.songs.shuffled(), 0) }
                Tile("favorites", Icons.Rounded.Favorite) { go(1, 2) }
                Tile("for you", Icons.Rounded.AutoAwesome) { go(3, 0) }
                Tile("stats", Icons.Rounded.BarChart) { go(1, 3) }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("recently played") }
            item { SongCards(vm, recent) }
        }
        if (vm.playlists.isNotEmpty()) {
            item { SectionTitle("your playlists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(vm.playlists) { p -> PlaylistCard(p, p.id in vm.favLists, { openPl(p.id) }, Modifier.width(150.dp)) }
                }
            }
        }
        if (top.isNotEmpty()) {
            item { SectionTitle("most played") }
            item { SongCards(vm, top) }
        }
        if (artists.isNotEmpty()) {
            item { SectionTitle("your artists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(artists) { (name, list) -> ArtistCard(name, list, { openArtist(name) }, Modifier.width(130.dp)) }
                }
            }
        }
        if (recent.isEmpty() && vm.playlists.isEmpty()) {
            item { Empty("nothing here yet.\nopen the playlists tab and make your first one.") }
        }
    }
}

@Composable
fun Tile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(62.dp).glass(22.dp, 0.1f), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mist, modifier = Modifier.size(26.dp))
        }
        Text(label, color = Lilac, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun Hero(vm: Vm, recent: List<Song>) {
    val acc = AppTheme.accent
    val f by rememberInfiniteTransition(label = "hero").animateFloat(0f, 6.2832f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "f")
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greet = when { hour < 5 -> "still awake"; hour < 12 -> "good morning"; hour < 18 -> "good afternoon"; else -> "good evening" }
    val shape = RoundedCornerShape(32.dp)
    val listened = remember(Stats.ver) { Stats.allMs() }
    Box(
        Modifier.padding(16.dp).fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(acc.a.copy(0.55f), acc.b.copy(0.2f), Color.Transparent)))
            .border(1.dp, Color.White.copy(0.16f), shape)
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greet, color = Lilac, fontSize = 13.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "music\nheaven", color = Mist, fontSize = 44.sp, lineHeight = 46.sp, fontFamily = FONTS[AppTheme.titleFont], fontWeight = FontWeight.Light,
                        style = TextStyle(shadow = Shadow(acc.b.copy(0.9f), blurRadius = 36f))
                    )
                }
                Image(
                    painterResource(R.drawable.logo), null,
                    Modifier.size(96.dp).graphicsLayer { translationY = sin(f) * 10f; rotationZ = sin(f) * 4f }.clip(RoundedCornerShape(26.dp))
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chipstat("${vm.songs.size}", "songs", Modifier.weight(1f))
                Chipstat("${vm.playlists.size}", "playlists", Modifier.weight(1f))
                Chipstat(fmtShort(listened), "listened", Modifier.weight(1f))
            }
            val last = recent.firstOrNull()
            if (last != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().glass(22.dp, 0.1f).clickable { vm.play(recent, 0) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Art(last.artUri, CircleShape, acc.a, acc.b, Modifier.size(46.dp), "♪")
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("pick up where you left off", color = Lilac.copy(0.7f), fontSize = 11.sp)
                        Text(last.title, color = Mist, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(
                        Icons.Rounded.PlayArrow, null, tint = Abyss,
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(acc.a, acc.b))).padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Chipstat(big: String, small: String, modifier: Modifier) {
    Column(modifier.glass(18.dp, 0.08f).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, color = Mist, fontSize = 18.sp, fontFamily = FONTS[1])
        Text(small, color = Lilac.copy(0.7f), fontSize = 11.sp)
    }
}

/** Personal picks that learn from what you actually listen to. */
@Composable
fun ForYouTab(vm: Vm) {
    var salt by remember { mutableIntStateOf(0) }
    val r = remember(vm.songs, vm.favSongs, salt) { buildRecs(vm.songs, vm.favSongs, salt) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Header("for you", if (r.learned) "picked from what you really play" else "play a few songs and i'll learn your taste") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("play my mix", Icons.Rounded.PlayArrow, { vm.play(r.mix, 0) }, strong = true)
                Pill("refresh", Icons.Rounded.Refresh, { salt++ })
            }
        }
        if (r.mix.isEmpty()) item { Empty("no songs found yet") }
        else {
            item { SectionTitle("your daily mix") }
            itemsIndexed(r.mix) { i, s -> SongRow(s, s.id == vm.nowId, s.id in vm.favSongs, { vm.toggleFav(s.id) }, { vm.play(r.mix, i) }, onEdit = { vm.editing = s }) }
        }
        if (r.because.isNotEmpty() && r.topArtist != null) {
            item { SectionTitle("because you love ${r.topArtist}") }
            item { SongCards(vm, r.because) }
        }
        if (r.dust.isNotEmpty()) {
            item { SectionTitle("dust these off") }
            item { SongCards(vm, r.dust) }
        }
        if (r.fresh.isNotEmpty()) {
            item { SectionTitle("fresh finds") }
            item { SongCards(vm, r.fresh) }
        }
    }
}
