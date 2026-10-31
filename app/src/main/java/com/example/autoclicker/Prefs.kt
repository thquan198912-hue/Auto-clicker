package com.example.autoclicker

import android.content.Context

/** Lưu cài đặt và vị trí các chấm trên màn hình. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("autoclicker", Context.MODE_PRIVATE)

    var swipeMode: Boolean
        get() = sp.getBoolean("swipeMode", false)
        set(v) { sp.edit().putBoolean("swipeMode", v).apply() }

    /** Khoảng nghỉ giữa hai lần thao tác (ms). */
    var intervalMs: Long
        get() = sp.getLong("intervalMs", 500L)
        set(v) { sp.edit().putLong("intervalMs", v).apply() }

    /** Số lần lặp, 0 = lặp mãi. */
    var repeatCount: Int
        get() = sp.getInt("repeatCount", 0)
        set(v) { sp.edit().putInt("repeatCount", v).apply() }

    var swipeDurationMs: Long
        get() = sp.getLong("swipeDurationMs", 300L)
        set(v) { sp.edit().putLong("swipeDurationMs", v).apply() }

    // Vị trí góc trên-trái của cửa sổ nổi, -1 = chưa đặt.
    var ax: Int
        get() = sp.getInt("ax", -1)
        set(v) { sp.edit().putInt("ax", v).apply() }
    var ay: Int
        get() = sp.getInt("ay", -1)
        set(v) { sp.edit().putInt("ay", v).apply() }
    var bx: Int
        get() = sp.getInt("bx", -1)
        set(v) { sp.edit().putInt("bx", v).apply() }
    var by: Int
        get() = sp.getInt("by", -1)
        set(v) { sp.edit().putInt("by", v).apply() }
    var px: Int
        get() = sp.getInt("px", -1)
        set(v) { sp.edit().putInt("px", v).apply() }
    var py: Int
        get() = sp.getInt("py", -1)
        set(v) { sp.edit().putInt("py", v).apply() }
}
