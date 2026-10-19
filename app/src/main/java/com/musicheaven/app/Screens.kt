package com.musicheaven.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

fun artistsOf(songs: List<Song>): List<Pair<String, List<Song>>> =
    songs.groupBy { it.artist }.toList().sortedByDescending { it.second.size }

@Composable
fun Header(title: String, sub: String? = null) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
            title, color = Mist, fontSize = 34.sp, fontFamily = FONTS[AppTheme.titleFont], fontWeight = FontWeight.Light,
            style = TextStyle(shadow = Shadow(Lilac.copy(0.8f), blurRadius = 30f))
        )
        if (sub != null) Text(sub, color = Lilac.copy(0.75f), fontSize = 14.sp)
    }
}

@Composable
fun SectionTitle(t: String) =
    Text(t, color = Lilac, fontSize = 18.sp, fontFamily = FONTS[1], modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))

@Composable
fun Empty(t: String) = Text(t, color = Lilac.copy(0.7f), modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)

@Composable
fun HField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value, onChange, modifier, label = { Text(label) }, singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Mist, unfocusedTextColor = Mist, focusedBorderColor = Lilac, unfocusedBorderColor = Lilac.copy(0.4f),
            focusedLabelColor = Lilac, unfocusedLabelColor = Lilac.copy(0.7f), cursorColor = Lilac
        )
    )
}

@Composable
fun SongRow(
    s: Song, active: Boolean, fav: Boolean, onFav: () -> Unit, onClick: () -> Unit,
    onEdit: (() -> Unit)? = null, extra: @Composable () -> Unit = {}
) {
    val acc = AppTheme.accent
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
            .glass(18.dp, if (active) 0.16f else 0.05f).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Art(s.artUri, RoundedCornerShape(12.dp), acc.a, acc.b, Modifier.size(48.dp), "♪")
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, color = if (active) Rose else Mist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text("${s.artist}  ·  ${fmt(s.durationMs)}", color = Lilac.copy(0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (fav) Rose else Lilac.copy(0.55f),
            modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onFav).padding(7.dp)
        )
        if (onEdit != null) Icon(
            Icons.Rounded.Edit, null, tint = Lilac.copy(0.5f),
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onEdit).padding(7.dp)
        )
        extra()
    }
}

