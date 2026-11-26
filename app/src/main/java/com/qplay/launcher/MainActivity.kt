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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.qplay.launcher.ui.QPlayTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.FileOutputStream
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.coroutines.launch

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

    private fun playSoundEffect(effect: Int) { window.decorView.playSoundEffect(effect) }

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
        var settingsCategory by remember { mutableStateOf("Perfil") }

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
            val tileSize = (maxWidth * 0.19f).coerceIn(136.dp, 210.dp)
            val libraryApps = apps.filter { it.packageName in selectedApps }
            val visibleGames = games.filter { game -> when (libraryFilter) { "Favoritos" -> game.uri.toString() in favoriteKeys; "Grupos" -> !groupMap[game.uri.toString()].isNullOrBlank(); else -> true } }
            val visibleApps = libraryApps.filter { app -> when (libraryFilter) { "Favoritos" -> app.packageName in favoriteKeys; "Grupos" -> !groupMap[app.packageName].isNullOrBlank(); else -> true } }
            val totalItems = visibleGames.size + visibleApps.size

            Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = maxWidth * 0.025f, vertical = maxHeight * 0.025f)) {
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
                        if (settingsOpen) Dialog(
                            onDismissRequest = { settingsOpen = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                        ) {
                            BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
                                val compact = maxWidth < 820.dp || maxHeight < 460.dp
                                val sideWidth = if (compact) 132.dp else (maxWidth * 0.22f).coerceIn(180.dp, 230.dp)
                                val outerPad = if (compact) 8.dp else 16.dp
                                Column(Modifier.fillMaxSize().padding(horizontal = outerPad, vertical = if (compact) 8.dp else 12.dp)) {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Settings, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(if (compact) 22.dp else 28.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("CONFIGURAÇÕES DO QPLAY", color = Color.White, fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { settingsOpen = false }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("VOLTAR", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold, fontSize = if (compact) 11.sp else 13.sp) }
                                    }
                                    Divider(color = Color(0xFF777777))
                                    Row(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
                                        Column(Modifier.width(sideWidth).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            listOf("Perfil", "Aparência", "Biblioteca", "Imagens", "Emuladores", "Controles", "Sistema").forEach { category ->
                                                val active = settingsCategory == category
                                                Surface(onClick = { settingsCategory = category }, color = if (active) Color(0xFF252B30) else Color.Transparent, shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(Modifier.width(4.dp).height(if (compact) 34.dp else 40.dp).background(if (active) Color(0xFF35BCEB) else Color.Transparent))
                                                        Text(category, color = if (active) Color(0xFF35BCEB) else Color.White, fontSize = if (compact) 12.sp else 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp, vertical = if (compact) 7.dp else 9.dp))
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(if (compact) 8.dp else 14.dp))
                                        Divider(Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF484848))
                                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = if (compact) 10.dp else 18.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(settingsCategory.uppercase(), color = Color.White, fontSize = if (compact) 17.sp else 21.sp, fontWeight = FontWeight.Bold)
                                            when (settingsCategory) {
                                                "Perfil" -> {
                                                    Text("Personalize sua identidade no QPlay.", color = Muted, fontSize = if (compact) 12.sp else 15.sp)
                                                    OutlinedTextField(value = displayName, onValueChange = { displayName = it; prefs.edit().putString("display_name", it).apply() }, label = { Text("Nome do perfil") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                                    Text("A foto/avatar pode ser escolhida pelo botão de perfil na tela HOME.", color = Muted, fontSize = 11.sp)
                                                }
                                                "Aparência" -> {
                                                    Text("Escolha uma cor de destaque para a interface.", color = Muted)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        listOf(Color(0xFF35BCEB), Color(0xFF8B72E8), Color(0xFF56C58A), Color(0xFFE6A64A), Color(0xFFE45B68)).forEach { c ->
                                                            Surface(onClick = { prefs.edit().putLong("accent_color", c.value.toLong()).apply() }, color = c, shape = RoundedCornerShape(50), modifier = Modifier.size(if (compact) 30.dp else 38.dp), border = BorderStroke(2.dp, Color.White)) {}
                                                        }
                                                    }
                                                }
                                                "Biblioteca" -> {
                                                    Text("Gerencie as fontes da sua biblioteca.", color = Muted)
                                                    Button(onClick = { settingsOpen = false; folderPicker.launch(folderUri) }) { Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Selecionar pasta de jogos") }
                                                    Button(onClick = { settingsOpen = false; tab = "Aplicativos" }) { Icon(Icons.Rounded.Apps, null); Spacer(Modifier.width(8.dp)); Text("Escolher aplicativos") }
                                                    Button(onClick = { libraryFilter = "Todos"; settingsOpen = false }) { Text("Mostrar todos os jogos") }
                                                }
                                                "Imagens" -> {
                                                    Text("Busca de capas sem chave de API. A fonte Libretro oferece artes para vários consoles retrô; a disponibilidade varia por plataforma.", color = Muted)
                                                    Text("Use o editor de cada jogo para pesquisar e aplicar capas.", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                }
                                                "Emuladores" -> Text("Detecção e associação de emuladores serão configuradas aqui.", color = Muted)
                                                "Controles" -> Text("Mapeamento de botões, navegação e atalhos serão configurados aqui.", color = Muted)
                                                else -> Text("Sons, animações e comportamento de inicialização do QPlay.", color = Muted)
                                            }
                                        }
                                    }
                                    Divider(color = Color(0xFF777777))
                                    Row(Modifier.fillMaxWidth().heightIn(min = 30.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                        Text("B  VOLTAR", color = Color.White, fontSize = 10.sp, maxLines = 1)
                                        Spacer(Modifier.width(18.dp))
                                        Text("A  CONFIRMAR", color = Color.White, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
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
                        Text(focusedGameName.uppercase(), color = Color(0xFF35BCEB), fontSize = 25.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.weight(0.18f))
                        // A single continuous horizontal shelf; side tiles remain visible.
                        val shelfSidePadding = this@BoxWithConstraints.maxWidth * 0.045f
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = shelfSidePadding),
                            horizontalArrangement = Arrangement.spacedBy((tileSize * 0.055f).coerceAtLeast(8.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(tileSize + 42.dp).padding(vertical = 4.dp)
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
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(onClick = { tab = "Biblioteca" }, color = Color(0xFF252525), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF35BCEB))) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Icon(Icons.Rounded.ArrowBack, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(18.dp))
                                    Text("VOLTAR", color = Color(0xFF35BCEB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("APLICATIVOS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                Text("SELECIONE O QUE ENTRA NA SUA BIBLIOTECA", color = Muted, fontSize = 9.sp, letterSpacing = .5.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Marque os aplicativos que deseja exibir na central de jogos.", color = Muted, fontSize = 12.sp)
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
        var scraperOpen by remember { mutableStateOf(false) }
        var scraperQuery by remember { mutableStateOf(originalName) }
        var scraperSystem by remember { mutableStateOf("") }
        var scraperBusy by remember { mutableStateOf(false) }
        var scraperStatus by remember { mutableStateOf("") }
        var scraperResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        val scraperScope = rememberCoroutineScope()
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
        val tileScale by animateFloatAsState(if (focused) 1.07f else 1f, label = "tileScale")
        Surface(color = if (focused) Color(0xFF343434) else Color(0xFF292929), shape = RoundedCornerShape(3.dp), border = BorderStroke(if (focused) 4.dp else 2.dp, if (focused) Color(0xFF35BCEB) else Color(0xFF3A3A3A)), modifier = Modifier.width(tileSize).height(tileSize).graphicsLayer { scaleX = tileScale; scaleY = tileScale }.onFocusChanged { focused = it.isFocused; if (it.isFocused) { onFocusName(title); playSoundEffect(android.view.SoundEffectConstants.NAVIGATION_DOWN) } }.combinedClickable(onClick = { playSoundEffect(android.view.SoundEffectConstants.CLICK); onClick() }, onLongClick = { draftTitle = title; draftGroup = groupName; editOpen = true })) {
            Box(Modifier.fillMaxSize()) {
                if (cover.value != null) androidx.compose.foundation.Image(bitmap = cover.value!!, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF777777), modifier = Modifier.size(58.dp)) }
                if (isFavorite) Icon(Icons.Rounded.Favorite, "Favorito", tint = Color(0xFFE65B68), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }

        if (scraperOpen) Dialog(
            onDismissRequest = { scraperOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                val compact = maxWidth < 760.dp || maxHeight < 500.dp
                Surface(
                    color = Color(0xFF202020),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color(0xFF35BCEB)),
                    modifier = Modifier.fillMaxWidth(0.995f).fillMaxHeight(0.99f)
                ) {
                    Column(Modifier.fillMaxSize().padding(if (compact) 12.dp else 20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ImageSearch, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(if (compact) 22.dp else 28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("CENTRAL DE CAPAS", color = Color(0xFF35BCEB), fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Pesquise, visualize e escolha a arte do jogo", color = Muted, fontSize = if (compact) 10.sp else 12.sp)
                            }
                            TextButton(onClick = { scraperOpen = false }) { Text("FECHAR  ✕", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold) }
                        }
                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.fillMaxHeight().fillMaxWidth(if (compact) 0.38f else 0.31f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("JOGO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                OutlinedTextField(
                                    value = scraperQuery,
                                    onValueChange = { scraperQuery = it },
                                    label = { Text("Nome do jogo") },
                                    singleLine = false,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("PLATAFORMA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                var systemMenu by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { systemMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (scraperSystem.isBlank()) "Selecionar console ▾" else scraperSystem.replace("_", " "), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    DropdownMenu(expanded = systemMenu, onDismissRequest = { systemMenu = false }) {
                                        DropdownMenuItem(text = { Text("Nintendo Switch — não disponível no Libretro") }, enabled = false, onClick = {})
                                        listOf(
                                            "Nintendo_-_Nintendo_64", "Nintendo_-_Game_Boy_Advance",
                                            "Nintendo_-_Nintendo_Entertainment_System", "Nintendo_-_Super_Nintendo_Entertainment_System",
                                            "Nintendo_-_GameCube", "Nintendo_-_Wii", "Sony_-_PlayStation",
                                            "Sony_-_PlayStation_2", "Sega_-_Mega_Drive_-_Genesis", "Sega_-_Dreamcast"
                                        ).forEach { system ->
                                            DropdownMenuItem(text = { Text(system.replace("_", " ")) }, onClick = { scraperSystem = system; systemMenu = false })
                                        }
                                    }
                                }
                                Text("A plataforma não será escolhida aleatoriamente. Selecione o console correto para pesquisar no catálogo.", color = Muted, fontSize = 10.sp)
                                Button(
                                    enabled = scraperQuery.isNotBlank() && scraperSystem.isNotBlank() && !scraperBusy,
                                    onClick = {
                                        scraperBusy = true
                                        scraperStatus = "Consultando catálogo público…"
                                        scraperResults = emptyList()
                                        scraperScope.launch {
                                            try {
                                                val matches = withContext(Dispatchers.IO) {
                                                    val base = "https://api.github.com/repos/libretro-thumbnails/" + scraperSystem
                                                    fun getJson(url: String): String {
                                                        val conn = URL(url).openConnection() as HttpURLConnection
                                                        conn.connectTimeout = 15000
                                                        conn.readTimeout = 30000
                                                        conn.setRequestProperty("Accept", "application/vnd.github+json")
                                                        conn.setRequestProperty("User-Agent", "QPlay-Android")
                                                        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                                                        try {
                                                            val code = conn.responseCode
                                                            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                                                            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                                                            if (code !in 200..299) throw IllegalStateException("Catálogo respondeu HTTP $code")
                                                            return body
                                                        } finally { conn.disconnect() }
                                                    }
                                                    val repository = org.json.JSONObject(getJson(base))
                                                    val branch = repository.optString("default_branch").ifBlank { "master" }
                                                    val treeJson = org.json.JSONObject(getJson("$base/git/trees/$branch?recursive=1"))
                                                    val tree = treeJson.optJSONArray("tree") ?: JSONArray()
                                                    val query = scraperQuery.trim().lowercase()
                                                        .replace(Regex("[^a-z0-9 ]"), " ")
                                                        .replace(Regex("\\s+"), " ").trim()
                                                    val words = query.split(" ").filter { it.length > 1 }
                                                    val scored = mutableListOf<Triple<Int, String, String>>()
                                                    for (i in 0 until tree.length()) {
                                                        val item = tree.optJSONObject(i) ?: continue
                                                        val path = item.optString("path")
                                                        if (!path.startsWith("Named_Boxarts/")) continue
                                                        if (!path.substringAfterLast('/').contains('.')) continue
                                                        val name = path.substringAfterLast('/')
                                                        val clean = name.substringBeforeLast(".").lowercase()
                                                            .replace(Regex("[^a-z0-9 ]"), " ")
                                                            .replace(Regex("\\s+"), " ").trim()
                                                        val score = words.count { clean.contains(it) }
                                                        if (score > 0) {
                                                            val encodedPath = Uri.encode(path, "/")
                                                            val download = "https://raw.githubusercontent.com/libretro-thumbnails/$scraperSystem/$branch/$encodedPath"
                                                            scored.add(Triple(score, name, download))
                                                        }
                                                    }
                                                    scored.sortedWith(compareByDescending<Triple<Int, String, String>> { it.first }
                                                        .thenBy { it.second.length }).take(24).map { it.second to it.third }
                                                }
                                                scraperResults = matches
                                                scraperStatus = if (matches.isEmpty()) "Nenhuma capa encontrada neste console. Tente outro nome ou plataforma." else "${matches.size} capas encontradas."
                                            } catch (e: Exception) {
                                                scraperStatus = "Não foi possível consultar este catálogo. Verifique a conexão."
                                            }
                                            scraperBusy = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35BCEB))
                                ) { Text(if (scraperBusy) "PESQUISANDO…" else "PESQUISAR CAPAS", color = Color(0xFF101820), fontWeight = FontWeight.Bold) }
                                Text(scraperStatus, color = if (scraperStatus.startsWith("Não") || scraperStatus.startsWith("Nenhuma")) Color(0xFFFFC078) else Muted, fontSize = 10.sp)
                            }
                            Divider(Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF484848))
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("RESULTADOS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text("${scraperResults.size} imagens", color = Muted, fontSize = 10.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                if (scraperResults.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Collections, null, tint = Color(0xFF66717A), modifier = Modifier.size(42.dp))
                                            Text(if (scraperBusy) "Buscando imagens…" else "As capas encontradas aparecerão aqui", color = Muted, fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(if (compact) 88.dp else 112.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(scraperResults.size) { index ->
                                            val result = scraperResults[index]
                                            val preview = produceState<ImageBitmap?>(null, result.second) {
                                                value = withContext(Dispatchers.IO) {
                                                    try {
                                                        val c = URL(result.second).openConnection()
                                                        c.connectTimeout = 12000; c.readTimeout = 18000
                                                        BitmapFactory.decodeStream(c.getInputStream())?.asImageBitmap()
                                                    } catch (_: Exception) { null }
                                                }
                                            }
                                            Surface(
                                                onClick = {
                                                    scraperBusy = true
                                                    scraperStatus = "Baixando e aplicando capa…"
                                                    scraperScope.launch {
                                                        try {
                                                            val localUri = withContext(Dispatchers.IO) {
                                                                val c = URL(result.second).openConnection()
                                                                c.connectTimeout = 15000; c.readTimeout = 25000
                                                                val bitmap = BitmapFactory.decodeStream(c.getInputStream()) ?: throw IllegalStateException("Imagem inválida")
                                                                val file = File(filesDir, "qplay-cover-" + key.hashCode() + ".png")
                                                                FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                                                                Uri.fromFile(file)
                                                            }
                                                            coverUri = localUri
                                                            prefs.edit().putString("cover_" + key, localUri.toString()).apply()
                                                            scraperOpen = false
                                                            android.widget.Toast.makeText(this@MainActivity, "Capa aplicada ao jogo!", android.widget.Toast.LENGTH_SHORT).show()
                                                        } catch (_: Exception) { scraperStatus = "Não foi possível baixar esta imagem." }
                                                        scraperBusy = false
                                                    }
                                                },
                                                color = Color(0xFF292929),
                                                shape = RoundedCornerShape(9.dp),
                                                border = BorderStroke(1.dp, Color(0xFF454545))
                                            ) {
                                                Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(5.dp)).background(Color(0xFF171717)), contentAlignment = Alignment.Center) {
                                                        if (preview.value != null) androidx.compose.foundation.Image(bitmap = preview.value!!, contentDescription = result.first, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                        else Icon(Icons.Rounded.SportsEsports, null, tint = Muted, modifier = Modifier.size(28.dp))
                                                    }
                                                    Text(result.first.substringBeforeLast("."), color = Color.White, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Text("APLICAR CAPA", color = Color(0xFF35BCEB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("FONTE: LIBRETRO THUMBNAILS • SEM API KEY", color = Muted, fontSize = 9.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { scraperOpen = false }) { Text("VOLTAR", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        if (editOpen) Dialog(
            onDismissRequest = { editOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                val compactEdit = maxWidth < 760.dp || maxHeight < 440.dp
                Surface(
                    color = Color(0xFF202020),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color(0xFF35BCEB)),
                    modifier = Modifier.fillMaxWidth(0.995f).fillMaxHeight(0.99f)
                ) {
                    Column(Modifier.fillMaxSize().padding(if (compactEdit) 12.dp else 20.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Tune, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(25.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("PERSONALIZAR JOGO", color = Color(0xFF35BCEB), fontSize = if (compactEdit) 16.sp else 21.sp, fontWeight = FontWeight.ExtraBold)
                                Text(title, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { editOpen = false }) { Text("FECHAR  ✕", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold) }
                        }
                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column(Modifier.fillMaxHeight().fillMaxWidth(if (compactEdit) 0.34f else 0.30f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CAPA DO JOGO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Surface(color = Color(0xFF151515), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFF454545)), modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (cover.value != null) androidx.compose.foundation.Image(bitmap = cover.value!!, contentDescription = "Capa atual", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp))
                                        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.SportsEsports, null, tint = Muted, modifier = Modifier.size(42.dp))
                                            Text("SEM CAPA", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                OutlinedButton(onClick = { scraperQuery = title; scraperOpen = true; scraperStatus = ""; scraperResults = emptyList() }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFF35BCEB)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                                    Icon(Icons.Rounded.Search, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("BUSCAR CAPA", color = Color(0xFF35BCEB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFF555555)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                                    Icon(Icons.Rounded.Image, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("GALERIA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Divider(Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF484848))
                            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("INFORMAÇÕES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                OutlinedTextField(value = draftTitle, onValueChange = { draftTitle = it }, label = { Text("Nome do jogo") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF35BCEB), focusedLabelColor = Color(0xFF35BCEB), cursorColor = Color(0xFF35BCEB)))
                                OutlinedTextField(value = draftGroup, onValueChange = { draftGroup = it }, label = { Text("Grupo / coleção") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF35BCEB), focusedLabelColor = Color(0xFF35BCEB), cursorColor = Color(0xFF35BCEB)))
                                Surface(color = Color(0xFF2D2D2D), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().clickable { favorite = !favorite }) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Favorite, null, tint = if (favorite) Color(0xFFE65B68) else Muted)
                                        Spacer(Modifier.width(9.dp))
                                        Text("Adicionar aos favoritos", color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                        Checkbox(checked = favorite, onCheckedChange = { favorite = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF35BCEB)))
                                    }
                                }
                                Text("As alterações serão salvas na biblioteca deste dispositivo.", color = Muted, fontSize = 10.sp)
                            }
                        }
                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { editOpen = false }, border = BorderStroke(1.dp, Color(0xFF555555))) { Text("CANCELAR", color = Color.White) }
                            Spacer(Modifier.width(10.dp))
                            Button(onClick = { title = draftTitle.ifBlank { originalName }; prefs.edit().putString("edit_name_" + key, title).apply(); groupName = draftGroup.trim(); onMetadataChanged(favorite, groupName); editOpen = false; onFocusName(title) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35BCEB))) { Text("SALVAR ALTERAÇÕES", color = Color(0xFF101820), fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }
        }
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
