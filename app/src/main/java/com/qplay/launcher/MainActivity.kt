package com.qplay.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qplay.launcher.data.DemoGame
import com.qplay.launcher.data.DemoGames
import com.qplay.launcher.ui.QPlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContent { QPlayTheme { QPlayApp() } }
    }
}

private enum class Page { HOME, LIBRARY, SETTINGS }

@Composable
private fun QPlayApp() {
    var page by remember { mutableStateOf(Page.HOME) }
    var selected by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<DemoGame?>(null) }
    var showQuickMenu by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val games = DemoGames.items

    BackHandler(enabled = showExitConfirm || showQuickMenu || details != null || page != Page.HOME) {
        when {
            showExitConfirm -> showExitConfirm = false
            showQuickMenu -> showQuickMenu = false
            details != null -> details = null
            else -> page = Page.HOME
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF10131B))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 22.dp)) {
            Header(page, onHome = { page = Page.HOME }, onLibrary = { page = Page.LIBRARY }, onSettings = { page = Page.SETTINGS })
            Spacer(Modifier.height(26.dp))
            when (page) {
                Page.HOME -> HomeScreen(games, selected, onSelect = { selected = it }, onOpen = { details = games[selected] }, onLibrary = { page = Page.LIBRARY })
                Page.LIBRARY -> LibraryScreen(games, onSelect = { selected = it; details = games[it] })
                Page.SETTINGS -> SettingsScreen()
            }
            Spacer(Modifier.weight(1f))
            Footer(onMenu = { showQuickMenu = true })
        }

        if (details != null) GameDetails(details!!, onBack = { details = null }, onPlay = {
            // Prototype only: real shortcut launch integration is implemented after validating Peanuts shortcuts.
            showQuickMenu = true
        })

        if (showQuickMenu) QuickMenu(
            onContinue = { showQuickMenu = false },
            onSwitch = { showQuickMenu = false; details = null; page = Page.HOME },
            onClose = { showQuickMenu = false; showExitConfirm = true },
            onSettings = { showQuickMenu = false; page = Page.SETTINGS }
        )
        if (showExitConfirm) AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Encerrar jogo?") },
            text = { Text("No protótipo, a execução real ainda não está conectada. O encerramento será implementado após validar a integração com os atalhos do Peanuts.") },
            confirmButton = { TextButton(onClick = { showExitConfirm = false; details = null; page = Page.HOME }) { Text("Entendi") } },
            dismissButton = { TextButton(onClick = { showExitConfirm = false }) { Text("Voltar") } },
            containerColor = Color(0xFF252B39), titleContentColor = Color.White, textContentColor = Color(0xFFB8C1D1)
        )
    }
}