@Composable
fun SongCards(vm: Vm, list: List<Song>) {
    val acc = AppTheme.accent
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(list) { i, s ->
            Column(Modifier.width(120.dp).clickable { vm.play(list, i) }) {
                Art(s.artUri, coverShape(2), acc.a, acc.b, Modifier.size(120.dp), "♪")
                Text(s.title, color = Mist, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Text(s.artist, color = Lilac.copy(0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PlaylistCard(p: Playlist, fav: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val acc = ACCENTS[p.accent]
    Column(modifier.clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(0.88f).aspectRatio(1f)) {
            Art(p.coverPath?.let { File(it) }, coverShape(p.shape), acc.a, acc.b, Modifier.fillMaxSize())
            if (fav) Icon(Icons.Rounded.Favorite, null, tint = Rose, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(p.name, color = Mist, fontFamily = FONTS[p.font], fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${p.songIds.size} songs", color = Lilac.copy(0.7f), fontSize = 12.sp)
    }
}

@Composable
fun ArtistCard(name: String, list: List<Song>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val acc = AppTheme.accent
    Column(modifier.clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Art(list.first().artUri, CircleShape, acc.a, acc.b, Modifier.fillMaxWidth(0.8f).aspectRatio(1f), "♪")
        Spacer(Modifier.height(8.dp))
        Text(name, color = Mist, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${list.size} songs", color = Lilac.copy(0.7f), fontSize = 12.sp)
    }
}

// ───────────────────────── library ─────────────────────────

@Composable
fun LibraryTab(vm: Vm, seg: Int, setSeg: (Int) -> Unit, openPl: (String) -> Unit, openArtist: (String) -> Unit) {
    Column {
        Header("library", "${vm.songs.size} songs, all offline")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("songs", "artists", "favorites", "stats").forEachIndexed { i, n -> Chip(n, seg == i) { setSeg(i) } }
        }
        Spacer(Modifier.height(10.dp))
        when (seg) {
            0 -> SongsList(vm)
            1 -> ArtistsGrid(vm, openArtist)
            2 -> FavoritesView(vm, openPl)
            else -> StatsView(vm)
        }
    }
}

@Composable
fun SongsList(vm: Vm) {
    var q by remember { mutableStateOf("") }
    val list = vm.songs.filter { q.isBlank() || it.title.contains(q, true) || it.artist.contains(q, true) }
    HField(q, { q = it }, "search", Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    Spacer(Modifier.height(8.dp))
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(list) { i, s -> SongRow(s, s.id == vm.nowId, s.id in vm.favSongs, { vm.toggleFav(s.id) }, { vm.play(list, i) }, onEdit = { vm.editing = s }) }
    }
}

@Composable
fun ArtistsGrid(vm: Vm, openArtist: (String) -> Unit) {
    val artists = remember(vm.songs) { artistsOf(vm.songs) }
    if (artists.isEmpty()) { Empty("no songs found yet"); return }
    LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(8.dp)) {
        items(artists) { (name, list) -> ArtistCard(name, list, { openArtist(name) }) }
    }
}

@Composable
fun FavoritesView(vm: Vm, openPl: (String) -> Unit) {
    var seg by remember { mutableIntStateOf(0) }
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("songs", seg == 0) { seg = 0 }
        Chip("playlists", seg == 1) { seg = 1 }
    }
    Spacer(Modifier.height(10.dp))
    if (seg == 0) {
        val l = vm.songs.filter { it.id in vm.favSongs }
        if (l.isEmpty()) Empty("tap the heart on any song")
        LazyColumn { itemsIndexed(l) { i, s -> SongRow(s, s.id == vm.nowId, true, { vm.toggleFav(s.id) }, { vm.play(l, i) }, onEdit = { vm.editing = s }) } }
    } else {
        val l = vm.playlists.filter { it.id in vm.favLists }
        if (l.isEmpty()) Empty("tap the heart inside a playlist")
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(8.dp)) { items(l) { p -> PlaylistCard(p, true, { openPl(p.id) }) } }
    }
}

/** Exact listening time for every song, counted live. */
@Composable
fun StatsView(vm: Vm) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val acc = AppTheme.accent
    val rows = remember(tick, vm.songs, Stats.ver) {
        vm.songs.map { it to Stats.total(it.id) }.filter { it.second > 0 }.sortedByDescending { it.second }
    }
    val max = (rows.firstOrNull()?.second ?: 1L).coerceAtLeast(1L)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(22.dp, 0.1f).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBlock(fmtLong(rows.sumOf { it.second }), "total listening")
                StatBlock("${Stats.allPlays()}", "plays")
                StatBlock("${rows.size}", "songs heard")
            }
        }
        if (rows.isEmpty()) item { Empty("nothing counted yet. play a song and watch the time grow.") }
        itemsIndexed(rows) { i, (s, ms) ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).glass(18.dp, if (s.id == vm.nowId) 0.16f else 0.05f)
                    .clickable { vm.play(rows.map { it.first }, i) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${i + 1}", color = Lilac.copy(0.6f), fontSize = 14.sp, modifier = Modifier.width(26.dp))
                Art(s.artUri, RoundedCornerShape(12.dp), acc.a, acc.b, Modifier.size(44.dp), "♪")
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = Mist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                    Text(s.artist, color = Lilac.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
                        Box(Modifier.fillMaxWidth((ms.toFloat() / max).coerceIn(0.02f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(acc.a, acc.b))))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmtLong(ms), color = Mist, fontSize = 13.sp)
                    Text("${Stats.plays(s.id)} plays", color = Lilac.copy(0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StatBlock(big: String, small: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, color = Mist, fontSize = 18.sp, fontFamily = FONTS[1])
        Text(small, color = Lilac.copy(0.7f), fontSize = 11.sp)
    }
}

// ───────────────────────── artists ─────────────────────────

@Composable
fun ArtistDetail(vm: Vm, name: String, onBack: () -> Unit) {
    val acc = AppTheme.accent
    val list = remember(vm.songs, name) { vm.songs.filter { it.artist == name } }
    val listened = remember(list, Stats.ver) { list.sumOf { Stats.base(it.id) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Mist, modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp))
                }
                Art(list.firstOrNull()?.artUri, CircleShape, acc.a, acc.b, Modifier.size(170.dp), "♪")
                Spacer(Modifier.height(12.dp))
                Text(name, color = Mist, fontSize = 28.sp, fontFamily = FONTS[AppTheme.titleFont], textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(acc.b.copy(0.9f), blurRadius = 30f)))
                Text("${list.size} songs  ·  ${fmtShort(listened)} listened", color = Lilac.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("play", Icons.Rounded.PlayArrow, { vm.play(list, 0) }, strong = true)
                    Pill("shuffle", Icons.Rounded.Shuffle, { vm.play(list.shuffled(), 0) })
                }
            }
        }
        itemsIndexed(list) { i, s -> SongRow(s, s.id == vm.nowId, s.id in vm.favSongs, { vm.toggleFav(s.id) }, { vm.play(list, i) }, onEdit = { vm.editing = s }) }
    }
}

