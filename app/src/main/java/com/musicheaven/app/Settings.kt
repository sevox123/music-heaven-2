package com.musicheaven.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().glass(24.dp, 0.06f).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = Mist, fontSize = 18.sp, fontFamily = FONTS[1])
        content()
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Mist, modifier = Modifier.weight(1f))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Mist, checkedTrackColor = AppTheme.accent.a,
                uncheckedThumbColor = Lilac, uncheckedTrackColor = Color.White.copy(0.15f), uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SliderRow(label: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, color = Lilac, fontSize = 13.sp)
        Slider(v, onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = AppTheme.accent.b, activeTrackColor = AppTheme.accent.b))
    }
}

@Composable
fun Swatch(brush: Brush, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(brush)
            .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else Color.White.copy(0.2f), CircleShape).clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(vm: Vm) {
    val T = AppTheme
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val pickWall = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { vm.importImage(it)?.let { f -> T.upd { wallpaper = f } } } }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> hasMic = ok; if (ok) Spectrum.start() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Header("settings", "make it feel like yours")

        Section("theme") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                THEMES.forEachIndexed { i, t ->
                    Column(Modifier.clickable { T.upd { theme = i; bgIdx = -1; accIdx = -1 } }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Swatch(Brush.linearGradient(listOf(t.bg, t.accent.a, t.accent.b)), T.theme == i && T.bgIdx < 0 && T.accIdx < 0) { T.upd { theme = i; bgIdx = -1; accIdx = -1 } }
                        Text(t.name, color = Lilac, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Section("colors") {
            Text("background color", color = Lilac, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("theme default", T.bgIdx < 0) { T.upd { bgIdx = -1 } }
                BG_SWATCHES.forEachIndexed { i, c -> Swatch(Brush.linearGradient(listOf(c, c)), T.bgIdx == i) { T.upd { bgIdx = i } } }
            }
            Text("accent color", color = Lilac, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("theme default", T.accIdx < 0) { T.upd { accIdx = -1 } }
                ACCENTS.forEachIndexed { i, a -> Swatch(Brush.linearGradient(listOf(a.a, a.b)), T.accIdx == i) { T.upd { accIdx = i } } }
            }
        }

        Section("wallpaper") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("choose image", Icons.Rounded.Wallpaper, { pickWall.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, strong = true)
                if (T.wallpaper != null) Pill("remove", Icons.Rounded.Close, { T.upd { wallpaper = null } })
            }
            if (T.wallpaper != null) SliderRow("dimming", T.wallDim, 0f..0.95f) { T.upd { wallDim = it } }
            Text("playlists with their own background still use it.", color = Lilac.copy(0.6f), fontSize = 12.sp)
        }

        Section("falling flowers") {
            SwitchRow("purple flowers drifting down", T.petals) { T.upd { petals = it } }
            if (T.petals) {
                SliderRow("amount", T.petalAmount, 0.2f..2f) { T.upd { petalAmount = it } }
                SliderRow("speed", T.petalSpeed, 0.4f..2.5f) { T.upd { petalSpeed = it } }
            }
        }

        Section("ambient magic") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AMBIENTS.forEachIndexed { i, n -> Chip(n, T.ambient == i) { T.upd { ambient = i } } }
            }
            if (T.ambient != 0) SliderRow("amount", T.ambientAmount, 0.2f..2f) { T.upd { ambientAmount = it } }
            SwitchRow("drifting aurora glow", T.aurora) { T.upd { aurora = it } }
            if (T.aurora) SliderRow("aurora speed", T.auroraSpeed, 0.3f..3f) { T.upd { auroraSpeed = it } }
        }

        Section("beat visualizer") {
            Text("reacts to the music on the player screen", color = Lilac.copy(0.7f), fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VIZ_MODES.forEachIndexed { i, n -> Chip(n, T.viz == i) { T.upd { viz = i } } }
            }
            Text("colors", color = Lilac, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VIZ_COLORS.forEachIndexed { i, n -> Chip(n, T.vizColor == i) { T.upd { vizColor = i } } }
            }
            SliderRow("sensitivity", T.vizSens, 0.4f..2f) { T.upd { vizSens = it } }
            SwitchRow("cover bounces with the bass", T.bounce) { T.upd { bounce = it } }
            if (hasMic) Text("reading the real beat of your music ✓", color = Lilac, fontSize = 13.sp)
            else {
                Text("without audio access the visualizer uses a simulated beat. allow it to react to the real music. nothing is recorded.", color = Lilac.copy(0.7f), fontSize = 12.sp)
                Pill("enable real beat", Icons.Rounded.GraphicEq, { askMic.launch(Manifest.permission.RECORD_AUDIO) }, strong = true)
            }
        }

        Section("typography") {
            Text("title style", color = Lilac, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FONT_NAMES.forEachIndexed { i, n -> Chip(n, T.titleFont == i) { T.upd { titleFont = i } } }
            }
        }

        Section("your data") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("clear recent", Icons.Rounded.History, { Stats.clearRecent() })
                Pill("reset stats", Icons.Rounded.RestartAlt, { confirmReset = true })
            }
            Text("music heaven  ·  made just for you", color = Lilac.copy(0.5f), fontSize = 12.sp)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, containerColor = Deep,
        title = { Text("reset listening stats?", color = Mist) },
        text = { Text("play time, play counts and skips go back to zero. your playlists and favorites stay.", color = Lilac) },
        confirmButton = { TextButton({ Stats.resetAll(); confirmReset = false }) { Text("reset", color = Rose) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("cancel", color = Lilac) } }
    )
}
