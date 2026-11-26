package com.qplay.launcher.data

import androidx.compose.ui.graphics.Color

data class DemoGame(val title: String, val subtitle: String, val genre: String, val symbol: String, val color: Color)

object DemoGames {
    val items = listOf(
        DemoGame("Super Mario", "Uma aventura para começar", "PLATAFORMA", "🍄", Color(0xFFB84B4B)),
        DemoGame("Zelda", "Explore um mundo vasto", "AVENTURA", "🗡️", Color(0xFF547D65)),
        DemoGame("Metroid", "Uma jornada espacial", "AÇÃO", "🚀", Color(0xFF526B9D)),
        DemoGame("Sonic", "Velocidade máxima", "PLATAFORMA", "💨", Color(0xFF3D78B9)),
        DemoGame("Kirby", "Uma aventura colorida", "AVENTURA", "⭐", Color(0xFFB76D9B)),
        DemoGame("Donkey Kong", "Diversão na selva", "AÇÃO", "🌴", Color(0xFF9A7547))
    )
}
