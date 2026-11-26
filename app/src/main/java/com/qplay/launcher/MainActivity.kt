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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
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
import androidx.exifinterface.media.ExifInterface
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
        var settingsCategory by remember { mutableStateOf("Ícones Nintendo Switch") }
        var settingsQuery by remember { mutableStateOf("") }
        var settingsResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        var settingsBusy by remember { mutableStateOf(false) }
        var settingsStatus by remember { mutableStateOf("Escolha um jogo do Switch e pesquise o ícone oficial.") }
        var settingsTargetKey by remember { mutableStateOf("") }
        var settingsTargetMenu by remember { mutableStateOf(false) }
        val settingsScope = rememberCoroutineScope()
        var avatarUri by remember { mutableStateOf(prefs.getString("profile_avatar_uri", null)?.let(Uri::parse)) }
        val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    avatarUri = uri
                    prefs.edit().putString("profile_avatar_uri", uri.toString()).apply()
                } catch (_: SecurityException) {
                    android.widget.Toast.makeText(this, "Não foi possível guardar esta foto.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        val avatarBitmap = produceState<ImageBitmap?>(null, avatarUri) {
            value = withContext(Dispatchers.IO) {
                try {
                    avatarUri?.let { uri ->
                        val orientation = contentResolver.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL) } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        val decoded = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        if (decoded == null) null else {
                            val matrix = android.graphics.Matrix()
                            when (orientation) {
                                3 -> matrix.postRotate(180f)
                                6 -> matrix.postRotate(90f)
                                8 -> matrix.postRotate(270f)
                                2 -> matrix.preScale(-1f, 1f)
                                4 -> matrix.preScale(1f, -1f)
                                5 -> { matrix.preScale(-1f, 1f); matrix.postRotate(90f) }
                                7 -> { matrix.preScale(-1f, 1f); matrix.postRotate(270f) }
                            }
                            android.graphics.Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).asImageBitmap()
                        }
                    }
                } catch (_: Exception) { null }
            }
        }

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
                    Surface(
                        onClick = { avatarPicker.launch(arrayOf("image/*")) },
                        color = Color(0xFF171923),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF35BCEB)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (avatarBitmap.value != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = avatarBitmap.value!!,
                                    contentDescription = "Avatar do perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp))
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painterResource(com.qplay.launcher.R.drawable.qplay_logo),
                                    contentDescription = "QPlay",
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
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
                            val dialogView = LocalView.current
                            DisposableEffect(dialogView) {
                                val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
                                val decorView = dialogWindow?.decorView
                                val previousSystemUiVisibility = decorView?.systemUiVisibility
                                val previousWidth = dialogWindow?.attributes?.width
                                val previousHeight = dialogWindow?.attributes?.height
                                // Force the actual Android dialog window to match the available display,
                                // not just the Compose content hosted inside a potentially smaller window.
                                dialogWindow?.setLayout(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                if (decorView != null) {
                                    decorView.systemUiVisibility = (previousSystemUiVisibility ?: 0) or
                                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                }
                                onDispose {
                                    if (decorView != null && previousSystemUiVisibility != null) {
                                        decorView.systemUiVisibility = previousSystemUiVisibility
                                    }
                                    if (dialogWindow != null && previousWidth != null && previousHeight != null) {
                                        dialogWindow.setLayout(previousWidth, previousHeight)
                                    }
                                }
                            }
                            BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                                val compact = maxHeight < 520.dp || maxWidth < 850.dp
                                val selectedGame = games.firstOrNull { it.uri.toString() == settingsTargetKey } ?: games.firstOrNull()
                                Surface(
                                    modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f),
                                    color = Color(0xFF202020), shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(2.dp, Color(0xFF35BCEB))
                                ) {
                                    Column(Modifier.fillMaxSize().padding(if (compact) 10.dp else 18.dp)) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.ImageSearch, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(if (compact) 24.dp else 30.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("ÍCONES NINTENDO SWITCH", color = Color(0xFF35BCEB), fontSize = if (compact) 16.sp else 22.sp, fontWeight = FontWeight.ExtraBold)
                                                Text("Pesquise e aplique a arte oficial do jogo", color = Muted, fontSize = if (compact) 10.sp else 13.sp)
                                            }
                                            TextButton(onClick = { settingsOpen = false }) {
                                                Icon(Icons.Rounded.Close, null, tint = Color(0xFF35BCEB))
                                                Text("FECHAR", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(Modifier.weight(0.85f)) {
                                                OutlinedButton(onClick = { settingsTargetMenu = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
                                                    Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF35BCEB))
                                                    Spacer(Modifier.width(7.dp))
                                                    Text(selectedGame?.name ?: "Selecione um jogo", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(Icons.Rounded.ArrowDropDown, null, tint = Color.White)
                                                }
                                                DropdownMenu(expanded = settingsTargetMenu, onDismissRequest = { settingsTargetMenu = false }) {
                                                    games.forEach { game ->
                                                        DropdownMenuItem(text = { Text(game.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { settingsTargetKey = game.uri.toString(); settingsTargetMenu = false })
                                                    }
                                                }
                                            }
                                            OutlinedTextField(value = settingsQuery, onValueChange = { settingsQuery = it }, modifier = Modifier.weight(1.1f), singleLine = true, label = { Text("Nome do jogo") }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
                                            Button(
                                                enabled = settingsQuery.isNotBlank() && !settingsBusy,
                                                onClick = {
                                                    settingsBusy = true
                                                    settingsStatus = "Buscando no catálogo Nintendo Switch…"
                                                    settingsResults = emptyList()
                                                    settingsScope.launch {
                                                        try {
                                                            settingsResults = withContext(Dispatchers.IO) { searchSwitchCovers(settingsQuery) }
                                                            settingsStatus = if (settingsResults.isEmpty()) "Nenhum ícone encontrado. Confira o nome e tente novamente." else "${settingsResults.size} ícones encontrados. Selecione um para aplicar."
                                                        } catch (_: Exception) {
                                                            settingsStatus = "Não foi possível consultar o catálogo. Verifique a conexão e tente novamente."
                                                        } finally { settingsBusy = false }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35BCEB)),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                            ) {
                                                Icon(Icons.Rounded.Search, null, tint = Color(0xFF101820))
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (settingsBusy) "BUSCANDO…" else "BUSCAR", color = Color(0xFF101820), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(settingsStatus, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp, bottom = 8.dp))
                                        Divider(color = Color(0xFF555555))
                                        if (settingsResults.isEmpty()) {
                                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                                    Icon(Icons.Rounded.Collections, null, tint = Color(0xFF66717A), modifier = Modifier.size(48.dp))
                                                    Text(if (settingsBusy) "Consultando ícones…" else "Os ícones encontrados aparecerão aqui", color = Muted, fontSize = 13.sp)
                                                }
                                            }
                                        } else {
                                            LazyVerticalGrid(columns = GridCells.Adaptive(if (compact) 82.dp else 112.dp), modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                items(settingsResults.size) { index ->
                                                    val result = settingsResults[index]
                                                    val preview = produceState<ImageBitmap?>(null, result.second) {
                                                        value = withContext(Dispatchers.IO) {
                                                            try {
                                                                val c = URL(result.second).openConnection()
                                                                c.connectTimeout = 12000; c.readTimeout = 18000
                                                                BitmapFactory.decodeStream(c.getInputStream())?.asImageBitmap()
                                                            } catch (_: Exception) { null }
                                                        }
                                                    }
                                                    Surface(onClick = {
                                                        val targetKey = selectedGame?.uri?.toString()
                                                        if (targetKey.isNullOrBlank()) settingsStatus = "Adicione primeiro um jogo Nintendo Switch à biblioteca."
                                                        else {
                                                            settingsBusy = true
                                                            settingsScope.launch {
                                                                try {
                                                                    withContext(Dispatchers.IO) {
                                                                        val c = URL(result.second).openConnection()
                                                                        c.connectTimeout = 15000; c.readTimeout = 25000
                                                                        val bitmap = BitmapFactory.decodeStream(c.getInputStream()) ?: error("Imagem inválida")
                                                                        val file = File(filesDir, "switch-icon-${targetKey.hashCode()}.png")
                                                                        FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                                                                        prefs.edit().putString("cover_$targetKey", Uri.fromFile(file).toString()).apply()
                                                                    }
                                                                    refreshToken++
                                                                    settingsStatus = "Ícone aplicado a ${selectedGame.name}."
                                                                    settingsOpen = false
                                                                    android.widget.Toast.makeText(this@MainActivity, "Ícone aplicado!", android.widget.Toast.LENGTH_SHORT).show()
                                                                } catch (_: Exception) { settingsStatus = "Não foi possível baixar o ícone." }
                                                                finally { settingsBusy = false }
                                                            }
                                                        }
                                                    }, color = Color(0xFF292929), shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Color(0xFF454545))) {
                                                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF171717)), contentAlignment = Alignment.Center) {
                                                                if (preview.value != null) androidx.compose.foundation.Image(bitmap = preview.value!!, contentDescription = result.first, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                                else Icon(Icons.Rounded.SportsEsports, null, tint = Muted, modifier = Modifier.size(28.dp))
                                                            }
                                                            Text(result.first.substringBeforeLast("."), color = Color.White, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(13.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("APLICAR", color = Color(0xFF35BCEB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp))
                                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(7.dp))
                                            Text("CATÁLOGO DE ÍCONES NINTENDO SWITCH", color = Muted, fontSize = 9.sp)
                                            Spacer(Modifier.weight(1f))
                                            TextButton(onClick = { settingsOpen = false }) { Text("VOLTAR", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold) }
                                        }
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
                        // Align the title with the first visible game tile.
                        val shelfWidth = this@BoxWithConstraints.maxWidth * 0.95f
                        val shelfSpacing = (tileSize * 0.055f).coerceAtLeast(8.dp)
                        val shelfContentWidth = tileSize * totalItems.toFloat() + shelfSpacing * (totalItems - 1).toFloat()
                        val shelfSidePadding = if (shelfContentWidth <= shelfWidth) (shelfWidth - shelfContentWidth) / 2f else shelfWidth * 0.045f
                        Spacer(Modifier.weight(1f))
                        Text(focusedGameName.uppercase(), color = Color(0xFF35BCEB), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth().padding(start = shelfSidePadding))
                        Spacer(Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = shelfSidePadding),
                            horizontalArrangement = Arrangement.spacedBy(shelfSpacing),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(tileSize + 42.dp).padding(vertical = 4.dp)
                        ) {
                            rowItems(visibleGames, key = { it.uri.toString() }) { game ->
                                val key = game.uri.toString()
                                SwitchGameCard(key, game.name, prefs, tileSize, refreshToken, false, key in favoriteKeys, groupMap[key].orEmpty(), visibleGames.firstOrNull()?.uri == game.uri,
                                    { launchNup(game) }, { focusedGameName = it }) { fav, group ->
                                    favoriteKeys = if (fav) favoriteKeys + key else favoriteKeys - key
                                    groupMap = groupMap + (key to group)
                                    prefs.edit().putStringSet("favorites", favoriteKeys).putString("group_" + key, group).apply()
                                }
                            }
                            rowItems(visibleApps, key = { "app:" + it.packageName }) { app ->
                                val key = app.packageName
                                SwitchGameCard(key, app.name, prefs, tileSize, refreshToken, true, key in favoriteKeys, groupMap[key].orEmpty(), visibleApps.firstOrNull()?.packageName == app.packageName,
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
    private fun SwitchGameCard(key: String, originalName: String, prefs: android.content.SharedPreferences, tileSize: androidx.compose.ui.unit.Dp, coverRefresh: Int, isNativeApp: Boolean, isFavorite: Boolean, initialGroup: String, initiallySelected: Boolean, onClick: () -> Unit, onFocusName: (String) -> Unit, onMetadataChanged: (Boolean, String) -> Unit) {
        var focused by remember(key) { mutableStateOf(initiallySelected) }
        var favorite by remember(key) { mutableStateOf(isFavorite) }
        var draftGroup by remember(key) { mutableStateOf(initialGroup) }
        var groupName by remember(key) { mutableStateOf(initialGroup) }
        var title by remember(key) { mutableStateOf(prefs.getString("edit_name_" + key, originalName) ?: originalName) }
        var coverUri by remember(key, coverRefresh) { mutableStateOf(prefs.getString("cover_" + key, null)?.let(Uri::parse)) }

        val cover = produceState<ImageBitmap?>(null, coverUri, coverRefresh) {
            value = withContext(Dispatchers.IO) { try { coverUri?.let { uri -> contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream) }?.asImageBitmap() } } catch (_: Exception) { null } }
        }
        val nativeIcon = produceState<ImageBitmap?>(null, key, isNativeApp) {
            value = if (!isNativeApp) null else withContext(Dispatchers.IO) {
                try {
                    val drawable = packageManager.getApplicationIcon(key)
                    val w = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 192).coerceAtMost(512)
                    val h = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 192).coerceAtMost(512)
                    val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap.asImageBitmap()
                } catch (_: Exception) { null }
            }
        }
        val tileScale by animateFloatAsState(if (focused) 1.07f else 1f, label = "tileScale")
        Surface(color = if (focused) Color(0xFF343434) else Color(0xFF292929), shape = RoundedCornerShape(3.dp), border = BorderStroke(if (focused) 4.dp else 2.dp, if (focused) Color(0xFF35BCEB) else Color(0xFF3A3A3A)), modifier = Modifier.width(tileSize).height(tileSize).graphicsLayer { scaleX = tileScale; scaleY = tileScale }.onFocusChanged { focused = it.isFocused; if (it.isFocused) { onFocusName(title); playSoundEffect(android.view.SoundEffectConstants.NAVIGATION_DOWN) } }.combinedClickable(onClick = { playSoundEffect(android.view.SoundEffectConstants.CLICK); onClick() }, onLongClick = { playSoundEffect(android.view.SoundEffectConstants.CLICK) })) {
            Box(Modifier.fillMaxSize()) {
                val displayedIcon = cover.value ?: nativeIcon.value
                if (displayedIcon != null) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Image(bitmap = displayedIcon, contentDescription = title, contentScale = if (isNativeApp && cover.value == null) ContentScale.Fit else ContentScale.Crop, modifier = Modifier.fillMaxSize().padding(if (isNativeApp && cover.value == null) 18.dp else 0.dp))
                    }
                } else Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SportsEsports, null, tint = Color(0xFF777777), modifier = Modifier.size(58.dp)) }
                if (isFavorite) Icon(Icons.Rounded.Favorite, "Favorito", tint = Color(0xFFE65B68), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
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

    private fun searchSwitchCovers(queryText: String): List<Pair<String, String>> {
        val cacheFile = File(cacheDir, "switch-cover-catalog.json")
        fun downloadCatalog(): String {
            val connection = URL("https://raw.githubusercontent.com/Marxally/switchicons/main/data.json")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 45000
            connection.setRequestProperty("User-Agent", "QPlay-Android")
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                cacheFile.writeText(json)
                return json
            } finally {
                connection.disconnect()
            }
        }

        val catalogJson = if (cacheFile.exists() && cacheFile.length() > 0L) {
            if (System.currentTimeMillis() - cacheFile.lastModified() < 24 * 60 * 60 * 1000L) {
                cacheFile.readText()
            } else {
                try { downloadCatalog() } catch (_: Exception) { cacheFile.readText() }
            }
        } else {
            downloadCatalog()
        }

        fun normalize(value: String): String = java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[™®©]"), "")
            .replace(Regex("[^a-z0-9]+"), " ").trim()

        val query = normalize(queryText)
        val words = query.split(Regex("\\s+")).filter { it.length > 1 }
        val entries = org.json.JSONObject(catalogJson).optJSONArray("games") ?: return emptyList()
        val prefix = "https://img-eshop.cdn.nintendo.net/i/"
        return (0 until entries.length()).mapNotNull { index ->
            val item = entries.optJSONArray(index) ?: return@mapNotNull null
            val name = item.optString(1).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val iconRef = item.optString(2).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val normalizedName = normalize(name)
            val nameWords = normalizedName.split(Regex("\\s+")).filter { it.isNotBlank() }
            val matched = words.count { word -> nameWords.any { it == word || it.startsWith(word) } }
            val score = when {
                normalizedName == query -> 1000
                normalizedName.startsWith(query) && query.isNotBlank() -> 800 + matched
                normalizedName.contains(query) && query.isNotBlank() -> 600 + matched
                matched > 0 -> matched * 10
                else -> 0
            }
            if (score == 0) null else {
                val iconUrl = if (iconRef.startsWith("http", true)) iconRef else prefix + iconRef + ".jpg"
                Triple(score, name, iconUrl)
            }
        }.sortedWith(compareByDescending<Triple<Int, String, String>> { it.first }.thenBy { it.second.length })
            .take(40).map { it.second to it.third }
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