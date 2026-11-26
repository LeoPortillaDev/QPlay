package com.qplay.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.qplay.launcher.ui.QPlayTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledGame(val name: String, val packageName: String, val isGame: Boolean)
private data class NupGame(val name: String, val uri: Uri, val fileName: String)

private val Bg = Color(0xFF0B0D12)
private val Panel = Color(0xFF171C26)
private val Panel2 = Color(0xFF202735)
private val Muted = Color(0xFF929BAD)
private val Accent = Color(0xFFE34B43)

class MainActivity : ComponentActivity() {
    private val peanutsPackages = listOf("dev.nxnative.android", "dev.bananative.android", "dev.nxnative.android.play")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.navigationBarColor = android.graphics.Color.rgb(11, 13, 18)
        setContent { QPlayTheme { QPlayApp() } }
    }

    private fun installedApps(): List<InstalledGame> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName && it.packageName != peanutsPackage() }
            .map { InstalledGame(packageManager.getApplicationLabel(it).toString(), it.packageName, it.category == android.content.pm.ApplicationInfo.CATEGORY_GAME) }
            .sortedWith(compareByDescending<InstalledGame> { it.isGame }.thenBy { it.name.lowercase() })
    }

    private fun peanutsPackage(): String? {
        val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return installed.firstOrNull { app ->
            val label = packageManager.getApplicationLabel(app).toString()
            app.packageName != packageName && (app.packageName in peanutsPackages || label.contains("Peanuts", true) || app.packageName.contains("peanuts", true))
        }?.packageName
    }

    private fun launchAndroidApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }

    private fun launchNup(game: NupGame) {
        val pkg = peanutsPackage()
        if (pkg == null) {
            android.widget.Toast.makeText(this, "Peanuts Native não foi encontrado.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(pkg, "dev.peanuts.launch.LaunchActivity")
            data = game.uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "O Peanuts não aceitou abrir este arquivo. Vamos validar a integração com esta versão.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun QPlayApp() {
        val prefs = remember { getSharedPreferences("qplay_library", MODE_PRIVATE) }
        var folderUri by remember { mutableStateOf(prefs.getString("rom_folder_uri", null)?.let(Uri::parse)) }
        var games by remember { mutableStateOf<List<NupGame>>(emptyList()) }
        var apps by remember { mutableStateOf(installedApps()) }
        var tab by remember { mutableStateOf("Biblioteca") }
        var scanning by remember { mutableStateOf(false) }
        var refreshToken by remember { mutableIntStateOf(0) }
        var settingsOpen by remember { mutableStateOf(false) }
        var selectedApps by remember { mutableStateOf(prefs.getStringSet("selected_apps", emptySet()).orEmpty()) }
        var scanMessage by remember { mutableStateOf("Escolha a pasta onde estão seus arquivos .nup") }
        val peanuts = peanutsPackage()

        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    folderUri = uri
                    prefs.edit().putString("rom_folder_uri", uri.toString()).apply()
                } catch (_: SecurityException) {
                    scanMessage = "Não foi possível guardar a permissão desta pasta."
                }
            }
        }

        LaunchedEffect(folderUri, refreshToken) {
            val root = folderUri
            if (root != null) {
                scanning = true
                scanMessage = "Procurando arquivos .nup…"
                val found = withContext(Dispatchers.IO) {
                    scanNupFolder(root)
                }
                games = found
                scanMessage = if (found.isEmpty()) "Nenhum arquivo .nup encontrado nesta pasta." else "${found.size} jogo(s) encontrado(s)"
                scanning = false
            } else {
                games = emptyList()
            }
        }

        BackHandler(enabled = settingsOpen || tab != "Biblioteca") { if (settingsOpen) settingsOpen = false else tab = "Biblioteca" }

        Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 42.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.foundation.Image(painterResource(com.qplay.launcher.R.drawable.qplay_logo), contentDescription = "QPlay", modifier = Modifier.size(48.dp))
                    Text("QPLAY", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.width(16.dp))
                Text("GAME LIBRARY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (peanuts != null) "PEANUTS CONECTADO" else "PEANUTS NÃO DETECTADO", color = if (peanuts != null) Color(0xFF8FC6A0) else Color(0xFFE2A09A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${games.size} JOGOS NA BIBLIOTECA", color = Muted, fontSize = 9.sp)
                }
                Box {
                    IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Rounded.Menu, "Menu", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                        DropdownMenuItem(text = { Text("Selecionar pasta dos jogos") }, leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }, onClick = { settingsOpen = false; folderPicker.launch(folderUri) })
                        DropdownMenuItem(text = { Text("Escolher aplicativos") }, leadingIcon = { Icon(Icons.Rounded.Apps, null) }, onClick = { settingsOpen = false; tab = "Aplicativos" })
                        DropdownMenuItem(text = { Text("Atualizar biblioteca") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { settingsOpen = false; refreshToken++; apps = installedApps() })
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (tab == "Biblioteca") "JOGAR" else "APLICATIVOS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text(if (tab == "Biblioteca") "Escolha seu próximo jogo" else "Selecione o que deseja ter na sua central", color = Muted, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Biblioteca", "Aplicativos").forEach { item -> FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item) }) }
                }
            }
            Spacer(Modifier.height(25.dp))
            if (tab == "Biblioteca") {
                if (scanning) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
                } else {
                    val libraryApps = apps.filter { it.packageName in selectedApps }
                    if (games.isEmpty() && libraryApps.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF596477), modifier = Modifier.size(58.dp))
                                Text("Sua biblioteca está esperando", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Adicione uma pasta de jogos ou escolha aplicativos no menu.", color = Muted, fontSize = 13.sp)
                                Button(onClick = { folderPicker.launch(folderUri) }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                                    Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Selecionar pasta")
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SEUS JOGOS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${games.size + libraryApps.size} ITENS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        var focusedGameName by remember { mutableStateOf(games.firstOrNull()?.name ?: libraryApps.firstOrNull()?.name.orEmpty()) }
                        Text(focusedGameName, color = Color(0xFF36BCEB), fontSize = 25.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 5.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                            rowItems(games, key = { it.uri.toString() }) { game -> SwitchGameCard(game.uri.toString(), game.name, prefs, { launchNup(game) }, { focusedGameName = it }) }
                            rowItems(libraryApps, key = { "app:" + it.packageName }) { app -> SwitchGameCard(app.packageName, app.name, prefs, { launchAndroidApp(app.packageName) }, { focusedGameName = it }) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Circle, null, tint = Accent, modifier = Modifier.size(7.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("A / ENTER  •  ABRIR JOGO", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(1f))
                            Text(folderUri?.lastPathSegment?.substringAfterLast(':') ?: "PASTA NÃO DEFINIDA", color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            } else {
                Text("Marque os aplicativos que deseja exibir junto com seus jogos. Clique no nome para abrir.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 230.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        SelectableAppTile(app, app.packageName in selectedApps, onToggle = {
                            selectedApps = if (app.packageName in selectedApps) selectedApps - app.packageName else selectedApps + app.packageName
                            prefs.edit().putStringSet("selected_apps", selectedApps).apply()
                        }, onLaunch = { launchAndroidApp(app.packageName) })
                    }
                }
            }
        }
    }

    @Composable
    private fun SwitchGameCard(key: String, originalName: String, prefs: android.content.SharedPreferences, onClick: () -> Unit, onFocusName: (String) -> Unit) {
        var focused by remember { mutableStateOf(false) }
        var editOpen by remember { mutableStateOf(false) }
        var title by remember(key) { mutableStateOf(prefs.getString("edit_name_" + key, originalName) ?: originalName) }
        var coverUri by remember(key) { mutableStateOf(prefs.getString("cover_" + key, null)?.let(Uri::parse)) }
        var draftTitle by remember { mutableStateOf(title) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); coverUri = uri; prefs.edit().putString("cover_" + key, uri.toString()).apply() }
                catch (_: Exception) { android.widget.Toast.makeText(this, "Não foi possível guardar esta imagem.", android.widget.Toast.LENGTH_SHORT).show() }
            }
        }
        val cover = produceState<ImageBitmap?>(null, coverUri) {
            value = withContext(Dispatchers.IO) { try { coverUri?.let { uri -> contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream) }?.asImageBitmap() } } catch (_: Exception) { null } }
        }
        Surface(color = if (focused) Color(0xFF343B47) else Color(0xFF292929), shape = RoundedCornerShape(3.dp), border = BorderStroke(if (focused) 4.dp else 2.dp, if (focused) Color(0xFF39C5E8) else Color(0xFF3A3A3A)), modifier = Modifier.width(220.dp).height(220.dp).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocusName(title) }.combinedClickable(onClick = onClick, onLongClick = { draftTitle = title; editOpen = true })) {
            Box(Modifier.fillMaxSize()) {
                if (cover.value != null) androidx.compose.foundation.Image(bitmap = cover.value!!, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF777777), modifier = Modifier.size(58.dp)) }
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0xCC111318)).padding(horizontal = 10.dp, vertical = 8.dp)) { Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
        if (editOpen) AlertDialog(onDismissRequest = { editOpen = false }, title = { Text("Editar jogo") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = draftTitle, onValueChange = { draftTitle = it }, label = { Text("Nome exibido") }, singleLine = true)
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Icon(Icons.Rounded.Image, null); Spacer(Modifier.width(8.dp)); Text(if (coverUri == null) "Escolher capa" else "Trocar capa") }
            }
        }, confirmButton = { TextButton(onClick = { title = draftTitle.ifBlank { originalName }; prefs.edit().putString("edit_name_" + key, title).apply(); editOpen = false; onFocusName(title) }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { editOpen = false }) { Text("Cancelar") } })
    }
    @Composable
    private fun SelectableAppTile(app: InstalledGame, selected: Boolean, onToggle: () -> Unit, onLaunch: () -> Unit) {
        Surface(color = if (selected) Color(0xFF26352F) else Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (selected) Color(0xFF78B58A) else Color(0xFF293140)), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(onClick = onLaunch)) {
                    Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (app.isGame) "JOGO ANDROID" else "APLICATIVO ANDROID", color = Muted, fontSize = 9.sp)
                }
                Checkbox(checked = selected, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF78B58A)))
            }
        }
    }

    private fun scanNupFolder(treeUri: Uri): List<NupGame> {
        val root = DocumentFile.fromTreeUri(this, treeUri) ?: return emptyList()
        val found = mutableListOf<NupGame>()
        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 8 || found.size >= 500) return
            dir.listFiles().forEach { file ->
                if (file.isDirectory) walk(file, depth + 1)
                else if (file.isFile && file.name?.substringAfterLast('.', "")?.equals("nup", true) == true) {
                    val filename = file.name ?: "Jogo .nup"
                    found.add(NupGame(filename.substringBeforeLast('.').replace('_', ' '), file.uri, filename))
                }
            }
        }
        walk(root, 0)
        return found.sortedBy { it.name.lowercase() }
    }

    @Composable
    private fun QPlayAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        Surface(onClick = onClick, color = if (focused) Accent else Panel2, shape = RoundedCornerShape(12.dp), border = if (focused) BorderStroke(2.dp, Color.White) else null, modifier = Modifier.onFocusChanged { focused = it.isFocused }.focusable()) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
            }
        }
    }

    @Composable
    private fun GameTile(game: NupGame, onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        Surface(onClick = onClick, color = if (focused) Color(0xFF303A4B) else Panel, shape = RoundedCornerShape(18.dp), border = if (focused) BorderStroke(2.dp, Accent) else BorderStroke(1.dp, Color(0xFF293140)), modifier = Modifier.fillMaxWidth().height(150.dp).onFocusChanged { focused = it.isFocused }.focusable()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(43.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF303B4D)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFFE0E5ED), modifier = Modifier.size(24.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(game.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(".NUP  •  TOQUE / A PARA ABRIR", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .4.sp)
                }
            }
        }
    }

    @Composable
    private fun AppTile(app: InstalledGame, onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        Surface(onClick = onClick, color = if (focused) Color(0xFF303A4B) else Panel, shape = RoundedCornerShape(16.dp), border = if (focused) BorderStroke(2.dp, Accent) else null, modifier = Modifier.height(86.dp).onFocusChanged { focused = it.isFocused }.focusable()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Apps, null, tint = Muted, modifier = Modifier.size(26.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (app.isGame) "JOGO ANDROID" else "APLICATIVO", color = Muted, fontSize = 9.sp)
                }
                Icon(Icons.Rounded.PlayArrow, null, tint = Accent)
            }
        }
    }
}
