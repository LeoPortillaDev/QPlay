package com.qplay.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QPlayColors = darkColorScheme(
    primary = Color(0xFF8798FF),
    secondary = Color(0xFF9BAAFF),
    background = Color(0xFF10131B),
    surface = Color(0xFF202737),
    onPrimary = Color(0xFF10131B),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable fun QPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = QPlayColors, content = content)
}