@Composable private fun Header(page: Page, onHome: () -> Unit, onLibrary: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Q", color = Color(0xFF8B9CFF), fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("PLAY", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(Modifier.width(42.dp))
        NavItem("Início", page == Page.HOME, onHome)
        NavItem("Biblioteca", page == Page.LIBRARY, onLibrary)
        NavItem("Configurações", page == Page.SETTINGS, onSettings)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF9BA7C2), modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(8.dp))
        Text("MODO CONTROLE", color = Color(0xFF9BA7C2), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable private fun NavItem(label: String, active: Boolean, onClick: () -> Unit) {
    Text(label, modifier = Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).background(if (active) Color(0xFF29334B) else Color.Transparent).padding(horizontal = 14.dp, vertical = 9.dp), color = if (active) Color.White else Color(0xFF8993A8), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
}

@Composable private fun HomeScreen(games: List<DemoGame>, selected: Int, onSelect: (Int) -> Unit, onOpen: () -> Unit, onLibrary: () -> Unit) {
    Text("SUA CENTRAL DE JOGOS", color = Color(0xFF8995AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    Text("Vamos jogar?", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(22.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(games) { index, game ->
            GameTile(game, selected == index, Modifier.width(174.dp).height(205.dp).clickable { onSelect(index) }.then(if (selected == index) Modifier.border(2.dp, Color(0xFF9BAAFF), RoundedCornerShape(18.dp)) else Modifier))
        }
        item { AddTile(onLibrary) }
    }
    Spacer(Modifier.height(22.dp))
    val game = games[selected]
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(game.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(game.subtitle, color = Color(0xFF9BA5B9), fontSize = 13.sp)
        }
        Button(onClick = onOpen, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8798FF), contentColor = Color(0xFF10131B)), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 25.dp, vertical = 14.dp)) {
            Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(7.dp)); Text("VER JOGO", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable private fun GameTile(game: DemoGame, selected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(game.color, game.color.copy(alpha = .55f), Color(0xFF202638)))).padding(14.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(game.symbol, fontSize = 44.sp)
            Column {
                Text(game.genre.uppercase(), color = Color.White.copy(alpha = .72f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(game.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            }
        }
        if (selected) Box(Modifier.align(Alignment.TopEnd).size(9.dp).clip(RoundedCornerShape(50)).background(Color.White))
    }
}

@Composable private fun AddTile(onClick: () -> Unit) {
    Column(Modifier.width(140.dp).height(205.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1B202C)).border(1.dp, Color(0xFF343C50), RoundedCornerShape(18.dp)).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Add, null, tint = Color(0xFF9BAAFF), modifier = Modifier.size(34.dp)); Spacer(Modifier.height(10.dp)); Text("Adicionar", color = Color.White, fontWeight = FontWeight.Bold); Text("jogos", color = Color(0xFF8995AE), fontSize = 12.sp)
    }
}

@Composable private fun LibraryScreen(games: List<DemoGame>, onSelect: (Int) -> Unit) {
    Text("BIBLIOTECA", color = Color(0xFF8995AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Text("Todos os seus jogos", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        games.chunked(4).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { colIndex, game ->
                    val index = rowIndex * 4 + colIndex
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1D2331)).clickable { onSelect(index) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(game.symbol, fontSize = 29.sp); Spacer(Modifier.width(12.dp)); Column { Text(game.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1); Text(game.genre, color = Color(0xFF8995AE), fontSize = 11.sp) }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable private fun GameDetails(game: DemoGame, onBack: () -> Unit, onPlay: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xEE10131B)).padding(38.dp)) {
        Column(Modifier.align(Alignment.CenterStart).widthIn(max = 520.dp)) {
            Text("DETALHES DO JOGO", color = Color(0xFF9BAAFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(game.symbol, fontSize = 76.sp)
            Text(game.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(game.subtitle, color = Color(0xFF9BA5B9), fontSize = 15.sp)
            Spacer(Modifier.height(28.dp))
            Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8798FF), contentColor = Color(0xFF10131B))) { Icon(Icons.Rounded.PlayArrow, null); Text("  INICIAR JOGO", fontWeight = FontWeight.ExtraBold) }
            TextButton(onClick = onBack) { Text("Voltar à biblioteca", color = Color(0xFFB7C0D2)) }
            Text("DEMONSTRAÇÃO VISUAL — integração pendente", color = Color(0xFF78849B), fontSize = 10.sp)
        }
    }
}

@Composable private fun QuickMenu(onContinue: () -> Unit, onSwitch: () -> Unit, onClose: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xB9000000)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(350.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF202737)).border(1.dp, Color(0xFF3A465F), RoundedCornerShape(24.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("MENU QPLAY", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, letterSpacing = 1.sp)
            Text("CONTROLE DA SESSÃO", color = Color(0xFF8F9CB5), fontSize = 10.sp, letterSpacing = 1.sp)
            MenuButton("Continuar jogo", Icons.Rounded.PlayArrow, onContinue)
            MenuButton("Trocar de jogo", Icons.Rounded.GridView, onSwitch)
            MenuButton("Fechar jogo", Icons.Rounded.PowerSettingsNew, onClose, danger = true)
            MenuButton("Configurações", Icons.Rounded.Settings, onSettings)
            Text("A / Selecionar     B / Voltar", color = Color(0xFF8995AE), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        }
    }
}

@Composable private fun MenuButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (danger) Color(0xFF422B35) else Color(0xFF2B3549)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) Color(0xFFFF8B9A) else Color.White); Spacer(Modifier.width(12.dp)); Text(label, color = if (danger) Color(0xFFFFA5B0) else Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun SettingsScreen() {
    Text("CONFIGURAÇÕES", color = Color(0xFF8995AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Text("Personalize sua central", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    listOf("Aparência" to "Tema escuro e identidade QPlay", "Controles" to "Reconhecimento e navegação", "Biblioteca" to "Organização e capas", "Integrações" to "Atalhos e aplicativos compatíveis").forEach {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF1D2331)).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(it.first, color = Color.White, fontWeight = FontWeight.Bold); Text(it.second, color = Color(0xFF8995AE), fontSize = 12.sp) }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF8995AE))
        }
    }
}

@Composable private fun Footer(onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("QPLAY  •  PROTÓTIPO 0.1", color = Color(0xFF59647A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MENU RÁPIDO", color = Color(0xFF8995AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF29334B)).clickable(onClick = onMenu).padding(horizontal = 12.dp, vertical = 7.dp)) { Text("☰", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}
