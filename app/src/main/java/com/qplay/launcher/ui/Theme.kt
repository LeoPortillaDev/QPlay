package com.qplay.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Paleta da tela inicial do Switch (fundo cinza claro, texto grafite, destaque ciano/teal).
 * Cyan e Teal seguem a cor de destaque escolhida em Configurações > Aparência
 * (null = cores originais do Switch). São state: mudar a cor atualiza a tela na hora.
 */
object SwitchColors {
    var customAccent: Color? by mutableStateOf<Color?>(null)
        private set

    fun setAccent(color: Color?) { customAccent = color }

    val Cyan: Color get() = customAccent ?: Color(0xFF00D2E6)
    val Teal: Color get() = customAccent ?: Color(0xFF1FC6B5)

    val Bg = Color(0xFFEBEBEB)
    val Ink = Color(0xFF2D2D2D)
    val Muted = Color(0xFF8A8A8A)
    val Card = Color(0xFF2E2E2E)
    val EmptySlot = Color(0xFFEDEDED)
    val Green = Color(0xFF9BE000)
    val Line = Color(0xFF4A4A4A)

    // Ícones da fileira inferior
    val Red = Color(0xFFE60012)
    val Orange = Color(0xFFFF9F00)
    val Blue = Color(0xFF1B5FE0)
    val Gray = Color(0xFF6B6B6B)
}

@Composable fun QPlayTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = SwitchColors.Teal,
        secondary = SwitchColors.Cyan,
        background = SwitchColors.Bg,
        surface = Color.White,
        onPrimary = Color.White,
        onBackground = SwitchColors.Ink,
        onSurface = SwitchColors.Ink
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
