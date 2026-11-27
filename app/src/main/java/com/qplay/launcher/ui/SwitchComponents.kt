package com.qplay.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ------------------------------------------------------------------ */
/* Entrada por controle                                                 */
/* ------------------------------------------------------------------ */

/** Confirma o item em foco pelo botão A do controle, Enter ou centro do D-pad. */
fun Modifier.gamepadA(onClick: () -> Unit): Modifier = this.onKeyEvent { ev ->
    val confirm = ev.key == Key.ButtonA ||
        ev.key == Key.Enter ||
        ev.key == Key.NumPadEnter ||
        ev.key == Key.DirectionCenter
    if (confirm) {
        if (ev.type == KeyEventType.KeyUp) onClick()
        true
    } else false
}

/* ------------------------------------------------------------------ */
/* Barra de status: relógio, Wi-Fi e bateria                            */
/* ------------------------------------------------------------------ */

private data class SystemStatus(val time: String, val ampm: String, val battery: Float, val online: Boolean)

private fun readStatus(context: Context): SystemStatus {
    val use24h = android.text.format.DateFormat.is24HourFormat(context)
    val now = Date()
    val time = SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    val ampm = if (use24h) "" else SimpleDateFormat("a", Locale.getDefault()).format(now).uppercase()

    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) level / scale.toFloat() else 1f
    } ?: 1f

    val online = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (_: Exception) { true }

    return SystemStatus(time, ampm, battery, online)
}

/** [unit] = altura da tela. Todos os tamanhos são proporcionais a ela, como no console. */
@Composable
fun SwitchStatusBar(unit: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readStatus(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            status = readStatus(context)
            delay(15_000)
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            status.time,
            color = SwitchColors.Ink,
            fontSize = (unit.value * 0.062f).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        if (status.ampm.isNotEmpty()) {
            Spacer(Modifier.width(unit * 0.004f))
            Text(status.ampm, color = SwitchColors.Ink, fontSize = (unit.value * 0.036f).sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(unit * 0.05f))
        Icon(
            if (status.online) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
            contentDescription = "Wi-Fi",
            tint = SwitchColors.Ink,
            modifier = Modifier.size(unit * 0.065f)
        )
        Spacer(Modifier.width(unit * 0.04f))
        BatteryIcon(status.battery, Modifier.size(unit * 0.088f, unit * 0.042f))
    }
}

@Composable
fun BatteryIcon(level: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.height * 0.14f
        val nubW = size.width * 0.08f
        val bodyW = size.width - nubW
        val r = size.height * 0.22f
        drawRoundRect(
            SwitchColors.Ink,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyW - stroke, size.height - stroke),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(stroke)
        )
        val pad = stroke * 1.9f
        val fillW = (bodyW - pad * 2) * level.coerceIn(0f, 1f)
        drawRoundRect(
            SwitchColors.Ink,
            topLeft = Offset(pad, pad),
            size = Size(fillW, size.height - pad * 2),
            cornerRadius = CornerRadius(r / 2, r / 2)
        )
        drawRoundRect(
            SwitchColors.Ink,
            topLeft = Offset(bodyW, size.height * 0.3f),
            size = Size(nubW, size.height * 0.4f),
            cornerRadius = CornerRadius(nubW / 2, nubW / 2)
        )
    }
}

/* ------------------------------------------------------------------ */
/* Avatar do usuário (canto superior esquerdo)                          */
/* ------------------------------------------------------------------ */

