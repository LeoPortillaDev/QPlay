package com.qplay.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.SoundEffectConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.qplay.launcher.ui.ButtonHint
import com.qplay.launcher.ui.KeyboardRequest
import com.qplay.launcher.ui.LocalSwitchKeyboard
import com.qplay.launcher.ui.QPlayTheme
import com.qplay.launcher.ui.SwitchAvatar
import com.qplay.launcher.ui.SwitchBottomBar
import com.qplay.launcher.ui.SwitchCircleButton
import com.qplay.launcher.ui.SwitchColors
import com.qplay.launcher.ui.SwitchEmptySlot
import com.qplay.launcher.ui.SwitchKeyboard
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
private const val TILE_VIEWPORT_SIDE = 0.120f
private const val TILE_EDGE_INSET = 0.032f
private const val TILE_GAP = 0.019f
private const val CAPTION_TOP = 0.648f
private const val DOCK_TOP = 0.665f
private const val DOCK_DIAMETER = 0.119f
private const val MIN_SLOTS = 8

class MainActivity : ComponentActivity() {
    private val peanutsPackages = listOf("dev.nxnative.android", "dev.bananative.android", "dev.nxnative.android.play")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        SwitchColors.setDarkMode(prefs.getBoolean("dark_mode", false))
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

    override fun onDestroy() {
        uiToneGenerator?.release()
        uiToneGenerator = null
        super.onDestroy()
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

    private var uiToneGenerator: ToneGenerator? = null
    private var uiToneVolume: Int = -1

    private fun playUiTone(tone: Int) {
        val prefs = getSharedPreferences("qplay_library", MODE_PRIVATE)
        if (!prefs.getBoolean("ui_sounds_enabled", true)) return
        val volume = prefs.getInt("ui_sound_volume", 35).coerceIn(0, 100)
        if (volume == 0) return
        if (uiToneGenerator == null || uiToneVolume != volume) {
            uiToneGenerator?.release()
            uiToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
            uiToneVolume = volume
        }
        uiToneGenerator?.startTone(tone, 65)
    }

    private fun toast(text: String, long: Boolean = false) {
        android.widget.Toast.makeText(this, text, if (long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun launchAndroidApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }

    private fun launchNup(game: NupGame) {
        if (!game.fileName.substringAfterLast('.', "").equals("nup", true)) {
            launchSwitchPackage(game)
            return
        }
        val pkg = peanutsPackage()
        if (pkg == null) {
            toast("Peanuts Native não foi encontrado.", long = true)
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
            toast("O Peanuts não aceitou abrir este arquivo. Vamos validar a integração com esta versão.", long = true)
        }
    }

    private fun launchSwitchPackage(game: NupGame) {
        val extension = game.fileName.substringAfterLast('.', "").lowercase()
        val mime = when (extension) {
            "nsp" -> "application/x-nx-nsp"
            "xci" -> "application/x-nx-xci"
            else -> null
        }
        if (mime == null) {
            toast("Formato de jogo não reconhecido.")
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(game.uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            toast("Nenhum emulador declarou suporte à abertura direta deste arquivo. Abra o Lemon ou outro emulador e adicione a pasta de jogos.", long = true)
            return
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            toast("O emulador não conseguiu abrir este arquivo. Tente adicionar a pasta de jogos nas configurações dele.", long = true)
        }
    }

    private fun scanNupFolder(treeUri: Uri): List<NupGame> {
        val root = DocumentFile.fromTreeUri(this, treeUri) ?: return emptyList()
        val found = mutableListOf<NupGame>()
        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 8 || found.size >= 500) return
            dir.listFiles().forEach { file ->
                if (file.isDirectory) walk(file, depth + 1)
                else if (file.isFile && file.name?.substringAfterLast('.', "")?.lowercase()?.let { it in setOf("nup", "nsp", "xci") } == true) {
                    val filename = file.name ?: "Jogo"
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
        if (uri == null) {
            null
        } else {
            val bmp = decodeSampled(maxSide) { contentResolver.openInputStream(uri) }
            if (bmp == null) null else applyOrientation(bmp, exifOrientation(uri)).asImageBitmap()
        }
    } catch (_: Exception) { null }

    /** Orientação gravada pela câmera no arquivo (EXIF). Sem aplicá-la, fotos tiradas em pé aparecem deitadas. */
    private fun exifOrientation(uri: Uri): Int = try {
        contentResolver.openInputStream(uri)?.use {
            android.media.ExifInterface(it).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
        } ?: android.media.ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) { android.media.ExifInterface.ORIENTATION_NORMAL }

    private fun applyOrientation(src: Bitmap, orientation: Int): Bitmap {
        val m = android.graphics.Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            android.media.ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            android.media.ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

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
        var scanMessage by remember { mutableStateOf("Escolha a pasta onde estão seus jogos (.nup, .nsp ou .xci)") }
        var displayName by remember { mutableStateOf(prefs.getString("display_name", "") ?: "") }
        var favoriteKeys by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty()) }
        var groupMap by remember { mutableStateOf(stringMap(prefs, "group_")) }
        var titleMap by remember { mutableStateOf(stringMap(prefs, "edit_name_")) }
        var coverMap by remember { mutableStateOf(stringMap(prefs, "cover_")) }
        var libraryFilter by remember { mutableStateOf("Todos") }
        var settingsCategory by remember { mutableStateOf("Perfil") }
        var uiSoundsEnabled by remember { mutableStateOf(prefs.getBoolean("ui_sounds_enabled", true)) }
        var uiSoundVolume by remember { mutableStateOf(prefs.getInt("ui_sound_volume", 35).toFloat()) }
        var avatarUri by remember { mutableStateOf(prefs.getString("profile_avatar_uri", null)?.let(Uri::parse)) }

        // Rascunho da página "Personalizar jogo"
        var editEntry by remember { mutableStateOf<Entry?>(null) }
        var keyboard by remember { mutableStateOf<KeyboardRequest?>(null) }
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

        LaunchedEffect(folderUri, refreshToken) {
            val root = folderUri
            if (root != null) {
                scanning = true
                scanMessage = "Procurando jogos (.nup, .nsp e .xci)…"
                val found = withContext(Dispatchers.IO) { scanNupFolder(root) }
                games = found
                scanMessage = if (found.isEmpty()) "Nenhum jogo .nup, .nsp ou .xci encontrado nesta pasta." else "${found.size} jogo(s) encontrado(s)"
                scanning = false
            } else {
                games = emptyList()
            }
        }

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
        // Declarado depois: tem prioridade. Voltar com o teclado aberto só fecha o teclado.
        BackHandler(enabled = keyboard != null) { keyboard = null }

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
        CompositionLocalProvider(LocalSwitchKeyboard provides { req: KeyboardRequest -> keyboard = req }) {
        Box(Modifier.fillMaxSize().onKeyEvent { ev ->
            if (ev.key == Key.ButtonB) {
                if (ev.type == KeyEventType.KeyUp) onBackPressedDispatcher.onBackPressed()
                true
            } else false
        }) {
            val activeKeyboard = keyboard
            // O teclado substitui a página inteira (nada de Dialog nem teclado do Android).
            if (activeKeyboard != null) SwitchKeyboard(activeKeyboard, onClose = { keyboard = null })
            else when (screen) {
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
                    darkMode = SwitchColors.isDark,
                    onDarkMode = { enabled ->
                        SwitchColors.setDarkMode(enabled)
                        prefs.edit().putBoolean("dark_mode", enabled).apply()
                    },
                    folderLabel = folderUri?.lastPathSegment?.substringAfterLast(':') ?: "não definida",
                    onPickFolder = pickFolder,
                    onApps = { screen = Screen.Apps },
                    onRefresh = refresh,
                    libraryFilter = libraryFilter,
                    onFilter = { libraryFilter = it },
                    onClearCatalog = { CoverCatalog.clearCache(applicationContext); toast("Cache do catálogo limpo") },
                    peanutsStatus = if (peanuts != null) "Peanuts: conectado ($peanuts)" else "Peanuts: não detectado",
                    version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty(),
                    uiSoundsEnabled = uiSoundsEnabled,
                    onUiSoundsEnabled = { enabled ->
                        uiSoundsEnabled = enabled
                        prefs.edit().putBoolean("ui_sounds_enabled", enabled).apply()
                    },
                    uiSoundVolume = uiSoundVolume,
                    onUiSoundVolume = { volume ->
                        uiSoundVolume = volume
                        prefs.edit().putInt("ui_sound_volume", volume.toInt()).apply()
                    }
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
        var focusedIndex by remember { mutableIntStateOf(0) }
        val gamesListState = rememberLazyListState()
        var dockFocus by remember { mutableIntStateOf(-1) }
        val firstFocus = remember { FocusRequester() }
        val entryKeys = remember(entries) { entries.map { it.key } }

        LaunchedEffect(scanning, entryKeys, filter) {
            if (!scanning) {
                // Sempre retorna ao início quando a biblioteca recebe jogos ou o filtro muda.
                gamesListState.scrollToItem(0)
                delay(100)
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
                state = gamesListState,
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(y = u * (TILE_TOP - 0.03f))
                    .padding(horizontal = u * TILE_VIEWPORT_SIDE)
                    .fillMaxWidth()
                    .height(tile + u * 0.06f),
                contentPadding = PaddingValues(start = u * TILE_EDGE_INSET, end = u * TILE_EDGE_INSET, top = u * 0.03f, bottom = u * 0.03f),
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
                        onLongClick = { playUiTone(ToneGenerator.TONE_PROP_ACK); onEdit(e) },
                        onFocused = { focusedName = it; focusedIndex = i }
                    )
                }
                items(emptyCount, key = { "empty:$it" }) { i ->
                    SwitchEmptySlot(tile, onPickFolder, if (entries.isEmpty() && i == 0) Modifier.focusRequester(firstFocus) else Modifier)
                }
            }

            // O nome fica acima da capa selecionada, centralizado sobre o próprio jogo.
            val focusedItemOffset = gamesListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == focusedIndex }?.offset ?: 0
            val density = androidx.compose.ui.platform.LocalDensity.current
            if (focusedName.isNotBlank() && dockFocus < 0 && entries.isNotEmpty()) {
                Box(
                    Modifier.align(Alignment.TopStart)
                        .padding(start = u * TILE_VIEWPORT_SIDE, top = u * (TILE_TOP - 0.075f))
                        .offset(x = with(density) { focusedItemOffset.toDp() })
                        .width(tile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        focusedName,
                        color = SwitchColors.Teal,
                        fontSize = labelSize.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Mensagens de estado só aparecem quando a biblioteca está vazia ou carregando.
            val emptyState = scanning || totalEntries == 0 || entries.isEmpty()
            val caption = when {
                dockFocus >= 0 -> ""
                scanning || totalEntries == 0 -> scanMessage
                entries.isEmpty() -> "Nada em “$filter”. Troque o filtro em Configurações > Biblioteca."
                else -> ""
            }
            if (caption.isNotEmpty()) {
                Text(
                    caption,
                    color = SwitchColors.Muted,
                    fontSize = labelSize.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(start = u * TILE_VIEWPORT_SIDE, end = u * TILE_VIEWPORT_SIDE, top = u * 0.185f)
                        .fillMaxWidth()
                )
            }

            // Fileira de botões redondos
            fun dockFocusChanged(index: Int): (Boolean) -> Unit = { f ->
                if (f) {
                    dockFocus = index
                    playUiTone(ToneGenerator.TONE_PROP_BEEP2)
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
                        playUiTone(ToneGenerator.TONE_PROP_BEEP2)
                    }
                }
                .onKeyEvent { ev ->
                    // Botão Y (ou X) do controle = editar o jogo
                    if (ev.key == Key.ButtonY || ev.key == Key.ButtonX) {
                        if (ev.type == KeyEventType.KeyUp) onLongClick()
                        true
                    } else false
                }
                .gamepadA { playUiTone(ToneGenerator.TONE_PROP_ACK); onClick() }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { playUiTone(ToneGenerator.TONE_PROP_ACK); onClick() },
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
