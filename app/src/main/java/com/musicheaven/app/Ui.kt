package com.musicheaven.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val Abyss: Color get() = AppTheme.bg
val Deep: Color get() = AppTheme.deep
val Lilac: Color get() = AppTheme.soft
val Mist = Color(0xFFF1E8FF)
val Rose = Color(0xFFF5C6E6)

val FONTS = listOf(FontFamily.SansSerif, FontFamily.Serif, FontFamily.Cursive)
val FONT_NAMES = listOf("soft", "serif", "script")
val SHAPE_NAMES = listOf("orb", "tile", "arch", "petal")

fun coverShape(i: Int): Shape = when (i) {
    0 -> CircleShape
    1 -> RoundedCornerShape(26.dp)
    2 -> RoundedCornerShape(50, 50, 8, 8)
    else -> RoundedCornerShape(50, 8, 50, 8)
}

fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

fun Modifier.glass(r: Dp = 22.dp, alpha: Float = 0.08f): Modifier {
    val s = RoundedCornerShape(r)
    return this.background(Color.White.copy(alpha), s).border(1.dp, Color.White.copy(0.14f), s).clip(s)
}

/** Signature background: drifting aurora, wallpaper, falling flowers and ambient effects, all from Settings. */
@Composable
fun Aurora(a: Color, b: Color, bgPath: String?, dim: Float) {
    val T = AppTheme
    val path = bgPath ?: T.wallpaper
    val d = if (bgPath != null) dim else T.wallDim
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween((30000 / T.auroraSpeed).toInt().coerceAtLeast(4000), easing = LinearEasing)), label = "t"
    )
    val up by rememberInfiniteTransition(label = "petal").animateFloat(
        0f, 1f, infiniteRepeatable(tween((60000 / T.petalSpeed).toInt().coerceAtLeast(8000), easing = LinearEasing)), label = "up"
    )
    val ua by rememberInfiniteTransition(label = "amb").animateFloat(
        0f, 1f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "ua"
    )
    Box(Modifier.fillMaxSize().background(Abyss)) {
        if (path != null) AsyncImage(File(path), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Canvas(Modifier.fillMaxSize()) {
            if (path != null) drawRect(Abyss.copy(alpha = d))
            if (T.aurora) {
                val cols = listOf(a, b, Color(0xFF3B2A7A))
                for (i in 0..2) {
                    val c = Offset(size.width * (0.5f + 0.45f * sin(t + i * 2.1f)), size.height * (0.2f + 0.3f * i + 0.15f * cos(t + i * 1.3f)))
                    val r = size.width * 0.9f
                    drawCircle(Brush.radialGradient(listOf(cols[i].copy(alpha = 0.34f), Color.Transparent), c, r), r, c)
                }
            }
            if (T.ambient != 0) drawAmbient(T.ambient, ua, (26 * T.ambientAmount).toInt(), a, b)
            if (T.petals) drawPetals(up, (20 * T.petalAmount).toInt())
        }
    }
}

@Composable
fun Art(model: Any?, shape: Shape, a: Color, b: Color, modifier: Modifier, glyph: String = "☾") {
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(a.copy(0.85f), b.copy(0.85f)))), contentAlignment = Alignment.Center) {
        Text(glyph, color = Color.White.copy(0.7f), fontSize = 26.sp)
        if (model != null) AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun Pill(text: String, icon: ImageVector, onClick: () -> Unit, strong: Boolean = false) {
    Row(
        Modifier.glass(50.dp, if (strong) 0.22f else 0.08f).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Mist, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Mist, fontSize = 14.sp)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (selected) Abyss else Mist, fontSize = 13.sp,
        modifier = Modifier.glass(50.dp, if (selected) 0.92f else 0.08f).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
