package com.qplay.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qplay.launcher.ui.ButtonHint
import com.qplay.launcher.ui.LocalUiSound
import com.qplay.launcher.ui.SwitchButton
import com.qplay.launcher.ui.SwitchColors
import com.qplay.launcher.ui.SwitchListItem
import com.qplay.launcher.ui.SwitchPage
import com.qplay.launcher.ui.SwitchTextField
import com.qplay.launcher.ui.SwitchTileFrame
import com.qplay.launcher.ui.gamepadA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** App instalado que pode aparecer na biblioteca. */
data class InstalledApp(val name: String, val packageName: String, val isGame: Boolean)

private val BackOk = listOf(ButtonHint("B", "Voltar"), ButtonHint("A", "OK"))

/* ================================================================== */
/* Configurações                                                        */
/* ================================================================== */

private val SettingsCategories = listOf("Perfil", "Aparência", "Biblioteca", "Imagens", "Controles", "Sistema")
private val AccentChoices = listOf(Color(0xFF35BCEB), Color(0xFF8B72E8), Color(0xFF56C58A), Color(0xFFE6A64A), Color(0xFFE45B68))

@Composable
fun SettingsPage(
    category: String,
    onCategory: (String) -> Unit,
    displayName: String,
    onDisplayName: (String) -> Unit,
    avatar: ImageBitmap?,
    onPickAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onAccent: (Color?) -> Unit,
    darkMode: Boolean,
    onDarkMode: (Boolean) -> Unit,
    folderLabel: String,
    onPickFolder: () -> Unit,
    onApps: () -> Unit,
    onRefresh: () -> Unit,
    libraryFilter: String,
    onFilter: (String) -> Unit,
    onClearCatalog: () -> Unit,
    peanutsStatus: String,
    version: String,
    uiSoundsEnabled: Boolean,
    onUiSoundsEnabled: (Boolean) -> Unit,
    uiSoundVolume: Float,
    onUiSoundVolume: (Float) -> Unit
) {
    SwitchPage(title = "Configurações", subtitle = "Switch $version", hints = BackOk) { u ->
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(80); runCatching { first.requestFocus() } }
        val body = (u.value * 0.032f).sp
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(u * 0.62f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(u * 0.012f)
            ) {
                SettingsCategories.forEachIndexed { i, c ->
                    SwitchListItem(c, c == category, { onCategory(c) }, if (i == 0) Modifier.focusRequester(first) else Modifier)
                }
            }
            Spacer(Modifier.width(u * 0.04f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(SwitchColors.Border))
            Spacer(Modifier.width(u * 0.04f))
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(u * 0.025f)
            ) {
                Text(category, color = SwitchColors.Ink, fontSize = (u.value * 0.045f).sp, fontWeight = FontWeight.Medium)
                when (category) {
                    "Perfil" -> {
                        Text("Personalize sua identidade no Switch.", color = SwitchColors.Muted, fontSize = body)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u * 0.04f)) {
                            Box(
                                Modifier.size(u * 0.2f).shadow(2.dp, CircleShape).clip(CircleShape).background(Color(0xFF7FDDEB))
                                    .border(3.dp, SwitchColors.Surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatar != null) Image(bitmap = avatar, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(u * 0.11f))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(u * 0.02f)) {
                                SwitchButton("Escolher foto", onPickAvatar, icon = Icons.Rounded.Image)
                                if (avatar != null) SwitchButton("Remover foto", onRemoveAvatar, icon = Icons.Rounded.Delete)
                            }
                        }
                        SwitchTextField(
                            label = "Nome do perfil", value = displayName, onValueChange = onDisplayName,
                            title = "Insira um apelido para este usuário.", subtitle = "Isto pode ser alterado a qualquer momento.",
                            maxLength = 16, modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                    "Aparência" -> {
                        Text("Escolha o tema e a cor de destaque do Switch.", color = SwitchColors.Muted, fontSize = body)
                        Row(horizontalArrangement = Arrangement.spacedBy(u * 0.02f), verticalAlignment = Alignment.CenterVertically) {
                            SwitchButton("Modo claro", { onDarkMode(false) }, primary = !darkMode)
                            SwitchButton("Modo escuro", { onDarkMode(true) }, primary = darkMode)
                        }
                        Text("Cor de destaque da seleção", color = SwitchColors.Muted, fontSize = body)
                        Row(horizontalArrangement = Arrangement.spacedBy(u * 0.03f), verticalAlignment = Alignment.CenterVertically) {
                            Swatch(null, SwitchColors.customAccent == null, u * 0.085f) { onAccent(null) }
                            AccentChoices.forEach { c -> Swatch(c, SwitchColors.customAccent == c, u * 0.085f) { onAccent(c) } }
                        }
                        Text("O primeiro círculo restaura as cores originais do Switch.", color = SwitchColors.Muted, fontSize = (u.value * 0.026f).sp)
                    }
                    "Biblioteca" -> {
                        Text("Pasta: $folderLabel", color = SwitchColors.Ink, fontSize = body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(u * 0.02f)) {
                            SwitchButton("Pasta de jogos", onPickFolder, icon = Icons.Rounded.FolderOpen)
                            SwitchButton("Aplicativos", onApps, icon = Icons.Rounded.Apps)
                        }
                        SwitchButton("Atualizar biblioteca", onRefresh, icon = Icons.Rounded.Refresh)
                        Text("Mostrar na tela inicial", color = SwitchColors.Muted, fontSize = body)
                        Row(horizontalArrangement = Arrangement.spacedBy(u * 0.02f)) {
                            listOf("Todos", "Favoritos", "Grupos").forEach { f ->
                                SwitchButton(f, { onFilter(f) }, primary = libraryFilter == f)
                            }
                        }
                    }
                    "Imagens" -> {
                        Text(
                            "As capas vêm do catálogo público de ícones do Switch. Segure um jogo na tela inicial, abra Personalizar e use Buscar capa.",
                            color = SwitchColors.Muted, fontSize = body
                        )
                        SwitchButton("Limpar cache do catálogo", onClearCatalog, icon = Icons.Rounded.Delete)
                    }
                    "Controles" -> Text(
                        "Direcional navega, A abre e B volta. Mapeamento de botões e atalhos serão configurados aqui.",
                        color = SwitchColors.Muted, fontSize = body
                    )
                    else -> {
                        Text(peanutsStatus, color = SwitchColors.Ink, fontSize = body)
                        Text("Efeitos sonoros da interface", color = SwitchColors.Muted, fontSize = body)
                        SwitchButton(
                            if (uiSoundsEnabled) "Sons ligados" else "Sons desligados",
                            { onUiSoundsEnabled(!uiSoundsEnabled) },
                            primary = uiSoundsEnabled
                        )
                        Text("Volume dos efeitos: ${uiSoundVolume.toInt()}%", color = SwitchColors.Muted, fontSize = body)
                        Slider(
                            value = uiSoundVolume,
                            onValueChange = onUiSoundVolume,
                            valueRange = 0f..100f,
                            enabled = uiSoundsEnabled
                        )
                        Text("Cliques curtos ao navegar e confirmar, com volume ajustável.", color = SwitchColors.Muted, fontSize = (u.value * 0.026f).sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Swatch(color: Color?, selected: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val uiSound = LocalUiSound.current
    val activate = { uiSound(true); onClick() }
    val fill = color ?: Color(0xFF1FC6B5)
    Box(
        Modifier
            .size(size)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) uiSound(false) }
            .gamepadA(activate)
            .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = activate)
            .shadow(2.dp, CircleShape)
            .background(fill, CircleShape)
            .border(
                if (focused || selected) 4.dp else 2.dp,
                if (focused) SwitchColors.Cyan else if (selected) SwitchColors.Ink else SwitchColors.Surface,
                CircleShape
            )
    )
}

/* ================================================================== */
/* Personalizar jogo                                                    */
/* ================================================================== */

@Composable
fun EditPage(
    gameName: String,
    draftTitle: String,
    onDraftTitle: (String) -> Unit,
    draftGroup: String,
    onDraftGroup: (String) -> Unit,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    cover: ImageBitmap?,
    onSearchCover: () -> Unit,
    onGallery: () -> Unit,
    onRemoveCover: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedSection by remember { mutableStateOf("Informações do software") }
    val sections = listOf("Informações do software", "Nome do jogo", "Grupo / coleção", "Favoritos", "Capa do jogo")
    val firstSection = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { firstSection.requestFocus() } }

    SwitchPage(title = "Personalizar jogo", subtitle = gameName, hints = BackOk) { u ->
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = u * 0.025f, vertical = u * 0.012f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(draftTitle.ifBlank { gameName }, color = SwitchColors.Ink, fontSize = (u.value * 0.038f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("BIBLIOTECA DE JOGOS", color = SwitchColors.Muted, fontSize = (u.value * 0.019f).sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.weight(1f))
                CompactEditAction("Salvar", Icons.Rounded.Check, onSave, u, accent = true)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SwitchColors.Border))
            Row(
                Modifier.fillMaxSize().padding(horizontal = u * 0.025f, vertical = u * 0.014f),
                horizontalArrangement = Arrangement.spacedBy(u * 0.025f)
            ) {
                Column(
                    Modifier.weight(0.29f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(u * 0.006f)
                ) {
                    sections.forEachIndexed { index, section ->
                        SwitchListItem(section, selectedSection == section, { selectedSection = section },
                            if (index == 0) Modifier.focusRequester(firstSection) else Modifier)
                    }
                    Spacer(Modifier.height(u * 0.008f))
                    CompactEditAction("Cancelar", Icons.Rounded.Close, onCancel, u)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(SwitchColors.Border))
                Column(
                    Modifier.weight(0.71f).fillMaxHeight().verticalScroll(rememberScrollState())
                        .padding(start = u * 0.02f, end = u * 0.012f),
                    verticalArrangement = Arrangement.spacedBy(u * 0.018f)
                ) {
                    Text(selectedSection, color = SwitchColors.Teal, fontSize = (u.value * 0.038f).sp, fontWeight = FontWeight.Medium)
                    when (selectedSection) {
                        "Informações do software" -> {
                            Text("Resumo deste jogo na sua biblioteca.", color = SwitchColors.Muted, fontSize = (u.value * 0.026f).sp)
                            Row(
                                Modifier.fillMaxWidth().padding(top = u * 0.012f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(u * 0.022f)
                            ) {
                                Box(Modifier.size(u * 0.17f).clip(RoundedCornerShape(2.dp)).background(SwitchColors.Card), contentAlignment = Alignment.Center) {
                                    if (cover != null) Image(bitmap = cover, contentDescription = "Capa", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    else Icon(Icons.Rounded.SportsEsports, null, tint = SwitchColors.Muted, modifier = Modifier.size(u * 0.075f))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(u * 0.012f)) {
                                    Text(draftTitle.ifBlank { gameName }, color = SwitchColors.Ink, fontSize = (u.value * 0.033f).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(if (favorite) "Favorito" else "Não favoritado", color = SwitchColors.Muted, fontSize = (u.value * 0.025f).sp)
                                    Text(draftGroup.ifBlank { "Sem coleção" }, color = SwitchColors.Muted, fontSize = (u.value * 0.025f).sp)
                                }
                            }
                        }
                        "Nome do jogo" -> {
                            Text("Nome exibido na biblioteca.", color = SwitchColors.Muted, fontSize = (u.value * 0.026f).sp)
                            SwitchTextField(
                                label = "Nome do jogo", value = draftTitle, onValueChange = onDraftTitle,
                                title = "Insira o nome do jogo.", subtitle = "Este nome aparece na tela inicial.",
                                maxLength = 60, modifier = Modifier.fillMaxWidth(0.88f)
                            )
                        }
                        "Grupo / coleção" -> {
                            Text("Agrupe jogos usando o mesmo nome de coleção.", color = SwitchColors.Muted, fontSize = (u.value * 0.026f).sp)
                            SwitchTextField(
                                label = "Grupo / coleção", value = draftGroup, onValueChange = onDraftGroup,
                                title = "Insira o nome do grupo.", subtitle = "Ex.: Corrida ou Aventura.",
                                maxLength = 30, modifier = Modifier.fillMaxWidth(0.88f)
                            )
                        }
                        "Favoritos" -> {
                            CompactEditAction(
                                if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                                Icons.Rounded.Favorite,
                                { onFavorite(!favorite) }, u, accent = favorite
                            )
                        }
                        "Capa do jogo" -> {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(u * 0.055f)
                            ) {
                                Box(Modifier.size(u * 0.29f).clip(RoundedCornerShape(2.dp)).background(SwitchColors.Card), contentAlignment = Alignment.Center) {
                                    if (cover != null) Image(bitmap = cover, contentDescription = "Capa atual", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    else Icon(Icons.Rounded.SportsEsports, null, tint = SwitchColors.Muted, modifier = Modifier.size(u * 0.09f))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(u * 0.009f)) {
                                    CompactEditAction("Buscar capa", Icons.Rounded.Search, onSearchCover, u)
                                    CompactEditAction("Galeria", Icons.Rounded.Image, onGallery, u)
                                    if (cover != null) CompactEditAction("Remover capa", Icons.Rounded.Delete, onRemoveCover, u, danger = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactEditAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    unit: androidx.compose.ui.unit.Dp,
    accent: Boolean = false,
    danger: Boolean = false
) {
    val tint = when {
        danger -> Color(0xFFE05252)
        accent -> SwitchColors.Teal
        else -> SwitchColors.Ink
    }
    var focused by remember { mutableStateOf(false) }
    val uiSound = LocalUiSound.current
    val activate = { uiSound(true); onClick() }
    val shape = RoundedCornerShape(5.dp)
    Row(
        Modifier.onFocusChanged { focused = it.isFocused; if (it.isFocused) uiSound(false) }
            .clip(shape)
            .then(if (focused) Modifier.border(2.dp, SwitchColors.Cyan, shape) else Modifier)
            .gamepadA(activate)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = activate
            )
            .padding(horizontal = unit * 0.012f, vertical = unit * 0.008f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(unit * 0.009f)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(unit * 0.032f))
        Text(label, color = tint, fontSize = (unit.value * 0.026f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ================================================================== */
/* Buscar capa (catálogo de ícones do Switch)                           */
/* ================================================================== */

@Composable
fun CoversPage(
    gameName: String,
    query: String,
    onQuery: (String) -> Unit,
    busy: Boolean,
    status: String,
    results: List<CoverCatalog.Hit>,
    onSearch: () -> Unit,
    onApply: (CoverCatalog.Hit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val search = { focusManager.clearFocus(); onSearch() }
    SwitchPage(
        title = "Buscar capa", subtitle = gameName,
        hints = listOf(ButtonHint("B", "Voltar"), ButtonHint("A", "Aplicar capa"))
    ) { u ->
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(u * 0.015f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u * 0.02f), verticalAlignment = Alignment.CenterVertically) {
                SwitchTextField(
                    label = "Nome do jogo (em inglês dá mais resultados)", value = query, onValueChange = onQuery,
                    title = "Buscar capa", subtitle = "Digite o nome do jogo. Em inglês dá mais resultados.",
                    maxLength = 60, onSubmitted = onSearch, modifier = Modifier.weight(1f)
                )
                SwitchButton(if (busy) "Buscando…" else "Buscar", { search() }, icon = Icons.Rounded.Search, primary = true, enabled = query.isNotBlank() && !busy)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) {
                    CircularProgressIndicator(color = SwitchColors.Teal, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(status, color = SwitchColors.Muted, fontSize = (u.value * 0.03f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (results.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Image, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(u * 0.12f))
                        Text(if (busy) "Procurando capas…" else "As capas encontradas aparecem aqui", color = SwitchColors.Muted, fontSize = (u.value * 0.03f).sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(u * 0.29f),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = u * 0.02f, vertical = u * 0.025f),
                    horizontalArrangement = Arrangement.spacedBy(u * 0.035f),
                    verticalArrangement = Arrangement.spacedBy(u * 0.035f)
                ) {
                    items(results.size, key = { results[it].url }) { index ->
                        CoverResultTile(results[index]) { onApply(results[index]) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoverResultTile(hit: CoverCatalog.Hit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val uiSound = LocalUiSound.current
    val activate = { uiSound(true); onClick() }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "cover-scale")
    val preview by produceState<ImageBitmap?>(null, hit.url) {
        value = withContext(Dispatchers.IO) { CoverCatalog.loadBitmap(hit.url)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .zIndex(if (focused) 1f else 0f)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) uiSound(false) }
            .gamepadA(activate)
            .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = activate)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .shadow(if (focused) 10.dp else 3.dp, shape)
                .background(SwitchColors.Card)
                .then(if (focused) Modifier.border(4.dp, SwitchColors.Cyan, shape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            val p = preview
            if (p != null) Image(bitmap = p, contentDescription = hit.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF46567A), Color(0xFF1F2638)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.SportsEsports, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(34.dp)) }
        }
        Text(
            hit.name, color = if (focused) SwitchColors.Teal else SwitchColors.Ink, fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

/* ================================================================== */
/* Aplicativos (escolher o que aparece na tela inicial)                 */
/* ================================================================== */

@Composable
fun AppsPage(
    apps: List<InstalledApp>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onLaunch: (String) -> Unit
) {
    SwitchPage(
        title = "Aplicativos", subtitle = "Marque o que deseja ver na tela inicial. Segure para abrir o app.",
        hints = listOf(ButtonHint("B", "Voltar"), ButtonHint("A", "Marcar"))
    ) { u ->
        val first = remember { FocusRequester() }
        LaunchedEffect(apps.isNotEmpty()) { if (apps.isNotEmpty()) { delay(80); runCatching { first.requestFocus() } } }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 230.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps.size, key = { apps[it].packageName }) { index ->
                val app = apps[index]
                AppRow(
                    app, app.packageName in selected, { onToggle(app.packageName) }, { onLaunch(app.packageName) },
                    if (index == 0) Modifier.focusRequester(first) else Modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(app: InstalledApp, selected: Boolean, onToggle: () -> Unit, onLaunch: () -> Unit, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val uiSound = LocalUiSound.current
    val activate = { uiSound(true); onToggle() }
    val shape = RoundedCornerShape(4.dp)
    val borderColor = when {
        focused -> SwitchColors.Cyan
        selected -> SwitchColors.Teal
        else -> SwitchColors.Border
    }
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) uiSound(false) }
            .gamepadA(activate)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = activate,
                onLongClick = onLaunch
            )
            .background(SwitchColors.Surface, shape)
            .border(if (focused) 3.dp else 2.dp, borderColor, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.name, color = SwitchColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (app.isGame) "JOGO ANDROID" else "APLICATIVO ANDROID", color = SwitchColors.Muted, fontSize = 9.sp)
        }
        Checkbox(checked = selected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = SwitchColors.Teal))
    }
}
