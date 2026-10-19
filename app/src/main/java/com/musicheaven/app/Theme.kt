package com.musicheaven.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

class Accent(val name: String, val a: Color, val b: Color)

val ACCENTS = listOf(
    Accent("iris", Color(0xFF7B6EE6), Color(0xFFC9B8F6)),
    Accent("lilac", Color(0xFFB596EE), Color(0xFFF5CFF1)),
    Accent("abyss", Color(0xFF1F3C9E), Color(0xFF63B3F0)),
    Accent("ghost rose", Color(0xFFE59BC4), Color(0xFFFBD9EA)),
    Accent("moonmint", Color(0xFF6FB3B8), Color(0xFFCDEFEA)),
    Accent("sunset haze", Color(0xFFE8998D), Color(0xFFFFD9B8)),
    Accent("orchid", Color(0xFFB45CD0), Color(0xFFEBB5F5)),
    Accent("ocean", Color(0xFF2B7FB8), Color(0xFF9FE0F5))
)

class ThemeDef(val name: String, val bg: Color, val accent: Accent, val soft: Color)

val THEMES = listOf(
    ThemeDef("ghost iris", Color(0xFF070615), ACCENTS[0], Color(0xFFC9B8F6)),
    ThemeDef("abyss", Color(0xFF030B1F), ACCENTS[2], Color(0xFFA9CDF5)),
    ThemeDef("lilac dream", Color(0xFF140B26), ACCENTS[1], Color(0xFFDCC6F7)),
    ThemeDef("rose quartz", Color(0xFF1E0A1A), ACCENTS[3], Color(0xFFF5C6E6)),
    ThemeDef("moonmint", Color(0xFF06161A), ACCENTS[4], Color(0xFFB7E3DE)),
    ThemeDef("midnight plum", Color(0xFF120716), ACCENTS[6], Color(0xFFE0B8DC)),
    ThemeDef("twilight", Color(0xFF0F1038), ACCENTS[0], Color(0xFFC8C6F5)),
    ThemeDef("ethereal dawn", Color(0xFF241A3A), ACCENTS[5], Color(0xFFF0D3F0))
)

val BG_SWATCHES = listOf(
    Color(0xFF070615), Color(0xFF030B1F), Color(0xFF140B26), Color(0xFF1E0A1A), Color(0xFF06161A),
    Color(0xFF000000), Color(0xFF101B2D), Color(0xFF1A1030), Color(0xFF241A3A), Color(0xFF2B1B3D)
)
val AMBIENTS = listOf("off", "dust", "stars", "fireflies", "bubbles", "moonfall", "sparkles")
val VIZ_MODES = listOf("off", "bars", "radial", "wave", "rings", "glow")
val VIZ_COLORS = listOf("accent", "pastel rainbow", "white")

/** All look-and-feel settings. Reading any field inside a composable makes the UI update live. */
object AppTheme {
    private var sp: SharedPreferences? = null
    var theme by mutableIntStateOf(0)
    var bgIdx by mutableIntStateOf(-1)
    var accIdx by mutableIntStateOf(-1)
    var wallpaper by mutableStateOf<String?>(null)
    var wallDim by mutableFloatStateOf(0.5f)
    var aurora by mutableStateOf(true)
    var auroraSpeed by mutableFloatStateOf(1f)
    var petals by mutableStateOf(true)
    var petalAmount by mutableFloatStateOf(1f)
    var petalSpeed by mutableFloatStateOf(1f)
    var ambient by mutableIntStateOf(1)
    var ambientAmount by mutableFloatStateOf(1f)
    var viz by mutableIntStateOf(1)
    var vizSens by mutableFloatStateOf(1f)
    var vizColor by mutableIntStateOf(0)
    var bounce by mutableStateOf(true)
    var titleFont by mutableIntStateOf(1)

    val bg: Color get() = if (bgIdx >= 0) BG_SWATCHES[bgIdx] else THEMES[theme].bg
    val deep: Color get() = lerp(bg, Color.White, 0.08f)
    val soft: Color get() = THEMES[theme].soft
    val accent: Accent get() = if (accIdx >= 0) ACCENTS[accIdx] else THEMES[theme].accent

    fun init(ctx: Context) {
        if (sp != null) return
        val s = ctx.getSharedPreferences("look", Context.MODE_PRIVATE); sp = s
        theme = s.getInt("theme", 0).coerceIn(0, THEMES.size - 1)
        bgIdx = s.getInt("bgIdx", -1).coerceIn(-1, BG_SWATCHES.size - 1)
        accIdx = s.getInt("accIdx", -1).coerceIn(-1, ACCENTS.size - 1)
        wallpaper = s.getString("wall", "")?.ifEmpty { null }
        wallDim = s.getFloat("wallDim", 0.5f)
        aurora = s.getBoolean("aurora", true); auroraSpeed = s.getFloat("auroraSpeed", 1f)
        petals = s.getBoolean("petals", true); petalAmount = s.getFloat("petalAmount", 1f); petalSpeed = s.getFloat("petalSpeed", 1f)
        ambient = s.getInt("ambient", 1).coerceIn(0, AMBIENTS.size - 1); ambientAmount = s.getFloat("ambientAmount", 1f)
        viz = s.getInt("viz", 1).coerceIn(0, VIZ_MODES.size - 1); vizSens = s.getFloat("vizSens", 1f)
        vizColor = s.getInt("vizColor", 0).coerceIn(0, VIZ_COLORS.size - 1); bounce = s.getBoolean("bounce", true)
        titleFont = s.getInt("titleFont", 1).coerceIn(0, 2)
    }

    fun save() {
        sp?.edit()
            ?.putInt("theme", theme)?.putInt("bgIdx", bgIdx)?.putInt("accIdx", accIdx)
            ?.putString("wall", wallpaper ?: "")?.putFloat("wallDim", wallDim)
            ?.putBoolean("aurora", aurora)?.putFloat("auroraSpeed", auroraSpeed)
            ?.putBoolean("petals", petals)?.putFloat("petalAmount", petalAmount)?.putFloat("petalSpeed", petalSpeed)
            ?.putInt("ambient", ambient)?.putFloat("ambientAmount", ambientAmount)
            ?.putInt("viz", viz)?.putFloat("vizSens", vizSens)?.putInt("vizColor", vizColor)?.putBoolean("bounce", bounce)
            ?.putInt("titleFont", titleFont)
            ?.apply()
    }

    fun upd(block: AppTheme.() -> Unit) { block(); save() }
}
