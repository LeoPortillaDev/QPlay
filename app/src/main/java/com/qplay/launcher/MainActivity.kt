package com.qplay.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qplay.launcher.ui.QPlayTheme

private data class InstalledGame(val name: String, val packageName: String, val isGame: Boolean)
private data class PeanutsGame(val name: String, val titleId: String)

class MainActivity : ComponentActivity() {
    private val peanutsPackages = listOf(
        "dev.nxnative.android",
        "dev.bananative.android",
        "dev.nxnative.android.play"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContent { QPlayTheme { QPlayApp() } }
    }

    private fun installedApps(): List<InstalledGame> {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName && it.packageName !in peanutsPackages }
            .map {
                val label = pm.getApplicationLabel(it).toString()
                InstalledGame(label, it.packageName, it.category == android.content.pm.ApplicationInfo.CATEGORY_GAME)
            }
            .sortedWith(compareByDescending<InstalledGame> { it.isGame }.thenBy { it.name.lowercase() })
    }

    private fun peanutsPackage(): String? = peanutsPackages.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }

    private fun launchAndroidApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }

    private fun launchPeanuts(titleId: String) {
        val pkg = peanutsPackage() ?: return
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(pkg, "dev.peanuts.launch.LaunchActivity")
            putExtra("title_id", titleId.trim().uppercase())
        }
        try { startActivity(intent) } catch (_: Exception) {
            // The installed Peanuts build may not expose the documented launcher alias.
        }
    }

    @Composable
    private fun QPlayApp() {
        var apps by remember { mutableStateOf(installedApps()) }
        var peanuts by remember { mutableStateOf(loadPeanutsGames()) }
        var showAllApps by remember { mutableStateOf(false) }
        var showAddPeanuts by remember { mutableStateOf(false) }
        var gameName by remember { mutableStateOf("") }
        var titleId by remember { mutableStateOf("") }
        var tab by remember { mutableStateOf("Jogos") }
        val detectedPeanuts = peanutsPackage()

        BackHandler(enabled = showAddPeanuts) { showAddPeanuts = false }

        Column(Modifier.fillMaxSize().background(Color(0xFF10131B)).padding(horizontal = 30.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Q", color = Color(0xFFB3B9C8), fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("PLAY", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                Text("CENTRAL DE JOGOS", color = Color(0xFF8995AE), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = tab == "Jogos", onClick = { tab = "Jogos" }, label = { Text("Jogos") })
                FilterChip(selected = tab == "Peanuts", onClick = { tab = "Peanuts" }, label = { Text("Peanuts Native") })
                FilterChip(selected = tab == "Aplicativos", onClick = { tab = "Aplicativos" }, label = { Text("Aplicativos") })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { apps = installedApps(); peanuts = loadPeanutsGames() }) { Icon(Icons.Rounded.Refresh, "Atualizar", tint = Color.White) }
            }
            Spacer(Modifier.height(12.dp))

            when (tab) {
                "Jogos" -> {
                    val games = apps.filter { it.isGame }
                    Text("JOGOS INSTALADOS", color = Color(0xFF8995AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    if (games.isEmpty()) EmptyState("Nenhum jogo identificado pelo Android", "Você pode adicionar jogos do Peanuts na aba correspondente.")
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(games, key = { it.packageName }) { app -> AppRow(app.name, app.packageName) { launchAndroidApp(app.packageName) } }
                    }
                }
                "Aplicativos" -> {
                    Text("APLICATIVOS INICIÁVEIS", color = Color(0xFF8995AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(apps, key = { it.packageName }) { app -> AppRow(app.name, app.packageName) { launchAndroidApp(app.packageName) } }
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PEANUTS NATIVE", color = Color(0xFF8995AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(if (detectedPeanuts == null) "Não detectado" else "Conectado: $detectedPeanuts", color = if (detectedPeanuts == null) Color(0xFFFFA0A0) else Color(0xFF9CC9A7), fontSize = 12.sp)
                        }
                        Button(onClick = { showAddPeanuts = true }, enabled = detectedPeanuts != null) {
                            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text("Adicionar jogo")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (peanuts.isEmpty()) EmptyState("Sua biblioteca Peanuts está vazia", "Adicione um jogo informando o nome e o Title ID. O QPlay tentará abri-lo pelo Peanuts Native.")
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(peanuts, key = { it.titleId }) {
                            AppRow(it.name, "TITLE ID: ${it.titleId}") { launchPeanuts(it.titleId) }
                        }
                    }
                    Text("A importação automática da biblioteca interna ainda está em investigação. Esta primeira etapa testa a abertura externa documentada pelo Peanuts.", color = Color(0xFF78849B), fontSize = 10.sp)
                }
            }
        }

        if (showAddPeanuts) AlertDialog(
            onDismissRequest = { showAddPeanuts = false },
            title = { Text("Adicionar jogo do Peanuts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = gameName, onValueChange = { gameName = it }, label = { Text("Nome do jogo") }, singleLine = true)
                    OutlinedTextField(value = titleId, onValueChange = { titleId = it.uppercase().filter { c -> c.isLetterOrDigit() } }, label = { Text("Title ID") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                    Text("O Title ID identifica o jogo dentro da biblioteca do Peanuts.", color = Color.Gray, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = titleId.trim().uppercase()
                    if (gameName.isNotBlank() && id.isNotBlank()) {
                        val updated = (peanuts.filterNot { it.titleId == id } + PeanutsGame(gameName.trim(), id))
                        savePeanutsGames(updated)
                        peanuts = updated
                        showAddPeanuts = false
                        gameName = ""; titleId = ""
                    }
                }, enabled = gameName.isNotBlank() && titleId.isNotBlank()) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showAddPeanuts = false }) { Text("Cancelar") } }
        )
    }

    private fun loadPeanutsGames(): List<PeanutsGame> {
        val prefs = getSharedPreferences("qplay_library", MODE_PRIVATE)
        return prefs.getStringSet("peanuts_entries", emptySet()).orEmpty().mapNotNull { row ->
            val split = row.split("|", limit = 2)
            if (split.size == 2) PeanutsGame(split[0], split[1]) else null
        }.sortedBy { it.name.lowercase() }
    }

    private fun savePeanutsGames(items: List<PeanutsGame>) {
        getSharedPreferences("qplay_library", MODE_PRIVATE).edit()
            .putStringSet("peanuts_entries", items.map { "${it.name}|${it.titleId}" }.toSet()).apply()
    }

    @Composable private fun AppRow(name: String, detail: String, onClick: () -> Unit) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF1D2331)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF303B52)), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(detail, color = Color(0xFF8995AE), fontSize = 10.sp, maxLines = 1)
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFFB8C4E0))
        }
    }

    @Composable private fun EmptyState(title: String, message: String) {
        Column(Modifier.fillMaxWidth().padding(top = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF65718A), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(message, color = Color(0xFF8995AE), fontSize = 12.sp)
        }
    }
}