@Composable
fun SwitchAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    image: ImageBitmap? = null,
    onClick: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val interactive = if (onClick != null) {
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .gamepadA(onClick)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    } else Modifier
    Box(
        modifier
            .size(size)
            .then(interactive)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF7FDDEB))
            .border(if (focused) size * 0.06f else size * 0.03f, if (focused) SwitchColors.Cyan else Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/* ------------------------------------------------------------------ */
/* Moldura do jogo (borda ciano pulsante quando selecionado)            */
/* ------------------------------------------------------------------ */

@Composable
fun SwitchTileFrame(
    size: Dp,
    focused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "tile-pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.55f, targetValue = 1f, label = "glow",
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
    )
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(120), label = "tile-scale")
    val border = size * 0.028f
    Box(
        modifier
            .size(size)
            .zIndex(if (focused) 1f else 0f)
            .scale(scale)
            .shadow(if (focused) 10.dp else 3.dp, RoundedCornerShape(2.dp))
            .background(SwitchColors.Card)
    ) {
        content()
        if (focused) {
            Box(
                Modifier
                    .size(size)
                    .border(border, SwitchColors.Cyan.copy(alpha = glow), RoundedCornerShape(2.dp))
                    .border(border * 0.35f, Color.White, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun SwitchEmptySlot(size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val ring = if (focused) SwitchColors.Cyan else Color.White
    Box(
        modifier
            .size(size)
            .onFocusChanged { focused = it.isFocused }
            .gamepadA(onClick)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .background(SwitchColors.EmptySlot)
            .border(if (focused) size * 0.028f else size * 0.01f, ring, RoundedCornerShape(2.dp))
    )
}

/* ------------------------------------------------------------------ */
/* Botões circulares da fileira inferior                                */
/* ------------------------------------------------------------------ */

@Composable
fun SwitchCircleButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    diameter: Dp,
    labelSize: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusedChange: (Boolean) -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val ringWidth by animateDpAsState(if (focused) diameter * 0.055f else 0.dp, tween(100), label = "ring")
    val outer = diameter * 1.24f
    Box(
        modifier
            .size(outer)
            .onFocusChanged { focused = it.isFocused; onFocusedChange(it.isFocused) }
            .gamepadA(onClick)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Border com largura 0 desenha um fio fino; só desenha o anel quando há largura.
        if (ringWidth > 0.1.dp) {
            Box(
                Modifier
                    .size(outer)
                    .border(ringWidth, SwitchColors.Teal, CircleShape)
            )
        }
        Box(
            Modifier
                .size(diameter)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(diameter * 0.5f))
        }
        if (focused) {
            Text(
                label,
                color = SwitchColors.Teal,
                fontSize = labelSize.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .wrapContentWidth(unbounded = true)
                    .offset(y = diameter * 0.43f)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Rodapé: controle à esquerda, dicas de botões à direita               */
/* ------------------------------------------------------------------ */

data class ButtonHint(val key: String, val label: String)

@Composable
fun SwitchBottomBar(
    unit: Dp,
    hints: List<ButtonHint>,
    modifier: Modifier = Modifier,
    contentColor: Color = SwitchColors.Ink,
    badgeBg: Color = SwitchColors.Ink,
    badgeFg: Color = Color(0xFFEBEBEB)
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Indicador do controle (ícone + 4 luzes de jogador)
        Column(verticalArrangement = Arrangement.spacedBy(unit * 0.006f)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(unit * 0.012f)
                        .background(if (i == 0) SwitchColors.Green else Color(0xFFBDBDBD))
                )
            }
        }
        Spacer(Modifier.width(unit * 0.012f))
        Icon(Icons.Rounded.SportsEsports, "Controle", tint = contentColor, modifier = Modifier.size(unit * 0.075f))
        Spacer(Modifier.weight(1f))
        hints.forEachIndexed { index, hint ->
            if (index > 0) Spacer(Modifier.width(unit * 0.05f))
            ButtonBadge(hint.key, unit * 0.045f, badgeBg, badgeFg)
            Spacer(Modifier.width(unit * 0.015f))
            Text(hint.label, color = contentColor, fontSize = (unit.value * 0.045f).sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Selo redondo com a letra do botão (A, B, X, Y, L, R, +). A letra é desenhada medindo o contorno real do glifo,
 * então fica exatamente no centro do círculo (um Text comum deixa a letra mais baixa por causa da altura de linha).
 */
@Composable
fun ButtonBadge(letter: String, diameter: Dp, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val bounds = remember { android.graphics.Rect() }
    Canvas(modifier.size(diameter)) {
        drawCircle(bg)
        paint.color = fg.toArgb()
        paint.textSize = this.size.minDimension * (if (letter.length > 1) 0.42f else 0.58f)
        paint.getTextBounds(letter, 0, letter.length, bounds)
        drawContext.canvas.nativeCanvas.drawText(letter, center.x, center.y - bounds.exactCenterY(), paint)
    }
}

/* ------------------------------------------------------------------ */
/* Página em tela cheia (Configurações, Personalizar, Buscar capa…)     */
/* ------------------------------------------------------------------ */

/** Posições do rodapé, compartilhadas com a tela inicial (frações da altura da tela). */
const val LINE_TOP = 0.900f
const val BOTTOM_TOP = 0.925f

/**
 * Página que ocupa a janela inteira da Activity (já em modo imersivo), então nunca fica menor que a tela
 * como acontecia com Dialog. Título no topo, conteúdo no meio e rodapé com dicas de botões.
 */
@Composable
fun SwitchPage(
    title: String,
    hints: List<ButtonHint>,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    content: @Composable BoxScope.(Dp) -> Unit
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(SwitchColors.Bg)
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        val u = maxHeight
        Column(Modifier.fillMaxSize().padding(start = u * 0.07f, end = u * 0.07f, top = u * 0.04f, bottom = u * 0.11f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(title, color = SwitchColors.Ink, fontSize = (u.value * 0.052f).sp, fontWeight = FontWeight.Medium, maxLines = 1)
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        color = SwitchColors.Muted,
                        fontSize = (u.value * 0.03f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f).padding(start = u * 0.03f)
                    )
                }
            }
            Spacer(Modifier.height(u * 0.012f))
            Box(Modifier.fillMaxWidth().height(1.5.dp).background(SwitchColors.Line))
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = u * 0.02f)) { content(u) }
        }
        Box(
            Modifier.align(Alignment.TopStart)
                .padding(top = u * LINE_TOP, start = u * 0.023f, end = u * 0.023f)
                .fillMaxWidth().height(1.5.dp).background(SwitchColors.Line)
        )
        SwitchBottomBar(
            u, hints,
            Modifier.align(Alignment.TopStart).padding(top = u * BOTTOM_TOP, start = u * 0.07f, end = u * 0.087f).fillMaxWidth()
        )
    }
}

/* ------------------------------------------------------------------ */
/* Botão, item de lista e campo de texto no estilo do Switch            */
/* ------------------------------------------------------------------ */

@Composable
fun SwitchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val bg = if (primary) SwitchColors.Teal else Color.White
    val fg = if (primary) Color.White else SwitchColors.Ink
    Row(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .gamepadA { if (enabled) onClick() }
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(if (focused) 6.dp else 1.dp, shape)
            .background(bg, shape)
            .border(if (focused) 3.dp else 1.dp, if (focused) SwitchColors.Cyan else Color(0xFFD0D0D0), shape)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun SwitchListItem(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .gamepadA(onClick)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .background(if (selected || focused) Color.White else Color.Transparent, shape)
            .then(if (focused) Modifier.border(3.dp, SwitchColors.Cyan, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(22.dp).background(if (selected) SwitchColors.Teal else Color.Transparent))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = if (selected) SwitchColors.Teal else SwitchColors.Ink,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun switchTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SwitchColors.Cyan,
    unfocusedBorderColor = Color(0xFFBDBDBD),
    focusedLabelColor = SwitchColors.Teal,
    unfocusedLabelColor = SwitchColors.Muted,
    cursorColor = SwitchColors.Teal,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor = SwitchColors.Ink,
    unfocusedTextColor = SwitchColors.Ink
)
