package com.qplay.launcher

import android.content.ComponentName
import android.content.Intent
<<<<<<< HEAD
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.SoundEffectConstants
import android.view.WindowManager
=======
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.content.IntentFilter
import android.os.BatteryManager
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
<<<<<<< HEAD
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.qplay.launcher.ui.ButtonHint
import com.qplay.launcher.ui.QPlayTheme
import com.qplay.launcher.ui.SwitchAvatar
import com.qplay.launcher.ui.SwitchBottomBar
import com.qplay.launcher.ui.SwitchCircleButton
import com.qplay.launcher.ui.SwitchColors
import com.qplay.launcher.ui.SwitchEmptySlot
import com.qplay.launcher.ui.SwitchStatusBar
import com.qplay.launcher.ui.SwitchTileFrame
import com.qplay.launcher.ui.gamepadA
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

private data class NupGame(val name: String, val uri: Uri, val fileName: String)

/**
 * Um item da fileira de jogos: arquivo .nup (via Peanuts) ou app Android instalado.
 * [key] é a chave usada nas preferências (uri do arquivo ou nome do pacote) — mantida igual às versões
 * anteriores para não perder nomes, capas, favoritos e grupos já salvos.
 */
private data class Entry(val key: String, val name: String, val pkg: String? = null, val nup: NupGame? = null)

private enum class Screen { Home, Apps, Settings, Edit, Covers }

/*
 * Medidas da tela inicial: todas são frações da ALTURA da tela ("u"), tiradas de um
 * print da tela inicial do Switch. Assim o layout mantém a proporção em qualquer aparelho.
 */
private const val AVATAR_SIZE = 0.083f
private const val AVATAR_X = 0.083f
private const val AVATAR_Y = 0.048f
private const val STATUS_TOP = 0.062f
private const val STATUS_END = 0.094f
private const val TILE_SIZE = 0.356f
private const val TILE_TOP = 0.270f
private const val TILE_START = 0.149f
private const val TILE_GAP = 0.019f
private const val CAPTION_TOP = 0.648f
private const val DOCK_TOP = 0.685f
private const val DOCK_DIAMETER = 0.119f
private const val MIN_SLOTS = 8
=======
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
>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c

