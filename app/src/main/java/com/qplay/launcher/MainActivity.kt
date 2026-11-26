package com.qplay.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.content.IntentFilter
import android.os.BatteryManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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

private val Bg = Color(0xFF303030)
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
        window.navigationBarColor = android.graphics.Color.rgb(48, 48, 48)
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
        var apps by remember { mutableStateOf<List<InstalledGame>>(emptyList()) }
        var tab by remember { mutableStateOf("Biblioteca") }
        var scanning by remember { mutableStateOf(false) }
        var refreshToken by remember { mutableIntStateOf(0) }
        var settingsOpen by remember { mutableStateOf(false) }
        var selectedApps by remember { mutableStateOf(prefs.getStringSet("selected_apps", emptySet()).orEmpty()) }
        var scanMessage by remember { mutableStateOf("Escolha a pasta onde estão seus arquivos .nup") }
        var peanuts by remember { mutableStateOf<String?>(null) }
        var displayName by remember { mutableStateOf(prefs.getString("display_name", "") ?: "") }
        var nameDialog by remember { mutableStateOf(false) }
        var clock by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
        var battery by remember { mutableIntStateOf(100) }
        var favoriteKeys by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty()) }
        var groupMap by remember { mutableStateOf(prefs.all.filterKeys { it.startsWith("group_") }.mapKeys { it.key.removePrefix("group_") }.mapValues { it.value as? String ?: "" }) }
        var libraryFilter by remember { mutableStateOf("Todos") }

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

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) { val discoveredApps = installedApps(); val discoveredPeanuts = peanutsPackage(); withContext(Dispatchers.Main) { apps = discoveredApps; peanuts = discoveredPeanuts } }
        }
        LaunchedEffect(Unit) {
            while (true) {
                clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) battery = level * 100 / scale
                delay(30000)
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

        if (nameDialog) {
            var draftName by remember { mutableStateOf(displayName) }
            AlertDialog(onDismissRequest = { nameDialog = false }, title = { Text("Personalizar saudação") }, text = {
                OutlinedTextField(value = draftName, onValueChange = { draftName = it }, label = { Text("Como deseja ser chamado?") }, singleLine = true)
            }, confirmButton = { TextButton(onClick = { displayName = draftName.trim(); prefs.edit().putString("display_name", displayName).apply(); nameDialog = false }) { Text("Salvar") } },
                dismissButton = { TextButton(onClick = { nameDialog = false }) { Text("Cancelar") } })
        }

        BackHandler(enabled = settingsOpen || nameDialog || tab != "Biblioteca") { if (settingsOpen) settingsOpen = false else tab = "Biblioteca" }

        BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
            val tileSize = (maxWidth * 0.205f).coerceIn(148.dp, 226.dp)
            val libraryApps = apps.filter { it.packageName in selectedApps }
            val visibleGames = games.filter { game -> when (libraryFilter) { "Favoritos" -> game.uri.toString() in favoriteKeys; "Grupos" -> !groupMap[game.uri.toString()].isNullOrBlank(); else -> true } }
            val visibleApps = libraryApps.filter { app -> when (libraryFilter) { "Favoritos" -> app.packageName in favoriteKeys; "Grupos" -> !groupMap[app.packageName].isNullOrBlank(); else -> true } }
            val totalItems = visibleGames.size + visibleApps.size

            Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = maxWidth * 0.025f, vertical = maxHeight * 0.045f)) {
                // Top system strip: discreet brand, status and settings.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painterResource(com.qplay.launcher.R.drawable.qplay_logo),
                        contentDescription = "QPlay",
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Spacer(Modifier.width(12.dp))
                    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
                    Text(if (displayName.isBlank()) greeting else "$greeting, $displayName", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("$clock  •  $battery%", color = Color(0xFFE6E6E6), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(12.dp))

                    Box {
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Rounded.Settings, "Configurações", tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                        DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                            DropdownMenuItem(text = { Text("Selecionar pasta dos jogos") }, leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }, onClick = { settingsOpen = false; folderPicker.launch(folderUri) })
                            DropdownMenuItem(text = { Text("Escolher aplicativos") }, leadingIcon = { Icon(Icons.Rounded.Apps, null) }, onClick = { settingsOpen = false; tab = "Aplicativos" })
                            DropdownMenuItem(text = { Text("Meu nome / saudação") }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, onClick = { settingsOpen = false; nameDialog = true })
                            DropdownMenuItem(text = { Text("Todos os jogos") }, onClick = { settingsOpen = false; libraryFilter = "Todos" })
                            DropdownMenuItem(text = { Text("Favoritos") }, leadingIcon = { Icon(Icons.Rounded.Favorite, null) }, onClick = { settingsOpen = false; libraryFilter = "Favoritos" })
                            DropdownMenuItem(text = { Text("Grupos") }, leadingIcon = { Icon(Icons.Rounded.Folder, null) }, onClick = { settingsOpen = false; libraryFilter = "Grupos" })
                            DropdownMenuItem(text = { Text("Atualizar biblioteca") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { settingsOpen = false; refreshToken++; apps = emptyList() })
                        }
                    }
                }

                if (tab == "Biblioteca") {
                    if (scanning && totalItems == 0) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Accent)
                        }
                    } else if (totalItems == 0) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
                                Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF596477), modifier = Modifier.size(64.dp))
                                Text("Sua biblioteca está esperando", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                Text("Adicione uma pasta de jogos ou escolha aplicativos no menu.", color = Muted, fontSize = 13.sp)
                                Button(onClick = { folderPicker.launch(folderUri) }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                                    Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Selecionar pasta")
                                }
                            }
                        }
                    } else {
                        var focusedGameName by remember { mutableStateOf(games.firstOrNull()?.name ?: libraryApps.firstOrNull()?.name.orEmpty()) }
                        Spacer(Modifier.weight(0.35f))
                        Text(focusedGameName.uppercase(), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(0.18f))
                        // A single continuous horizontal shelf; side tiles remain visible.
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy((tileSize * 0.075f).coerceAtLeast(10.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(tileSize + 34.dp)
                        ) {
                            rowItems(visibleGames, key = { it.uri.toString() }) { game ->
                                val key = game.uri.toString()
                                SwitchGameCard(key, game.name, prefs, tileSize, key in favoriteKeys, groupMap[key].orEmpty(), visibleGames.firstOrNull()?.uri == game.uri,
                                    { launchNup(game) }, { focusedGameName = it }) { fav, group ->
                                    favoriteKeys = if (fav) favoriteKeys + key else favoriteKeys - key
                                    groupMap = groupMap + (key to group)
                                    prefs.edit().putStringSet("favorites", favoriteKeys).putString("group_" + key, group).apply()
                                }
                            }
                            rowItems(visibleApps, key = { "app:" + it.packageName }) { app ->
                                val key = app.packageName
                                SwitchGameCard(key, app.name, prefs, tileSize, key in favoriteKeys, groupMap[key].orEmpty(), visibleApps.firstOrNull()?.packageName == app.packageName,
                                    { launchAndroidApp(app.packageName) }, { focusedGameName = it }) { fav, group ->
                                    favoriteKeys = if (fav) favoriteKeys + key else favoriteKeys - key
                                    groupMap = groupMap + (key to group)
                                    prefs.edit().putStringSet("favorites", favoriteKeys).putString("group_" + key, group).apply()
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 24.dp)) {
                        Text("APLICATIVOS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Escolha quais aplicativos aparecem na sua central de jogos.", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(18.dp))
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
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun SwitchGameCard(key: String, originalName: String, prefs: android.content.SharedPreferences, tileSize: androidx.compose.ui.unit.Dp, isFavorite: Boolean, initialGroup: String, initiallySelected: Boolean, onClick: () -> Unit, onFocusName: (String) -> Unit, onMetadataChanged: (Boolean, String) -> Unit) {
        var focused by remember(key) { mutableStateOf(initiallySelected) }
        var favorite by remember(key) { mutableStateOf(isFavorite) }
        var draftGroup by remember(key) { mutableStateOf(initialGroup) }
        var groupName by remember(key) { mutableStateOf(initialGroup) }
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
        Surface(color = if (focused) Color(0xFF343434) else Color(0xFF292929), shape = RoundedCornerShape(3.dp), border = BorderStroke(if (focused) 4.dp else 2.dp, if (focused) Color(0xFF35BCEB) else Color(0xFF3A3A3A)), modifier = Modifier.width(tileSize).height(tileSize).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocusName(title) }.combinedClickable(onClick = onClick, onLongClick = { draftTitle = title; draftGroup = groupName; editOpen = true })) {
            Box(Modifier.fillMaxSize()) {
                if (cover.value != null) androidx.compose.foundation.Image(bitmap = cover.value!!, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF777777), modifier = Modifier.size(58.dp)) }
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0xCC111318)).padding(horizontal = 10.dp, vertical = 8.dp)) { Text(title, color = if (focused) Color(0xFF27B8E8) else Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                if (isFavorite) Icon(Icons.Rounded.Favorite, "Favorito", tint = Color(0xFFE65B68), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
        if (editOpen) AlertDialog(onDismissRequest = { editOpen = false }, title = { Text("Editar jogo") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = draftTitle, onValueChange = { draftTitle = it }, label = { Text("Nome exibido") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = favorite, onCheckedChange = { favorite = it })
                    Text("Adicionar aos favoritos")
                }
                OutlinedTextField(value = draftGroup, onValueChange = { draftGroup = it }, label = { Text("Grupo") }, singleLine = true)
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Icon(Icons.Rounded.Image, null); Spacer(Modifier.width(8.dp)); Text(if (coverUri == null) "Escolher capa" else "Trocar capa") }
            }
        }, confirmButton = { TextButton(onClick = { title = draftTitle.ifBlank { originalName }; prefs.edit().putString("edit_name_" + key, title).apply(); groupName = draftGroup.trim(); onMetadataChanged(favorite, groupName); editOpen = false; onFocusName(title) }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { editOpen = false }) { Text("Cancelar") } })
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
