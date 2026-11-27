package com.qplay.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent

/**
 * Captura o HOME somente enquanto um emulador conhecido está em primeiro plano.
 * O evento é consumido e um menu de acessibilidade é desenhado sobre o jogo.
 */
class GameMenuAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var foregroundPackage: String? = null
    private var overlay: View? = null
    private var windowManager: WindowManager? = null
    private var selected = 0
    private val options = listOf("Voltar ao jogo", "Encerrar e voltar à biblioteca")
    private var optionViews: List<TextView> = emptyList()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        // Não perca a identificação do emulador quando o Android/Samsung emitir
        // eventos de janelas do sistema por cima do jogo.
        if (isSupportedEmulator(pkg)) foregroundPackage = pkg
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val key = event.keyCode
        // Controles Bluetooth podem mapear o botão HOME para BUTTON_MODE ou MENU,
        // em vez de KEYCODE_HOME. Capturamos as três variantes enquanto o emulador
        // conhecido continua sendo o app em primeiro plano.
        val isHome = key == KeyEvent.KEYCODE_HOME ||
            key == KeyEvent.KEYCODE_BUTTON_MODE ||
            key == KeyEvent.KEYCODE_MENU
        if (isHome && isSupportedEmulator(foregroundPackage)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) showMenu()
            return true
        }

        if (overlay != null) {
            if (event.action != KeyEvent.ACTION_DOWN) return true
            when (key) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
                    selected = (selected - 1).coerceAtLeast(0); refreshSelection(); return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    selected = (selected + 1).coerceAtMost(options.lastIndex); refreshSelection(); return true
                }
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    activateSelection(); return true
                }
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    hideMenu(); return true
                }
            }
        }
        return false
    }

    private fun isSupportedEmulator(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val p = pkg.lowercase()
        return p.contains("lemon") || p.contains("peanuts") ||
            p == "dev.nxnative.android" || p == "dev.bananative.android" ||
            p == "dev.nxnative.android.play" || p == "com.antutu.abenchmark"
    }

    private fun showMenu() {
        if (overlay != null) return
        selected = 0
        val root = FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 30, 36, 26)
            background = rounded(0xFFF5F5F5.toInt(), 24)
        }
        val title = TextView(this).apply {
            text = "MENU DO JOGO"
            setTextColor(0xFF202020.toInt())
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 24)
        }
        panel.addView(title)
        optionViews = options.mapIndexed { index, label ->
            TextView(this).apply {
                text = label
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(0xFF202020.toInt())
                setPadding(28, 20, 28, 20)
                background = rounded(if (index == 0) 0xFFBCEBFA.toInt() else 0xFFE2E2E2.toInt(), 12)
                isClickable = true
                setOnClickListener { selected = index; activateSelection() }
                val params = LinearLayout.LayoutParams(460, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.topMargin = 10
                panel.addView(this, params)
            }
        }
        val hint = TextView(this).apply {
            text = "Direcional: navegar     A: selecionar     B: continuar"
            setTextColor(0xFF666666.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(12, 22, 12, 2)
        }
        panel.addView(hint)
        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        root.addView(panel, panelParams)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        runCatching {
            windowManager?.addView(root, params)
            overlay = root
            root.requestFocus()
            refreshSelection()
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun refreshSelection() {
        optionViews.forEachIndexed { index, view ->
            view.background = rounded(
                if (index == selected) 0xFF8DDCF4.toInt() else 0xFFE2E2E2.toInt(),
                12
            )
            view.setTextColor(0xFF202020.toInt())
        }
    }

    private fun activateSelection() {
        when (selected) {
            0 -> hideMenu()
            1 -> {
                hideMenu()
                // Envia voltar ao emulador para encerrar a sessão atual e traz a biblioteca do QPlay.
                performGlobalAction(GLOBAL_ACTION_BACK)
                handler.postDelayed({
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    runCatching { startActivity(intent) }
                }, 250)
            }
        }
    }

    private fun hideMenu() {
        val view = overlay ?: return
        runCatching { windowManager?.removeView(view) }
        overlay = null
        optionViews = emptyList()
    }

    override fun onDestroy() {
        hideMenu()
        super.onDestroy()
    }
}
