package com.qplay.launcher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.net.Uri
import androidx.compose.ui.graphics.Color as ComposeColor
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Estado do serviço mostrado em Configurações > Controles, para descobrir por que o HOME falha. */
object ServiceDiag {
    @Volatile var lastKey = "nenhuma ainda"
    @Volatile var lastKeyAt = 0L
    @Volatile var homeSeen = 0
    @Volatile var homeIgnored = 0
    @Volatile var menuOpened = 0
    @Volatile var foreground = "nenhum"
    @Volatile var lastWindow = "—"
    @Volatile var forceStop = "—"

    fun summary(serviceOn: Boolean): String {
        if (!serviceOn) return "Serviço desligado ou ainda não conectado."
        val ago = if (lastKeyAt == 0L) "—" else "há ${(System.currentTimeMillis() - lastKeyAt) / 1000}s"
        return "Último botão: $lastKey ($ago)\n" +
            "Emulador reconhecido: $foreground\n" +
            "Última janela: $lastWindow\n" +
            "HOME/MENU recebidos: $homeSeen · menu aberto: $menuOpened · ignorados (sem emulador): $homeIgnored\n" +
            "Última parada forçada: $forceStop"
    }
}

/**
 * Captura o botão de menu somente enquanto um emulador conhecido está em primeiro plano.
 * O menu é desenhado sobre o jogo e aceita teclas e eixos de controles compatíveis.
 */
class GameMenuAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var foregroundPackage: String? = null
        set(value) {
            field = value
            ServiceDiag.foreground = value ?: "nenhum"
        }
    private var overlay: View? = null
    private var windowManager: WindowManager? = null
    private var selected = 0
    private var lastAxisX = 0
    private var lastAxisY = 0

    private enum class MenuMode { MAIN, APPS }
    private class MenuItem(val label: String, val action: () -> Unit)

    private var mode = MenuMode.MAIN
    private var mainItems: List<MenuItem> = emptyList()
    private var mainViews: List<TextView> = emptyList()
    private var cardViews: List<LinearLayout> = emptyList()
    private var cardPackages: List<String> = emptyList()
    private var appsSelected = 0
    private var appsStrip: View? = null
    private var appsScroll: android.widget.HorizontalScrollView? = null
    private var appsRow: LinearLayout? = null
    private var hintView: TextView? = null
    private val recentApps = mutableListOf<String>()

    // Cores do menu (seguem o tema e a cor de destaque escolhidos no QPlay)
    private var dark = false
    private var cAccent = 0xFF1FC6B5.toInt()
    private var cPanel = 0xFFF5F5F5.toInt()
    private var cInk = 0xFF2D2D2D.toInt()
    private var cMuted = 0xFF8A8A8A.toInt()
    private var cCard = 0xFFFFFFFF.toInt()

    companion object {
        @Volatile var instance: GameMenuAccessibilityService? = null
        private val FORCE_LABELS = listOf("forçar parada", "forçar encerramento", "force stop", "forzar detención")
        private val CONFIRM_LABELS = listOf("ok", "forçar parada", "forçar encerramento", "force stop", "sim", "yes", "parar")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val prefs: SharedPreferences by lazy { getSharedPreferences("qplay_library", MODE_PRIVATE) }
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "game_menu_enabled") applyKeyFilter()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        instance = this
        applyKeyFilter()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    /**
     * Liga/desliga o filtro de teclas do serviço (Configurações > Controles > "Menu do jogo pelo botão HOME").
     * Desligado, o serviço deixa de interceptar qualquer tecla do controle: serve para isolar problemas de controle nos jogos.
     */
    private fun applyKeyFilter() {
        val info = serviceInfo ?: return
        val enabled = prefs.getBoolean("game_menu_enabled", true)
        info.flags = if (enabled) {
            info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        } else {
            info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
        }
        serviceInfo = info
        if (!enabled) {
            hideMenu()
            foregroundPackage = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg != packageName && !isSystemPackage(pkg) &&
            event.className?.toString().orEmpty().endsWith("Activity")
        ) noteRecent(pkg)
        if (isSupportedEmulator(pkg)) {
            foregroundPackage = pkg
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val windowClass = event.className?.toString().orEmpty()
        ServiceDiag.lastWindow = "$pkg / ${windowClass.substringAfterLast('.')}"
        when {
            // Janela do próprio QPlay: só a tela principal conta como "voltei à biblioteca" (o menu também é do QPlay).
            pkg == packageName -> if (windowClass.endsWith("MainActivity")) foregroundPackage = null
            // Barras do sistema, teclado e janelas Samsung por cima do jogo não tiram o emulador da frente.
            isSystemPackage(pkg) -> Unit
            // Outra tela cheia (launcher, outro app) assumiu: libera o HOME para o Android.
            // Avisos, toasts e diálogos não são "Activity" e não contam.
            windowClass.endsWith("Activity") -> foregroundPackage = null
        }
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val key = event.keyCode
        val isHome = key == KeyEvent.KEYCODE_HOME ||
            key == KeyEvent.KEYCODE_BUTTON_MODE ||
            key == KeyEvent.KEYCODE_MENU
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            ServiceDiag.lastKey = "${KeyEvent.keyCodeToString(key)} · dispositivo ${event.deviceId}"
            ServiceDiag.lastKeyAt = System.currentTimeMillis()
            if (isHome) {
                ServiceDiag.homeSeen++
                if (!isSupportedEmulator(foregroundPackage)) ServiceDiag.homeIgnored++
            }
        }
        if (isHome && isSupportedEmulator(foregroundPackage)) {
            // HOME abre o menu; com o menu aberto, HOME de novo fecha.
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (overlay != null) hideMenu() else showMenu()
            }
            return true
        }
        if (overlay != null) return handleOverlayKey(key, event.action)
        return false
    }

    /** Teclas do controle com o menu aberto: nenhuma chega ao jogo enquanto ele estiver visível. */
    private fun handleOverlayKey(key: Int, action: Int): Boolean {
        val down = action == KeyEvent.ACTION_DOWN
        var handled = true
        when (key) {
            KeyEvent.KEYCODE_DPAD_UP -> if (down) navigate(0, -1)
            KeyEvent.KEYCODE_DPAD_DOWN -> if (down) navigate(0, 1)
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_L1 -> if (down) navigate(-1, 0)
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_R1 -> if (down) navigate(1, 0)
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> if (down) confirm()
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> if (down) cancel()
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X -> if (down && mode == MenuMode.APPS) closeSelectedApp()
            else -> handled = false
        }
        return handled || overlay != null
    }

    /** Alguns controles enviam o direcional como eixo (HAT) em vez de tecla. */
    private fun handleOverlayMotion(event: MotionEvent): Boolean {
        val isJoystick = event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
            event.isFromSource(InputDevice.SOURCE_GAMEPAD)
        if (!isJoystick || event.action != MotionEvent.ACTION_MOVE) return false

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val x = if (hatX != 0f) hatX else event.getAxisValue(MotionEvent.AXIS_X)
        val y = if (hatY != 0f) hatY else event.getAxisValue(MotionEvent.AXIS_Y)
        val dx = if (x < -0.6f) -1 else if (x > 0.6f) 1 else 0
        val dy = if (y < -0.6f) -1 else if (y > 0.6f) 1 else 0
        if (dx != 0 && dx != lastAxisX) navigate(dx, 0)
        if (dy != 0 && dy != lastAxisY) navigate(0, dy)
        lastAxisX = dx
        lastAxisY = dy
        return true
    }

    private fun navigate(dx: Int, dy: Int) {
        if (mode == MenuMode.MAIN) {
            val step = if (dy != 0) dy else dx
            if (step == 0) return
            selected = (selected + step).coerceIn(0, mainItems.lastIndex)
            refreshMain()
        } else {
            if (dx != 0) {
                appsSelected = (appsSelected + dx).coerceIn(0, (cardPackages.size - 1).coerceAtLeast(0))
                refreshApps()
            } else if (dy < 0) {
                closeSelectedApp()   // "para cima" fecha o app, como arrastar o cartão no seletor de apps
            }
        }
    }

    private fun confirm() {
        if (mode == MenuMode.MAIN) mainItems.getOrNull(selected)?.action?.invoke() else openSelectedApp()
    }

    private fun cancel() {
        if (mode == MenuMode.APPS) leaveApps() else hideMenu()
    }

    private fun isSupportedEmulator(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        if (pkg == "dev.lemon.lemon_emulator" ||
            pkg == "com.antutu.ABenchMark" ||
            pkg == "dev.nxnative.android" ||
            pkg == "dev.bananative.android" ||
            pkg == "dev.nxnative.android.play"
        ) return true
        // Builds personalizadas (ex.: Peanuts com outro nome de pacote): o QPlay grava o pacote que detectou
        // e o último emulador que ele mesmo abriu.
        return pkg == prefs.getString("peanuts_pkg", null) || pkg == prefs.getString("last_emulator_pkg", null)
    }

    private fun isSystemPackage(pkg: String): Boolean =
        pkg == "android" ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.samsung.android.") ||
            pkg.startsWith("com.sec.android.") ||
            pkg.contains("inputmethod", ignoreCase = true)

    /* ---------------------------------------------------------------- */
    /* Menu do jogo (painel lateral estilo console)                      */
    /* ---------------------------------------------------------------- */

    private fun loadPalette() {
        dark = prefs.getBoolean("dark_mode", false)
        val saved = runCatching { prefs.getLong("accent_color", 0L) }.getOrDefault(0L)
        cAccent = if (saved != 0L) ComposeColor(saved.toULong()).toArgb() else 0xFF1FC6B5.toInt()
        cPanel = if (dark) 0xFF3A3A3A.toInt() else 0xFFF5F5F5.toInt()
        cInk = if (dark) 0xFFF4F4F4.toInt() else 0xFF2D2D2D.toInt()
        cMuted = if (dark) 0xFFB5B5B5.toInt() else 0xFF8A8A8A.toInt()
        cCard = if (dark) 0xFF4A4A4A.toInt() else 0xFFFFFFFF.toInt()
    }

    private fun appLabel(pkg: String?): String {
        if (pkg == null) return "QPlay"
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    private fun loadAvatar(): android.graphics.Bitmap? {
        val uri = prefs.getString("profile_avatar_uri", null)?.let(Uri::parse) ?: return null
        return try {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) sample *= 2
            val bmp = contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null
            val orientation = contentResolver.openInputStream(uri)?.use {
                android.media.ExifInterface(it).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            } ?: android.media.ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) bmp else {
                val m = android.graphics.Matrix().apply { postRotate(degrees) }
                android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildAvatar(name: String, size: Int): View {
        val oval = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(cAccent)
        }
        val bitmap = loadAvatar()
        if (bitmap != null) {
            return android.widget.ImageView(this).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
                background = oval
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
        }
        return TextView(this).apply {
            text = name.trim().take(1).uppercase()
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = oval
        }
    }

    private fun showMenu() {
        if (overlay != null) return
        loadPalette()
        selected = 0
        mode = MenuMode.MAIN
        lastAxisX = 0
        lastAxisY = 0
        val dm = resources.displayMetrics
        val panelWidth = maxOf((dm.widthPixels * 0.36f).toInt(), dp(270)).coerceAtMost(dm.widthPixels)

        mainItems = listOf(
            MenuItem("Continuar jogo") { hideMenu() },
            MenuItem("Apps abertos") { showApps() },
            MenuItem("Biblioteca QPlay") { goToLibrary() },
            MenuItem("Encerrar jogo e voltar") { endGame() }
        )

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xCC000000.toInt())
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            setOnKeyListener { _, code, event -> handleOverlayKey(code, event.action) }
            setOnGenericMotionListener { _, event -> handleOverlayMotion(event) }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(14))
            background = GradientDrawable().apply {
                setColor(cPanel)
                val r = dp(22).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            }
            elevation = dp(10).toFloat()
        }

        // Perfil: foto + nome (os mesmos de Configurações > Perfil) e o que está rodando.
        val name = prefs.getString("display_name", "").orEmpty().ifBlank { "Jogador" }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val avatarSize = dp(54)
        header.addView(buildAvatar(name, avatarSize), LinearLayout.LayoutParams(avatarSize, avatarSize))
        val nameColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        nameColumn.addView(TextView(this).apply {
            text = name
            setTextColor(cInk)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        nameColumn.addView(TextView(this).apply {
            text = "Jogando: " + appLabel(foregroundPackage)
            setTextColor(cMuted)
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        header.addView(nameColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(header)

        val divider = View(this).apply { setBackgroundColor(cAccent) }
        panel.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply {
            topMargin = dp(16)
            bottomMargin = dp(10)
        })

        mainViews = mainItems.mapIndexed { index, item ->
            TextView(this).apply {
                text = item.label
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setPadding(dp(16), dp(12), dp(16), dp(12))
                isClickable = true
                setOnClickListener { selected = index; refreshMain(); confirm() }
                panel.addView(this, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) })
            }
        }

        panel.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        val hint = TextView(this).apply {
            setTextColor(cMuted)
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(10), dp(4), dp(0))
        }
        hintView = hint
        panel.addView(hint)
        root.addView(panel, FrameLayout.LayoutParams(panelWidth, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START))

        // Faixa de "Apps abertos" (aparece ao lado do painel).
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        strip.addView(TextView(this).apply {
            text = "Apps abertos"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), 0, 0, dp(12))
        })
        val scroll = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(row)
        strip.addView(scroll)
        root.addView(
            strip,
            FrameLayout.LayoutParams(
                (dm.widthPixels - panelWidth - dp(44)).coerceAtLeast(dp(160)),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL or Gravity.START
            ).apply { leftMargin = panelWidth + dp(22) }
        )
        appsStrip = strip
        appsScroll = scroll
        appsRow = row

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        // Cobre também a área da câmera/recorte (faixa lateral que ficava colorida) e as barras do sistema.
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        @Suppress("DEPRECATION")
        root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        runCatching {
            windowManager?.addView(root, params)
            ServiceDiag.menuOpened++
            overlay = root
            root.requestFocus()
            refreshMain()
            // O painel entra deslizando pela esquerda, como o menu de um console.
            panel.translationX = -panelWidth.toFloat()
            panel.animate().translationX(0f).setDuration(170).start()
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun refreshMain() {
        mainViews.forEachIndexed { index, view ->
            val active = index == selected
            view.background = if (active) rounded(cAccent, dp(12)) else null
            view.setTextColor(if (active) 0xFFFFFFFF.toInt() else cInk)
            view.typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        hintView?.text = if (mode == MenuMode.MAIN) "Cima/Baixo: escolher   A: selecionar   B: voltar ao jogo" else
            "Esq/Dir: escolher   Cima ou Y: fechar app   A: abrir   B: voltar"
    }

    /* ------------------------ Apps abertos ------------------------ */

    private fun noteRecent(pkg: String) {
        recentApps.remove(pkg)
        recentApps.add(0, pkg)
        while (recentApps.size > 8) recentApps.removeAt(recentApps.lastIndex)
    }

    private fun showApps() {
        val candidates = ArrayList<String>()
        foregroundPackage?.let { candidates.add(it) }
        recentApps.forEach { if (it !in candidates) candidates.add(it) }
        cardPackages = candidates.filter { it != packageName && packageManager.getLaunchIntentForPackage(it) != null }
        appsSelected = 0
        val row = appsRow ?: return
        row.removeAllViews()
        if (cardPackages.isEmpty()) {
            row.addView(TextView(this).apply {
                text = "Nenhum app aberto registrado ainda."
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                setPadding(dp(4), dp(8), dp(4), dp(8))
            })
        }
        cardViews = cardPackages.map { pkg ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8), dp(14), dp(8), dp(10))
                isClickable = true
            }
            card.addView(android.widget.ImageView(this).apply {
                runCatching { setImageDrawable(packageManager.getApplicationIcon(pkg)) }
            }, LinearLayout.LayoutParams(dp(56), dp(56)))
            card.addView(TextView(this).apply {
                text = appLabel(pkg)
                setTextColor(cInk)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(8), 0, 0)
            }, LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT))
            card.setOnClickListener { appsSelected = cardViews.indexOf(card); openSelectedApp() }
            row.addView(card, LinearLayout.LayoutParams(dp(108), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = dp(10)
            })
            card
        }
        mode = MenuMode.APPS
        appsStrip?.visibility = View.VISIBLE
        refreshApps()
        refreshMain()
    }

    private fun leaveApps() {
        mode = MenuMode.MAIN
        appsStrip?.visibility = View.GONE
        refreshMain()
    }

    private fun refreshApps() {
        cardViews.forEachIndexed { index, card ->
            val active = index == appsSelected
            card.background = GradientDrawable().apply {
                setColor(cCard)
                cornerRadius = dp(14).toFloat()
                if (active) setStroke(dp(3), cAccent)
            }
        }
        cardViews.getOrNull(appsSelected)?.let { card ->
            appsScroll?.post { appsScroll?.smoothScrollTo((card.left - dp(24)).coerceAtLeast(0), 0) }
        }
    }

    private fun openSelectedApp() {
        val pkg = cardPackages.getOrNull(appsSelected) ?: return
        val current = foregroundPackage
        hideMenu()
        if (pkg == current) return
        val intent = packageManager.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
        runCatching { startActivity(intent) }
    }

    /** Fecha o app escolhido: Informações do app > Forçar parada, ou o método simples se a permissão não estiver ativa. */
    private fun closeSelectedApp() {
        val pkg = cardPackages.getOrNull(appsSelected) ?: return
        recentApps.remove(pkg)
        if (pkg == foregroundPackage) foregroundPackage = null
        if (prefs.getString("dirty_pkg", null) == pkg) prefs.edit().remove("dirty_pkg").apply()
        hideMenu()
        if (canForceStop()) {
            forceStop(pkg) { }
        } else {
            runCatching { (getSystemService(ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses(pkg) }
            openLibrary()
        }
    }

    /* ------------------------ Ações do menu ------------------------ */

    private fun openLibrary() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    /** Vai para a biblioteca mantendo o jogo aberto em segundo plano. */
    private fun goToLibrary() {
        hideMenu()
        foregroundPackage = null
        openLibrary()
    }

    private fun endGame() {
        val emulator = foregroundPackage
        hideMenu()
        foregroundPackage = null
        if (emulator != null && prefs.getBoolean("force_stop_accessibility", true) && canForceStop()) {
            prefs.edit().remove("dirty_pkg").apply()
            forceStop(emulator) { }
            return
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({ openLibrary() }, 250)
        if (emulator != null) {
            handler.postDelayed({
                runCatching { (getSystemService(ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses(emulator) }
            }, 1200)
        }
    }

    private fun hideMenu() {
        val view = overlay ?: return
        runCatching { windowManager?.removeView(view) }
        overlay = null
        mainViews = emptyList()
        cardViews = emptyList()
        appsStrip = null
        appsScroll = null
        appsRow = null
        hintView = null
        mode = MenuMode.MAIN
        lastAxisX = 0
        lastAxisY = 0
    }

    /* ---------------------------------------------------------------- */
    /* Parada forçada do emulador (Informações do app > Forçar parada)   */
    /* ---------------------------------------------------------------- */

    /** Só funciona se o serviço foi (re)ativado depois de ganhar a permissão de ler a tela. */
    fun canForceStop(): Boolean {
        val caps = serviceInfo?.capabilities ?: return false
        return (caps and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0
    }

    /**
     * Abre as Informações do app do emulador, toca em "Forçar parada" e confirma.
     * É o mesmo que o usuário faria com o dedo; o Android não oferece API pública para encerrar outro app em primeiro plano.
     * Ao terminar, traz o QPlay de volta e chama [done].
     */
    fun forceStop(pkg: String, done: () -> Unit) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            ServiceDiag.forceStop = "não abriu as Informações do app"
            done()
            return
        }
        var stage = 0
        var tries = 0
        val tick = object : Runnable {
            override fun run() {
                tries++
                var finished = false
                val root = rootInActiveWindow
                if (root != null) {
                    if (stage == 0) {
                        val button = findClickable(root, FORCE_LABELS)
                        if (button != null) {
                            if (!button.isEnabled) {
                                ServiceDiag.forceStop = "botão desativado (emulador já estava parado)"
                                finished = true
                            } else if (button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                stage = 1
                            }
                        }
                    } else {
                        val ok = findClickable(root, CONFIRM_LABELS)
                        if (ok != null && ok.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            ServiceDiag.forceStop = "concluída"
                            finished = true
                        }
                    }
                }
                if (!finished && tries > 24) {
                    ServiceDiag.forceStop = if (stage == 0) "não achei o botão Forçar parada" else "não achei a confirmação"
                    finished = true
                }
                if (finished) finishForceStop(done) else handler.postDelayed(this, 250)
            }
        }
        handler.postDelayed(tick, 700)
    }

    private fun finishForceStop(done: () -> Unit) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            runCatching { startActivity(intent) }
            // Com o QPlay de volta à frente, o Android permite que ele abra o próximo jogo.
            handler.postDelayed({ done() }, 700)
        }, 350)
    }

    private fun findClickable(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
            for (node in nodes) {
                val text = node.text?.toString()?.trim().orEmpty()
                if (!text.equals(label, ignoreCase = true)) continue
                var target: AccessibilityNodeInfo? = node
                var depth = 0
                while (target != null && !target.isClickable && depth < 4) {
                    target = target.parent
                    depth++
                }
                if (target != null && target.isClickable) return target
            }
        }
        return null
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        runCatching { prefs.unregisterOnSharedPreferenceChangeListener(prefsListener) }
        hideMenu()
        foregroundPackage = null
        super.onDestroy()
    }
}
