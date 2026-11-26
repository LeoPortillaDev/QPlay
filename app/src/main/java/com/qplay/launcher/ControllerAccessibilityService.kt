package com.qplay.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ControllerAccessibilityService : AccessibilityService() {
    private var minusDown = false
    private var plusDown = false
    private var menu: View? = null
    private var lastShown = 0L
    private val wm by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    override fun onServiceConnected() { super.onServiceConnected() }
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit
    override fun onInterrupt() = hideMenu()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        val key = event.keyCode
        // Most Android gamepads map the Switch -/+ buttons to SELECT/START.
        val isMinus = key == KeyEvent.KEYCODE_BUTTON_SELECT || key == KeyEvent.KEYCODE_MINUS
        val isPlus = key == KeyEvent.KEYCODE_BUTTON_START || key == KeyEvent.KEYCODE_PLUS
        if (isMinus) minusDown = down
        if (isPlus) plusDown = down
        if (down && event.repeatCount == 0 && minusDown && plusDown && System.currentTimeMillis() - lastShown > 900) {
            lastShown = System.currentTimeMillis()
            showMenu()
            return true
        }
        return false
    }

    private fun showMenu() {
        if (!Settings.canDrawOverlaysCompat(this)) {
            Toast.makeText(this, "Ative a permissão de sobreposição do QPlay.", Toast.LENGTH_LONG).show()
            return
        }
        hideMenu()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 22, 26, 22)
            setBackgroundColor(0xF01A202B.toInt())
        }
        val title = TextView(this).apply {
            text = "QPLAY  •  MENU DO JOGO"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 4, 8, 18)
        }
        panel.addView(title)
        addAction(panel, "FECHAR JOGO") {
            Toast.makeText(this, "O Peanuts Native ainda não autoriza apps externos a executar Stop Game.", Toast.LENGTH_LONG).show()
            hideMenu()
        }
        addAction(panel, "FECHAR QPLAY") {
            hideMenu()
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            launch?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            launch?.putExtra("qplay_close_from_overlay", true)
            if (launch != null) startActivity(launch)
        }
        addAction(panel, "CANCELAR") { hideMenu() }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try {
            wm.addView(panel, params)
            menu = panel
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível mostrar o menu flutuante.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(310, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun hideMenu() {
        menu?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        menu = null
    }

    override fun onDestroy() { hideMenu(); super.onDestroy() }
}

private object Settings {
    fun canDrawOverlaysCompat(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 23) android.provider.Settings.canDrawOverlays(context) else true
}
