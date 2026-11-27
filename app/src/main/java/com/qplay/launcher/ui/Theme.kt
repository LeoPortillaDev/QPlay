package com.qplay.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
    var isDark: Boolean by mutableStateOf(false)
        private set

    fun setAccent(color: Color?) { customAccent = color }
    fun setDarkMode(enabled: Boolean) { isDark = enabled }

    val Cyan: Color get() = customAccent ?: Color(0xFF00D2E6)
    val Teal: Color get() = customAccent ?: Color(0xFF1FC6B5)

    val Bg: Color get() = if (isDark) Color(0xFF2B2B2B) else Color(0xFFEBEBEB)
    val Ink: Color get() = if (isDark) Color(0xFFF4F4F4) else Color(0xFF2D2D2D)
    val Muted: Color get() = if (isDark) Color(0xFFB5B5B5) else Color(0xFF8A8A8A)
    val Card: Color get() = if (isDark) Color(0xFF353535) else Color(0xFF2E2E2E)
    val EmptySlot: Color get() = if (isDark) Color(0xFF303030) else Color(0xFFEDEDED)
    val Green = Color(0xFF9BE000)
    val Line: Color get() = if (isDark) Color(0xFFD0D0D0) else Color(0xFF4A4A4A)
    val Surface: Color get() = if (isDark) Color(0xFF414141) else Color.White
    val Panel: Color get() = if (isDark) Color(0xFF3A3A3A) else Color(0xFFF5F5F5)
    val Border: Color get() = if (isDark) Color(0xFF666666) else Color(0xFFD5D5D5)
    val Selected: Color get() = if (isDark) Color(0xFF4A4A4A) else Color.White

    // Ícones da fileira inferior
    val Red = Color(0xFFE60012)
    val Orange = Color(0xFFFF9F00)
    val Blue = Color(0xFF1B5FE0)
    val Gray = Color(0xFF6B6B6B)
}

@Composable fun QPlayTheme(content: @Composable () -> Unit) {
    val scheme = if (SwitchColors.isDark) {
        darkColorScheme(
            primary = SwitchColors.Teal,
            secondary = SwitchColors.Cyan,
            background = SwitchColors.Bg,
            surface = SwitchColors.Surface,
            onPrimary = Color.White,
            onBackground = SwitchColors.Ink,
            onSurface = SwitchColors.Ink
        )
    } else {
        lightColorScheme(
            primary = SwitchColors.Teal,
            secondary = SwitchColors.Cyan,
            background = SwitchColors.Bg,
            surface = SwitchColors.Surface,
            onPrimary = Color.White,
            onBackground = SwitchColors.Ink,
            onSurface = SwitchColors.Ink
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
