package com.musicheaven.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: Vm by viewModels()

    private fun perms(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun hasAudio() = ContextCompat.checkSelfPermission(
        this, if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var granted by remember { mutableStateOf(hasAudio()) }
                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted = hasAudio() }
                LaunchedEffect(granted) { if (granted) { vm.load(); vm.connect() } else ask.launch(perms()) }
                LaunchedEffect(Unit) { while (true) { vm.tick(); delay(150) } }
                if (granted) HeavenApp(vm) else Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                    Text("music heaven needs access to the music on your phone.\nallow it in settings to begin.", color = Lilac, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun HeavenApp(vm: Vm) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var seg by rememberSaveable { mutableStateOf(0) }
    var openPl by rememberSaveable { mutableStateOf<String?>(null) }
    var openArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var showNow by rememberSaveable { mutableStateOf(false) }
    val pl = vm.playlists.firstOrNull { it.id == openPl }
    val acc = pl?.let { ACCENTS[it.accent] } ?: AppTheme.accent
    val a by animateColorAsState(acc.a, tween(900), label = "a")
    val b by animateColorAsState(acc.b, tween(900), label = "b")
    BackHandler((openPl != null || openArtist != null) && !showNow) { openPl = null; openArtist = null }
    val key = when {
        openPl != null -> "pl:$openPl"
        openArtist != null -> "ar:$openArtist"
        else -> "tab$tab"
    }

    Box(Modifier.fillMaxSize()) {
        Aurora(a, b, pl?.bgPath, pl?.dim ?: 0.5f)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    key,
                    transitionSpec = { (fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 24 }) togetherWith fadeOut(tween(200)) },
                    label = "screen"
                ) { k ->
                    when {
                        k.startsWith("pl:") -> PlaylistDetail(vm, k.removePrefix("pl:")) { openPl = null }
                        k.startsWith("ar:") -> ArtistDetail(vm, k.removePrefix("ar:")) { openArtist = null }
                        else -> when (k.removePrefix("tab").toInt()) {
                            0 -> HomeTab(vm, { openPl = it }, { t, sg -> tab = t; seg = sg }, { openArtist = it })
                            1 -> LibraryTab(vm, seg, { seg = it }, { openPl = it }, { openArtist = it })
                            2 -> PlaylistsTab(vm) { openPl = it }
                            3 -> ForYouTab(vm)
                            else -> SettingsTab(vm)
                        }
                    }
                }
            }
            MiniPlayer(vm) { showNow = true }
            NavBar(tab) { tab = it; openPl = null; openArtist = null }
        }
        AnimatedVisibility(
            showNow && vm.now != null,
            enter = slideInVertically(tween(450)) { it } + fadeIn(tween(450)),
            exit = slideOutVertically(tween(350)) { it } + fadeOut(tween(350))
        ) { NowPlaying(vm) { showNow = false } }
        vm.editing?.let { s -> EditSongSheet(vm, s) { vm.editing = null } }
    }
}
