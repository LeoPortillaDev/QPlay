package com.qplay.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------ */
/* Pedido de texto: qualquer tela abre o teclado do Switch por aqui      */
/* ------------------------------------------------------------------ */

/**
 * @param onDone chamado com o texto final quando o usuário confirma (OK). Cancelar não chama.
 */
data class KeyboardRequest(
    val title: String,
    val subtitle: String = "",
    val initial: String = "",
    val maxLength: Int = 40,
    val onDone: (String) -> Unit
)

/** A raiz do app fornece a função que abre o teclado; as páginas só usam [SwitchTextField]. */
val LocalSwitchKeyboard = compositionLocalOf<(KeyboardRequest) -> Unit> { { } }

/**
 * Campo de texto que NÃO usa o teclado do Android: ao tocar (ou apertar A) abre o [SwitchKeyboard].
 * [onSubmitted] roda depois que o texto é confirmado (ex.: disparar uma busca).
 */
@Composable
fun SwitchTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = label,
    subtitle: String = "",
    maxLength: Int = 40,
    onSubmitted: (() -> Unit)? = null
) {
    val openKeyboard = LocalSwitchKeyboard.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val open: () -> Unit = {
        openKeyboard(KeyboardRequest(title, subtitle, value, maxLength) { result ->
            onValueChange(result)
            onSubmitted?.invoke()
        })
    }
    Column(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .gamepadA(open)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = open)
            .background(Color.White, shape)
            .border(if (focused) 3.dp else 1.dp, if (focused) SwitchColors.Cyan else Color(0xFFBDBDBD), shape)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (focused) SwitchColors.Teal else SwitchColors.Muted, fontSize = 11.sp, maxLines = 1)
        Text(
            value.ifEmpty { "Toque para digitar" },
            color = if (value.isEmpty()) SwitchColors.Muted else SwitchColors.Ink,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------------------------------------------------------ */
/* Teclado em tela cheia (mesmo desenho do teclado do Switch)           */
/* ------------------------------------------------------------------ */

private enum class KbLayer { Abc, Sym, Acc }

private fun rowsFor(layer: KbLayer): List<String> = when (layer) {
    KbLayer.Abc -> listOf("1234567890=", "qwertyuiopº", "asdfghjklçª", "zxcvbnm,.-+")
    KbLayer.Sym -> listOf("!@#\$%&*()_=", "~`|\\{}[]<>^", "'\":;?/¿¡€£§", ",.-+×÷±°©®¶")
    KbLayer.Acc -> listOf("àáâãäåæçèéê", "ëìíîïòóôõöø", "ùúûüýÿñßœþð", "ńňšžčřłśźżğ")
}

private val KbHints = listOf(
    ButtonHint("L", "←"), ButtonHint("R", "→"),
    ButtonHint("B", "Apagar"), ButtonHint("X", "Cancelar"), ButtonHint("A", "Selecionar")
)

/**
 * Teclado do Switch. Toque ou direcional + A. Atalhos do controle: B apaga, Y espaço, X cancela,
 * + (Start) confirma, L/R movem o cursor, clique do analógico esquerdo = maiúscula.
 * Ocupa a janela inteira (não é Dialog) e nunca abre o teclado do Android.
 */
@Composable
fun SwitchKeyboard(request: KeyboardRequest, onClose: () -> Unit) {
    var text by remember { mutableStateOf(request.initial.take(request.maxLength)) }
    var cursor by remember { mutableIntStateOf(text.length) }
    var layer by remember { mutableStateOf(KbLayer.Abc) }
    var shift by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { first.requestFocus() } }

    fun insert(s: String) {
        if (text.length + s.length > request.maxLength) return
        text = text.substring(0, cursor) + s + text.substring(cursor)
        cursor += s.length
        shift = false
    }
    fun backspace() {
        if (cursor > 0) {
            text = text.removeRange(cursor - 1, cursor)
            cursor -= 1
        }
    }
    fun finish() {
        request.onDone(text.trim())
        onClose()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF12181D))
            .windowInsetsPadding(WindowInsets.displayCutout)
            .onKeyEvent { ev ->
                val k = ev.key
                val mapped = k == Key.ButtonB || k == Key.ButtonY || k == Key.ButtonX || k == Key.ButtonStart ||
                    k == Key.ButtonL1 || k == Key.ButtonR1 || k == Key.ButtonThumbL
                if (mapped && ev.type == KeyEventType.KeyUp) {
                    when (k) {
                        Key.ButtonB -> backspace()
                        Key.ButtonY -> insert(" ")
                        Key.ButtonX -> onClose()
                        Key.ButtonStart -> finish()
                        Key.ButtonL1 -> cursor = (cursor - 1).coerceAtLeast(0)
                        Key.ButtonR1 -> cursor = (cursor + 1).coerceAtMost(text.length)
                        else -> shift = !shift
                    }
                }
                mapped
            }
    ) {
        val u = maxHeight
        val keyH = u * 0.083f
        val gap = u * 0.007f
        val keyFont = (u.value * 0.05f).sp
        val smallFont = (u.value * 0.036f).sp
        val badge = u * 0.03f

        Column(Modifier.fillMaxSize()) {
            // Parte de cima: pergunta + campo com cursor e contador
            Column(Modifier.weight(1f).fillMaxWidth().padding(start = u * 0.11f, end = u * 0.11f, top = u * 0.06f)) {
                Text(request.title, color = Color.White, fontSize = (u.value * 0.045f).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (request.subtitle.isNotEmpty()) {
                    Text(request.subtitle, color = Color(0xFFA0A8B0), fontSize = (u.value * 0.032f).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(0.72f).align(Alignment.CenterHorizontally)) {
                    Row(Modifier.fillMaxWidth().height(u * 0.09f), verticalAlignment = Alignment.CenterVertically) {
                        Text(text.substring(0, cursor), color = Color.White, fontSize = (u.value * 0.05f).sp, maxLines = 1)
                        Box(Modifier.width(2.dp).height(u * 0.06f).background(Color(0xFF3D8BFF)))
                        Text(text.substring(cursor), color = Color.White, fontSize = (u.value * 0.05f).sp, maxLines = 1)
                    }
                    Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color.White))
                    Text(
                        "${text.length}/${request.maxLength}",
                        color = Color(0xFF8A9199), fontSize = (u.value * 0.03f).sp,
                        modifier = Modifier.align(Alignment.End).padding(top = u * 0.01f)
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            // Parte de baixo: teclas
            Column(
                Modifier.fillMaxWidth().background(Color(0xFF4A4A4A)).padding(horizontal = u * 0.04f, vertical = u * 0.014f),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Column(Modifier.weight(11f), verticalArrangement = Arrangement.spacedBy(gap)) {
                        rowsFor(layer).forEachIndexed { r, chars ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                chars.forEachIndexed { c, ch ->
                                    val out = (if (shift && layer != KbLayer.Sym) ch.uppercaseChar() else ch).toString()
                                    KbKey(
                                        modifier = Modifier.weight(1f).height(keyH),
                                        onClick = { insert(out) },
                                        label = out,
                                        fontSize = keyFont,
                                        focusRequester = if (r == 0 && c == 0) first else null
                                    )
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            KbKey(Modifier.weight(1f).height(keyH), { shift = !shift }, icon = Icons.Rounded.ArrowUpward, fontSize = keyFont, active = shift, dim = true)
                            KbKey(Modifier.weight(1f).height(keyH), { layer = KbLayer.Abc }, label = "ABC", fontSize = smallFont, active = layer == KbLayer.Abc, dim = true)
                            KbKey(Modifier.weight(1f).height(keyH), { layer = KbLayer.Sym }, label = "#+=", fontSize = smallFont, active = layer == KbLayer.Sym, dim = true)
                            KbKey(Modifier.weight(1f).height(keyH), { layer = KbLayer.Acc }, label = "ÀÁÂ", fontSize = smallFont, active = layer == KbLayer.Acc, dim = true)
                            KbKey(Modifier.weight(7f).height(keyH), { insert(" ") }, label = "Espaço", fontSize = smallFont, badge = "Y", badgeSize = badge)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                        KbKey(Modifier.fillMaxWidth().height(keyH), { backspace() }, icon = Icons.AutoMirrored.Rounded.Backspace, fontSize = keyFont, dim = true, badge = "B", badgeSize = badge)
                        KbKey(Modifier.fillMaxWidth().height(keyH * 2 + gap), { finish() }, label = "Enter", fontSize = smallFont, dim = true)
                        KbKey(Modifier.fillMaxWidth().height(keyH * 2 + gap), { finish() }, label = "OK", fontSize = smallFont, dim = true, badge = "+", badgeSize = badge)
                    }
                }
                SwitchBottomBar(
                    u, KbHints,
                    Modifier.fillMaxWidth().padding(horizontal = u * 0.01f),
                    contentColor = Color(0xFFEBEBEB), badgeBg = Color(0xFFEBEBEB), badgeFg = Color(0xFF4A4A4A)
                )
            }
        }
    }
}

@Composable
private fun KbKey(
    modifier: Modifier,
    onClick: () -> Unit,
    fontSize: TextUnit,
    label: String = "",
    icon: ImageVector? = null,
    active: Boolean = false,
    dim: Boolean = false,
    badge: String? = null,
    badgeSize: Dp = 14.dp,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(2.dp)
    val base = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
    Box(
        base
            .onFocusChanged { focused = it.isFocused }
            .gamepadA(onClick)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .background(if (focused) Color(0xFF2E3A4B) else if (dim) Color(0xFF555555) else Color(0xFF5E5E5E), shape)
            .then(if (focused) Modifier.border(3.dp, SwitchColors.Cyan, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, label, tint = if (active) SwitchColors.Teal else Color.White, modifier = Modifier.size(fontSize.value.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = if (active) SwitchColors.Teal else Color.White, fontSize = fontSize, fontWeight = FontWeight.Normal, maxLines = 1)
                if (active) Box(Modifier.width(24.dp).height(2.dp).background(SwitchColors.Teal))
            }
        }
        if (badge != null) {
            ButtonBadge(badge, badgeSize, Color(0xFFDDDDDD), Color(0xFF3A3A3A), Modifier.align(Alignment.TopEnd).padding(3.dp))
        }
    }
}
