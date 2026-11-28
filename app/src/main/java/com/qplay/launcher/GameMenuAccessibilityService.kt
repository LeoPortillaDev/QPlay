package com.qplay.launcher

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
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

/**
 * Captura o botão de menu somente enquanto um emulador conhecido está em primeiro plano.
 * O menu é desenhado sobre o jogo e aceita teclas e eixos de controles compatíveis.
 */
class GameMenuAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var foregroundPackage: String? = null
    private var overlay: View? = null
    private var windowManager: WindowManager? = null
    private var selected = 0
    private var lastAxisDir = 0
    private val options = listOf("Voltar ao jogo", "Encerrar e voltar à biblioteca")
    private var optionViews: List<TextView> = emptyList()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        when {
            isSupportedEmulator(pkg) -> foregroundPackage = pkg
            isSystemPackage(pkg) -> Unit
            pkg == packageName -> {
                if (event.className?.toString()?.endsWith("MainActivity") == true) {
                    foregroundPackage = null
                }
            }
            else -> foregroundPackage = null
        }
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val key = event.keyCode
        val isHome = key == KeyEvent.KEYCODE_HOME ||
            key == KeyEvent.KEYCODE_BUTTON_MODE ||
            key == KeyEvent.KEYCODE_MENU
        if (isHome && isSupportedEmulator(foregroundPackage)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) showMenu()
            return true
        }
        if (overlay != null) return handleOverlayKey(key, event.action)
        return false
    }

    private fun handleOverlayKey(key: Int, action: Int): Boolean {
        val handled = when (key) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_L1 -> {
                if (action == KeyEvent.ACTION_DOWN) move(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (action == KeyEvent.ACTION_DOWN) move(1)
                true
            }
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (action == KeyEvent.ACTION_DOWN) activateSelection()
                true
            }
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (action == KeyEvent.ACTION_DOWN) hideMenu()
                true
            }
            else -> false
        }
        return handled || overlay != null
    }

    private fun handleOverlayMotion(event: MotionEvent): Boolean {
        val isJoystick = event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
            event.isFromSource(InputDevice.SOURCE_GAMEPAD)
        if (!isJoystick || event.action != MotionEvent.ACTION_MOVE) return false

        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val value = when {
            hatY != 0f -> hatY
            hatX != 0f -> hatX
            else -> event.getAxisValue(MotionEvent.AXIS_Y)
        }
        val direction = when {
            value < -0.6f -> -1
            value > 0.6f -> 1
            else -> 0
        }
        if (direction != 0 && direction != lastAxisDir) move(direction)
        lastAxisDir = direction
        return true
    }

    private fun move(delta: Int) {
        selected = (selected + delta).coerceIn(0, options.lastIndex)
        optionViews.getOrNull(selected)?.requestFocus()
        refreshSelection()
    }

    private fun isSupportedEmulator(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        return pkg == "dev.lemon.lemon_emulator" ||
            pkg == "com.antutu.ABenchMark" ||
            pkg == "dev.nxnative.android" ||
            pkg == "dev.bananative.android" ||
            pkg == "dev.nxnative.android.play"
    }

    private fun isSystemPackage(pkg: String): Boolean =
        pkg == "android" ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.samsung.android.") ||
            pkg.startsWith("com.sec.android.") ||
            pkg.contains("inputmethod", ignoreCase = true)

    private fun showMenu() {
        if (overlay != null) return
        selected = 0
        lastAxisDir = 0
        val root = FrameLayout(this).apply {
            setBackgroundColor(0xC9000000.toInt())
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            setOnKeyListener { _, code, event -> handleOverlayKey(code, event.action) }
            setOnGenericMotionListener { _, event -> handleOverlayMotion(event) }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(14), dp(18), dp(12))
            background = rounded(0xFFF5F5F5.toInt(), dp(14))
        }
        val title = TextView(this).apply {
            text = "MENU DO JOGO"
            setTextColor(0xFF202020.toInt())
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(4), dp(6), dp(12))
        }
        panel.addView(title)
        optionViews = options.mapIndexed { index, label ->
            TextView(this).apply {
                text = label
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(0xFF202020.toInt())
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = rounded(if (index == 0) 0xFF8DDCF4.toInt() else 0xFFE2E2E2.toInt(), dp(8))
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        selected = index
                        refreshSelection()
                    }
                }
                setOnClickListener { selected = index; activateSelection() }
                val params = LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT)
                params.topMargin = dp(6)
                panel.addView(this, params)
            }
        }
        val hint = TextView(this).apply {
            text = "Direcional: navegar     A: selecionar     B: continuar"
            setTextColor(0xFF666666.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(12), dp(6), dp(2))
        }
        panel.addView(hint)
        root.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
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
            optionViews.firstOrNull()?.requestFocus()
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
                dp(8)
            )
            view.setTextColor(0xFF202020.toInt())
        }
    }

    private fun activateSelection() {
        when (selected) {
            0 -> hideMenu()
            1 -> {
                val emulator = foregroundPackage
                hideMenu()
                performGlobalAction(GLOBAL_ACTION_BACK)
                handler.postDelayed({
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    runCatching { startActivity(intent) }
                    foregroundPackage = null
                }, 250)
                if (emulator != null) {
                    handler.postDelayed({
                        runCatching {
                            (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                                .killBackgroundProcesses(emulator)
                        }
                    }, 1200)
                }
            }
        }
    }

    private fun hideMenu() {
        val view = overlay ?: return
        runCatching { windowManager?.removeView(view) }
        overlay = null
        optionViews = emptyList()
        lastAxisDir = 0
    }

    override fun onDestroy() {
        hideMenu()
        foregroundPackage = null
        super.onDestroy()
    }
}
