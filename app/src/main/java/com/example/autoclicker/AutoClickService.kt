package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Dịch vụ trợ năng: hiện thanh điều khiển nổi + chấm A/B, rồi lặp lại
 * thao tác chạm hoặc vuốt bằng dispatchGesture().
 */
class AutoClickService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AutoClickService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var wm: WindowManager

    // Thanh điều khiển
    private var panel: LinearLayout? = null
    private var btnStart: TextView? = null
    private var btnMode: TextView? = null

    // Chấm đánh dấu vị trí
    private var markerA: TextView? = null
    private var markerAParams: WindowManager.LayoutParams? = null
    private var markerB: TextView? = null
    private var markerBParams: WindowManager.LayoutParams? = null

    // Trạng thái chạy
    private var running = false
    private var doneCount = 0
    private var gSwipe = false
    private var gX1 = 0f
    private var gY1 = 0f
    private var gX2 = 0f
    private var gY2 = 0f
    private var gInterval = 500L
    private var gDuration = 300L
    private var gMax = 0

    private val fireRunnable = Runnable { fire() }

    // ---------------------------------------------------------------- vòng đời

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        instance = this
        showOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Không cần xử lý sự kiện nào.
    }

    override fun onInterrupt() {
        stopRunning()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopRunning()
        if (::wm.isInitialized) removeOverlayViews()
        instance = null
        return super.onUnbind(intent)
    }

    // ------------------------------------------------------------ giao diện nổi

    /** Hiện thanh điều khiển (hoặc cập nhật nếu đã hiện). */
    fun showOverlay() {
        if (panel != null) {
            applyModeUi()
            return
        }
        val dm = resources.displayMetrics
        val size = dp(56)

        // Chấm A
        val a = makeMarker("A", 0xFF2E7D32.toInt())
        val pa = overlayParams(size, size).apply {
            x = if (prefs.ax >= 0) prefs.ax else dm.widthPixels / 2 - size / 2
            y = if (prefs.ay >= 0) prefs.ay else dm.heightPixels / 2 - size / 2
        }
        wm.addView(a, pa)
        dragify(a, a, pa) {
            prefs.ax = pa.x.coerceAtLeast(0)
            prefs.ay = pa.y.coerceAtLeast(0)
        }
        markerA = a
        markerAParams = pa

        // Thanh điều khiển
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(0xE6212121.toInt())
            }
        }
        val handle = TextView(this).apply {
            text = "≡"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(dp(6), 0, dp(10), 0)
        }
        val start = pill(getString(R.string.bar_start), 0xFF2E7D32.toInt()).apply {
            setOnClickListener { toggleRun() }
        }
        val mode = pill("", 0xFF1565C0.toInt()).apply {
            setOnClickListener { toggleMode() }
        }
        val close = pill("✕", 0xFFC62828.toInt()).apply {
            setOnClickListener { hideOverlay() }
        }
        bar.addView(handle)
        bar.addView(start, childParams())
        bar.addView(mode, childParams())
        bar.addView(close, childParams())

        val pp = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            x = if (prefs.px >= 0) prefs.px else dp(8)
            y = if (prefs.py >= 0) prefs.py else dp(80)
        }
        wm.addView(bar, pp)
        dragify(handle, bar, pp) {
            prefs.px = pp.x.coerceAtLeast(0)
            prefs.py = pp.y.coerceAtLeast(0)
        }

        panel = bar
        btnStart = start
        btnMode = mode
        applyModeUi()
        updateUi()
    }

    /** Người dùng nhấn ✕: ẩn giao diện nổi (dịch vụ vẫn bật). */
    fun hideOverlay() {
        removeOverlayViews()
        toast(getString(R.string.toast_hidden))
    }

    private fun removeOverlayViews() {
        stopRunning()
        panel?.let { safeRemove(it) }
        panel = null
        btnStart = null
        btnMode = null
        markerA?.let { safeRemove(it) }
        markerA = null
        markerAParams = null
        detachMarkerB()
    }

    private fun applyModeUi() {
        val swipe = prefs.swipeMode
        btnMode?.text = getString(if (swipe) R.string.bar_mode_swipe else R.string.bar_mode_tap)
        markerA?.text = if (swipe) "A" else "+"
        if (swipe && markerB == null) attachMarkerB()
        if (!swipe && markerB != null) detachMarkerB()
    }

    private fun attachMarkerB() {
        val dm = resources.displayMetrics
        val size = dp(56)
        val b = makeMarker("B", 0xFFC62828.toInt())
        val pb = overlayParams(size, size).apply {
            x = if (prefs.bx >= 0) prefs.bx else dm.widthPixels / 2 - size / 2
            y = if (prefs.by >= 0) prefs.by else dm.heightPixels / 2 - size / 2 + dp(180)
        }
        wm.addView(b, pb)
        dragify(b, b, pb) {
            prefs.bx = pb.x.coerceAtLeast(0)
            prefs.by = pb.y.coerceAtLeast(0)
        }
        markerB = b
        markerBParams = pb
    }

    private fun detachMarkerB() {
        markerB?.let { safeRemove(it) }
        markerB = null
        markerBParams = null
    }

    private fun updateUi() {
        val start = btnStart ?: return
        start.text = getString(if (running) R.string.bar_stop else R.string.bar_start)
        (start.background as? GradientDrawable)
            ?.setColor(if (running) 0xFFC62828.toInt() else 0xFF2E7D32.toInt())
        btnMode?.alpha = if (running) 0.4f else 1f
    }

    /** Khi đang chạy, chấm phải "xuyên thấu" để thao tác lọt xuống ứng dụng bên dưới. */
    private fun applyTouchable(touchable: Boolean) {
        listOf(markerA to markerAParams, markerB to markerBParams).forEach { (v, p) ->
            if (v == null || p == null || !v.isAttachedToWindow) return@forEach
            p.flags = if (touchable) {
                p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            v.alpha = if (touchable) 0.85f else 0.35f
            wm.updateViewLayout(v, p)
        }
    }

    // ------------------------------------------------------------- điều khiển

    private fun toggleRun() {
        if (running) stopRunning() else startRunning()
    }

    private fun toggleMode() {
        if (running) return
        prefs.swipeMode = !prefs.swipeMode
        applyModeUi()
    }

    private fun startRunning() {
        val a = markerA ?: return
        gSwipe = prefs.swipeMode

        val ca = centerOf(a)
        gX1 = ca.first
        gY1 = ca.second
        if (gSwipe) {
            val b = markerB ?: return
            val cb = centerOf(b)
            gX2 = cb.first
            gY2 = cb.second
        }

        gInterval = prefs.intervalMs.coerceAtLeast(20L)
        gDuration = prefs.swipeDurationMs.coerceIn(50L, 10_000L)
        gMax = prefs.repeatCount
        doneCount = 0
        running = true
        applyTouchable(false)
        updateUi()
        handler.postDelayed(fireRunnable, 500L)
    }

    fun stopRunning() {
        running = false
        handler.removeCallbacks(fireRunnable)
        applyTouchable(true)
        updateUi()
    }

    private fun fire() {
        if (!running) return
        val path = Path().apply {
            moveTo(gX1, gY1)
            if (gSwipe) lineTo(gX2, gY2)
        }
        val duration = if (gSwipe) gDuration else 40L
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, duration))
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    afterGesture()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    afterGesture()
                }
            },
            null
        )
        if (!accepted) {
            stopRunning()
            toast(getString(R.string.toast_gesture_failed))
        }
    }

    private fun afterGesture() {
        if (!running) return
        doneCount++
        if (gMax > 0 && doneCount >= gMax) {
            val n = doneCount
            stopRunning()
            toast(getString(R.string.toast_done, n))
            return
        }
        handler.postDelayed(fireRunnable, gInterval)
    }

    // ---------------------------------------------------------------- tiện ích

    private fun centerOf(v: View): Pair<Float, Float> {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return Pair(loc[0] + v.width / 2f, loc[1] + v.height / 2f)
    }

    private fun overlayParams(w: Int, h: Int): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun makeMarker(label: String, color: Int): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 20f
        alpha = 0.85f
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(3), Color.WHITE)
        }
    }

    private fun pill(label: String, color: Int): TextView = TextView(this).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 15f
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(color)
        }
    }

    private fun childParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(6) }

    /** Cho phép kéo [target] bằng cách chạm và kéo [handle]. */
    private fun dragify(
        handle: View,
        target: View,
        params: WindowManager.LayoutParams,
        onMoved: () -> Unit
    ) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        handle.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - downX).toInt()
                    params.y = startY + (e.rawY - downY).toInt()
                    wm.updateViewLayout(target, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    onMoved()
                    true
                }
                else -> false
            }
        }
    }

    private fun safeRemove(v: View) {
        try {
            wm.removeView(v)
        } catch (e: Exception) {
            // View đã bị gỡ trước đó.
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