// ───────────────────────── playlists ─────────────────────────

@Composable
fun PlaylistsTab(vm: Vm, openPl: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        item(span = { GridItemSpan(2) }) { Header("playlists", "made by hand, for you") }
        item {
            Column(Modifier.padding(8.dp).clickable { creating = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(0.88f).aspectRatio(1f).glass(26.dp, 0.05f), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, tint = Lilac, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("new playlist", color = Lilac, fontSize = 15.sp)
            }
        }
        items(vm.playlists) { p -> PlaylistCard(p, p.id in vm.favLists, { openPl(p.id) }) }
    }
    if (creating) AlertDialog(
        onDismissRequest = { creating = false }, containerColor = Deep,
        title = { Text("name your playlist", color = Mist) },
        text = { HField(name, { name = it }, "e.g. midnight lilac") },
        confirmButton = { TextButton({ val id = vm.newPlaylist(name); name = ""; creating = false; openPl(id) }) { Text("create", color = Rose) } },
        dismissButton = { TextButton({ creating = false }) { Text("cancel", color = Lilac) } }
    )
}

@Composable
fun PlaylistDetail(vm: Vm, id: String, onBack: () -> Unit) {
    val p = vm.playlists.firstOrNull { it.id == id } ?: return
    val acc = ACCENTS[p.accent]
    val list = p.songIds.mapNotNull { vm.song(it) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Mist, modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp))
                    Spacer(Modifier.weight(1f))
                    Icon(
                        if (id in vm.favLists) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (id in vm.favLists) Rose else Mist,
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { vm.toggleFavPl(id) }.padding(8.dp)
                    )
                }
                Art(p.coverPath?.let { File(it) }, coverShape(p.shape), acc.a, acc.b, Modifier.size(190.dp))
                Spacer(Modifier.height(14.dp))
                Text(p.name, color = Mist, fontSize = 30.sp, fontFamily = FONTS[p.font], textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(acc.b.copy(0.9f), blurRadius = 30f)))
                if (p.desc.isNotBlank()) Text(p.desc, color = Lilac.copy(0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("${list.size} songs", color = Lilac.copy(0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("play", Icons.Rounded.PlayArrow, { vm.play(list, 0, id) }, strong = true)
                    Pill("shuffle", Icons.Rounded.Shuffle, { vm.play(list.shuffled(), 0, id) })
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("add songs", Icons.Rounded.Add, { adding = true })
                    Pill("customize", Icons.Rounded.AutoAwesome, { editing = true })
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Lilac, modifier = Modifier.size(42.dp).clip(CircleShape).clickable { confirmDel = true }.padding(9.dp))
                }
            }
        }
        if (list.isEmpty()) item { Empty("empty for now. add songs you pick yourself from your phone.") }
        itemsIndexed(list) { i, s ->
            SongRow(s, s.id == vm.nowId, s.id in vm.favSongs, { vm.toggleFav(s.id) }, { vm.play(list, i, id) }, onEdit = { vm.editing = s }) {
                Icon(
                    Icons.Rounded.Close, null, tint = Lilac.copy(0.5f),
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { vm.updatePl(p.copy(songIds = p.songIds - s.id)) }.padding(7.dp)
                )
            }
        }
    }
    if (adding) AddSongsSheet(vm, p) { adding = false }
    if (editing) CustomizeSheet(vm, id) { editing = false }
    if (confirmDel) AlertDialog(
        onDismissRequest = { confirmDel = false }, containerColor = Deep,
        title = { Text("delete \"${p.name}\"?", color = Mist) },
        text = { Text("your songs stay on the phone. only this playlist disappears.", color = Lilac) },
        confirmButton = { TextButton({ confirmDel = false; onBack(); vm.deletePl(id) }) { Text("delete", color = Rose) } },
        dismissButton = { TextButton({ confirmDel = false }) { Text("keep", color = Lilac) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsSheet(vm: Vm, p: Playlist, onDone: () -> Unit) {
    var sel by remember { mutableStateOf(p.songIds.toSet()) }
    var q by remember { mutableStateOf("") }
    val list = vm.songs.filter { q.isBlank() || it.title.contains(q, true) || it.artist.contains(q, true) }
    ModalBottomSheet(onDismissRequest = onDone, containerColor = Deep, contentColor = Mist) {
        Column(Modifier.fillMaxHeight(0.85f)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("pick songs (${sel.size})", color = Mist, fontSize = 20.sp, fontFamily = FONTS[1], modifier = Modifier.weight(1f))
                Pill("done", Icons.Rounded.Check, {
                    vm.updatePl(p.copy(songIds = p.songIds.filter { it in sel } + sel.filter { it !in p.songIds })); onDone()
                }, strong = true)
            }
            HField(q, { q = it }, "search your phone", Modifier.fillMaxWidth().padding(16.dp))
            LazyColumn {
                items(list) { s ->
                    val on = s.id in sel
                    SongRow(s, on, s.id in vm.favSongs, { vm.toggleFav(s.id) }, { sel = if (on) sel - s.id else sel + s.id }) {
                        Icon(if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (on) Rose else Lilac.copy(0.5f), modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(vm: Vm, id: String, onDone: () -> Unit) {
    fun cur() = vm.playlists.first { it.id == id }
    val p = cur()
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.importImage(it)?.let { f -> vm.updatePl(cur().copy(coverPath = f)) } } }
    val pickBg = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.importImage(it)?.let { f -> vm.updatePl(cur().copy(bgPath = f)) } } }
    val img = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    ModalBottomSheet(onDismissRequest = onDone, containerColor = Deep.copy(alpha = 0.96f), contentColor = Mist) {
        Column(Modifier.fillMaxHeight(0.85f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("make it yours", color = Mist, fontSize = 22.sp, fontFamily = FONTS[1])
            HField(p.name, { vm.updatePl(cur().copy(name = it)) }, "name")
            HField(p.desc, { vm.updatePl(cur().copy(desc = it)) }, "a line about the mood")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("cover image", Icons.Rounded.Image, { pickCover.launch(img) })
                Pill("background", Icons.Rounded.Wallpaper, { pickBg.launch(img) })
            }
            if (p.coverPath != null || p.bgPath != null) Text(
                "remove cover / background", color = Lilac.copy(0.7f), fontSize = 13.sp,
                modifier = Modifier.clickable { vm.updatePl(cur().copy(coverPath = null, bgPath = null)) }
            )
            Text("cover shape", color = Lilac)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SHAPE_NAMES.forEachIndexed { i, n ->
                    Column(Modifier.clickable { vm.updatePl(cur().copy(shape = i)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        val on = p.shape == i
                        Box(Modifier.size(54.dp).clip(coverShape(i)).background(Brush.linearGradient(if (on) listOf(ACCENTS[p.accent].a, ACCENTS[p.accent].b) else listOf(Color.White.copy(0.12f), Color.White.copy(0.12f)))))
                        Text(n, color = Lilac, fontSize = 12.sp)
                    }
                }
            }
            Text("colors", color = Lilac)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ACCENTS.forEachIndexed { i, a ->
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(a.a, a.b)))
                            .border(if (p.accent == i) 2.dp else 0.dp, Color.White, CircleShape).clickable { vm.updatePl(cur().copy(accent = i)) }
                    )
                }
            }
            Text("title style", color = Lilac)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FONT_NAMES.forEachIndexed { i, n ->
                    Text(
                        n, color = if (p.font == i) Abyss else Mist, fontFamily = FONTS[i], fontSize = 16.sp,
                        modifier = Modifier.glass(50.dp, if (p.font == i) 0.9f else 0.08f).clickable { vm.updatePl(cur().copy(font = i)) }.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            Text("background dimming", color = Lilac)
            Slider(
                p.dim, { vm.updatePl(cur().copy(dim = it)) }, valueRange = 0.1f..0.9f,
                colors = SliderDefaults.colors(thumbColor = ACCENTS[p.accent].b, activeTrackColor = ACCENTS[p.accent].b)
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

// ───────────────────────── song editor ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongSheet(vm: Vm, s: Song, onDone: () -> Unit) {
    val acc = AppTheme.accent
    var title by remember { mutableStateOf(s.title) }
    var artist by remember { mutableStateOf(s.artist) }
    var album by remember { mutableStateOf(s.album) }
    var cover by remember { mutableStateOf(vm.metas[s.id]?.cover) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.importImage(it)?.let { f -> cover = f } } }
    ModalBottomSheet(onDismissRequest = onDone, containerColor = Deep, contentColor = Mist) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("edit song", color = Mist, fontSize = 22.sp, fontFamily = FONTS[1])
            Row(verticalAlignment = Alignment.CenterVertically) {
                Art(cover?.let { File(it) } ?: s.artUri, RoundedCornerShape(18.dp), acc.a, acc.b, Modifier.size(84.dp), "♪")
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("change cover", Icons.Rounded.Image, { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    if (cover != null) Text("remove custom cover", color = Lilac.copy(0.7f), fontSize = 13.sp, modifier = Modifier.clickable { cover = null })
                }
            }
            HField(title, { title = it }, "song name")
            HField(artist, { artist = it }, "artist")
            HField(album, { album = it }, "album")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("save", Icons.Rounded.Check, { vm.editSong(s.id, title, artist, album, cover); onDone() }, strong = true)
                if (s.edited) Pill("reset to original", Icons.Rounded.Refresh, { vm.resetSong(s.id); onDone() })
            }
            Text("edits live only inside music heaven. your audio files are never changed.", color = Lilac.copy(0.6f), fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────── bars ─────────────────────────

@Composable
fun MiniPlayer(vm: Vm, onOpen: () -> Unit) {
    val s = vm.now ?: return
    val acc = AppTheme.accent
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().glass(24.dp, 0.12f).clickable(onClick = onOpen)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(s.artUri, CircleShape, acc.a, acc.b, Modifier.size(42.dp), "♪")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, color = Mist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                Text(s.artist, color = Lilac.copy(0.7f), maxLines = 1, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.SkipPrevious, null, tint = Mist, modifier = Modifier.size(38.dp).clip(CircleShape).clickable { vm.prev() }.padding(6.dp))
            Icon(
                if (vm.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Abyss,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(acc.a, acc.b))).clickable { vm.toggle() }.padding(8.dp)
            )
            Icon(Icons.Rounded.SkipNext, null, tint = Mist, modifier = Modifier.size(38.dp).clip(CircleShape).clickable { vm.next() }.padding(6.dp))
        }
        Box(Modifier.fillMaxWidth((vm.pos.toFloat() / vm.dur).coerceIn(0f, 1f)).height(2.dp).background(acc.b))
    }
}

@Composable
fun NavBar(tab: Int, onTab: (Int) -> Unit) {
    val acc = AppTheme.accent
    val items = listOf(Icons.Rounded.Home, Icons.Rounded.LibraryMusic, Icons.Rounded.QueueMusic, Icons.Rounded.AutoAwesome, Icons.Rounded.Settings)
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().glass(30.dp, 0.1f).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { i, ic ->
            val bg by animateColorAsState(if (tab == i) acc.b.copy(0.28f) else Color.Transparent, label = "nav")
            Icon(
                ic, null, tint = if (tab == i) Mist else Lilac.copy(0.6f),
                modifier = Modifier.size(width = 52.dp, height = 40.dp).clip(RoundedCornerShape(20.dp)).background(bg).clickable { onTab(i) }.padding(8.dp)
            )
        }
    }
}