class MainActivity : ComponentActivity() {
    private val peanutsPackages = listOf("dev.nxnative.android", "dev.bananative.android", "dev.nxnative.android.play")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
<<<<<<< HEAD
        applyImmersive()
        window.navigationBarColor = android.graphics.Color.rgb(235, 235, 235)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        // Cor de destaque escolhida em Configurações > Aparência (formato salvo pelas versões anteriores).
        val prefs = getSharedPreferences("qplay_library", MODE_PRIVATE)
        val savedAccent = runCatching { prefs.getLong("accent_color", 0L) }.getOrDefault(0L)
        if (savedAccent != 0L) SwitchColors.setAccent(Color(savedAccent.toULong()))
        setContent { QPlayTheme { QPlayApp() } }
    }

    /** Tela cheia de verdade: esconde barra de status e de navegação. */
    @Suppress("DEPRECATION")
    private fun applyImmersive() {
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    // Seletor de arquivos, teclado e notificações fazem as barras voltarem: reaplica ao recuperar o foco.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    /* ------------------------------------------------------------------ */
    /* Integração com apps / Peanuts                                        */
    /* ------------------------------------------------------------------ */

    private fun installedApps(): List<InstalledApp> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val peanuts = peanutsPackage()
        return packageManager.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName && it.packageName != peanuts }
            .map { InstalledApp(packageManager.getApplicationLabel(it).toString(), it.packageName, it.category == android.content.pm.ApplicationInfo.CATEGORY_GAME) }
            .sortedWith(compareByDescending<InstalledApp> { it.isGame }.thenBy { it.name.lowercase() })
    }

    private fun peanutsPackage(): String? {
        val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return installed.firstOrNull { app ->
            val label = packageManager.getApplicationLabel(app).toString()
            app.packageName != packageName && (app.packageName in peanutsPackages || label.contains("Peanuts", true) || app.packageName.contains("peanuts", true))
        }?.packageName
    }

    private fun playSoundEffect(effect: Int) { window.decorView.playSoundEffect(effect) }

    private fun toast(text: String, long: Boolean = false) {
        android.widget.Toast.makeText(this, text, if (long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT).show()
    }

=======
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

>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c
    private fun launchAndroidApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }

    private fun launchNup(game: NupGame) {
        val pkg = peanutsPackage()
        if (pkg == null) {
<<<<<<< HEAD
            toast("Peanuts Native não foi encontrado.", long = true)
=======
            android.widget.Toast.makeText(this, "Peanuts Native não foi encontrado.", android.widget.Toast.LENGTH_LONG).show()
>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c
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
<<<<<<< HEAD
            toast("O Peanuts não aceitou abrir este arquivo. Vamos validar a integração com esta versão.", long = true)
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

    /* ------------------------------------------------------------------ */
    /* Imagens                                                              */
    /* ------------------------------------------------------------------ */

    /** Decodifica a imagem já reduzida para não estourar a memória com fotos/capas grandes. */
    private fun decodeSampled(maxSide: Int, open: () -> InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
        return open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    }

    private fun decodeUri(uri: Uri?, maxSide: Int): ImageBitmap? = try {
        if (uri == null) null else decodeSampled(maxSide) { contentResolver.openInputStream(uri) }?.asImageBitmap()
    } catch (_: Exception) { null }

    private fun appIcon(pkg: String): ImageBitmap? = runCatching {
        val drawable = packageManager.getApplicationIcon(pkg)
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 256, 256)
        drawable.draw(android.graphics.Canvas(bmp))
        bmp.asImageBitmap()
    }.getOrNull()

    /** Baixa a capa escolhida e guarda no armazenamento do app. Nome novo a cada vez para a imagem atualizar na tela. */
    private fun saveCover(url: String, key: String): Uri {
        val prefix = "qplay-cover-" + key.hashCode()
        filesDir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
        val file = File(filesDir, prefix + "-" + System.currentTimeMillis() + ".jpg")
        CoverCatalog.downloadTo(url, file)
        return Uri.fromFile(file)
    }

    private fun stringMap(prefs: SharedPreferences, prefix: String): Map<String, String> =
        prefs.all.filterKeys { it.startsWith(prefix) }.mapKeys { it.key.removePrefix(prefix) }.mapValues { it.value as? String ?: "" }

    /* ------------------------------------------------------------------ */
    /* Raiz do app: estado + navegação entre páginas                        */
    /* ------------------------------------------------------------------ */

    @Composable
    private fun QPlayApp() {
        val prefs = remember { getSharedPreferences("qplay_library", MODE_PRIVATE) }
        val scope = rememberCoroutineScope()

        var folderUri by remember { mutableStateOf(prefs.getString("rom_folder_uri", null)?.let(Uri::parse)) }
        var games by remember { mutableStateOf<List<NupGame>>(emptyList()) }
        var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
        var peanuts by remember { mutableStateOf<String?>(null) }
        var screen by remember { mutableStateOf(Screen.Home) }
        var scanning by remember { mutableStateOf(false) }
        var refreshToken by remember { mutableIntStateOf(0) }
        var selectedApps by remember { mutableStateOf(prefs.getStringSet("selected_apps", emptySet()).orEmpty()) }
        var scanMessage by remember { mutableStateOf("Escolha a pasta onde estão seus arquivos .nup") }
        var displayName by remember { mutableStateOf(prefs.getString("display_name", "") ?: "") }
        var favoriteKeys by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty()) }
        var groupMap by remember { mutableStateOf(stringMap(prefs, "group_")) }
        var titleMap by remember { mutableStateOf(stringMap(prefs, "edit_name_")) }
        var coverMap by remember { mutableStateOf(stringMap(prefs, "cover_")) }
        var libraryFilter by remember { mutableStateOf("Todos") }
        var settingsCategory by remember { mutableStateOf("Perfil") }
        var avatarUri by remember { mutableStateOf(prefs.getString("profile_avatar_uri", null)?.let(Uri::parse)) }

        // Rascunho da página "Personalizar jogo"
        var editEntry by remember { mutableStateOf<Entry?>(null) }
        var draftTitle by remember { mutableStateOf("") }
        var draftGroup by remember { mutableStateOf("") }
        var draftFavorite by remember { mutableStateOf(false) }

        // Página "Buscar capa"
        var coverQuery by remember { mutableStateOf("") }
        var coverBusy by remember { mutableStateOf(false) }
        var coverStatus by remember { mutableStateOf("") }
        var coverResults by remember { mutableStateOf<List<CoverCatalog.Hit>>(emptyList()) }
        var coverSearchToken by remember { mutableIntStateOf(0) }

        val avatarBitmap by produceState<ImageBitmap?>(null, avatarUri) {
            value = withContext(Dispatchers.IO) { decodeUri(avatarUri, 512) }
        }

        val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    avatarUri = uri
                    prefs.edit().putString("profile_avatar_uri", uri.toString()).apply()
                } catch (_: SecurityException) {
                    toast("Não foi possível guardar esta foto.")
                }
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
        val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val target = editEntry
            if (uri != null && target != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    coverMap = coverMap + (target.key to uri.toString())
                    prefs.edit().putString("cover_" + target.key, uri.toString()).apply()
                } catch (_: Exception) {
                    toast("Não foi possível guardar esta imagem.")
                }
            }
        }

        LaunchedEffect(refreshToken) {
            val (found, peanutsPkg) = withContext(Dispatchers.IO) { installedApps() to peanutsPackage() }
            apps = found
            peanuts = peanutsPkg
        }
=======
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
                    avatarUri?.let { uri -> contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }
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
>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c

        LaunchedEffect(folderUri, refreshToken) {
            val root = folderUri
            if (root != null) {
                scanning = true
                scanMessage = "Procurando arquivos .nup…"
<<<<<<< HEAD
                val found = withContext(Dispatchers.IO) { scanNupFolder(root) }
=======
                val found = withContext(Dispatchers.IO) {
                    scanNupFolder(root)
                }
>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c
                games = found
                scanMessage = if (found.isEmpty()) "Nenhum arquivo .nup encontrado nesta pasta." else "${found.size} jogo(s) encontrado(s)"
                scanning = false
            } else {
                games = emptyList()
            }
        }

<<<<<<< HEAD
        // Busca de capas: roda ao abrir a página e a cada clique em "Buscar". Uma busca nova cancela a anterior.
        LaunchedEffect(coverSearchToken) {
            if (coverSearchToken == 0) return@LaunchedEffect
            val q = coverQuery
            coverBusy = true
            coverStatus = "Consultando catálogo público…"
            coverResults = emptyList()
            try {
                val found = withContext(Dispatchers.IO) { CoverCatalog.search(applicationContext, q) }
                coverResults = found
                coverStatus = if (found.isEmpty()) "Nenhuma capa para “$q”. Tente só parte do nome, em inglês."
                else "${found.size} capas encontradas"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coverStatus = e.message?.takeIf { it.startsWith("HTTP") }?.let { "Falha no catálogo: $it" }
                    ?: "Falha ao buscar capas. Verifique a internet e tente novamente."
            }
            coverBusy = false
        }

        BackHandler(enabled = screen != Screen.Home) {
            screen = if (screen == Screen.Covers) Screen.Edit else Screen.Home
        }

        val entries = remember(games, apps, selectedApps) {
            games.map { Entry(it.uri.toString(), it.name, nup = it) } +
                apps.filter { it.packageName in selectedApps }.map { Entry(it.packageName, it.name, pkg = it.packageName) }
        }
        val visibleEntries = remember(entries, libraryFilter, favoriteKeys, groupMap) {
            entries.filter {
                when (libraryFilter) {
                    "Favoritos" -> it.key in favoriteKeys
                    "Grupos" -> !groupMap[it.key].isNullOrBlank()
                    else -> true
                }
            }
        }

        val pickFolder: () -> Unit = { folderPicker.launch(folderUri) }
        val refresh: () -> Unit = { refreshToken++ }
        val openEdit: (Entry) -> Unit = { e ->
            editEntry = e
            draftTitle = titleMap[e.key] ?: e.name
            draftGroup = groupMap[e.key].orEmpty()
            draftFavorite = e.key in favoriteKeys
            screen = Screen.Edit
        }

        // Botão B do controle = voltar
        Box(Modifier.fillMaxSize().onKeyEvent { ev ->
            if (ev.key == Key.ButtonB) {
                if (ev.type == KeyEventType.KeyUp) onBackPressedDispatcher.onBackPressed()
                true
            } else false
        }) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    entries = visibleEntries,
                    totalEntries = entries.size,
                    filter = libraryFilter,
                    titles = titleMap,
                    covers = coverMap,
                    favorites = favoriteKeys,
                    displayName = displayName,
                    avatar = avatarBitmap,
                    scanning = scanning,
                    scanMessage = scanMessage,
                    onOpen = { e ->
                        val nup = e.nup
                        val pkg = e.pkg
                        if (nup != null) launchNup(nup) else if (pkg != null) launchAndroidApp(pkg)
                    },
                    onEdit = openEdit,
                    onProfile = { settingsCategory = "Perfil"; screen = Screen.Settings },
                    onPickFolder = pickFolder,
                    onRefresh = refresh,
                    onApps = { screen = Screen.Apps },
                    onPeanuts = { toast(if (peanuts != null) "Peanuts conectado ($peanuts)" else "Peanuts não detectado neste aparelho") },
                    onSettings = { screen = Screen.Settings },
                    onQuit = { finish() }
                )

                Screen.Apps -> AppsPage(
                    apps = apps,
                    selected = selectedApps,
                    onToggle = { pkg ->
                        selectedApps = if (pkg in selectedApps) selectedApps - pkg else selectedApps + pkg
                        prefs.edit().putStringSet("selected_apps", selectedApps).apply()
                    },
                    onLaunch = { pkg -> launchAndroidApp(pkg) }
                )

                Screen.Settings -> SettingsPage(
                    category = settingsCategory,
                    onCategory = { settingsCategory = it },
                    displayName = displayName,
                    onDisplayName = { displayName = it; prefs.edit().putString("display_name", it).apply() },
                    avatar = avatarBitmap,
                    onPickAvatar = { avatarPicker.launch(arrayOf("image/*")) },
                    onRemoveAvatar = { avatarUri = null; prefs.edit().remove("profile_avatar_uri").apply() },
                    onAccent = { c ->
                        SwitchColors.setAccent(c)
                        if (c == null) prefs.edit().remove("accent_color").apply()
                        else prefs.edit().putLong("accent_color", c.value.toLong()).apply()
                    },
                    folderLabel = folderUri?.lastPathSegment?.substringAfterLast(':') ?: "não definida",
                    onPickFolder = pickFolder,
                    onApps = { screen = Screen.Apps },
                    onRefresh = refresh,
                    libraryFilter = libraryFilter,
                    onFilter = { libraryFilter = it },
                    onClearCatalog = { CoverCatalog.clearCache(applicationContext); toast("Cache do catálogo limpo") },
                    peanutsStatus = if (peanuts != null) "Peanuts: conectado ($peanuts)" else "Peanuts: não detectado",
                    version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
                )

                Screen.Edit -> {
                    val e = editEntry
                    if (e != null) {
                        val coverString = coverMap[e.key]
                        val editCover by produceState<ImageBitmap?>(null, coverString) {
                            value = withContext(Dispatchers.IO) { decodeUri(coverString?.let(Uri::parse), 900) }
                        }
                        EditPage(
                            gameName = e.name,
                            draftTitle = draftTitle, onDraftTitle = { draftTitle = it },
                            draftGroup = draftGroup, onDraftGroup = { draftGroup = it },
                            favorite = draftFavorite, onFavorite = { draftFavorite = it },
                            cover = editCover,
                            onSearchCover = {
                                coverQuery = CoverCatalog.cleanQuery(draftTitle).ifBlank { draftTitle }
                                coverResults = emptyList()
                                coverStatus = ""
                                coverSearchToken++
                                screen = Screen.Covers
                            },
                            onGallery = { galleryPicker.launch(arrayOf("image/*")) },
                            onRemoveCover = {
                                coverMap = coverMap - e.key
                                prefs.edit().remove("cover_" + e.key).apply()
                            },
                            onSave = {
                                val newTitle = draftTitle.ifBlank { e.name }
                                val group = draftGroup.trim()
                                titleMap = titleMap + (e.key to newTitle)
                                groupMap = groupMap + (e.key to group)
                                favoriteKeys = if (draftFavorite) favoriteKeys + e.key else favoriteKeys - e.key
                                prefs.edit()
                                    .putString("edit_name_" + e.key, newTitle)
                                    .putString("group_" + e.key, group)
                                    .putStringSet("favorites", favoriteKeys)
                                    .apply()
                                screen = Screen.Home
                            },
                            onCancel = { screen = Screen.Home }
                        )
                    }
                }

                Screen.Covers -> {
                    val e = editEntry
                    CoversPage(
                        gameName = e?.name.orEmpty(),
                        query = coverQuery,
                        onQuery = { coverQuery = it },
                        busy = coverBusy,
                        status = coverStatus,
                        results = coverResults,
                        onSearch = { coverSearchToken++ },
                        onApply = { hit ->
                            if (e != null && !coverBusy) scope.launch {
                                coverBusy = true
                                coverStatus = "Baixando e aplicando capa…"
                                try {
                                    val saved = withContext(Dispatchers.IO) { saveCover(hit.url, e.key) }
                                    coverMap = coverMap + (e.key to saved.toString())
                                    prefs.edit().putString("cover_" + e.key, saved.toString()).apply()
                                    toast("Capa aplicada!")
                                    screen = Screen.Edit
                                } catch (ex: CancellationException) {
                                    throw ex
                                } catch (_: Exception) {
                                    coverStatus = "Não foi possível baixar esta imagem. Escolha outra."
                                }
                                coverBusy = false
                            }
                        }
                    )
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Tela inicial (igual à do Switch)                                     */
    /* ------------------------------------------------------------------ */

    @Composable
    private fun HomeScreen(
        entries: List<Entry>,
        totalEntries: Int,
        filter: String,
        titles: Map<String, String>,
        covers: Map<String, String>,
        favorites: Set<String>,
        displayName: String,
        avatar: ImageBitmap?,
        scanning: Boolean,
        scanMessage: String,
        onOpen: (Entry) -> Unit,
        onEdit: (Entry) -> Unit,
        onProfile: () -> Unit,
        onPickFolder: () -> Unit,
        onRefresh: () -> Unit,
        onApps: () -> Unit,
        onPeanuts: () -> Unit,
        onSettings: () -> Unit,
        onQuit: () -> Unit
    ) {
        var focusedName by remember { mutableStateOf("") }
        var dockFocus by remember { mutableIntStateOf(-1) }
        val firstFocus = remember { FocusRequester() }

        LaunchedEffect(scanning) {
            if (!scanning) {
                delay(60)
                runCatching { firstFocus.requestFocus() }
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(SwitchColors.Bg)
                .windowInsetsPadding(WindowInsets.displayCutout)
        ) {
            val u = maxHeight
            val tile = u * TILE_SIZE
            val dock = u * DOCK_DIAMETER
            val labelSize = u.value * 0.038f

            // Avatar (canto superior esquerdo): abre Configurações > Perfil
            SwitchAvatar(u * AVATAR_SIZE, Modifier.offset(u * AVATAR_X, u * AVATAR_Y), image = avatar, onClick = onProfile)
            if (displayName.isNotBlank()) {
                Text(
                    displayName,
                    color = SwitchColors.Ink,
                    fontSize = (u.value * 0.04f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(u * (AVATAR_X + AVATAR_SIZE + 0.02f), u * (AVATAR_Y + 0.02f)).fillMaxWidth(0.3f)
                )
            }

            // Relógio, Wi-Fi e bateria (canto superior direito)
            SwitchStatusBar(u, Modifier.align(Alignment.TopEnd).padding(top = u * STATUS_TOP, end = u * STATUS_END))

            // Fileira de jogos + espaços vazios
            val emptyCount = (MIN_SLOTS - entries.size).coerceAtLeast(2)
            LazyRow(
                modifier = Modifier.align(Alignment.TopStart).offset(y = u * (TILE_TOP - 0.03f)).fillMaxWidth().height(tile + u * 0.06f),
                contentPadding = PaddingValues(start = u * TILE_START, end = u * 0.1f, top = u * 0.03f, bottom = u * 0.03f),
                horizontalArrangement = Arrangement.spacedBy(u * TILE_GAP),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(entries, key = { _, e -> (if (e.pkg != null) "app:" else "nup:") + e.key }) { i, e ->
                    GameTile(
                        entry = e,
                        title = titles[e.key] ?: e.name,
                        coverUri = covers[e.key]?.let(Uri::parse),
                        favorite = e.key in favorites,
                        size = tile,
                        modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = { onOpen(e) },
                        onLongClick = { onEdit(e) },
                        onFocused = { focusedName = it }
                    )
                }
                rowItems(emptyCount, key = { "empty:$it" }) { i ->
                    SwitchEmptySlot(tile, onPickFolder, if (entries.isEmpty() && i == 0) Modifier.focusRequester(firstFocus) else Modifier)
                }
            }

            // Nome do jogo selecionado (ou mensagem de estado) logo abaixo da fileira
            val emptyState = scanning || totalEntries == 0 || entries.isEmpty()
            val caption = when {
                dockFocus >= 0 -> ""
                scanning || totalEntries == 0 -> scanMessage
                entries.isEmpty() -> "Nada em “$filter”. Troque o filtro em Configurações > Biblioteca."
                else -> focusedName
            }
            if (caption.isNotEmpty()) {
                Text(
                    caption,
                    color = if (emptyState) SwitchColors.Muted else SwitchColors.Teal,
                    fontSize = labelSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = u * TILE_START, end = u * 0.1f, top = u * CAPTION_TOP)
                )
            }

            // Fileira de botões redondos
            fun dockFocusChanged(index: Int): (Boolean) -> Unit = { f ->
                if (f) {
                    dockFocus = index
                    playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
                } else if (dockFocus == index) dockFocus = -1
            }
            Row(Modifier.align(Alignment.TopCenter).padding(top = u * DOCK_TOP)) {
                SwitchCircleButton(Icons.Rounded.Apps, "Aplicativos", SwitchColors.Red, dock, labelSize, onApps, onFocusedChange = dockFocusChanged(0))
                SwitchCircleButton(Icons.Rounded.FolderOpen, "Pasta de jogos", SwitchColors.Orange, dock, labelSize, onPickFolder, onFocusedChange = dockFocusChanged(1))
                SwitchCircleButton(Icons.Rounded.Refresh, "Atualizar", SwitchColors.Blue, dock, labelSize, onRefresh, onFocusedChange = dockFocusChanged(2))
                SwitchCircleButton(Icons.Rounded.SportsEsports, "Peanuts", SwitchColors.Gray, dock, labelSize, onPeanuts, onFocusedChange = dockFocusChanged(3))
                SwitchCircleButton(Icons.Rounded.Settings, "Configurações", SwitchColors.Gray, dock, labelSize, onSettings, onFocusedChange = dockFocusChanged(4))
                SwitchCircleButton(Icons.Rounded.PowerSettingsNew, "Sair", SwitchColors.Gray, dock, labelSize, onQuit, onFocusedChange = dockFocusChanged(5))
            }

            // Linha divisória + rodapé
            Box(
                Modifier.align(Alignment.TopStart)
                    .padding(top = u * com.qplay.launcher.ui.LINE_TOP, start = u * 0.023f, end = u * 0.023f)
                    .fillMaxWidth().height(1.5.dp).background(SwitchColors.Line)
            )
            SwitchBottomBar(
                u,
                if (dockFocus >= 0) listOf(ButtonHint("A", "OK")) else listOf(ButtonHint("A", "Abrir"), ButtonHint("Y", "Editar")),
                Modifier.align(Alignment.TopStart).padding(top = u * com.qplay.launcher.ui.BOTTOM_TOP, start = u * 0.07f, end = u * 0.087f).fillMaxWidth()
            )
        }
    }

    /* ------------------------------------------------------------------ */
    /* Quadrado de jogo (capa, ícone do app ou nome) com seleção ciano      */
    /* ------------------------------------------------------------------ */

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun GameTile(
        entry: Entry,
        title: String,
        coverUri: Uri?,
        favorite: Boolean,
        size: Dp,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        onFocused: (String) -> Unit
    ) {
        var focused by remember { mutableStateOf(false) }
        val cover by produceState<ImageBitmap?>(null, coverUri) {
            value = withContext(Dispatchers.IO) { decodeUri(coverUri, 900) }
        }
        val icon by produceState<ImageBitmap?>(null, entry.pkg) {
            value = withContext(Dispatchers.IO) { entry.pkg?.let { appIcon(it) } }
        }

        SwitchTileFrame(
            size = size,
            focused = focused,
            modifier = modifier
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) {
                        onFocused(title)
                        playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
                    }
                }
                .onKeyEvent { ev ->
                    // Botão Y (ou X) do controle = editar o jogo
                    if (ev.key == Key.ButtonY || ev.key == Key.ButtonX) {
                        if (ev.type == KeyEventType.KeyUp) onLongClick()
                        true
                    } else false
                }
                .gamepadA { playSoundEffect(SoundEffectConstants.CLICK); onClick() }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { playSoundEffect(SoundEffectConstants.CLICK); onClick() },
                    onLongClick = onLongClick
                )
        ) {
            val c = cover
            val appImage = icon
            Box(Modifier.fillMaxSize()) {
                if (c != null) {
                    Image(bitmap = c, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF46567A), Color(0xFF1F2638)))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appImage != null) Image(bitmap = appImage, contentDescription = title, modifier = Modifier.size(size * 0.46f))
                        else Icon(Icons.Rounded.SportsEsports, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(size * 0.3f))
                    }
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x99000000)).padding(horizontal = size * 0.05f, vertical = size * 0.035f)) {
                        Text(title, color = Color.White, fontSize = (size.value * 0.065f).sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (favorite) {
                    Icon(Icons.Rounded.Favorite, "Favorito", tint = Color(0xFFE65B68), modifier = Modifier.align(Alignment.TopEnd).padding(size * 0.04f).size(size * 0.1f))
                }
            }
        }
    }
}
=======
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

            Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = maxWidth * 0.018f, vertical = maxHeight * 0.018f)) {
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
                            BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x99000000)).windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)), contentAlignment = Alignment.Center) {
                                val compact = maxWidth < 820.dp || maxHeight < 460.dp
                                val sideWidth = if (compact) 132.dp else (maxWidth * 0.22f).coerceIn(180.dp, 230.dp)
                                val outerPad = if (compact) 8.dp else 16.dp
                                Column(Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f).background(Bg).padding(horizontal = if (compact) 6.dp else 10.dp, vertical = 4.dp)) {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Settings, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(if (compact) 22.dp else 28.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("CONFIGURAÇÕES DO QPLAY", color = Color.White, fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { settingsOpen = false }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("VOLTAR", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold, fontSize = if (compact) 11.sp else 13.sp) }
                                    }
                                    Divider(color = Color(0xFF777777))
                                    Row(Modifier.weight(1f).fillMaxWidth()) {
                                        Column(Modifier.width(sideWidth).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            listOf("Perfil", "Aparência", "Biblioteca", "Imagens", "Controles", "Sistema").forEach { category ->
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
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                        Surface(
                                                            onClick = { avatarPicker.launch(arrayOf("image/*")) },
                                                            color = Color(0xFF171923),
                                                            shape = RoundedCornerShape(50),
                                                            border = BorderStroke(2.dp, Color(0xFF35BCEB)),
                                                            modifier = Modifier.size(64.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                if (avatarBitmap.value != null) {
                                                                    androidx.compose.foundation.Image(bitmap = avatarBitmap.value!!, contentDescription = "Avatar escolhido", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50)))
                                                                } else {
                                                                    Icon(Icons.Rounded.Person, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(34.dp))
                                                                }
                                                            }
                                                        }
                                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text("FOTO DE PERFIL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            Text("Toque para escolher uma imagem da galeria. Ela substituirá o ícone do QPlay.", color = Muted, fontSize = 11.sp)
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                TextButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }) { Text("ESCOLHER FOTO", color = Color(0xFF35BCEB)) }
                                                                if (avatarUri != null) TextButton(onClick = { avatarUri = null; prefs.edit().remove("profile_avatar_uri").apply() }) { Text("REMOVER", color = Color(0xFFE45B68)) }
                                                            }
                                                        }
                                                    }
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
                                                    Text("Nintendo Switch usa ícones oficiais da eShop. Jogos nativos usam os ícones dos próprios aplicativos.", color = Muted)
                                                    Text("Use o editor de cada jogo para pesquisar e aplicar imagens.", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                }
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
                val compact = maxWidth < 900.dp || maxHeight < 600.dp
                Surface(
                    color = Color(0xFF202020),
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, Color(0xFF35BCEB)),
                    modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f)
                ) {
                    Column(Modifier.fillMaxSize().padding(if (compact) 10.dp else 16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ImageSearch, null, tint = Color(0xFF35BCEB), modifier = Modifier.size(if (compact) 22.dp else 28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ÍCONES NINTENDO SWITCH", color = Color(0xFF35BCEB), fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Pesquise e aplique o ícone oficial do jogo", color = Muted, fontSize = if (compact) 10.sp else 12.sp)
                            }
                            TextButton(onClick = { scraperOpen = false }) { Text("FECHAR  ✕", color = Color(0xFF35BCEB), fontWeight = FontWeight.Bold) }
                        }
                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f).fillMaxHeight().fillMaxWidth(if (compact) 0.38f else 0.31f).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("JOGO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                OutlinedTextField(
                                    value = scraperQuery,
                                    onValueChange = { scraperQuery = it },
                                    label = { Text("Nome do jogo") },
                                    singleLine = true,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("PLATAFORMA: NINTENDO SWITCH • eSHOP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Button(
                                    enabled = scraperQuery.isNotBlank() && !scraperBusy,
                                    onClick = {
                                        scraperBusy = true
                                        scraperStatus = "Consultando catálogo público…"
                                        scraperResults = emptyList()
                                        scraperScope.launch {
                                            try {
                                                val matches = withContext(Dispatchers.IO) {
                                                    searchSwitchCovers(scraperQuery)
                                                }
                                                scraperResults = matches
                                                scraperStatus = if (matches.isEmpty()) "Nenhuma capa correspondente neste console. Confira o nome e a plataforma selecionada." else "${matches.size} capas encontradas."
                                            } catch (e: Exception) {
                                                scraperStatus = e.message?.takeIf { it.contains("HTTP") }
                                                    ?.let { "Falha no catálogo: $it" }
                                                    ?: "Falha ao buscar capas. Verifique a internet e tente novamente."
                                            }
                                            scraperBusy = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35BCEB))
                                ) { Text(if (scraperBusy) "PESQUISANDO…" else "BUSCAR ÍCONES", color = Color(0xFF101820), fontWeight = FontWeight.Bold) }
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
                                            Text(if (scraperBusy) "Buscando imagens…" else "Os ícones encontrados aparecerão aqui", color = Muted, fontSize = 12.sp)
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
                                                            android.widget.Toast.makeText(this@MainActivity, "Ícone do Switch aplicado ao jogo!", android.widget.Toast.LENGTH_SHORT).show()
                                                        } catch (_: Exception) { scraperStatus = "Não foi possível baixar esta imagem." }
                                                        scraperBusy = false
                                                    }
                                                },
                                                color = Color(0xFF292929),
                                                shape = RoundedCornerShape(9.dp),
                                                border = BorderStroke(1.dp, Color(0xFF454545))
                                            ) {
                                                Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(5.dp)).background(Color(0xFF171717)), contentAlignment = Alignment.Center) {
                                                        if (preview.value != null) androidx.compose.foundation.Image(bitmap = preview.value!!, contentDescription = result.first, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                        else Icon(Icons.Rounded.SportsEsports, null, tint = Muted, modifier = Modifier.size(28.dp))
                                                    }
                                                    Text(result.first.substringBeforeLast("."), color = Color.White, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Text("APLICAR ÍCONE", color = Color(0xFF35BCEB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0xFF555555), modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("FONTE: CATÁLOGO DE ÍCONES NINTENDO SWITCH", color = Muted, fontSize = 9.sp)
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
                val compactEdit = maxWidth < 1100.dp || maxHeight < 700.dp
                Surface(
                    color = Color(0xFF202020),
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, Color(0xFF35BCEB)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.fillMaxSize().padding(if (compactEdit) 8.dp else 14.dp)) {
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
>>>>>>> d94da23781b2fe5462bd4f17339f43cf72672e6c
